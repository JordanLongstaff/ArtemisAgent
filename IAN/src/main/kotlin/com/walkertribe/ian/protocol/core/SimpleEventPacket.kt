package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.BaseArtemisPacket

/**
 * A superclass for handling SIMPLE_EVENT server packets.
 * @author rjwut
 */
abstract class SimpleEventPacket protected constructor(reader: PacketReader) : BaseArtemisPacket() {
    /**
     * SIMPLE_EVENT server packet subtypes.
     */
    object Subtype {
        const val PAUSE: Byte = 0x04
        const val PLAYER_SHIP_DAMAGE: Byte = 0x05
        const val END_GAME: Byte = 0x06
        const val JUMP_END: Byte = 0x0d
        const val SHIP_SETTINGS: Byte = 0x0f
        const val GAME_OVER_REASON: Byte = 0x14
        const val BIOMECH_STANCE: Byte = 0x19
        const val DOCKED: Byte = 0x1a
    }

    init {
        reader.skip(Int.SIZE_BYTES)
    }

    override fun writePayload(writer: PacketWriter) {
        writer.unsupported()
    }
}
