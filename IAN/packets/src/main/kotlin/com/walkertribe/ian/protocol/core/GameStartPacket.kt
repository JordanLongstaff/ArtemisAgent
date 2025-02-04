package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.enums.GameType
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketType

/**
 * Sent by the server when the simulation starts.
 *
 * @author rjwut
 */
@PacketType(type = CorePacketType.START_GAME)
class GameStartPacket(reader: PacketReader) : Packet.Server(reader) {
    /** What type of simulation is running (siege, single front, etc.) */
    val gameType: GameType =
        reader.run {
            skip(Int.SIZE_BYTES)
            readIntAsEnum()
        }
}
