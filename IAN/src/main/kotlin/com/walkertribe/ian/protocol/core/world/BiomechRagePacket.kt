package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.SimpleEventPacket

/**
 * Updates the client about the rage level of the biomech tribe.
 * @author rjwut
 */
@Packet(
    origin = Origin.SERVER,
    type = CorePacketType.SIMPLE_EVENT,
    subtype = SimpleEventPacket.Subtype.BIOMECH_STANCE
)
class BiomechRagePacket(reader: PacketReader) : SimpleEventPacket(reader) {
    /**
     * Returns the biomech rage level.
     */
    val rage: Int = reader.readInt()
}
