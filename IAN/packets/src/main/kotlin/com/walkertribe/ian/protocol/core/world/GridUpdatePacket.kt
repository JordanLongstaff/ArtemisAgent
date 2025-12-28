package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.grid.Coordinate
import com.walkertribe.ian.grid.Damage
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketType
import com.walkertribe.ian.protocol.core.CorePacketType

@PacketType(type = CorePacketType.SHIP_SYSTEM_SYNC)
class GridUpdatePacket(reader: PacketReader) : Packet.Server(reader) {
    val isFullUpdate: Boolean = reader.readByte().toInt() != 0

    val damages: Set<Damage> = buildSet {
        while (true) {
            val x = reader.readByte()
            if (x == END_DAMAGE) break
            val y = reader.readByte()
            val z = reader.readByte()
            val damage = reader.readFloat()
            add(Damage(Coordinate(x, y, z), damage))
        }

        while (reader.readByte() != END_DAMCON) {
            reader.skip(DAMCON_BLOCK)
        }
    }

    private companion object {
        const val END_DAMAGE: Byte = -1
        const val END_DAMCON: Byte = -2
        const val DAMCON_BLOCK = 32
    }
}
