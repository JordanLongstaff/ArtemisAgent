package com.walkertribe.ian.world

import com.walkertribe.ian.enums.ObjectType

/**
 * Black holes
 *
 * @author rjwut
 */
class ArtemisBlackHole(id: Int, timestamp: Long) :
    BaseArtemisObject<ArtemisBlackHole>(id, timestamp) {
    override val type: ObjectType = ObjectType.BLACK_HOLE

    object Dsl : BaseArtemisObject.Dsl<ArtemisBlackHole>() {
        override fun create(id: Int, timestamp: Long) = ArtemisBlackHole(id, timestamp)
    }
}
