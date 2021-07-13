package com.walkertribe.ian.enums

import com.walkertribe.ian.iface.PacketWriter

/**
 * All messages that can be sent by the comm officer implement this interface.
 * @author rjwut
 */
interface CommsMessage {
    /**
     * Returns the ID of this CommsMessage. IDs are unique per
     * CommsRecipientType.
     */
    val id: Int

    /**
     * Returns the CommsTargetType that can receive this CommsMessage.
     */
    val recipientType: CommsRecipientType

    /**
     * Writes this CommsMessage to the given PacketWriter. This write operation
     * should consist of writing the message ID, followed by the argument, if any.
     */
    fun writeTo(writer: PacketWriter)

    companion object {
        const val NO_ARG = 0x00730078
    }
}
