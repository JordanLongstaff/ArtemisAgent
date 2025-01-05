package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.world.Artemis

sealed class UnobservedObjectParser(objectType: ObjectType) :
    AbstractObjectParser<Nothing>(objectType) {
    data object Engineering : UnobservedObjectParser(ObjectType.ENGINEERING_CONSOLE) {
        override fun getByteCounts(version: Version): IntArray =
            byteCountSetup(
                Int.SIZE_BYTES to Artemis.SYSTEM_COUNT * 2,
                Byte.SIZE_BYTES to Artemis.SYSTEM_COUNT,
            )
    }

    data object Anomaly : UnobservedObjectParser(ObjectType.ANOMALY) {
        private const val OLD_BITS_COUNT = 6
        private const val NEW_BITS_COUNT = 2

        override fun getByteCounts(version: Version): IntArray =
            byteCountSetup(
                Int.SIZE_BYTES to OLD_BITS_COUNT,
                Byte.SIZE_BYTES to if (version < Version.BEACON) 0 else NEW_BITS_COUNT,
            )
    }

    data object Nebula : UnobservedObjectParser(ObjectType.NEBULA) {
        private const val OLD_BITS_COUNT = 6

        override fun getByteCounts(version: Version): IntArray =
            byteCountSetup(
                Float.SIZE_BYTES to OLD_BITS_COUNT,
                Byte.SIZE_BYTES to if (version < Version.NEBULA_TYPES) 0 else 1,
            )
    }

    data object Torpedo : UnobservedObjectParser(ObjectType.TORPEDO) {
        private const val BITS_COUNT = 8

        override fun getByteCounts(version: Version): IntArray =
            byteCountSetup(Int.SIZE_BYTES to BITS_COUNT)
    }

    data object Asteroid : UnobservedObjectParser(ObjectType.ASTEROID) {
        private const val BITS_COUNT = 3

        override fun getByteCounts(version: Version): IntArray =
            byteCountSetup(Float.SIZE_BYTES to BITS_COUNT)
    }

    data object GenericMesh : UnobservedObjectParser(ObjectType.GENERIC_MESH) {
        private const val POSITION_BITS_COUNT = 12
        private const val IDENTITY_BITS_COUNT = 3
        private const val FLOAT_BITS_COUNT = 6
        private const val UNKNOWN_BITS_COUNT = 2

        override fun getByteCounts(version: Version): IntArray =
            byteCountSetup(
                Float.SIZE_BYTES to POSITION_BITS_COUNT,
                S to IDENTITY_BITS_COUNT,
                Float.SIZE_BYTES to 1,
                Byte.SIZE_BYTES to 1,
                Float.SIZE_BYTES to FLOAT_BITS_COUNT,
                1 to 1,
                S to UNKNOWN_BITS_COUNT,
                Int.SIZE_BYTES to if (version < Version.NEBULA_TYPES) 0 else 1,
            )
    }

    data object Drone : UnobservedObjectParser(ObjectType.DRONE) {
        private const val BITS_COUNT = 9

        override fun getByteCounts(version: Version): IntArray =
            byteCountSetup(Float.SIZE_BYTES to BITS_COUNT)
    }

    private lateinit var byteCounts: IntArray

    abstract fun getByteCounts(version: Version): IntArray

    final override fun parseDsl(reader: PacketReader): Nothing? {
        byteCounts.forEachIndexed { bitIndex, byteCount ->
            if (byteCount == S) {
                reader.readString(bitIndex)
            } else {
                reader.readBytes(bitIndex, byteCount)
            }
        }
        return null
    }

    override fun getBitCount(version: Version): Int =
        getByteCounts(version).let {
            byteCounts = it
            it.size
        }

    private companion object {
        private const val S = -1

        private fun byteCountSetup(vararg buildPairs: Pair<Int, Int>): IntArray {
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
