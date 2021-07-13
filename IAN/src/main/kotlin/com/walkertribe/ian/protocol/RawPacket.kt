package com.walkertribe.ian.protocol

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketWriter

/**
 * Any packet that IAN has not parsed. This may be because it was not recognized
 * by any registered protocol, or because there are no registered packet
 * listeners that are interested in it.
 * @author rjwut
 */
sealed class RawPacket(
    final override val origin: Origin,
    final override val type: Int,

    /**
     * Returns the payload for this packet.
     */
    val payload: ByteArray
) : ArtemisPacket {
    /**
     * Any packet received that isn't of a type recognized by a registered protocol
     * will be returned as this class. In most cases, you won't be interested in
     * these: they're mainly intended for reverse-engineering of the protocol and
     * debugging.
     */
    class Unknown(origin: Origin, type: Int, payload: ByteArray) :
        RawPacket(origin, type, payload)

    /**
     * Any packet received for which no packet listeners have been registered will
     * be returned as this class.
     */
    class Unparsed(origin: Origin, type: Int, payload: ByteArray) :
        RawPacket(origin, type, payload)

    final override val timestamp: Long = System.currentTimeMillis()

    override suspend fun writeTo(writer: PacketWriter) {
        writer.unsupported()
    }
}
