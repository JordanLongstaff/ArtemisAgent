package com.walkertribe.ian.world

/** Represents either the front or rear shield strength of a ship or station. */
class Shields(timestamp: Long) {
    /** The percentage of the maximum strength the shield has left. Refreshes automatically. */
    var percentage: Float = Float.NaN
        private set

    /**
     * Whether the shield is damaged, i.e. if its current strength is below its maximum strength.
     */
    var isDamaged: Boolean = false
        private set

    /** The current strength of the shield. Unspecified: Float.NaN */
    val strength = Property.FloatProperty(timestamp).apply { addListener { refresh() } }

    /** The maximum strength of the shield. Unspecified: Float.NaN */
    val maxStrength = Property.FloatProperty(timestamp).apply { addListener { refresh() } }

    val hasData: Boolean
        get() = strength.hasValue || maxStrength.hasValue

    infix fun updates(shields: Shields) {
        strength updates shields.strength
        maxStrength updates shields.maxStrength
    }

    private fun refresh() {
        percentage = strength.value / maxStrength.value
        isDamaged = strength < maxStrength
    }
}
