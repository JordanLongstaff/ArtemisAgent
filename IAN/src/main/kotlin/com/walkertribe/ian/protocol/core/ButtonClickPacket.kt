package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.util.JamCrc

/**
 * Sent by the client whenever the game master or comms officer clicks a custom on-screen button.
 * @author rjwut
 */
@Packet(
    origin = Origin.CLIENT,
    type = CorePacketType.VALUE_INT,
    subtype = ValueIntPacket.Subtype.BUTTON_CLICK
)
class ButtonClickPacket(
    /**
     * Returns the label hash for the button that was clicked.
     */
    val hash: Int
) : ValueIntPacket(UNKNOWN) {
    /**
     * Creates a click command packet for the button with the given label.
     */
    constructor(label: CharSequence) : this(JamCrc.compute(label))

    override fun writePayload(writer: PacketWriter) {
        super.writePayload(writer)
        writer.writeInt(hash)
    }

    private companion object {
        const val UNKNOWN = 0x0d
    }
}
