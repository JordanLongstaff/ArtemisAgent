package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.PacketSubtype
import com.walkertribe.ian.protocol.core.SimpleEventPacket

/**
 * Updates the client about the rage level of the biomech tribe.
 *
 * @author rjwut
 */
@PacketSubtype(subtype = SimpleEventPacket.Subtype.BIOMECH_STANCE)
class BiomechRagePacket(reader: PacketReader) : SimpleEventPacket(reader) {
    /** Returns the biomech rage level. */
    val rage: Int = reader.readInt()
}
