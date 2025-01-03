package com.walkertribe.ian.world

import com.walkertribe.ian.enums.ObjectType

/**
 * Mines
 * @author rjwut
 */
class ArtemisMine(id: Int, timestamp: Long) : BaseArtemisObject<ArtemisMine>(id, timestamp) {
    override val type: ObjectType = ObjectType.MINE

    object Dsl : BaseArtemisObject.Dsl<ArtemisMine>() {
        override fun create(id: Int, timestamp: Long) = ArtemisMine(id, timestamp)
    }
}
