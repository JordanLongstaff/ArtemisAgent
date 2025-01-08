package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.util.Bit
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.world.Artemis
import com.walkertribe.ian.world.ArtemisNpc

/**
 * ObjectParser implementation for NPC ships
 *
 * @author rjwut
 */
object NpcShipParser : AbstractObjectParser<ArtemisNpc>(ObjectType.NPC_SHIP) {
    private const val FREQUENCY_COUNT = 5

    private enum class NpcBit : Bit {
        NAME,
        IMPULSE,
        RUDDER,
        MAX_IMPULSE,
        MAX_TURN_RATE,
        IS_ENEMY,
        SHIP_TYPE,
        X,
        Y,
        Z,
        PITCH,
        ROLL,
        HEADING,
        VELOCITY,
        SURRENDERED,
        IN_NEBULA,
        FORE_SHIELD,
        FORE_SHIELD_MAX,
        AFT_SHIELD,
        AFT_SHIELD_MAX,
        UNK_3_5,
        FLEET_NUMBER,
        SPECIAL_ABILITIES,
        SPECIAL_STATE,
        SINGLE_SCAN,
        DOUBLE_SCAN,
        VISIBILITY,
        SIDE,
        UNK_4_5,
        UNK_4_6,
        UNK_4_7,
        TARGET_X,
        TARGET_Y,
        TARGET_Z,
        TAGGED {
            override fun getIndex(version: Version): Int =
                if (version < Version.BEACON) -1 else ordinal
        },
        UNK_5_4 {
            override fun getIndex(version: Version): Int =
                if (version < Version.BEACON) -1 else ordinal
        };

        override fun getIndex(version: Version): Int = ordinal
    }

    override fun parseDsl(reader: PacketReader) =
        ArtemisNpc.Dsl.apply {
            name = reader.readString(NpcBit.NAME)
            impulse = reader.readFloat(NpcBit.IMPULSE)

            reader.readFloat(NpcBit.RUDDER)
            reader.readFloat(NpcBit.MAX_IMPULSE)
            reader.readFloat(NpcBit.MAX_TURN_RATE)

            isEnemy = reader.readBool(NpcBit.IS_ENEMY, Int.SIZE_BYTES)
            hullId = reader.readInt(NpcBit.SHIP_TYPE, -1)
            x = reader.readFloat(NpcBit.X)
            y = reader.readFloat(NpcBit.Y)
            z = reader.readFloat(NpcBit.Z)

            reader.readFloat(NpcBit.PITCH)
            reader.readFloat(NpcBit.ROLL)
            reader.readFloat(NpcBit.HEADING)
            reader.readFloat(NpcBit.VELOCITY)

            isSurrendered = reader.readBool(NpcBit.SURRENDERED, 1)
            isInNebula =
                reader.readBool(
                    NpcBit.IN_NEBULA,
                    if (reader.version < Version.NEBULA_TYPES) 2 else 1,
                )

            shieldsFront = reader.readFloat(NpcBit.FORE_SHIELD)
            shieldsFrontMax = reader.readFloat(NpcBit.FORE_SHIELD_MAX)
            shieldsRear = reader.readFloat(NpcBit.AFT_SHIELD)
            shieldsRearMax = reader.readFloat(NpcBit.AFT_SHIELD_MAX)

            reader.readBool(NpcBit.UNK_3_5, 2)
            reader.readByte(NpcBit.FLEET_NUMBER)
            reader.readInt(NpcBit.SPECIAL_ABILITIES, -1)
            reader.readInt(NpcBit.SPECIAL_STATE, -1)

            if (reader.has(NpcBit.SINGLE_SCAN)) {
                scanBits = reader.readInt()
            }

            reader.readInt(NpcBit.DOUBLE_SCAN, -1)
            reader.readInt(NpcBit.VISIBILITY, 0)

            side = reader.readByte(NpcBit.SIDE)

            reader.readBool(NpcBit.UNK_4_5, 1)
            reader.readBool(NpcBit.UNK_4_6, 1)
            reader.readBool(NpcBit.UNK_4_7, 1)

            reader.readFloat(NpcBit.TARGET_X)
            reader.readFloat(NpcBit.TARGET_Y)
            reader.readFloat(NpcBit.TARGET_Z)

            reader.readBool(NpcBit.TAGGED, 1)
            reader.readBool(NpcBit.UNK_5_4, 1)

            val bitCount = getBitCount(reader.version)
            val start = bitCount - Artemis.SYSTEM_COUNT - FREQUENCY_COUNT
            for (i in start until bitCount) {
                reader.readFloat(i)
            }
        }

    override fun getBitCount(version: Version): Int =
        NpcBit.entries.count { it.getIndex(version) >= 0 } + Artemis.SYSTEM_COUNT + FREQUENCY_COUNT
}
