package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.BaseArtemisPacket
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.util.Util.caretToNewline
import com.walkertribe.ian.util.Version

/**
 * Received when an incoming COMMs message arrives.
 */
@Packet(origin = Origin.SERVER, type = CorePacketType.COMM_TEXT)
class CommsIncomingPacket(reader: PacketReader) : BaseArtemisPacket() {
    /**
     * A String identifying the sender. This may not correspond to the name of
     * a game entity. For example, some messages from bases or friendly ships
     * have additional detail after the entity's name ("DS3 TSN Deep Space
     * Base"). Messages in scripted scenarios can have any String for the sender.
     */
    val sender: CharSequence

    /**
     * The content of the message.
     */
    val message: CharSequence

    override fun writePayload(writer: PacketWriter) {
        writer.unsupported()
    }

    init {
        reader.skip(if (reader.version < Version.COMM_FILTERS) Int.SIZE_BYTES else 2)
        sender = reader.readString()
        message = reader.readString().caretToNewline().trim()
    }
}
