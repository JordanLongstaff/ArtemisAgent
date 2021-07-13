package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.BaseArtemisPacket
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.util.Version

/**
 * Updates the current status of the single-seat craft bays.
 * @author rjwut
 */
@Packet(origin = Origin.SERVER, type = CorePacketType.CARRIER_RECORD)
class BayStatusPacket(reader: PacketReader) : BaseArtemisPacket() {
    val fighterCount: Int

    init {
        var count = 0
        while (true) {
            val id = reader.readInt()
            if (id == 0) {
                break
            }
            count++

            if (reader.version >= BAY_NUMBER_VERSION) {
                reader.readInt()
            }
            reader.readString()
            reader.readString()
            reader.readInt()
        }
        fighterCount = count
    }

    override fun writePayload(writer: PacketWriter) {
        writer.unsupported()
    }

    private companion object {
        val BAY_NUMBER_VERSION = Version(2, 6, 0)
    }
}
