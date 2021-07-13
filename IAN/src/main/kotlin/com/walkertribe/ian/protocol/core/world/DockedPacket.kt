package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.SimpleEventPacket

/**
 * Sent when a player ship docks. Specifically, this is when the base has finished drawing in the
 * ship with its tractor beam and resupply commences. To detect when a base has grabbed a ship with
 * its tractor beam, check ArtemisPlayer.getDockingBase().
 * @author rjwut
 */
@Packet(
    origin = Origin.SERVER,
    type = CorePacketType.SIMPLE_EVENT,
    subtype = SimpleEventPacket.Subtype.DOCKED
)
class DockedPacket(reader: PacketReader) : SimpleEventPacket(reader) {
    /**
     * The ID of the ship that has docked.
     */
    val objectId: Int = reader.readInt()
}
