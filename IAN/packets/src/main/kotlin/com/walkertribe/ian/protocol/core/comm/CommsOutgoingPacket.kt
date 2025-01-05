package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.enums.CommsMessage
import com.walkertribe.ian.enums.CommsRecipientType
import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.enums.OtherMessage
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.vesseldata.Faction
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.world.ArtemisNpc
import com.walkertribe.ian.world.ArtemisObject

/** Sends a message to another entity. */
class CommsOutgoingPacket(
    recipient: ArtemisObject<*>,

    /** The enum value representing the message to send. May include an argument. */
    val message: CommsMessage,
    vesselData: VesselData,
) : Packet.Client(CorePacketType.COMMS_MESSAGE) {
    /** The [CommsRecipientType] corresponding to the target object. */
    val recipientType: CommsRecipientType =
        requireNotNull(getRecipientType(recipient, vesselData)) {
            "Recipient cannot receive messages"
        }

    /** The ID of the target object. */
    val recipientId: Int = recipient.id

    init {
        val messageRecipientType = message.recipientType
        require(recipientType === messageRecipientType) {
            "Recipient type is $recipientType, but message recipient type is $messageRecipientType"
        }
    }

    override fun writePayload(writer: PacketWriter) {
        writer
            .writeEnumAsInt(recipientType)
            .writeInt(recipientId)
            .writeCommsMessage(message)
            .writeInt(NO_ARG_2)
    }

    private companion object {
        private const val NO_ARG = 0x00730078
        private const val NO_ARG_2 = 0x004f005e

        /**
         * Returns the [CommsRecipientType] that corresponds to the given [ArtemisObject], or null
         * if the object in question cannot receive Comms messages.
         */
        private fun getRecipientType(
            recipient: ArtemisObject<*>,
            vesselData: VesselData,
        ): CommsRecipientType? =
            when (recipient.type) {
                ObjectType.PLAYER_SHIP -> CommsRecipientType.PLAYER
                ObjectType.BASE -> CommsRecipientType.BASE
                ObjectType.NPC_SHIP -> {
                    val npc = recipient as ArtemisNpc
                    val enemy =
                        npc.getVessel(vesselData)?.getFaction(vesselData)?.get(Faction.ENEMY)
                            ?: npc.isEnemy.value.booleanValue
                    if (enemy) CommsRecipientType.ENEMY else CommsRecipientType.OTHER
                }
                else -> null
            }

        private fun PacketWriter.writeCommsMessage(message: CommsMessage): PacketWriter = apply {
            writeInt(message.id)
            writeInt(if (message is OtherMessage.GoDefend) message.targetID else NO_ARG)
        }
    }
}
