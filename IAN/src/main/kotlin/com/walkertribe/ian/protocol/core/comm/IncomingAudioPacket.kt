package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.enums.AudioMode
import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.ArtemisPacketException
import com.walkertribe.ian.protocol.BaseArtemisPacket
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.CorePacketType

/**
 * Received when an incoming COMMs audio message arrives.
 * @author dhleong
 */
@Packet(origin = Origin.SERVER, type = CorePacketType.INCOMING_MESSAGE)
class IncomingAudioPacket(reader: PacketReader) : BaseArtemisPacket() {
    /**
     * The ID assigned to this audio message.
     */
    val audioId: Int = reader.readInt()

    /**
     * Indicates whether this packet indicates that the message is available
     * (INCOMING) or playing (PLAYING).
     */
    val audioMode: AudioMode = reader.readAudioMode()

    override fun writePayload(writer: PacketWriter) {
        writer.unsupported()
    }

    private companion object {
        const val PLAYING = 1
        const val INCOMING = 2

        fun PacketReader.readAudioMode(): AudioMode = when (val mode = readInt()) {
            PLAYING -> AudioMode.Playing
            INCOMING -> AudioMode.Incoming(title = readString(), filename = readString())
            else -> throw ArtemisPacketException("Unknown audio mode: $mode")
        }
    }
}
