package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.util.BoolState
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.world.ArtemisPlayer

/**
 * ObjectParser implementation for player ship upgrade updates
 *
 * @author rjwut
 */
object UpgradesParser : AbstractObjectParser<ArtemisPlayer>(ObjectType.UPGRADES) {
    private const val DOUBLE_AGENT_INDEX = 8
    private const val FOLLOWING_UPGRADES = 19
    private const val TOTAL_BITS = 3 * (DOUBLE_AGENT_INDEX + 1 + FOLLOWING_UPGRADES)

    private val ALL_FIELDS = arrayOf(Field.ActiveState, Field.Count, Field.TimeRemaining)

    /** Represents the three fields about an upgrade in a player ship. */
    private sealed interface Field<V> {
        data object ActiveState : Field<BoolState> {
            override fun read(reader: PacketReader, bitIndex: Int, updateDsl: Boolean) {
                val active = reader.readBool(bitIndex, 1)
                if (updateDsl) {
                    ArtemisPlayer.Dsl.Upgrades.doubleAgentActive = active
                }
            }
        }

        data object Count : Field<Byte> {
            override fun read(reader: PacketReader, bitIndex: Int, updateDsl: Boolean) {
                val count = reader.readByte(bitIndex)
                if (updateDsl) {
                    ArtemisPlayer.Dsl.Upgrades.doubleAgentCount = count
                }
            }
        }

        data object TimeRemaining : Field<Int> {
            override fun read(reader: PacketReader, bitIndex: Int, updateDsl: Boolean) {
                val seconds = reader.readShort(bitIndex, -1)
                if (updateDsl) {
                    ArtemisPlayer.Dsl.Upgrades.doubleAgentSecondsLeft = seconds
                }
            }
        }

        fun read(reader: PacketReader, bitIndex: Int, updateDsl: Boolean)
    }

    override fun parseDsl(reader: PacketReader) =
        ArtemisPlayer.Dsl.Upgrades.apply {
            var bitIndex = 0

            ALL_FIELDS.forEach { field ->
                repeat(DOUBLE_AGENT_INDEX) { field.read(reader, bitIndex++, false) }
                field.read(reader, bitIndex++, true)
                repeat(FOLLOWING_UPGRADES) { field.read(reader, bitIndex++, false) }
            }
        }

    override fun getBitCount(version: Version): Int = TOTAL_BITS
}
