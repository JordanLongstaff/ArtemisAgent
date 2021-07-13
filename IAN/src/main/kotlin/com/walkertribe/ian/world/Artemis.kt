package com.walkertribe.ian.world

/**
 * Artemis-related constants
 * @author rjwut
 */
object Artemis {
    /**
     * The maximum number of tubes a ship can have. Note that none of the ships
     * in the stock install of Artemis have this many tubes, but a custom ship
     * might.
     */
    const val MAX_TUBES = 6

    /**
     * The maximum warp factor player ships can achieve.
     */
    const val MAX_WARP: Byte = 4

    /**
     * The number of available player ships.
     */
    const val SHIP_COUNT = 8

    /**
     * The number of ship systems.
     */
    const val SYSTEM_COUNT = 8

    /**
     * The length of the sides of the map (the X and Z dimensions).
     */
    const val MAP_SIZE = 100000
}
