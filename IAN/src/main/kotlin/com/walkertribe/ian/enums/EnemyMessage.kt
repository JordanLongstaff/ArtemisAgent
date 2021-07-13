package com.walkertribe.ian.enums

import com.walkertribe.ian.iface.PacketWriter

/**
 * Messages that can be sent to enemy NPCs.
 * @author rjwut
 */
enum class EnemyMessage : CommsMessage {
    WILL_YOU_SURRENDER,
    TAUNT_1,
    TAUNT_2,
    TAUNT_3;

    override val id: Int = ordinal
    override val recipientType: CommsRecipientType = CommsRecipientType.ENEMY

    override fun writeTo(writer: PacketWriter) {
        writer.writeInt(id).writeInt(CommsMessage.NO_ARG)
    }
}
