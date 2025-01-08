package com.walkertribe.ian.protocol

/**
 * Interface for classes which provide support for a set of packets.
 *
 * @author rjwut
 */
interface Protocol {
    /**
     * If this [Protocol] supports packets with the given type (and optional subtype), returns a
     * [PacketFactory] that can parse it; otherwise returns null.
     */
    fun getFactory(type: Int, subtype: Byte): PacketFactory<out Packet.Server>?
}
