package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.BaseArtemisPacket
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.RequiredPacket

/**
 * Sent by the server immediately on connection. The receipt of this packet
 * indicates a successful connection to the server.
 * @author rjwut
 */
@Packet(origin = Origin.SERVER, type = CorePacketType.PLAIN_TEXT_GREETING)
class WelcomePacket(reader: PacketReader) : BaseArtemisPacket(), RequiredPacket {
    /**
     * Returns the welcome message sent by the server.
     */
    val message: String = reader.readUsAsciiString()

    override fun writePayload(writer: PacketWriter) {
        writer.unsupported()
    }
}
