package de.blazemcworld.fireflow.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.metadata.display.TextDisplayMeta
import net.minestom.server.instance.Instance
import kotlin.math.*

class LineComponent {

    private val display = Entity(EntityType.TEXT_DISPLAY).apply {
        setNoGravity(true)
        val meta = entityMeta as TextDisplayMeta
        meta.text = Component.text("-")
        meta.backgroundColor = 0
        meta.transformationInterpolationDuration = 1
        meta.posRotInterpolationDuration = 1
    }

    var start = Pos2d.ZERO
    var end = Pos2d.ZERO
    var color: TextColor = NamedTextColor.WHITE

    fun update(inst: Instance) {
        val meta = display.entityMeta as TextDisplayMeta
        meta.text = Component.text("-").color(color)

        val dist = start.distance(end)
        meta.scale = Vec(dist * 8, 1.0, 1.0)
        val angle = atan2(end.y - start.y, start.x - end.x).toFloat()
        meta.leftRotation = floatArrayOf(0f, 0f, sin(angle / 2), cos(angle / 2))
        val v = ((start + end) * 0.5) + Pos2d(
            cos(angle) * dist * 0.1 - sin(angle) * 0.1625,
            -sin(angle) * dist * 0.1 - cos(angle) * 0.1625
        )
        display.setInstance(inst, v.to3d(15.999).withView(180f, 0f))
    }

    fun remove() {
        display.remove()
    }

    fun distance(p: Pos2d): Double {
        if (start == end) return start.distance(p)
        val ab = Pos2d(end.x - start.x, end.y - start.y)
        val ap = Pos2d(p.x - start.x, p.y - start.y)

        val projection = (ap.x * ab.x + ap.y * ab.y) / (ab.x * ab.x + ab.y * ab.y)

        val closestPoint = when {
            projection <= 0 -> start
            projection >= 1 -> end
            else -> Pos2d(start.x + projection * ab.x, start.y + projection * ab.y)
        }

        return sqrt((p.x - closestPoint.x).pow(2) + (p.y - closestPoint.y).pow(2))
    }

}