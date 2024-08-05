package de.blazemcworld.fireflow.preferences

import net.minestom.server.item.Material

object DeleteWarningPreference: Preference("Show delete warning") {
    override val states = mutableListOf(
        PreferenceState("On", Material.EMERALD_BLOCK),
        PreferenceState("Off", Material.REDSTONE_BLOCK)
    )
}