package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.ArtemisPacketException
import com.walkertribe.ian.protocol.BaseArtemisPacket
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.util.Util.toHex

@Packet(origin = Origin.SERVER, type = CorePacketType.COMMS_BUTTON)
class CommsButtonPacket(reader: PacketReader) : BaseArtemisPacket() {
    sealed interface Action {
        @JvmInline
        value class Remove(val label: CharSequence) : Action

        @JvmInline
        value class Create(val label: CharSequence) : Action

        data object RemoveAll : Action
    }

    /**
     * Returns whether to add or remove button(s).
     */
    val action: Action = reader.readAction()

    override fun writePayload(writer: PacketWriter) {
        writer.unsupported()
    }

    private companion object {
        const val REMOVE: Byte = 0x00
        const val CREATE: Byte = 0x02
        const val REMOVE_ALL: Byte = 0x64

        fun PacketReader.readAction(): Action = when (val actionValue = readByte()) {
            REMOVE -> Action.Remove(readString())
            CREATE -> Action.Create(readString())
            REMOVE_ALL -> Action.RemoveAll
            else -> throw ArtemisPacketException("Invalid action: ${actionValue.toHex()}")
        }
    }
}
