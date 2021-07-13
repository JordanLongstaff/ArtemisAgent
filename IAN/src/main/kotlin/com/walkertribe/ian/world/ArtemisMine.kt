package com.walkertribe.ian.world

import com.walkertribe.ian.enums.ObjectType

/**
 * Mines
 * @author rjwut
 */
class ArtemisMine(id: Int, timestamp: Long) : BaseArtemisObject(id, timestamp) {
    override val type: ObjectType = ObjectType.MINE

    override fun checkType(other: ArtemisObject) {
        require(other is ArtemisMine) { "Mine can only update other mines" }
    }

    internal object Dsl : BaseArtemisObject.Dsl<ArtemisMine>(ArtemisMine::class)
}
