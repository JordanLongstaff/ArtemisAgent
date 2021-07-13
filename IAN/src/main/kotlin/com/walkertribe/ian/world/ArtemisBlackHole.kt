package com.walkertribe.ian.world

import com.walkertribe.ian.enums.ObjectType

/**
 * Black holes
 * @author rjwut
 */
class ArtemisBlackHole(id: Int, timestamp: Long) : BaseArtemisObject(id, timestamp) {
    override val type: ObjectType = ObjectType.BLACK_HOLE

    override fun checkType(other: ArtemisObject) {
        require(other is ArtemisBlackHole) { "Black hole can only update other black holes" }
    }

    internal object Dsl : BaseArtemisObject.Dsl<ArtemisBlackHole>(ArtemisBlackHole::class)
}
