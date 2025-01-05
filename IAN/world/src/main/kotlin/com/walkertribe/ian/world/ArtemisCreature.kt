package com.walkertribe.ian.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.util.BoolState

/** Various spacefaring creatures (and wrecks) */
class ArtemisCreature(id: Int, timestamp: Long) :
    BaseArtemisObject<ArtemisCreature>(id, timestamp) {
    override val type: ObjectType = ObjectType.CREATURE

    /** Returns whether this creature is a typhon. */
    val isNotTyphon = Property.BoolProperty(timestamp)

    override val hasData: Boolean
        get() = super.hasData || isNotTyphon.hasValue

    override fun updates(other: ArtemisCreature) {
        super.updates(other)

        isNotTyphon updates other.isNotTyphon
    }

    object Dsl : BaseArtemisObject.Dsl<ArtemisCreature>() {
        var isNotTyphon: BoolState = BoolState.Unknown

        override fun create(id: Int, timestamp: Long) = ArtemisCreature(id, timestamp)

        override fun updates(obj: ArtemisCreature) {
            super.updates(obj)
            obj.isNotTyphon.value = isNotTyphon
            isNotTyphon = BoolState.Unknown
        }
    }
}
