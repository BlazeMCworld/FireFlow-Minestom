@file:Suppress("UnstableApiUsage")

package de.blazemcworld.fireflow.node

import de.blazemcworld.fireflow.Config
import de.blazemcworld.fireflow.space.Space
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.entity.attribute.Attribute
import net.minestom.server.item.Material
import net.minestom.server.network.NetworkBuffer
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.roundToInt

fun writeType(buffer: NetworkBuffer, t: ValueType<*>): Unit = buffer.run {
    t.generic?.let {
        write(NetworkBuffer.STRING, it.name)
        write(NetworkBuffer.BOOLEAN, true)
        write(NetworkBuffer.VAR_INT, t.generics.size)
        for ((k, v) in t.generics) {
            write(NetworkBuffer.STRING, k)
            writeType(buffer, v)
        }
    } ?: run {
        write(NetworkBuffer.STRING, t.name)
        write(NetworkBuffer.BOOLEAN, false)
    }
}

fun readType(buffer: NetworkBuffer): ValueType<*>? = buffer.run {
    val name = read(NetworkBuffer.STRING)
    val isGeneric = read(NetworkBuffer.BOOLEAN)
    if (!isGeneric) AllTypes.all.find { it is ValueType<*> && it.name == name } as ValueType<*>?
    else {
        val genericType = AllTypes.all.find { it is GenericType && name == it.name } as GenericType
        val genericsSize = read(NetworkBuffer.VAR_INT)
        genericType.create(LinkedHashMap<String, ValueType<*>>(genericsSize).apply { repeat(genericsSize) {
            this[read(NetworkBuffer.STRING)] = readType(buffer) ?: return@run null
        } })
    }
}

object AllTypes {
    val dataOnly = mutableListOf<SomeType>()
    val all = mutableListOf<SomeType>()

    init {
        dataOnly += PlayerType
        dataOnly += NumberType
        dataOnly += ConditionType
        dataOnly += TextType
        dataOnly += MessageType
        dataOnly += PositionType
        dataOnly += ListType
        dataOnly += DictionaryType
        dataOnly += VectorType
        all += dataOnly
        all += SignalType
    }
}

interface SomeType {
    val name: String
    val color: TextColor
    val material: Material
}

interface GenericType : SomeType {
    fun create(generics: MutableMap<String, ValueType<*>>): ValueType<*>
    val generics: Map<String, List<SomeType>>
}

abstract class ValueType<T : Any> : SomeType, NetworkBuffer.Type<T>  {
    abstract fun parse(str: String, space: Space): T?
    abstract fun compareEqual(left: Any?, right: Any?): Boolean
    abstract fun validate(something: Any?): T?
    open val generics = emptyMap<String, ValueType<*>>()
    open val generic: GenericType? = null
    open val extractions: MutableList<TypeExtraction<T, *>> = mutableListOf()
    open val insetable = false

    abstract fun stringify(v: T?): String
}

class PlayerReference(val uuid: UUID) {
    lateinit var space: Space
    constructor(p: Player, s: Space) : this(p.uuid) {
        space = s
    }

    fun resolve() = space.playInstance.getPlayerByUuid(uuid)
    fun withSpace(s: Space) = this.apply { space = s }
}

class TypeExtraction<I : Any, O : Any>(val icon: Material, val name: String, inputType: ValueType<I>, private val outputType: ValueType<O>, val extractor: (I) -> O) {
    val input: BaseNode.Input<I> = BaseNode.Input("", inputType)
    val output: BaseNode.Output<O> = BaseNode.Output(name, outputType)
    val formalName = "${inputType.name}-${name}"

    companion object {
        val list: HashMap<String, TypeExtraction<*, *>> = HashMap()

        fun get(name: String) = list[name]
    }

    init {
        list[formalName] = this
    }

    fun extract(input: I) = outputType.validate(extractor(input))
}

object PlayerType : ValueType<PlayerReference>() {
    override val name = "Player"

    override val color: TextColor = NamedTextColor.GOLD
    override val material: Material = Material.PLAYER_HEAD
    override val extractions: MutableList<TypeExtraction<PlayerReference, *>> = mutableListOf(
        TypeExtraction(Material.ANVIL, "UUID", this, TextType) { it.uuid.toString() },
        TypeExtraction(Material.NAME_TAG, "Username", this, TextType) { it.resolve()?.username ?: "Offline Player" },
        TypeExtraction(Material.GOLDEN_APPLE, "Health", this, NumberType) { it.resolve()?.health?.toDouble() ?: 0.0 },
        TypeExtraction(Material.GOLDEN_APPLE, "Max Health", this, NumberType) { it.resolve()?.getAttribute(Attribute.GENERIC_MAX_HEALTH)?.value ?: 0.0 },
        TypeExtraction(Material.COOKED_CHICKEN, "Food Level", this, NumberType) { it.resolve()?.food?.toDouble() ?: 0.0 },
        TypeExtraction(Material.COOKED_MUTTON, "Saturation", this, NumberType) { it.resolve()?.foodSaturation?.toDouble() ?: 0.0 },
        TypeExtraction(Material.BLAZE_POWDER, "Fire Ticks", this, NumberType) { it.resolve()?.fireTicks?.toDouble() ?: 0.0 },
        TypeExtraction(Material.OAK_SAPLING, "Alive Ticks", this, NumberType) { it.resolve()?.aliveTicks?.toDouble() ?: 0.0 },
        TypeExtraction(Material.SPIDER_EYE, "Eye Height", this, NumberType) { it.resolve()?.eyeHeight ?: 0.0 },
        TypeExtraction(Material.ELYTRA, "Flying Speed", this, NumberType) { it.resolve()?.flyingSpeed?.toDouble() ?: 0.0 },
        TypeExtraction(Material.LEATHER_BOOTS, "Walking Speed", this, NumberType) { it.resolve()?.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.value ?: 0.0 },
        TypeExtraction(Material.NETHERITE_SWORD, "Attack Damage", this, NumberType) { it.resolve()?.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)?.value ?: 0.0 },
        TypeExtraction(Material.DIAMOND_CHESTPLATE, "Armor", this, NumberType) { it.resolve()?.getAttribute(Attribute.GENERIC_ARMOR)?.value ?: 0.0 },
        TypeExtraction(Material.NETHERITE_CHESTPLATE, "Armor Toughness", this, NumberType) { it.resolve()?.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS)?.value ?: 0.0 },
        TypeExtraction(Material.SHIELD, "Knockback Resistance", this, NumberType) { it.resolve()?.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)?.value ?: 0.0 },
        TypeExtraction(Material.ARROW, "Stuck Arrows", this, NumberType) { it.resolve()?.arrowCount?.toDouble() ?: 0.0 },
        TypeExtraction(Material.SLIME_BALL, "Held Slot", this, NumberType) { it.resolve()?.heldSlot?.toDouble() ?: 0.0 },
        TypeExtraction(Material.ORANGE_DYE, "Latency", this, NumberType) { it.resolve()?.latency?.toDouble() ?: 0.0 },
        TypeExtraction(Material.EXPERIENCE_BOTTLE, "Experience Points", this, NumberType) { it.resolve()?.exp?.toDouble() ?: 0.0 },
        TypeExtraction(Material.EXPERIENCE_BOTTLE, "Experience Level", this, NumberType) { it.resolve()?.level?.toDouble() ?: 0.0 },
        TypeExtraction(Material.COMPASS, "Position", this, PositionType) { it.resolve()?.position ?: Pos(0.0, 0.0, 0.0, 0.0f, 0.0f) },
        TypeExtraction(Material.PRISMARINE_SHARD, "Direction", this, VectorType) { it.resolve()?.position?.direction() ?: Vec(0.0, 0.0, 0.0) },
        TypeExtraction(Material.RABBIT_FOOT, "Velocity", this, VectorType) { it.resolve()?.velocity ?: Vec(0.0, 0.0, 0.0) },
        // When item types come into play, we can add these
        TypeExtraction(Material.RED_DYE, "Is Dead", this, ConditionType) { it.resolve()?.isDead ?: false },
        TypeExtraction(Material.LIME_DYE, "Is Online", this, ConditionType) { it.resolve() != null },
        TypeExtraction(Material.NETHERITE_BOOTS, "Is Flying", this, ConditionType) { it.resolve()?.isFlying ?: false },
        TypeExtraction(Material.CHAINMAIL_LEGGINGS, "Is Sneaking", this, ConditionType) { it.resolve()?.isSneaking ?: false },
        TypeExtraction(Material.DIAMOND_BOOTS, "Is Sprinting", this, ConditionType) { it.resolve()?.isSprinting ?: false },
        TypeExtraction(Material.OAK_PRESSURE_PLATE, "Is Grounded", this, ConditionType) { it.resolve()?.isOnGround ?: false },
        TypeExtraction(Material.COOKED_BEEF, "Is Eating", this, ConditionType) { it.resolve()?.isEating ?: false },
        TypeExtraction(Material.ELYTRA, "Is Gliding", this, ConditionType) { it.resolve()?.isFlyingWithElytra ?: false },
        TypeExtraction(Material.COMMAND_BLOCK_MINECART, "Can Fly", this, ConditionType) { it.resolve()?.isAllowFlying ?: false },
    )

    override fun parse(str: String, space: Space) = kotlin.runCatching { PlayerReference(UUID.fromString(str)).withSpace(space) }.getOrNull()
    override fun compareEqual(left: Any?, right: Any?) = left is PlayerReference && right is PlayerReference && left.uuid == right.uuid
    override fun validate(something: Any?) = if (something is PlayerReference) something else null

    override fun write(buffer: NetworkBuffer, value: PlayerReference) = buffer.write(NetworkBuffer.UUID, value.uuid)
    override fun read(buffer: NetworkBuffer) = PlayerReference(buffer.read(NetworkBuffer.UUID))

    override fun stringify(v: PlayerReference?) = v?.run {
        MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(uuid)?.username ?: "Offline Player ($uuid)"
    } ?: "Unknown Player"
}

object SignalType : ValueType<Unit>() {
    override val name = "Signal"
    override val color: TextColor = NamedTextColor.AQUA
    override val material: Material = Material.LIGHT_BLUE_DYE

    override fun parse(str: String, space: Space) = null
    override fun compareEqual(left: Any?, right: Any?) = false
    override fun validate(something: Any?) = null

    override fun write(buffer: NetworkBuffer, value: Unit) {}
    override fun read(buffer: NetworkBuffer) {}

    override fun stringify(v: Unit?) = "Signal"
}

object NumberType : ValueType<Double>() {
    override val name = "Number"
    override val color: TextColor = NamedTextColor.RED
    override val material: Material = Material.SLIME_BALL
    override val insetable = true
    override val extractions: MutableList<TypeExtraction<Double, *>> = mutableListOf(
        TypeExtraction(Material.NAME_TAG, "As Text", this, TextType) { it.toString() },
        TypeExtraction(Material.GOLDEN_APPLE, "Absolute Value", this, NumberType) { abs(it) },
        TypeExtraction(Material.ANVIL, "Ceil", this, NumberType) { kotlin.math.ceil(it) },
        TypeExtraction(Material.IRON_INGOT, "Round", this, NumberType) { kotlin.math.round(it) },
        TypeExtraction(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, "Floor", this, NumberType) { kotlin.math.floor(it) },
        TypeExtraction(Material.IRON_BLOCK, "Square", this, NumberType) { it * it },
        TypeExtraction(Material.IRON_NUGGET, "Square Root", this, NumberType) { kotlin.math.sqrt(it) },
        TypeExtraction(Material.REDSTONE, "Negate", this, NumberType) { -it },
        TypeExtraction(Material.GLOWSTONE_DUST, "Increment", this, NumberType) { it + 1 },
        TypeExtraction(Material.GUNPOWDER, "Decrement", this, NumberType) { it - 1 },
        TypeExtraction(Material.OAK_LOG, "Log", this, NumberType) { kotlin.math.log(it, 10.0) },
        TypeExtraction(Material.BIRCH_LOG, "Ln", this, NumberType) { kotlin.math.ln(it) },
        TypeExtraction(Material.LIME_DYE, "Sine", this, NumberType) { kotlin.math.sin(it) },
        TypeExtraction(Material.PINK_DYE, "Cosine", this, NumberType) { kotlin.math.cos(it) },
        TypeExtraction(Material.LIGHT_BLUE_DYE, "Tangent", this, NumberType) { kotlin.math.tan(it) },
        TypeExtraction(Material.GREEN_DYE, "Arc Sine", this, NumberType) { kotlin.math.asin(it) },
        TypeExtraction(Material.PURPLE_DYE, "Arc Cosine", this, NumberType) { kotlin.math.acos(it) },
        TypeExtraction(Material.BLUE_DYE, "Arc Tangent", this, NumberType) { kotlin.math.atan(it) },
    )

    override fun parse(str: String, space: Space) = str.toDoubleOrNull()
    override fun compareEqual(left: Any?, right: Any?) = left is Double && right is Double && left == right
    override fun validate(something: Any?) = if (something is Double) something else null

    override fun write(buffer: NetworkBuffer, value: Double) = buffer.write(NetworkBuffer.DOUBLE, value)
    override fun read(buffer: NetworkBuffer) = buffer.read(NetworkBuffer.DOUBLE)

    override fun stringify(v: Double?) = (v ?: 0.0).toString()
}


object ConditionType : ValueType<Boolean>() {
    override val name = "Condition"
    override val color: TextColor = NamedTextColor.LIGHT_PURPLE
    override val material: Material = Material.ANVIL
    override val insetable = true
    override val extractions: MutableList<TypeExtraction<Boolean, *>> = mutableListOf(
        TypeExtraction(Material.NAME_TAG, "As Text", this, TextType) { it.toString() },
        TypeExtraction(Material.REDSTONE, "Not", this, ConditionType) { !it }
    )

    override fun parse(str: String, space: Space) = str == "true"
    override fun compareEqual(left: Any?, right: Any?) = left is Boolean && right is Boolean && left == right
    override fun validate(something: Any?) = if (something is Boolean) something else null

    override fun write(buffer: NetworkBuffer, value: Boolean) = buffer.write(NetworkBuffer.BOOLEAN, value)
    override fun read(buffer: NetworkBuffer) = buffer.read(NetworkBuffer.BOOLEAN)

    override fun stringify(v: Boolean?) = (v ?: false).toString()
}

object TextType : ValueType<String>() {
    override val name = "Text"
    override val color: TextColor = NamedTextColor.GREEN
    override val material: Material = Material.BOOK
    override val insetable = true
    override val extractions: MutableList<TypeExtraction<String, *>> = mutableListOf(
        TypeExtraction(Material.NAME_TAG, "As Message", this, MessageType) { Component.text(it) },
        TypeExtraction(Material.LEAD, "Length", this, NumberType) { it.length.toDouble() },
        TypeExtraction(Material.INK_SAC, "Format MiniMessage", this, MessageType) { Component.text(mm.serialize(Component.text(it))) },
        TypeExtraction(Material.PAPER, "Remove Padding Spaces", this, TextType) { it.trim() },
        TypeExtraction(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, "To Lower Case", this, TextType) { it.lowercase(Locale.getDefault()) },
        TypeExtraction(Material.GOLD_BLOCK, "To Upper Case", this, TextType) { it.uppercase(Locale.getDefault()) },
    )

    override fun parse(str: String, space: Space) = str
    override fun compareEqual(left: Any?, right: Any?) = left is String && right is String && left == right
    override fun validate(something: Any?) = if (something is String) something else null

    override fun write(buffer: NetworkBuffer, value: String) = buffer.write(NetworkBuffer.STRING, value)
    override fun read(buffer: NetworkBuffer) = buffer.read(NetworkBuffer.STRING)

    override fun stringify(v: String?) = v ?: ""
}

private val mm = MiniMessage.builder()
    .tags(TagResolver.builder().resolvers(
        StandardTags.color(),
        StandardTags.decorations(),
        StandardTags.font(),
        StandardTags.gradient(),
        StandardTags.keybind(),
        StandardTags.newline(),
        StandardTags.rainbow(),
        StandardTags.reset(),
        StandardTags.transition(),
        StandardTags.translatable(),
    ).build()).build()

object MessageType : ValueType<Component>() {
    override val name = "Message"
    override val color: TextColor = NamedTextColor.YELLOW
    override val material: Material = Material.ENCHANTED_BOOK
    override val insetable = true
    override val extractions: MutableList<TypeExtraction<Component, *>> = mutableListOf(
        TypeExtraction(Material.NAME_TAG, "As Text", this, TextType) { it.toString() },
        TypeExtraction(Material.LEAD, "Length", this, NumberType) { it.toString().length.toDouble() },
        TypeExtraction(Material.INK_SAC, "Format MiniMessage", this, MessageType) { mm.deserialize(it.toString()) },
        TypeExtraction(Material.WHITE_DYE, "Strip Formatting", this, MessageType) { Component.text(mm.stripTags(it.toString())) },
        TypeExtraction(Material.HEAVY_WEIGHTED_PRESSURE_PLATE, "To Lower Case", this, MessageType) { Component.text(it.toString().lowercase(Locale.getDefault())) },
        TypeExtraction(Material.GOLD_BLOCK, "To Upper Case", this, MessageType) { Component.text(it.toString().uppercase(Locale.getDefault())) }
    )

    override fun parse(str: String, space: Space) = mm.deserialize(str)
    override fun compareEqual(left: Any?, right: Any?) = left is Component && right is Component && mm.serialize(left) == mm.serialize(right)
    override fun validate(something: Any?) = if (something is Component) something else null

    override fun write(buffer: NetworkBuffer, value: Component) = buffer.write(NetworkBuffer.STRING, mm.serialize(value))
    override fun read(buffer: NetworkBuffer) = mm.deserialize(buffer.read(NetworkBuffer.STRING))

    override fun stringify(v: Component?) = v?.let { mm.serialize(it) } ?: ""
}

object PositionType : ValueType<Pos>() {
    override val name: String = "Position"
    override val color: TextColor = NamedTextColor.DARK_AQUA
    override val material: Material = Material.FILLED_MAP
    override val extractions: MutableList<TypeExtraction<Pos, *>> = mutableListOf(
        TypeExtraction(Material.NAME_TAG, "As Text", this, TextType) { "(${shorten(it.x)}, ${shorten(it.y)}, ${shorten(it.z)}, ${shorten(it.pitch)}, ${shorten(it.yaw)})" },
        TypeExtraction(Material.COMPASS, "Positional Vector", this, VectorType) { Vec(it.x, it.y, it.z) },
        TypeExtraction(Material.PRISMARINE_SHARD, "Directional Vector", this, VectorType) { Vec(it.pitch.toDouble(), it.yaw.toDouble(), 0.0) },
        TypeExtraction(Material.CONDUIT, "Corner Position", this, PositionType) { Pos(it.x.toInt().toDouble(), it.y.toInt().toDouble(), it.z.toInt().toDouble(), it.pitch, it.yaw) },
        TypeExtraction(Material.HEAVY_CORE, "Center Position", this, PositionType) { Pos(it.x.toInt().toDouble() + 0.5, it.y.toInt().toDouble() + 0.5, it.z.toInt().toDouble() + 0.5, it.pitch, it.yaw) },
        TypeExtraction(Material.ARROW, "Reset Direction", this, PositionType) { Pos(it.x, it.y, it.z, 0.0f, 0.0f) },
        TypeExtraction(Material.RED_DYE, "X", this, NumberType) { it.x },
        TypeExtraction(Material.LIME_DYE, "Y", this, NumberType) { it.y },
        TypeExtraction(Material.CYAN_DYE, "Z", this, NumberType) { it.z },
        TypeExtraction(Material.MAGENTA_DYE, "Yaw", this, NumberType) { it.yaw.toDouble() },
        TypeExtraction(Material.PURPLE_DYE, "Pitch", this, NumberType) { it.pitch.toDouble() },
    )

    override fun parse(str: String, space: Space) = null
    override fun compareEqual(left: Any?, right: Any?) = left is Pos && right is Pos && left == right
    override fun validate(something: Any?) = if (something is Pos) something else null

    override fun write(buffer: NetworkBuffer, value: Pos) = buffer.run { value.run {
        write(NetworkBuffer.DOUBLE, x)
        write(NetworkBuffer.DOUBLE, y)
        write(NetworkBuffer.DOUBLE, z)
        write(NetworkBuffer.FLOAT, pitch)
        write(NetworkBuffer.FLOAT, yaw)
    } }

    override fun read(buffer: NetworkBuffer) = buffer.run { Pos(
        read(NetworkBuffer.DOUBLE), read(NetworkBuffer.DOUBLE), read(NetworkBuffer.DOUBLE),
        read(NetworkBuffer.FLOAT), read(NetworkBuffer.FLOAT),
    ) }

    override fun stringify(v: Pos?) = (v ?: Pos.ZERO).run { arrayOf(
        shorten(x), shorten(y), shorten(z), shorten(pitch), shorten(yaw)
    ) }.joinToString(prefix="Pos(", postfix=")")

    private fun shorten(pos: Number) = (pos.toDouble() * 1000.0).roundToInt() / 1000.0
}

object ListType : GenericType {
    private val cache = WeakHashMap<ValueType<*>, Impl<*>>()
    override fun create(generics: MutableMap<String, ValueType<*>>): Impl<*> = create(generics["Type"]!!)
    fun <T : Any> create(type: ValueType<T>): Impl<T> = cache.computeIfAbsent(type) { Impl(type) } as Impl<T>

    override val generics = mapOf("Type" to AllTypes.dataOnly)
    override val name: String = "List"
    override val material: Material = Material.STRING
    override val color: TextColor = NamedTextColor.WHITE

    class Impl<T : Any>(val type: ValueType<T>) : ValueType<ListReference<T>>() {
        override val name: String = "List(${type.name})"
        override val color: TextColor = type.color
        override val material: Material = type.material

        override fun parse(str: String, space: Space) = null
        override fun compareEqual(left: Any?, right: Any?) = left is ListReference<*> && right is ListReference<*> && left.store == right.store
        override fun validate(something: Any?) = if (something is ListReference<*> && something.type == type) something as ListReference<T> else null

        override val generics = mapOf("Type" to type)
        override val generic = ListType

        override fun write(buffer: NetworkBuffer, value: ListReference<T>) = buffer.writeCollection(type, value.store)
        override fun read(buffer: NetworkBuffer) = ListReference(type, buffer.readCollection(type, Config.store.limits.maxListSize))

        override fun stringify(v: ListReference<T>?) = "List(${v?.store?.size ?: 0} Entries)"
    }
}
class ListReference<T : Any>(val type: ValueType<T>, val store: MutableList<T>)

data class DictionaryReference<K : Any, V : Any>(val key: ValueType<K>, val value: ValueType<V>, val store: MutableMap<K, V>)
object DictionaryType : GenericType {
    private val cache = WeakHashMap<ValueType<*>, WeakHashMap<ValueType<*>, Impl<*, *>>>()
    override fun create(generics: MutableMap<String, ValueType<*>>): Impl<*, *> = create(generics["Key"]!!, generics["Value"]!!)
    fun <K : Any, V : Any> create(key: ValueType<K>, value: ValueType<V>): Impl<K, V> = cache.computeIfAbsent(key) { WeakHashMap() }.computeIfAbsent(value) { Impl(key, value) } as Impl<K, V>

    override val generics = mapOf("Key" to AllTypes.dataOnly, "Value" to AllTypes.dataOnly)
    override val name: String = "Dictionary"
    override val material: Material = Material.COBWEB
    override val color: TextColor = NamedTextColor.WHITE

    class Impl<K : Any, V : Any>(private val k: ValueType<K>, private val v: ValueType<V>) : ValueType<DictionaryReference<K, V>>() {
        override val name: String = "Dictionary(${k.name}, ${v.name})"
        override val color: TextColor = k.color
        override val material: Material = k.material

        override fun parse(str: String, space: Space) = null
        override fun compareEqual(left: Any?, right: Any?) = left is DictionaryReference<*, *> && right is DictionaryReference<*, *> && left.store == right.store
        override fun validate(something: Any?) = if (something is DictionaryReference<*, *> && something.key == k && something.value == v) something as DictionaryReference<K, V> else null

        override val generics = mapOf("Key" to k, "Value" to v)
        override val generic = DictionaryType

        override fun write(buffer: NetworkBuffer, value: DictionaryReference<K, V>): Unit = buffer.run { writeMap(k, v, value.store) }
        override fun read(buffer: NetworkBuffer) = DictionaryReference(k, v, buffer.readMap(k, v, Config.store.limits.maxMapSize))

        override fun stringify(v: DictionaryReference<K, V>?) = "Dictionary(${v?.store?.size ?: 0} Entries)"
    }
}

object VectorType : ValueType<Vec>() {
    override val name: String = "Vector"
    override val color: TextColor = NamedTextColor.AQUA
    override val material: Material = Material.PRISMARINE_SHARD
    override val extractions: MutableList<TypeExtraction<Vec, *>> = mutableListOf(
        TypeExtraction(Material.NAME_TAG, "As Text", this, TextType) { "<${shorten(it.x)}, ${shorten(it.y)}, ${shorten(it.z)}>" },
        TypeExtraction(Material.COMPASS, "As Position", this, PositionType) { Pos(it.x, it.y, it.z, 0.0f, 0.0f) },
        TypeExtraction(Material.STONE, "Normalize", this, VectorType) { it.normalize() },
        TypeExtraction(Material.REDSTONE, "Invert", this, VectorType) { it.mul(-1.0) },
        TypeExtraction(Material.RED_DYE, "X", this, NumberType) { it.x },
        TypeExtraction(Material.LIME_DYE, "Y", this, NumberType) { it.y },
        TypeExtraction(Material.CYAN_DYE, "Z", this, NumberType) { it.z },
        TypeExtraction(Material.SPECTRAL_ARROW, "Magnitude", this, NumberType) { it.length() }
    )

    override fun parse(str: String, space: Space) = null
    override fun compareEqual(left: Any?, right: Any?) = left is Vec && right is Vec && left == right
    override fun validate(something: Any?) = if (something is Vec) something else null

    override fun write(buffer: NetworkBuffer, value: Vec) = buffer.write(NetworkBuffer.VECTOR3D, value)
    override fun read(buffer: NetworkBuffer) = Vec.fromPoint(buffer.read(NetworkBuffer.VECTOR3D))

    override fun stringify(v: Vec?) = v?.run { "Vector(${shorten(v.x)}, ${shorten(v.y)}, ${shorten(v.z)})" } ?: "Vector(0, 0, 0)"

    private fun shorten(num: Number) = (num.toDouble() * 1000.0).roundToInt() / 1000.0
}
