package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.world.BaseArtemisObject

/**
 * Abstract implementation of ObjectParser interface. Provides the common object parsing behavior
 * and delegates to the subclass's parseImpl() method to read individual properties.
 *
 * @author rjwut
 */
abstract class AbstractObjectParser<T : BaseArtemisObject<T>>
protected constructor(internal val objectType: ObjectType) : ObjectParser<T> {
    internal abstract fun parseDsl(reader: PacketReader): BaseArtemisObject.Dsl<T>?

    override fun parse(reader: PacketReader, timestamp: Long): T? {
        reader.skip(1) // type
        reader.startObject(getBitCount(reader.version))
        return parseDsl(reader)
            ?.takeIf { reader.isAcceptingCurrentObject }
            ?.build(reader.objectId, timestamp)
    }
}
