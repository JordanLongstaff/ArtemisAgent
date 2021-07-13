package com.walkertribe.ian.enums

import com.walkertribe.ian.world.ArtemisBase
import com.walkertribe.ian.world.ArtemisBlackHole
import com.walkertribe.ian.world.ArtemisCreature
import com.walkertribe.ian.world.ArtemisMine
import com.walkertribe.ian.world.ArtemisNpc
import com.walkertribe.ian.world.ArtemisPlayer
import com.walkertribe.ian.world.BaseArtemisObject

/**
 * World object types.
 * @author rjwut
 */
enum class ObjectType(internal val dsl: BaseArtemisObject.Dsl<out BaseArtemisObject>? = null) {
    PLAYER_SHIP(ArtemisPlayer.PlayerDsl),
    WEAPONS_CONSOLE(ArtemisPlayer.WeaponsDsl),
    ENGINEERING_CONSOLE,
    UPGRADES(ArtemisPlayer.UpgradesDsl),
    NPC_SHIP(ArtemisNpc.Dsl),
    BASE(ArtemisBase.Dsl),
    MINE(ArtemisMine.Dsl),
    ANOMALY,
    NEBULA,
    TORPEDO,
    BLACK_HOLE(ArtemisBlackHole.Dsl),
    ASTEROID,
    GENERIC_MESH,
    CREATURE(ArtemisCreature.Dsl),
    DRONE;

    /**
     * Returns the ID of this type.
     */
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
