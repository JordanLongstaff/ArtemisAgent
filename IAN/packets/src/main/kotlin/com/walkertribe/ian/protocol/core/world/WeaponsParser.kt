package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.enums.TubeState
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.util.Bit
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.world.Artemis
import com.walkertribe.ian.world.ArtemisPlayer

object WeaponsParser : AbstractObjectParser<ArtemisPlayer>(ObjectType.WEAPONS_CONSOLE) {
    data class OrdnanceCountBit(val ordnanceType: OrdnanceType) : Bit {
        override fun getIndex(version: Version): Int =
            if (ordnanceType existsIn version) ordnanceType.ordinal else -1
    }

    object UnknownBit : Bit {
        override fun getIndex(version: Version): Int {
            val countOrdnanceTypes = OrdnanceType.countForVersion(version)
            return if (countOrdnanceTypes < OrdnanceType.size) countOrdnanceTypes else -1
        }
    }

    data class TubeTimeBit(val index: Int) : Bit {
        override fun getIndex(version: Version): Int {
            val countOrdnanceTypes = OrdnanceType.countForVersion(version)
            return countOrdnanceTypes + index +
                if (countOrdnanceTypes < OrdnanceType.size) 1 else 0
        }
    }

    data class TubeStateBit(val index: Int) : Bit {
        override fun getIndex(version: Version): Int {
            val countOrdnanceTypes = OrdnanceType.countForVersion(version)
            return countOrdnanceTypes + Artemis.MAX_TUBES + index +
                if (countOrdnanceTypes < OrdnanceType.size) 1 else 0
        }
    }

    data class TubeContentsBit(val index: Int) : Bit {
        override fun getIndex(version: Version): Int {
            val countOrdnanceTypes = OrdnanceType.countForVersion(version)
            return countOrdnanceTypes + Artemis.MAX_TUBES * 2 + index +
                if (countOrdnanceTypes < OrdnanceType.size) 1 else 0
        }
    }

    private val ALL_BITS =
        OrdnanceType.entries.map(WeaponsParser::OrdnanceCountBit) + UnknownBit +
            0.until(Artemis.MAX_TUBES).flatMap {
                listOf(TubeTimeBit(it), TubeStateBit(it), TubeContentsBit(it))
            }

    override fun parseDsl(reader: PacketReader) = ArtemisPlayer.Dsl.Weapons.apply {
        OrdnanceType.entries.forEach {
            ordnanceCounts[it] = reader.readByte(OrdnanceCountBit(it))
        }

        reader.readByte(UnknownBit)

        repeat(Artemis.MAX_TUBES) {
            reader.readFloat(TubeTimeBit(it))
        }

        repeat(Artemis.MAX_TUBES) {
            tubeStates[it] = reader.readByteAsEnum<TubeState>(TubeStateBit(it))
        }

        repeat(Artemis.MAX_TUBES) {
            val contentsBit = TubeContentsBit(it)
            when (tubeStates[it]) {
                null, TubeState.UNLOADED -> reader.readByte(contentsBit)
                else -> tubeContents[it] = reader.readByteAsEnum<OrdnanceType>(contentsBit)
            }
        }
    }

    override fun getBitCount(version: Version): Int = ALL_BITS.count { it.getIndex(version) >= 0 }
}
