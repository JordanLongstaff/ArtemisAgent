package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketType
import com.walkertribe.ian.protocol.core.CorePacketType

/**
 * Sent by the server immediately on connection. The receipt of this packet indicates a successful
 * connection to the server.
 *
 * @author rjwut
 */
@PacketType(type = CorePacketType.PLAIN_TEXT_GREETING)
class WelcomePacket(reader: PacketReader) : Packet.Server(reader) {
    /** Returns the welcome message sent by the server. */
    val message: String = reader.readUsAsciiString()
}
