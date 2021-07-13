package com.walkertribe.ian.iface

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.protocol.ArtemisPacket
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.BytePacketBuilder
import io.ktor.utils.io.core.writeIntLittleEndian
import io.ktor.utils.io.writeIntLittleEndian

/**
 * Facilitates writing packets to an OutputStream. This object may be reused to
 * write as many packets as desired to a single OutputStream. To write a packet,
 * follow these steps:
 *  1. Invoke start().
 *  1. Write the payload data using the write*() methods. Payload data is
 * buffered by the PacketWriter, not written immediately to the
 * OutputStream.
 *  1. Invoke flush(). The proper values for the fields in the preamble will be
 * automatically computed and written, followed by the payload. The entire
 * packet is then flushed to the OutputStream.
 *
 * Once flush() has been called, you can start writing another packet by
 * invoking start() again.
 *
 * @author rjwut
 */
class PacketWriter(private val outputChannel: ByteWriteChannel) {
    private val builder: BytePacketBuilder by lazy { BytePacketBuilder() }

    /**
     * Starts a packet of the given type.
     */
    fun start(packetType: Int): PacketWriter = apply {
        check(builder.isEmpty) { "Packet was already started" }
        builder.writeIntLittleEndian(packetType)
    }

    /**
     * Writes an int (four bytes). You must invoke [start] before calling this
     * method.
     */
    fun writeInt(v: Int): PacketWriter = whileStarted { builder.writeIntLittleEndian(v) }

    /**
     * Writes an Enum value as an int. You must invoke [start] before calling
     * this method.
     */
    fun <E : Enum<E>> writeEnumAsInt(v: E): PacketWriter = writeInt(v.ordinal)

    /**
     * Writes a float (four bytes). You must invoke start() before calling this
     * method.
     */
    fun writeFloat(v: Float): PacketWriter = writeInt(v.toRawBits())

    /**
     * Writes the completed packet to the OutputStream. You must invoke start()
     * before calling this method. When this method returns, you will have to
     * call start() again before you can write more data.
     */
    suspend fun flush() = whileStarted {
        val payloadSize = builder.size

        outputChannel.writeIntLittleEndian(ArtemisPacket.HEADER)
        outputChannel.writeIntLittleEndian(
            payloadSize + ArtemisPacket.PREAMBLE_SIZE - Int.SIZE_BYTES
        )
        outputChannel.writeIntLittleEndian(Origin.CLIENT.toInt())
        outputChannel.writeIntLittleEndian(0)
        outputChannel.writeIntLittleEndian(payloadSize)
        outputChannel.writePacket(builder.build())
        outputChannel.flush()
    }

    fun unsupported() {
        throw UnsupportedOperationException(UNSUPPORTED)
    }

    fun close(cause: Throwable? = null) {
        builder.close()
        outputChannel.close(cause)
    }

    /**
     * Throws an IllegalStateException if start() has not been called since the
     * time this object was constructed or since the last call to flush().
     * Otherwise, runs the given block of code on the currently opened output stream.
     */
    private inline fun whileStarted(block: PacketWriter.() -> Unit): PacketWriter = apply {
        check(builder.isNotEmpty) { "Must invoke start() first" }
        block()
    }

    private companion object {
        const val UNSUPPORTED = "Client cannot send packets that should originate from server"
    }
}
