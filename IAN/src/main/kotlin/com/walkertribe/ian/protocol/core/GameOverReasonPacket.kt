package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.Packet

/**
 * Describes why the game has ended.
 * @author rjwut
 */
@Packet(
    origin = Origin.SERVER,
    type = CorePacketType.SIMPLE_EVENT,
    subtype = SimpleEventPacket.Subtype.GAME_OVER_REASON
)
class GameOverReasonPacket(reader: PacketReader) : SimpleEventPacket(reader), RequiredPacket {
    /**
     * The text describing why the game ended. Each element in this list is one line.
     */
    val text = mutableListOf<CharSequence>().apply {
        while (reader.hasMore) {
            add(reader.readString())
        }
    }.toList()

    init {
        reader.clearObjectIDs()
    }
}
