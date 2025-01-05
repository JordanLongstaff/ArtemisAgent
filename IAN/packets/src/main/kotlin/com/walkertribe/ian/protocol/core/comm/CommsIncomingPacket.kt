package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketType
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.util.Util.caretToNewline
import com.walkertribe.ian.util.Version

/** Received when an incoming COMMs message arrives. */
@PacketType(type = CorePacketType.COMM_TEXT)
class CommsIncomingPacket(reader: PacketReader) : Packet.Server(reader) {
    /**
     * A String identifying the sender. This may not correspond to the name of a game entity. For
     * example, some messages from bases or friendly ships have additional detail after the entity's
     * name (e.g. "DS3 Terran Deep Space Base"). Messages in scripted scenarios can have any String
     * for the sender.
     */
    val sender: String =
        reader
            .apply { skip(if (version < Version.COMM_FILTERS) Int.SIZE_BYTES else 2) }
            .readString()

    /** The content of the message. */
    val message: String = reader.readString().caretToNewline().trim()
}
