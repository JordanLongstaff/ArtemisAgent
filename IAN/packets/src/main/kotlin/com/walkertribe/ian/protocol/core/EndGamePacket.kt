package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.PacketSubtype

/**
 * Sent by the server when the "End Game" button is clicked on the statistics page.
 *
 * @author rjwut
 */
@PacketSubtype(subtype = SimpleEventPacket.Subtype.END_GAME)
class EndGamePacket(reader: PacketReader) : SimpleEventPacket(reader)
