@file:Suppress("UnstableApiUsage")

package de.blazemcworld.fireflow.space

import com.github.luben.zstd.Zstd
import de.blazemcworld.fireflow.FireFlow
import de.blazemcworld.fireflow.Lobby
import de.blazemcworld.fireflow.database.DatabaseHelper
import de.blazemcworld.fireflow.database.table.PlayersTable
import de.blazemcworld.fireflow.database.table.SpaceRolesTable
import de.blazemcworld.fireflow.database.table.SpaceRolesTable.role
import de.blazemcworld.fireflow.database.table.SpaceRolesTable.space
import de.blazemcworld.fireflow.gui.*
import de.blazemcworld.fireflow.inventory.ToolsInventory
import de.blazemcworld.fireflow.node.*
import de.blazemcworld.fireflow.node.impl.NodeList
import de.blazemcworld.fireflow.preferences.MousePreference
import de.blazemcworld.fireflow.tool.*
import de.blazemcworld.fireflow.util.PlayerExitInstanceEvent
import de.blazemcworld.fireflow.util.reset
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.InteractionMeta
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.inventory.PlayerInventoryItemChangeEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.anvil.AnvilLoader
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.NetworkBuffer
import net.minestom.server.timer.TaskSchedule
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.LinkedHashMap
import kotlin.math.abs

private typealias FunctionIO = Pair<FunctionInputsNode, FunctionOutputsNode>
private typealias Value<T> = Pair<ValueType<T>, Any>

private const val SPACE_MAGIC = 0x464c4f57

private val TOOLS_ITEM = ItemStack.builder(Material.CHEST)
    .customName(Component.text("Tools").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
    .lore(
        Component.text("Get tools for").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
        Component.text("editing your code").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
    ).build()

class Space(val id: Int) {
    val codeInstance: InstanceContainer
    val playInstance: InstanceContainer
    val codeNodes = mutableListOf<NodeComponent>()
    val functions = mutableListOf<FunctionIO>()
    val functionNodes = mutableSetOf<FunctionCallNode>()
    val varStore = mutableMapOf<String, Value<*>>()
    private var isUnused = false
    private var globalNodeContext: GlobalNodeContext
    private val spaceDir = File("spaces").resolve(id.toString())
    private val codeFile = spaceDir.resolve("code.flow")
    private val varsFile = spaceDir.resolve("vars.flow")

    init {
        FireFlow.LOGGER.info { "Loading Space #$id" }
        val manager = MinecraftServer.getInstanceManager()
        val playerHighlighters = WeakHashMap<Player, Tool.IOHighlighter>()
        codeInstance = manager.createInstanceContainer()
        playInstance = manager.createInstanceContainer()
        codeInstance.timeRate = 0
        playInstance.timeRate = 0

        codeInstance.setChunkSupplier(::LightingChunk)
        playInstance.setChunkSupplier(::LightingChunk)

        playInstance.chunkLoader = AnvilLoader("spaces/$id")

        codeInstance.setGenerator {
            if (it.absoluteStart().z() != 16.0) return@setGenerator
            it.modifier().fill(
                BlockVec(0, 0, 0).add(it.absoluteStart()),
                BlockVec(16, 128, 1).add(it.absoluteStart()),
                Block.POLISHED_BLACKSTONE
            )
        }
        playInstance.setGenerator gen@{
            if (abs(it.absoluteStart().x() + 8) > 16) return@gen
            if (abs(it.absoluteStart().z() + 8) > 16) return@gen
            it.modifier().fillHeight(-1, 0, Block.SMOOTH_STONE)
        }

        if (!spaceDir.exists()) spaceDir.mkdirs()
        codeFile.createNewFile()
        varsFile.createNewFile()
        readCodeFromDisk()
        readVariablesFromDisk()

        val scheduler = MinecraftServer.getSchedulerManager()
        scheduler.submitTask {
            save()

            if (codeInstance.players.size == 0 && playInstance.players.size == 0) {
                if (isUnused) {
                    FireFlow.LOGGER.info { "Unloading Space #$id" }
                    SpaceManager.forget(id)
                    manager.unregisterInstance(playInstance)
                    manager.unregisterInstance(codeInstance)
                    return@submitTask TaskSchedule.stop()
                }
                isUnused = true
            }
            return@submitTask TaskSchedule.minutes(1)
        }

        val playEvents = playInstance.eventNode()
        val codeEvents = codeInstance.eventNode()

        fun changeColor(player: Player, component: IOComponent?) {
            when (component) {
                is IOComponent.Output -> playerHighlighters[player]?.setColor(NamedTextColor.AQUA)
                is IOComponent.InsetInput<*> -> playerHighlighters[player]?.setColor(NamedTextColor.DARK_RED)
                else -> playerHighlighters[player]?.setColor(NamedTextColor.GRAY)
            }
        }

        playEvents.addListener(PlayerSpawnEvent::class.java) {
            isUnused = false
            it.player.reset()
            it.player.gameMode = GameMode.SURVIVAL
        }

        codeEvents.addListener(PlayerSpawnEvent::class.java) { it ->
            isUnused = false
            it.player.reset()
            it.player.isAllowFlying = true
            it.player.isFlying = true

            if (MousePreference.playerPreference[it.player] == 1.toByte()) {
                playerHighlighters[it.player] = Tool.IOHighlighter(NamedTextColor.GRAY, it.player, this, ::changeColor) { it is IOComponent.Output || it is IOComponent.InsetInput<*> }.also { it.selected() }
                val entity = Entity(EntityType.INTERACTION)
                entity.setNoGravity(true)
                val meta = entity.entityMeta as InteractionMeta
                meta.setNotifyAboutChanges(false)
                meta.width = -0.5F
                meta.height = -0.5F
                meta.setNotifyAboutChanges(true)
                entity.setInstance(this.codeInstance, Pos(0.0, 0.0, 0.0))
                it.player.addPassenger(entity)
            } else {
                it.player.inventory.setItemStack(8, TOOLS_ITEM)
                val preference = DatabaseHelper.getPreference(it.player, "auto-tools")
                if (preference.toInt() == 0) {
                    var index = 0
                    for (tool in Tool.allTools) {
                        if (index == 8) index++
                        it.player.inventory.setItemStack(index, tool.item)
                        index++
                    }
                }
            }
        }

        codeEvents.addListener(PlayerBlockBreakEvent::class.java) {
            it.isCancelled = true
        }
        codeEvents.addListener(PlayerBlockPlaceEvent::class.java) {
            it.isCancelled = true
        }

        val playerTools = WeakHashMap<Player, Tool.Handler>()

        codeEvents.addListener(ItemDropEvent::class.java) {
            it.isCancelled = true
        }

        fun updateTool(player: Player, quit: Boolean = false) {
            if (!quit) for (tool in Tool.allTools) {
                if (tool.item == player.itemInMainHand) {
                    if (playerTools[player]?.tool == tool) return
                    playerTools[player]?.deselect()
                    playerTools[player] = tool.handler(player, this).also { it.select() }
                    return
                }
            }
            playerTools[player]?.deselect()
            playerTools[player] = null
        }

        codeEvents.addListener(EntityAttackEvent::class.java) { event ->
            if (event.entity !is Player) return@addListener
            val player = event.entity as Player
            if (MousePreference.playerPreference[player] == 0.toByte()) return@addListener

            if (playerTools[player] == null) DeleteTool.handler(player, this).use()
            else {
                playerTools[player]?.deselect()
                playerTools[player] = null
                if (playerHighlighters[player] == null) playerHighlighters[player] = Tool.IOHighlighter(NamedTextColor.GRAY, player, this, ::changeColor) { it is IOComponent.Output || it is IOComponent.InsetInput<*> }.also { it.selected() }
            }
        }

        codeEvents.addListener(PlayerEntityInteractEvent::class.java) { event ->
            if (event.hand == Player.Hand.OFF || MousePreference.playerPreference[event.player] == 0.toByte()) return@addListener

            if (playerTools[event.player] == null) {
                val highlighter = playerHighlighters[event.player]
                if (highlighter != null && highlighter.hasSelection()) {
                    if (highlighter.getSelected() is IOComponent.Output) {
                        playerTools[event.player] = ConnectNodesTool.handler(event.player, this).also { it.select() }
                        playerTools[event.player]?.use()
                        playerHighlighters[event.player]?.deselect()
                        playerHighlighters[event.player] = null
                    } else {
                        playerTools[event.player] = InsetLiteralTool.handler(event.player, this).also { it.select() }
                        playerTools[event.player]?.use()
                        playerTools[event.player]?.deselect()
                        playerTools[event.player] = null
                    }
                } else {
                    playerTools[event.player] = MoveTool.handler(event.player, this).also { it.select() }
                    val tool = playerTools[event.player] ?: return@addListener
                    tool.use()
                    playerHighlighters[event.player]?.deselect()
                    playerHighlighters[event.player] = null
                }
            } else {
                val tool = playerTools[event.player] ?: return@addListener
                tool.use()
                if (tool.hasSelection()) return@addListener
                playerTools[event.player]?.deselect()
                playerTools[event.player] = null
                if (playerHighlighters[event.player] == null) playerHighlighters[event.player] = Tool.IOHighlighter(NamedTextColor.GRAY, event.player, this, ::changeColor) { it is IOComponent.Output || it is IOComponent.InsetInput<*> }.also { it.selected() }
            }
        }

        codeEvents.addListener(PlayerUseItemEvent::class.java) click@{
            if (it.hand == Player.Hand.OFF) return@click

            if (it.player.itemInMainHand == TOOLS_ITEM) {
                ToolsInventory.open(it.player)
                return@click
            }

            updateTool(it.player)
            playerTools[it.player]?.use()
        }

        codeEvents.addListener(PlayerChatEvent::class.java) { event ->
            if (MousePreference.playerPreference[event.player] == 1.toByte()) {
                val highlighter = playerHighlighters[event.player]
                if (highlighter != null && highlighter.hasSelection()) {
                    if (highlighter.getSelected() is IOComponent.InsetInput<*>) {
                        InsetLiteralTool.handler(event.player, this).chat(event.message)
                        event.isCancelled = true
                    }
                }
            } else event.isCancelled = playerTools[event.player]?.chat(event.message) ?: false
        }

        fun swapCallback(player: Player, remove: Boolean) {
            if (remove) {
                playerTools[player]?.deselect()
                playerTools[player] = null
                playerHighlighters[player] = Tool.IOHighlighter(NamedTextColor.GRAY, player, this, ::changeColor) { it is IOComponent.Output || it is IOComponent.InsetInput<*> }.also { it.selected() }
            }
        }

        codeEvents.addListener(PlayerSwapItemEvent::class.java) { event ->
            if (MousePreference.playerPreference[event.player] == 0.toByte()) {
                scheduler.execute { updateTool(event.player) }
                event.isCancelled = playerTools[event.player]?.swap() ?: false
            } else {
                if (playerTools[event.player] != null) {
                    val tool = playerTools[event.player] ?: return@addListener
                    if (tool.tool !is ConnectNodesTool) return@addListener
                    tool.swap(::swapCallback)
                    return@addListener
                }

                val connectTool = ConnectNodesTool.handler(event.player, this).also { it.select() }.also { it.use() }
                if (connectTool.hasSelection()) {
                    playerTools[event.player] = connectTool
                    val highlighter = playerHighlighters[event.player] ?: return@addListener
                    highlighter.deselect()
                    playerHighlighters[event.player] = null
                } else {
                    connectTool.deselect()
                    playerTools[event.player] = null
                    CreateNodeTool.handler(event.player, this).use()
                }
            }
        }
        codeEvents.addListener(PlayerInventoryItemChangeEvent::class.java) {
            if (MousePreference.playerPreference[it.player] == 0.toByte()) scheduler.execute { updateTool(it.player) }
        }
        codeEvents.addListener(PlayerChangeHeldSlotEvent::class.java) {
            if (MousePreference.playerPreference[it.player] == 0.toByte()) scheduler.execute { updateTool(it.player) }
        }
        codeEvents.addListener(PlayerExitInstanceEvent::class.java) { event ->
            playerHighlighters[event.player]?.deselect()
            playerHighlighters[event.player] = null
            playerTools[event.player]?.deselect()
            playerTools[event.player] = null
            event.player.passengers.forEach { it.remove() }
            updateTool(event.player, quit=true)
        }

        globalNodeContext = GlobalNodeContext(this)
    }

    fun codeCursor(player: Player): Pos2d {
        val norm = player.position.direction().dot(Vec(0.0, 0.0, -1.0))
        if (norm >= 0) return Pos2d.ZERO
        val start = Pos(player.position).asVec().add(0.0, player.eyeHeight, -16.0)
        val dist = -start.dot(Vec(0.0, 0.0, -1.0)) / norm
        if (dist < 0) return Pos2d.ZERO
        val out = start.add(player.position.direction().mul(dist))
        return Pos2d(out.x, out.y)
    }

    fun reload() {
        val players = playInstance.players.toSet()
        for (p in playInstance.players) {
            p.sendMessage(Component.text("Space is reloading!").color(NamedTextColor.RED))
            Lobby.playerJoin(p)
        }
        globalNodeContext.onDestroy.forEach { it() }
        globalNodeContext = GlobalNodeContext(this)
        val spaceID = this.id
        val data = transaction {
            val reloadPref = PlayersTable.preferences["reload"] ?: return@transaction emptyMap<UUID, Map<String, Any>>()
            val result = SpaceRolesTable.join(PlayersTable, JoinType.INNER, SpaceRolesTable.player, PlayersTable.id)
                .selectAll().where((space eq spaceID) and (PlayersTable.uuid inList players.map { it.uuid }))
                .adjustSelect { select(PlayersTable.uuid, role, reloadPref) }
            result.associate { it[PlayersTable.uuid] to mapOf( "role" to it[role], "reload" to it[reloadPref]) }
        }
        for (p in players) {
            val playerData = data[p.uuid] ?: continue
            val reloadType = playerData["reload"].toString().toInt()
            if (reloadType == 2) SpaceManager.sendToSpace(p, spaceID)
            if (reloadType == 1 && (playerData["role"] == SpaceRolesTable.Role.OWNER || playerData["role"] == SpaceRolesTable.Role.CONTRIBUTOR)) SpaceManager.sendToSpace(p, spaceID)
            if (reloadType == 0 && playerData["role"] == SpaceRolesTable.Role.OWNER) SpaceManager.sendToSpace(p, spaceID)
        }
    }

    private fun writeInsetValue(buffer: NetworkBuffer, i: IOComponent.InsetInput<*>) = buffer.run {
        (i as IOComponent.InsetInput<Any>).insetVal?.let {
            write(NetworkBuffer.BOOLEAN, true)
            write(i.type, it)
        } ?: write(NetworkBuffer.BOOLEAN, false)
    }

    private fun readInsetValue(buffer: NetworkBuffer, i: IOComponent.InsetInput<*>) = buffer.run {
        if (read(NetworkBuffer.BOOLEAN)) (i as IOComponent.InsetInput<Any>).insetVal = read(i.type)
    }

    private fun writePos2d(buffer: NetworkBuffer, p: Pos2d) = buffer.run {
        write(NetworkBuffer.FLOAT, p.x.toFloat())
        write(NetworkBuffer.FLOAT, p.y.toFloat())
    }

    private fun readPos2d(buffer: NetworkBuffer) = buffer.run { Pos2d(read(NetworkBuffer.FLOAT).toDouble(), read(NetworkBuffer.FLOAT).toDouble()) }

    private fun writeCodeToDisk() {
        if (!spaceDir.exists()) spaceDir.mkdirs()
        codeFile.writeBytes(NetworkBuffer().apply {
            write(NetworkBuffer.VAR_INT, functions.size) // functionsSize
            write(NetworkBuffer.VAR_INT, codeNodes.size) // nodesSize

            for ((input, output) in functions) { // functions
                write(NetworkBuffer.STRING, input.fn) // id

                fun writeFuncIO(n: BaseNode.IO<*>) {
                    write(NetworkBuffer.STRING, n.name)
                    writeType(this, n.type)
                }
                write(NetworkBuffer.VAR_INT, input.outputs.size) // inSize
                for (i in input.outputs) writeFuncIO(i)
                write(NetworkBuffer.VAR_INT, output.inputs.size) // outSize
                for (o in output.inputs) writeFuncIO(o)

                write(NetworkBuffer.VAR_INT, codeNodes.indexOf(input.component)) // inId
                write(NetworkBuffer.VAR_INT, codeNodes.indexOf(output.component)) // outId
            }

            for (n in codeNodes) { // nodes
                val type = n.node.generic?.let { NodeComponentType.GENERIC } ?: n.type
                write(NetworkBuffer.VAR_INT, type.ordinal)
                write(NetworkBuffer.BOOLEAN, n.node is FunctionCallNode) // fn
                type.write(this, n)
                writePos2d(this, n.pos)

                if (n.valueLiteral != "unset") writeOptional(NetworkBuffer.STRING, n.valueLiteral)
                else write(NetworkBuffer.BOOLEAN, false)
            }

            for (n in codeNodes) {
                write(NetworkBuffer.VAR_INT, n.inputs.size)
                for (i in n.inputs) {
                    if (i is IOComponent.InsetInput<*>) writeInsetValue(this, i)
                    write(NetworkBuffer.VAR_INT, i.connections.size)
                    for (c in i.connections) {
                        write(NetworkBuffer.VAR_INT, c.relays.size)
                        write(NetworkBuffer.VAR_INT, codeNodes.indexOf(c.output.node))
                        write(NetworkBuffer.VAR_INT, c.output.node.outputs.indexOf(c.output))
                        for (r in c.relays) writePos2d(this, r)
                    }
                }
            }
        }.let { NetworkBuffer(it.writeIndex()).run {
            write(NetworkBuffer.INT, SPACE_MAGIC)
	    val uncompressedSize = it.writeIndex()
            val compressed = Zstd.compress(it.readBytes(uncompressedSize), 9)
	    write(NetworkBuffer.VAR_INT, compressed.size)
            write(NetworkBuffer.VAR_INT, uncompressedSize)
            write(NetworkBuffer.RAW_BYTES, compressed)
            readBytes(writeIndex())
        } })
    }

    private fun readCodeFromDisk() {
        NetworkBuffer(ByteBuffer.wrap(codeFile.readBytes().apply { if (size < 6) return })).run {
            if (read(NetworkBuffer.INT) != SPACE_MAGIC) return
            val compressedSize = read(NetworkBuffer.VAR_INT)
            val uncompressedSize = read(NetworkBuffer.VAR_INT)
            NetworkBuffer(ByteBuffer.wrap(Zstd.decompress(readBytes(compressedSize), uncompressedSize)))
        }.run buffer@{
            codeNodes.clear()
            functions.clear()
            functionNodes.clear()

	    val functionsSize = read(NetworkBuffer.VAR_INT)
            val nodesSize = read(NetworkBuffer.VAR_INT)
            codeNodes += buildList<NodeComponent?>(nodesSize) {
                repeat(functionsSize) fn@{
                    val id = read(NetworkBuffer.STRING)
                    val inputs = FunctionInputsNode(id)
                    val outputs = FunctionOutputsNode(id)
                    repeat(read(NetworkBuffer.VAR_INT)) {
                        inputs.add(read(NetworkBuffer.STRING), readType(this@buffer) ?: return@fn)
                    }
                    repeat(read(NetworkBuffer.VAR_INT)) {
                        outputs.add(read(NetworkBuffer.STRING), readType(this@buffer) ?: return@fn)
                    }
                    functions.add(inputs to outputs)
                    this[read(NetworkBuffer.VAR_INT)] = inputs.component
                    this[read(NetworkBuffer.VAR_INT)] = outputs.component
                    functionNodes.add(FunctionCallNode(inputs, outputs))
                }
                repeat(nodesSize) {
                    val type = NodeComponentType.entries[read(NetworkBuffer.VAR_INT)]
                    this += run comp@{
                        type.read(this@buffer, if (read(NetworkBuffer.BOOLEAN)) functionNodes else NodeList.all)
                    }?.apply {
                        pos = readPos2d(this@buffer)
                        if (read(NetworkBuffer.BOOLEAN)) valueLiteral = read(NetworkBuffer.STRING)
                    }
                }
                repeat(nodesSize) n@{ n ->
                    val node = this[n] ?: return@n
                    repeat(read(NetworkBuffer.VAR_INT)) { id ->
                        node.inputs[id].let {
                            if (it is IOComponent.InsetInput<*>) readInsetValue(this@buffer, it)
                            repeat(read(NetworkBuffer.VAR_INT)) c@{ _ ->
                                val relaysSize = read(NetworkBuffer.VAR_INT)
                                (this[read(NetworkBuffer.VAR_INT)]
                                    ?: return@c).outputs[read(NetworkBuffer.VAR_INT)].connect(it, run {
                                    buildList(relaysSize) { repeat(relaysSize) { add(readPos2d(this@buffer)) } }
                                })
                            }
                        }
                    }
                }
                repeat(nodesSize) { (this[it] ?: return@repeat).update(codeInstance) }
            }.filterNotNull()
        }
    }

    fun save() {
        FireFlow.LOGGER.info { "Saving Space #$id" }
        playInstance.saveChunksToStorage()
        writeCodeToDisk()
        writeVariablesToDisk()
    }

    private fun writeVariablesToDisk() {
        if (!spaceDir.exists()) spaceDir.mkdirs()
        varsFile.writeBytes(NetworkBuffer().apply {
            write(NetworkBuffer.INT, SPACE_MAGIC)
            write(NetworkBuffer.VAR_INT, varStore.size)
            for ((k, v) in varStore) {
                (v.first as ValueType<Any>).let {
                    writeType(this, it)
                    write(NetworkBuffer.STRING, k)
                    write(it, v.second)
                }
            }
        }.run { readBytes(writeIndex()) })
    }

    private fun readVariablesFromDisk() = NetworkBuffer(ByteBuffer.wrap(varsFile.readBytes().apply { if (size < 5) return })).run buffer@{
        if (read(NetworkBuffer.INT) != SPACE_MAGIC) return
        varStore.clear()
        varStore += buildMap { repeat(read(NetworkBuffer.VAR_INT)) {
            (readType(this@buffer) ?: return@repeat).let { this[read(NetworkBuffer.STRING)] = it to read(it).apply {
                if (this is PlayerReference) withSpace(this@Space)
            } }
        } }
    }
}
