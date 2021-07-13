package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.IntelType
import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.BaseArtemisPacket
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.CorePacketType

/**
 * Provides intel on another vessel, typically as the result of a level 2 scan.
 * @author rjwut
 */
@Packet(origin = Origin.SERVER, type = CorePacketType.OBJECT_TEXT)
class IntelPacket(reader: PacketReader) : BaseArtemisPacket() {
    /**
     * The ID of the ship in question.
     */
    val id: Int = reader.readInt()

    /**
     * The type of intel received.
     */
    val intelType: IntelType = IntelType.entries[reader.readByte().toInt()]

    /**
     * The intel on that ship, as human-readable text.
     */
    val intel: CharSequence = reader.readString()

    override fun writePayload(writer: PacketWriter) {
        writer.unsupported()
    }
}
