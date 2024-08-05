@file:Suppress("UnstableApiUsage")

package de.blazemcworld.fireflow

import de.blazemcworld.fireflow.inventory.MySpacesInventory
import de.blazemcworld.fireflow.inventory.PreferencesInventory
import de.blazemcworld.fireflow.util.fireflowSetInstance
import de.blazemcworld.fireflow.util.reset
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.inventory.InventoryPreClickEvent
import net.minestom.server.event.item.ItemDropEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.trait.BlockEvent
import net.minestom.server.event.trait.CancellableEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import kotlin.math.abs

object Lobby {
    private val MY_SPACES_ITEM = ItemStack.builder(Material.ENCHANTED_BOOK)
        .customName(Component.text("My Spaces").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false))
        .lore(
            Component.text("Manage your spaces").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("using this item.").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        )
        .build()

    private val PREFERENCES_ITEM = ItemStack.builder(Material.PIGLIN_BANNER_PATTERN)
        .customName(Component.text("Preferences").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
        .lore(
            Component.text("Manage your preferences").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("using this item.").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        )
        .hideExtraTooltip()
        .build()

    val instance = MinecraftServer.getInstanceManager().createInstanceContainer()

    init {
        instance.setChunkSupplier(::LightingChunk)

        instance.setGenerator gen@{
            if (abs(it.absoluteStart().x() + 8) > 16) return@gen
            if (abs(it.absoluteStart().z() + 8) > 16) return@gen
            it.modifier().fillHeight(-1, 0, Block.POLISHED_ANDESITE)
        }
        instance.timeRate = 0

        val events = instance.eventNode()
        events.addListener(PlayerMoveEvent::class.java) {
            if (it.newPosition.y < -20) {
                it.newPosition = Pos.ZERO
            }
        }

        events.addListener(PlayerUseItemEvent::class.java) {
            it.isCancelled = true
            if (it.hand == Player.Hand.MAIN) handleRightClick(it.player)
        }
        events.addListener(PlayerUseItemOnBlockEvent::class.java) {
            if (it.hand == Player.Hand.MAIN) handleRightClick(it.player)
        }

        events.addListener(PlayerSpawnEvent::class.java) {
            it.player.reset()
            it.player.inventory.setItemStack(0, MY_SPACES_ITEM)
            it.player.inventory.setItemStack(8, PREFERENCES_ITEM)
        }

        events.apply {
            addListener(InstanceEvent::class.java) {
                if (it is BlockEvent && it is CancellableEvent) it.isCancelled = true
            }
        }
        events.addListener(ItemDropEvent::class.java) { it.isCancelled = true }
        events.addListener(InventoryPreClickEvent::class.java) { it.isCancelled = true }
        events.addListener(PlayerSwapItemEvent::class.java) { it.isCancelled = true }
    }

    private fun handleRightClick(player: Player) {
        if (player.itemInMainHand == MY_SPACES_ITEM) {
            MySpacesInventory.open(player)
        }

        if (player.itemInMainHand == PREFERENCES_ITEM) {
            PreferencesInventory.open(player)
        }
    }

    fun playerJoin(player: Player) {
        player.fireflowSetInstance(instance)
    }
}