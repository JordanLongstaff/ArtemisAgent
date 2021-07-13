package com.walkertribe.ian.world

import com.walkertribe.ian.enums.ObjectType

/**
 * Bases
 */
class ArtemisBase(id: Int, timestamp: Long) : BaseArtemisShielded(id, timestamp) {
    override val type: ObjectType = ObjectType.BASE

    override fun checkType(other: ArtemisObject) {
        require(other is ArtemisBase) { "Base can only update other bases" }
    }

    internal object Dsl : BaseArtemisShielded.Dsl<ArtemisBase>(ArtemisBase::class)
}
