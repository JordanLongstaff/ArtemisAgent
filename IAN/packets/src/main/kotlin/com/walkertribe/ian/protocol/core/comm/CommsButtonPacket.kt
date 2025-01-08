package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketException
import com.walkertribe.ian.protocol.PacketType
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.util.Util.toHex

@PacketType(type = CorePacketType.COMMS_BUTTON)
class CommsButtonPacket(reader: PacketReader) : Packet.Server(reader) {
    sealed interface Action {
        @JvmInline value class Remove internal constructor(val label: String) : Action

        @JvmInline value class Create internal constructor(val label: String) : Action

        data object RemoveAll : Action
    }

    /** Returns whether to add or remove button(s). */
    val action: Action = reader.readAction()

    private companion object {
        private const val REMOVE: Byte = 0x00
        private const val CREATE: Byte = 0x02
        private const val REMOVE_ALL: Byte = 0x64

        private fun PacketReader.readAction(): Action =
            when (val actionValue = readByte()) {
                REMOVE -> Action.Remove(readString())
                CREATE -> Action.Create(readString())
                REMOVE_ALL -> Action.RemoveAll
                else -> throw PacketException("Invalid action: ${actionValue.toHex()}")
            }
    }
}
