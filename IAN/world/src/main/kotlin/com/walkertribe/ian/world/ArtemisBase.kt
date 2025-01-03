package com.walkertribe.ian.world

import com.walkertribe.ian.enums.ObjectType

/**
 * Bases
 */
class ArtemisBase(id: Int, timestamp: Long) : BaseArtemisShielded<ArtemisBase>(id, timestamp) {
    override val type: ObjectType = ObjectType.BASE

    object Dsl : BaseArtemisShielded.Dsl<ArtemisBase>() {
        override fun create(id: Int, timestamp: Long) = ArtemisBase(id, timestamp)
    }
}
