package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketType
import com.walkertribe.ian.util.Version

/**
 * Updates the current status of the single-seat craft bays.
 *
 * @author rjwut
 */
@PacketType(type = CorePacketType.CARRIER_RECORD)
class BayStatusPacket(reader: PacketReader) : Packet.Server(reader) {
    val fighterCount: Int =
        reader.run {
            var count = 0
            while (true) {
                val id = readInt()
                if (id == 0) {
                    break
                }
                count++

                if (version >= BAY_NUMBER_VERSION) {
                    skip(Int.SIZE_BYTES)
                }
                skipString()
                skipString()
                skip(Int.SIZE_BYTES)
            }
            count
        }

    private companion object {
        private val BAY_NUMBER_VERSION = Version(2, 6, 0)
    }
}
