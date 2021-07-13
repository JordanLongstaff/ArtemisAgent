package com.walkertribe.ian.protocol

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketWriter
import kotlin.reflect.full.findAnnotation

/**
 * Implements common packet functionality.
 */
abstract class BaseArtemisPacket : ArtemisPacket {
    /**
     * Causes the packet's payload to be written to the given PacketWriter.
     */
    protected abstract fun writePayload(writer: PacketWriter)

    final override val origin: Origin
    final override val type: Int
    final override val timestamp: Long = System.currentTimeMillis()

    override suspend fun writeTo(writer: PacketWriter) {
        writer.start(type)
        writePayload(writer)
    }

    init {
        val anno = this::class.findAnnotation<Packet>()
            ?: throw ArtemisPacketException("$javaClass must have a @Packet annotation")
        origin = anno.origin
        type = anno.getHash()
    }
}
