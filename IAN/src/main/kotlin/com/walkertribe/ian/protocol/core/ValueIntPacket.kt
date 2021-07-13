package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.protocol.ArtemisPacketException
import com.walkertribe.ian.protocol.BaseArtemisPacket
import com.walkertribe.ian.protocol.Packet
import kotlin.reflect.full.findAnnotation

/**
 * A superclass for handling VALUE_INT client packets. Note that some packets
 * in the Artemis protocol technically have the valueInt type, but don't
 * actually follow the pattern of having a single int value. It may be that the
 * packets in question evolved over time and needed more values. Those packets
 * do not extend ValueIntPacket but are still mentioned in the SubType class.
 * @author rjwut
 */
abstract class ValueIntPacket(protected val argument: Int) : BaseArtemisPacket() {
    /**
     * VALUE_INT client packet subtypes.
     */
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

    constructor() : this(0) {
        shouldWriteArgument = false
    }

    private val subtype: Byte = this::class.findAnnotation<Packet>()?.run {
        subtype.also {
            if (it < 0) {
                throw ArtemisPacketException("@Packet annotation must have a subtype")
            }
        }
    } ?: throw ArtemisPacketException("$javaClass must have a @Packet annotation")

    override fun writePayload(writer: PacketWriter) {
        writer.writeInt(subtype.toInt())
        if (shouldWriteArgument) {
            writer.writeInt(argument)
        }
    }
}
