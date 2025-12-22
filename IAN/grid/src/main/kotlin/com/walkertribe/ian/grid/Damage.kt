package com.walkertribe.ian.grid

import kotlin.math.roundToInt

class Damage(internal val coord: Coordinate, val damage: Float) {
    init {
        require(damage >= 0f) { "Invalid damage: $damage" }
    }

    override fun hashCode(): Int = coord.hashCode()

    override fun equals(other: Any?): Boolean =
        this === other || (other is Damage && coord == other.coord)

    override fun toString(): String = "${(damage * PERCENT).roundToInt()}% damage at $coord"

    private companion object {
        const val PERCENT = 100
    }
}
