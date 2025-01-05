package com.walkertribe.ian.enums

/**
 * All messages that can be sent by the comm officer implement this interface.
 *
 * @author rjwut
 */
interface CommsMessage {
    /** Returns the ID of this CommsMessage. IDs are unique per CommsRecipientType. */
    val id: Int

    /** Returns the CommsTargetType that can receive this CommsMessage. */
    val recipientType: CommsRecipientType
}
