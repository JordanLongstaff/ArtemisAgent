package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.world.ArtemisObject

/**
 * Interface for classes which can parse objects from the payload of an ObjectUpdatePacket.
 *
 * @author rjwut
 */
interface ObjectParser<T : ArtemisObject<T>> {
    /**
     * Returns the number of bits in the bit field representing the available properties for this
     * type of object. If this value is greater than zero, the object will start with a BitField
     * which describes which of these properties are present.
     */
    fun getBitCount(version: Version): Int

    /** Reads and returns an object from the payload. */
    fun parse(reader: PacketReader, timestamp: Long): T?
}
