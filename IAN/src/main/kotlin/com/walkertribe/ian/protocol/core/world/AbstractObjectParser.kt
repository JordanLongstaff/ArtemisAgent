package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.world.ArtemisObject

/**
 * Abstract implementation of ObjectParser interface. Provides the common
 * object parsing behavior and delegates to the subclass's parseImpl() method
 * to read individual properties.
 * @author rjwut
 */
abstract class AbstractObjectParser protected constructor(
    internal val objectType: ObjectType
) : ObjectParser {
    protected abstract fun parseDsl(reader: PacketReader)

    override fun parse(reader: PacketReader, timestamp: Long): ArtemisObject? {
        reader.skip(1) // type
        reader.startObject(objectType, getBitCount(reader.version))
        parseDsl(reader)
        return objectType.dsl?.takeIf {
            reader.isAcceptingCurrentObject
        }?.create(reader.objectId, timestamp)
    }
}
