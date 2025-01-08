package com.walkertribe.ian.enums

/**
 * Messages that can be sent to enemy NPCs.
 *
 * @author rjwut
 */
enum class EnemyMessage : CommsMessage {
    WILL_YOU_SURRENDER,
    TAUNT_1,
    TAUNT_2,
    TAUNT_3;

    override val id: Int
        get() = ordinal

    override val recipientType: CommsRecipientType
        get() = CommsRecipientType.ENEMY
}
