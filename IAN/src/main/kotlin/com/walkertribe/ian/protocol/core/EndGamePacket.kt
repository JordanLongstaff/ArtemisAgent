package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.Packet

/**
 * Sent by the server when the "End Game" button is clicked on the statistics page.
 * @author rjwut
 */
@Packet(
    origin = Origin.SERVER,
    type = CorePacketType.SIMPLE_EVENT,
    subtype = SimpleEventPacket.Subtype.END_GAME
)
class EndGamePacket(reader: PacketReader) : SimpleEventPacket(reader)
