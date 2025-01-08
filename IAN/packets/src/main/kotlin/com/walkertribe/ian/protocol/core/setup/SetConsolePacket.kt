package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.enums.Console
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.core.ValueIntPacket

/**
 * Take or relinquish a bridge console.
 *
 * @author dhleong
 */
class SetConsolePacket(console: Console) : ValueIntPacket(Subtype.SET_CONSOLE, console.index) {
    override fun writePayload(writer: PacketWriter) {
        super.writePayload(writer)
        writer.writeInt(1)
    }
}
