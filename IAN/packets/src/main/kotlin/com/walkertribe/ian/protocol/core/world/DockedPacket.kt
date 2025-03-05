package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.PacketSubtype
import com.walkertribe.ian.protocol.core.SimpleEventPacket
import com.walkertribe.ian.world.ArtemisPlayer

/**
 * Sent when a player ship docks. Specifically, this is when the base has finished drawing in the
 * ship with its tractor beam and resupply commences. To detect when a base has grabbed a ship with
 * its tractor beam, check [ArtemisPlayer.dockingBase].
 *
 * @author rjwut
 */
@PacketSubtype(subtype = SimpleEventPacket.Subtype.DOCKED)
class DockedPacket(reader: PacketReader) : SimpleEventPacket(reader) {
    /** The ID of the ship that has docked. */
    val objectId: Int = reader.readInt()
}
