package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.BaseArtemisPacket
import com.walkertribe.ian.protocol.Packet

/**
 * Common parent class for ServerHeartbeatPacket and ClientHeartbeatPacket.
 * @author rjwut
 */
sealed interface HeartbeatPacket : ArtemisPacket, RequiredPacket {
    @Packet(
        origin = Origin.CLIENT,
        type = CorePacketType.VALUE_INT,
        subtype = ValueIntPacket.Subtype.CLIENT_HEARTBEAT,
    )
    data object Client : ValueIntPacket(), HeartbeatPacket {
        override fun writePayload(writer: PacketWriter) {
            writer.writeInt(Subtype.CLIENT_HEARTBEAT.toInt())
        }
    }

    @Packet(
        origin = Origin.SERVER,
        type = CorePacketType.HEARTBEAT,
    )
    @Suppress("UNUSED_PARAMETER")
    class Server(reader: PacketReader) : BaseArtemisPacket(), HeartbeatPacket {
        override fun writePayload(writer: PacketWriter) {
            writer.unsupported()
        }
    }
}
