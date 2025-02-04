package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.util.Bit
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.world.ArtemisMine

/**
 * ObjectParser implementation for mines
 *
 * @author rjwut
 */
object MineParser : AbstractObjectParser<ArtemisMine>(ObjectType.MINE) {
    private enum class MineBit : Bit {
        X,
        Y,
        Z;

        override fun getIndex(version: Version): Int = ordinal
    }

    override fun parseDsl(reader: PacketReader) =
        ArtemisMine.Dsl.apply {
            x = reader.readFloat(MineBit.X)
            y = reader.readFloat(MineBit.Y)
            z = reader.readFloat(MineBit.Z)
        }

    override fun getBitCount(version: Version): Int = MineBit.entries.size
}
