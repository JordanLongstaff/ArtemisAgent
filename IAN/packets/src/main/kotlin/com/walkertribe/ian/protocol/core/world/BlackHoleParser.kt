package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.util.Bit
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.world.ArtemisBlackHole

/**
 * ObjectParser implementation for black holes
 *
 * @author rjwut
 */
object BlackHoleParser : AbstractObjectParser<ArtemisBlackHole>(ObjectType.BLACK_HOLE) {
    private enum class BlackHoleBit : Bit {
        X,
        Y,
        Z;

        override fun getIndex(version: Version): Int = ordinal
    }

    override fun parseDsl(reader: PacketReader) =
        ArtemisBlackHole.Dsl.apply {
            x = reader.readFloat(BlackHoleBit.X)
            y = reader.readFloat(BlackHoleBit.Y)
            z = reader.readFloat(BlackHoleBit.Z)
        }

    override fun getBitCount(version: Version): Int = BlackHoleBit.entries.size
}
