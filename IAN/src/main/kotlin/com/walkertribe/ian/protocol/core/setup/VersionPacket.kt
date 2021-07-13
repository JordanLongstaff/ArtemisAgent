package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.ArtemisPacketException
import com.walkertribe.ian.protocol.BaseArtemisPacket
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.RequiredPacket
import com.walkertribe.ian.util.Version

/**
 * Gives the Artemis server's version number. Sent immediately after
 * WelcomePacket.
 * @author rjwut
 */
@Packet(origin = Origin.SERVER, type = CorePacketType.CONNECTED)
class VersionPacket(reader: PacketReader) : BaseArtemisPacket(), RequiredPacket {
    /**
     * @return The version number
     */
    val version = reader.run {
        skip(SKIPPED_BYTES)
        if (hasMore) {
            Version(
                readInt(),
                readInt(),
                readInt()
            )
        } else {
            throw ArtemisPacketException("ArtemisAgent does not support legacy versions")
        }
    }

    override fun writePayload(writer: PacketWriter) {
        writer.unsupported()
    }

    private companion object {
        const val SKIPPED_BYTES = Int.SIZE_BYTES + Float.SIZE_BYTES
    }
}
