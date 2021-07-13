package com.walkertribe.ian.protocol

import com.walkertribe.ian.iface.PacketReader
import kotlin.reflect.KClass

/**
 * Interface for objects which can convert a byte array to a packet.
 * @author rjwut
 */
interface PacketFactory<T : ArtemisPacket> {
    /**
     * Returns the class of ArtemisPacket that this PacketFactory can produce.
     * Note: It is legal to have more than one factory producing the same
     * Class.
     */
    val factoryClass: KClass<T>

    /**
     * Returns a packet constructed with a payload read from the given
     * PacketReader. (It is assumed that the preamble has already been read.)
     * This method should throw an ArtemisPacketException if the payload is
     * malformed.
     */
    @Throws(ArtemisPacketException::class)
    fun build(reader: PacketReader): T
}
