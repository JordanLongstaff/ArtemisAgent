package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.enums.AudioMode
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketException
import com.walkertribe.ian.protocol.PacketType
import com.walkertribe.ian.protocol.core.CorePacketType

/**
 * Received when an incoming COMMs audio message arrives.
 *
 * @author dhleong
 */
@PacketType(type = CorePacketType.INCOMING_MESSAGE)
class IncomingAudioPacket(reader: PacketReader) : Packet.Server(reader) {
    /** The ID assigned to this audio message. */
    val audioId: Int = reader.readInt()

    /**
     * Indicates whether this packet indicates that the message is available (INCOMING) or playing
     * (PLAYING).
     */
    val audioMode: AudioMode = reader.readAudioMode()

    private companion object {
        private const val PLAYING = 1
        private const val INCOMING = 2

        private fun PacketReader.readAudioMode(): AudioMode =
            when (val mode = readInt()) {
                PLAYING -> AudioMode.Playing
                INCOMING -> AudioMode.Incoming(title = readString(), filename = readString())
                else -> throw PacketException("Unknown audio mode: $mode")
            }
    }
}
