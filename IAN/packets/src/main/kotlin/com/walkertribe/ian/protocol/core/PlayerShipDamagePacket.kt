package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.PacketSubtype

/**
 * Notifies the client that the indicated ship has received an impact. This manifests as an
 * interface screw on the client.
 *
 * @author rjwut
 */
@PacketSubtype(subtype = SimpleEventPacket.Subtype.PLAYER_SHIP_DAMAGE)
class PlayerShipDamagePacket(reader: PacketReader) : SimpleEventPacket(reader) {
    /** The index of the ship being impacted (0-based). */
    val shipIndex: Int = reader.readInt()

    /** How long the interface screw should last, in seconds. */
    val duration: Float = reader.readFloat()
}
