package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.enums.AudioCommand
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.CorePacketType

/** Plays or deletes an audio message. */
class AudioCommandPacket(
    /** The ID of the audio message to which the command applies. */
    val audioId: Int,

    /** The action to perform with that message. */
    val command: AudioCommand,
) : Packet.Client(CorePacketType.CONTROL_MESSAGE) {
    override fun writePayload(writer: PacketWriter) {
        writer.writeInt(audioId).writeEnumAsInt(command)
    }
}
