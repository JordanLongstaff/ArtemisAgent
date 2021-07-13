package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.Packet

/**
 * Notifies the client that the indicated ship has received an impact. This
 * manifests as an interface screw on the client.
 * @author rjwut
 */
@Packet(
    origin = Origin.SERVER,
    type = CorePacketType.SIMPLE_EVENT,
    subtype = SimpleEventPacket.Subtype.PLAYER_SHIP_DAMAGE
)
class PlayerShipDamagePacket(reader: PacketReader) : SimpleEventPacket(reader) {
    /**
     * The index of the ship being impacted (0-based).
     */
    val shipIndex: Int = reader.readInt()

    /**
     * How long the interface screw should last, in seconds.
     */
    val duration: Float = reader.readFloat()
}
