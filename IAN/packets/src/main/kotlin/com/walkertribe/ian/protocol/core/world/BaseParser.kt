package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.util.Bit
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.world.ArtemisBase

/**
 * ObjectParser implementation for bases
 *
 * @author rjwut
 */
object BaseParser : AbstractObjectParser<ArtemisBase>(ObjectType.BASE) {
    private enum class BaseBit : Bit {
        NAME,
        SHIELDS,
        MAX_SHIELDS,
        UNK_1_4,
        HULL_ID,
        X,
        Y,
        Z,
        PITCH,
        ROLL,
        HEADING,
        UNK_2_4,
        UNK_2_5,
        SIDE;

        override fun getIndex(version: Version): Int = ordinal
    }

    override fun parseDsl(reader: PacketReader) =
        ArtemisBase.Dsl.apply {
            name = reader.readString(BaseBit.NAME)
            shieldsFront = reader.readFloat(BaseBit.SHIELDS)
            shieldsFrontMax = reader.readFloat(BaseBit.MAX_SHIELDS)
            reader.readBool(BaseBit.UNK_1_4, Int.SIZE_BYTES)
            hullId = reader.readInt(BaseBit.HULL_ID, -1)
            x = reader.readFloat(BaseBit.X)
            y = reader.readFloat(BaseBit.Y)
            z = reader.readFloat(BaseBit.Z)
            reader.readFloat(BaseBit.PITCH)
            reader.readFloat(BaseBit.ROLL)
            reader.readFloat(BaseBit.HEADING)
            reader.readBool(BaseBit.UNK_2_4, Int.SIZE_BYTES)
            reader.readBool(BaseBit.UNK_2_5, 1)
            reader.readByte(BaseBit.SIDE)
        }

    override fun getBitCount(version: Version): Int = BaseBit.entries.size
}
