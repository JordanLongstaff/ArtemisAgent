package com.walkertribe.ian.protocol

import com.walkertribe.ian.iface.PacketReader
import kotlin.reflect.KClass

/**
 * Interface for objects which can convert a byte array to a packet.
 *
 * @author rjwut
 */
interface PacketFactory<T : Packet.Server> {
    /**
     * Returns the class of [Packet] that this [PacketFactory] can produce. Note: It is legal to
     * have more than one factory producing the same class.
     */
    val factoryClass: KClass<T>

    /**
     * Returns a packet constructed with a payload read from the given [PacketReader]. (It is
     * assumed that the preamble has already been read.) This method should throw an
     * [PacketException] if the payload is malformed.
     */
    @Throws(PacketException::class) fun build(reader: PacketReader): T
}
