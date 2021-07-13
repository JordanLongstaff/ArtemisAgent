package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.enums.Console
import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.ValueIntPacket

/**
 * Take or relinquish a bridge console.
 * @author dhleong
 */
@Packet(
    origin = Origin.CLIENT,
    type = CorePacketType.VALUE_INT,
    subtype = ValueIntPacket.Subtype.SET_CONSOLE
)
class SetConsolePacket(console: Console) : ValueIntPacket(console.index) {
    public override fun writePayload(writer: PacketWriter) {
        super.writePayload(writer)
        writer.writeInt(1)
    }
}
