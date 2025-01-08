package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketType

/**
 * Heartbeat packet classes that are either sent or received.
 *
 * @author rjwut
 */
sealed interface HeartbeatPacket {
    data object Client : ValueIntPacket(Subtype.CLIENT_HEARTBEAT), HeartbeatPacket

    @PacketType(type = CorePacketType.HEARTBEAT)
    class Server(reader: PacketReader) : Packet.Server(reader), HeartbeatPacket
}
