package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.enums.CommsMessage
import com.walkertribe.ian.enums.CommsRecipientType
import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.BaseArtemisPacket
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.world.ArtemisObject

/**
 * Sends a message to another entity.
 */
@Packet(origin = Origin.CLIENT, type = CorePacketType.COMMS_MESSAGE)
class CommsOutgoingPacket(
    recipient: ArtemisObject,

    /**
     * The enum value representing the message to send. May include an argument.
     */
    val message: CommsMessage,

    vesselData: VesselData,
) : BaseArtemisPacket() {
    /**
     * The CommsRecipientType value corresponding to the target object.
     */
    val recipientType: CommsRecipientType = requireNotNull(CommsRecipientType(recipient, vesselData)) {
        "Recipient cannot receive messages"
    }

    /**
     * The ID of the target object.
     */
    val recipientId: Int = recipient.id

    /**
     * Creates an outgoing message with an argument. At this writing, only the
     * [com.walkertribe.ian.enums.OtherMessage.GoDefend](GoDefend)
     * message takes an argument, which is the ID of the object to be defended.
     * For messages with no argument, you can pass in [.NO_ARG], but it's
     * easier to just use the other constructor. An IllegalArgumentException
     * will be thrown if you provide an argument to a message which doesn't
     * accept one, or use NO_ARG with a message which requires one.
     */
    init {
        val messageRecipientType = message.recipientType
        require(recipientType === messageRecipientType) {
            "Recipient type is $recipientType, but message recipient type is $messageRecipientType"
        }
    }

    override fun writePayload(writer: PacketWriter) {
        writer.writeEnumAsInt(recipientType)
            .writeInt(recipientId)
        message.writeTo(writer)
        writer.writeInt(NO_ARG_2)
    }

    private companion object {
        const val NO_ARG_2 = 0x004f005e
    }
}
