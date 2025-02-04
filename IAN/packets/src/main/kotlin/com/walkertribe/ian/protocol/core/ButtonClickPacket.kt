package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.util.JamCrc

/**
 * Sent by the client whenever the game master or comms officer clicks a custom on-screen button.
 *
 * @author rjwut
 */
class ButtonClickPacket(
    /** Returns the label hash for the button that was clicked. */
    val hash: Int
) : ValueIntPacket(Subtype.BUTTON_CLICK, UNKNOWN) {
    /** Creates a click command packet for the button with the given label. */
    constructor(label: String) : this(JamCrc.compute(label))

    override fun writePayload(writer: PacketWriter) {
        super.writePayload(writer)
        writer.writeInt(hash)
    }

    private companion object {
        private const val UNKNOWN = 0x0d
    }
}
