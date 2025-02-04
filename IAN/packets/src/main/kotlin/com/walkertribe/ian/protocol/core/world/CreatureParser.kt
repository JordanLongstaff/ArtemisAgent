package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.util.Bit
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.world.ArtemisCreature

/**
 * ObjectParser implementation for creatures
 *
 * @author rjwut
 */
object CreatureParser : AbstractObjectParser<ArtemisCreature>(ObjectType.CREATURE) {
    private enum class CreatureBit : Bit {
        X,
        Y,
        Z,
        NAME,
        HEADING,
        PITCH,
        ROLL,
        CREATURE_TYPE,
        SCAN,
        UNK_2_2,
        UNK_2_3,
        UNK_2_4,
        UNK_2_5,
        UNK_2_6,
        HEALTH,
        MAX_HEALTH,
        UNK_3_1 {
            override fun getIndex(version: Version): Int =
                if (version < Version.BEACON) -1 else ordinal
        },
        UNK_3_2 {
            override fun getIndex(version: Version): Int =
                if (version < Version.NEBULA_TYPES) -1 else ordinal
        };

        override fun getIndex(version: Version): Int = ordinal
    }

    override fun parseDsl(reader: PacketReader) =
        ArtemisCreature.Dsl.apply {
            x = reader.readFloat(CreatureBit.X)
            y = reader.readFloat(CreatureBit.Y)
            z = reader.readFloat(CreatureBit.Z)
            reader.readString(CreatureBit.NAME)
            reader.readFloat(CreatureBit.HEADING)
            reader.readFloat(CreatureBit.PITCH)
            reader.readFloat(CreatureBit.ROLL)
            isNotTyphon =
                reader.readBool(CreatureBit.CREATURE_TYPE, Int.SIZE_BYTES).also {
                    if (it.booleanValue) {
                        reader.rejectCurrentObject()
                    }
                }
            reader.readInt(CreatureBit.SCAN, 0)
            reader.readBool(CreatureBit.UNK_2_2, Int.SIZE_BYTES)
            reader.readBool(CreatureBit.UNK_2_3, Int.SIZE_BYTES)
            reader.readBool(CreatureBit.UNK_2_4, Int.SIZE_BYTES)
            reader.readBool(CreatureBit.UNK_2_5, Int.SIZE_BYTES)
            reader.readBool(CreatureBit.UNK_2_6, Int.SIZE_BYTES)
            reader.readFloat(CreatureBit.HEALTH)
            reader.readFloat(CreatureBit.MAX_HEALTH)
            reader.readBool(CreatureBit.UNK_3_1, 1)
            reader.readBool(CreatureBit.UNK_3_2, Int.SIZE_BYTES)
        }

    override fun getBitCount(version: Version): Int =
        CreatureBit.entries.count { it.getIndex(version) >= 0 }
}
