package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.enums.GameType
import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.BaseArtemisPacket
import com.walkertribe.ian.protocol.Packet

/**
 * Sent by the server when the simulation starts.
 * @author rjwut
 */
@Packet(origin = Origin.SERVER, type = CorePacketType.START_GAME)
class GameStartPacket(reader: PacketReader) : BaseArtemisPacket(), RequiredPacket {
    /**
     * What type of simulation is running (siege, single front, etc.)
     */
    val gameType: GameType = reader.run {
        skip(Int.SIZE_BYTES)
        readIntAsEnum()
    }

    override fun writePayload(writer: PacketWriter) {
        writer.unsupported()
    }
}
