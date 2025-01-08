package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.PacketSubtype
import com.walkertribe.ian.util.BoolState

/**
 * Notifies the client that the game has paused or unpaused.
 *
 * @author rjwut
 */
@PacketSubtype(subtype = SimpleEventPacket.Subtype.PAUSE)
class PausePacket(reader: PacketReader) : SimpleEventPacket(reader) {
    val isPaused: BoolState = reader.readBool(Int.SIZE_BYTES)
}
