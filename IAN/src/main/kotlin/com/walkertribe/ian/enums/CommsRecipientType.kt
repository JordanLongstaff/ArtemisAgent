package com.walkertribe.ian.enums

import com.walkertribe.ian.vesseldata.Faction
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.world.ArtemisNpc
import com.walkertribe.ian.world.ArtemisObject

/**
 * The types of ArtemisObjects to which players can send COMMs messages.
 * @author rjwut
 */
enum class CommsRecipientType {
    /**
     * Other player ships
     */
    PLAYER,

    /**
     * NPC enemy ships
     */
    ENEMY,

    /**
     * Bases
     */
    BASE,

    /**
     * Other (civilian NPCs)
     */
    OTHER;

    companion object {
        /**
         * Returns the CommsRecipientType that corresponds to the given ArtemisObject,
         * or null if the object in question cannot receive COMMs messages.
         */
        operator fun invoke(
            recipient: ArtemisObject,
            vesselData: VesselData,
        ): CommsRecipientType? = when (recipient.type) {
            ObjectType.PLAYER_SHIP -> PLAYER
            ObjectType.BASE -> BASE
            ObjectType.NPC_SHIP -> {
                val npc = recipient as ArtemisNpc
                val enemy = npc.getVessel(vesselData)?.getFaction(vesselData)?.get(Faction.ENEMY)
                    ?: npc.isEnemy.value.booleanValue
                if (enemy) ENEMY else OTHER
            }
            else -> null
        }
    }
}
