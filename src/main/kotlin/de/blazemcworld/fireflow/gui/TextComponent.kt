package de.blazemcworld.fireflow.gui

import net.kyori.adventure.text.Component
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.instance.Instance

class TextComponent {

    private val display = Entity(EntityType.TEXT_DISPLAY).apply {
        setNoGravity(true)
        val meta = entityMeta as TextDisplayMeta
        meta.backgroundColor = 0
        meta.lineWidth = Int.MAX_VALUE
        meta.transformationInterpolationDuration = 1
        meta.posRotInterpolationDuration = 1
    }

    var pos = Pos2d.ZERO
    var text = Component.empty()

    fun update(inst: Instance) {
        val adjusted = pos + Pos2d(width() / 2, 1.0 / 32)
        val meta = display.entityMeta as TextDisplayMeta
        meta.text = text
        display.setInstance(inst, adjusted.to3d(15.999).withView(180f, 0f))
    }

    fun width() = TextWidth.calculate(text) / 40
    fun height() = 0.25

    fun remove() {
        display.remove()
    }

    fun size() = Pos2d(width(), height())

    fun includes(other: Pos2d) = pos.x < other.x && pos.y < other.y && pos.x + width() > other.x && pos.y + height() > other.y
}