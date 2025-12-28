package com.walkertribe.ian.enums

/** The ship systems. Also includes values for hallway and inaccessible nodes. */
enum class ShipSystem {
    HALLWAY,
    BEAMS,
    TORPEDOES,
    SENSORS,
    MANEUVER,
    IMPULSE,
    WARP_JUMP_DRIVE,
    FRONT_SHIELDS,
    REAR_SHIELDS;

    val value: Int by lazy { ordinal - BEAMS.ordinal }

    companion object {
        operator fun get(value: Int): ShipSystem? =
            try {
                entries[value + BEAMS.ordinal]
            } catch (_: IndexOutOfBoundsException) {
                null
            }
    }
}
