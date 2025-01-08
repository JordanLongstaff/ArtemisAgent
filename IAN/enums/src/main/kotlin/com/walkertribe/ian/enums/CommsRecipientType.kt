package com.walkertribe.ian.enums

/**
 * The types of ArtemisObjects to which players can send Comms messages.
 *
 * @author rjwut
 */
enum class CommsRecipientType {
    /** Other player ships */
    PLAYER,

    /** NPC enemy ships */
    ENEMY,

    /** Bases */
    BASE,

    /** Other (civilian NPCs) */
    OTHER,
}
