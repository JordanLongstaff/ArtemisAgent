package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.PacketSubtype

/**
 * Describes why the game has ended.
 *
 * @author rjwut
 */
@PacketSubtype(subtype = SimpleEventPacket.Subtype.GAME_OVER_REASON)
class GameOverReasonPacket(reader: PacketReader) : SimpleEventPacket(reader) {
    /** The text describing why the game ended. Each element in this list is one line. */
    val text =
        mutableListOf<String>()
            .apply {
                while (reader.hasMore) {
                    add(reader.readString())
                }
            }
            .toList()

    init {
        reader.clearObjectIDs()
    }
}
