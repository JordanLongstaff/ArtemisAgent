package com.walkertribe.ian.enums

/**
 * World object types.
 *
 * @author rjwut
 */
enum class ObjectType {
    PLAYER_SHIP,
    WEAPONS_CONSOLE,
    ENGINEERING_CONSOLE,
    UPGRADES,
    NPC_SHIP,
    BASE,
    MINE,
    ANOMALY,
    NEBULA,
    TORPEDO,
    BLACK_HOLE,
    ASTEROID,
    GENERIC_MESH,
    CREATURE,
    DRONE;

    /** Returns the ID of this type. */
    val id: Byte by lazy {
        var id = ordinal + 1
        if (this >= NEBULA) id++
        id.toByte()
    }

    companion object {
        operator fun get(id: Int): ObjectType? =
            if (id == 0) {
                null
            } else {
                requireNotNull(entries.find { it.id.toInt() == id }) {
                    "No ObjectType with this ID: $id"
                }
            }
    }
}
