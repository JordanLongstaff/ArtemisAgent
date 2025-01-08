package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.PacketSubtype
import com.walkertribe.ian.protocol.core.SimpleEventPacket
import com.walkertribe.ian.protocol.core.setup.Ship.Companion.readShip
import com.walkertribe.ian.world.Artemis

/**
 * Sent by the server to update the names, types and drives for each ship.
 *
 * @author dhleong
 */
@PacketSubtype(subtype = SimpleEventPacket.Subtype.SHIP_SETTINGS)
class AllShipSettingsPacket(reader: PacketReader) : SimpleEventPacket(reader) {
    val ships: List<Ship> = Array(Artemis.SHIP_COUNT) { reader.readShip() }.toList()

    /** Returns the ship with the given index (0-based). */
    operator fun get(shipIndex: Int): Ship = ships[shipIndex]
}
