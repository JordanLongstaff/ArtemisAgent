package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.world.Artemis

sealed class UnobservedObjectParser(objectType: ObjectType) : AbstractObjectParser(objectType) {
    data object Engineering : UnobservedObjectParser(ObjectType.ENGINEERING_CONSOLE) {
        override fun getByteCounts(version: Version): IntArray = byteCountSetup(
            4 to Artemis.SYSTEM_COUNT * 2,
            1 to Artemis.SYSTEM_COUNT,
        )
    }

    data object Anomaly : UnobservedObjectParser(ObjectType.ANOMALY) {
        override fun getByteCounts(version: Version): IntArray = byteCountSetup(
            4 to 6,
            1 to if (version < Version.BEACON) 0 else 2,
        )
    }

    data object Nebula : UnobservedObjectParser(ObjectType.NEBULA) {
        override fun getByteCounts(version: Version): IntArray = byteCountSetup(
            4 to 6,
            1 to if (version < Version.NEBULA_TYPES) 0 else 1,
        )
    }

    data object Torpedo : UnobservedObjectParser(ObjectType.TORPEDO) {
        override fun getByteCounts(version: Version): IntArray = byteCountSetup(
            4 to 8,
        )
    }

    data object Asteroid : UnobservedObjectParser(ObjectType.ASTEROID) {
        override fun getByteCounts(version: Version): IntArray = byteCountSetup(
            4 to 3,
        )
    }

    data object GenericMesh : UnobservedObjectParser(ObjectType.GENERIC_MESH) {
        override fun getByteCounts(version: Version): IntArray = byteCountSetup(
            4 to 12,
            S to 3,
            4 to 1,
            1 to 1,
            4 to 6,
            1 to 1,
            S to 2,
            4 to if (version < Version.NEBULA_TYPES) 0 else 1,
        )
    }

    data object Drone : UnobservedObjectParser(ObjectType.DRONE) {
        override fun getByteCounts(version: Version): IntArray = byteCountSetup(
            4 to 9,
        )
    }

    private lateinit var byteCounts: IntArray

    abstract fun getByteCounts(version: Version): IntArray

    override fun parseDsl(reader: PacketReader) {
        byteCounts.forEachIndexed { bitIndex, byteCount ->
            if (byteCount == S) {
                reader.readString(bitIndex)
            } else {
                reader.readBytes(bitIndex, byteCount)
            }
        }
    }

    override fun getBitCount(version: Version): Int = getByteCounts(version).let {
        byteCounts = it
        it.size
    }

    private companion object {
        const val S = -1

        fun byteCountSetup(vararg buildPairs: Pair<Int, Int>): IntArray {
            val byteCounts = IntArray(buildPairs.sumOf { it.second })

            var position = 0
            for ((bytes, length) in buildPairs) {
                val end = position + length
                byteCounts.fill(bytes, position, end)
                position = end
            }

            return byteCounts
        }
    }
}
