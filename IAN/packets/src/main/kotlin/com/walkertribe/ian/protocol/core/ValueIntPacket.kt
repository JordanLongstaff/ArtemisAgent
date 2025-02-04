package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.Packet

/**
 * A superclass for handling `valueInt` client packets. Note that some packets in the Artemis
 * protocol technically have the `valueInt` type, but don't actually follow the pattern of having a
 * single integer value. It may be that the packets in question evolved over time and needed more
 * values. Those packets still extend [ValueIntPacket] but no argument is written to the
 * [PacketWriter].
 *
 * @author rjwut
 */
abstract class ValueIntPacket(private val subtype: Byte, private val argument: Int) :
    Packet.Client(CorePacketType.VALUE_INT) {
    /** VALUE_INT client packet subtypes. */
    object Subtype {
        const val TOGGLE_RED_ALERT: Byte = 0x0a
        const val SET_SHIP: Byte = 0x0d
        const val SET_CONSOLE: Byte = 0x0e
        const val READY: Byte = 0x0f
        const val BUTTON_CLICK: Byte = 0x15
        const val ACTIVATE_UPGRADE_OLD: Byte = 0x1b
        const val ACTIVATE_UPGRADE_CURRENT: Byte = 0x1c
        const val CLIENT_HEARTBEAT: Byte = 0x24
    }

    private var shouldWriteArgument: Boolean = true

    constructor(subtype: Byte) : this(subtype, 0) {
        shouldWriteArgument = false
    }

    override fun writePayload(writer: PacketWriter) {
        writer.writeInt(subtype.toInt())
        if (shouldWriteArgument) {
            writer.writeInt(argument)
        }
    }
}
