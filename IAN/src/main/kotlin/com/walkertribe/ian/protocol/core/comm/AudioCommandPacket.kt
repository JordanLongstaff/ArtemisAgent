package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.enums.AudioCommand
import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.BaseArtemisPacket
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.CorePacketType

/**
 * Plays or deletes an audio message.
 */
@Packet(origin = Origin.CLIENT, type = CorePacketType.CONTROL_MESSAGE)
class AudioCommandPacket(
    /**
     * The ID of the audio message to which the command applies.
     */
    val audioId: Int,

    /**
     * The action to perform with that message.
     */
    val command: AudioCommand
) : BaseArtemisPacket() {
    override fun writePayload(writer: PacketWriter) {
        writer.writeInt(audioId).writeEnumAsInt(command)
    }
}
