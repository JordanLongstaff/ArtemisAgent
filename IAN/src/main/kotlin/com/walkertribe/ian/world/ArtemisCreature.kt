package com.walkertribe.ian.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.util.BoolState

/**
 * Various spacefaring creatures (and wrecks)
 */
class ArtemisCreature(id: Int, timestamp: Long) : BaseArtemisObject(id, timestamp) {
    override val type: ObjectType = ObjectType.CREATURE

    /**
     * Returns whether this creature is a typhon.
     */
    val isNotTyphon = Property.BoolProperty(timestamp)

    override val hasData: Boolean get() = super.hasData || isNotTyphon.hasValue

    override fun checkType(other: ArtemisObject) {
        require(other is ArtemisCreature) { "Creature can only update other creatures" }
    }

    override fun updates(other: ArtemisObject) {
        super.updates(other)
        if (other is ArtemisCreature) {
            updateProp(other, ArtemisCreature::isNotTyphon)
        }
    }

    internal object Dsl : BaseArtemisObject.Dsl<ArtemisCreature>(ArtemisCreature::class) {
        var isNotTyphon: BoolState = BoolState.Unknown

        override fun updates(obj: ArtemisCreature) {
            super.updates(obj)
            obj.isNotTyphon.value = isNotTyphon
            isNotTyphon = BoolState.Unknown
        }
    }
}
