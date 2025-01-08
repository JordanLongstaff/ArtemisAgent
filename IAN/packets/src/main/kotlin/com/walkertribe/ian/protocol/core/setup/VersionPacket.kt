package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketException
import com.walkertribe.ian.protocol.PacketType
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.util.Version

/**
 * Gives the Artemis server's version number. Sent immediately after WelcomePacket.
 *
 * @author rjwut
 */
@PacketType(type = CorePacketType.CONNECTED)
class VersionPacket(reader: PacketReader) : Packet.Server(reader) {
    /** @return The version number */
    val version =
        reader.run {
            skip(SKIPPED_BYTES)
            if (hasMore) {
                Version(readInt(), readInt(), readInt())
            } else {
                throw PacketException("ArtemisAgent does not support legacy versions")
            }
        }

    private companion object {
        private const val SKIPPED_BYTES = Int.SIZE_BYTES + Float.SIZE_BYTES
    }
}
