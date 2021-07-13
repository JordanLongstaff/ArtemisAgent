package com.walkertribe.ian.world

import kotlin.reflect.KClass

/**
 * Base implementation for ships (player or NPC).
 */
abstract class BaseArtemisShip(id: Int, timestamp: Long) : BaseArtemisShielded(id, timestamp) {
    /**
     * The strength of the aft shields.
     * Unspecified: Float.NaN
     */
    val shieldsRear = Property.FloatProperty(timestamp)

    /**
     * The maximum strength of the aft shields.
     * Unspecified: Float.NaN
     */
    val shieldsRearMax = Property.FloatProperty(timestamp)

    /**
     * Impulse setting, as a value from 0 (all stop) and 1 (full impulse).
     * Unspecified: Float.NaN
     */
    val impulse = Property.FloatProperty(timestamp)

    /**
     * The side the ship is on. Ships on the same side are friendly to one another.
     * Unspecified: -1
     */
    val side = Property.ByteProperty(timestamp)

    override fun updates(other: ArtemisObject) {
        if (other is BaseArtemisShip) {
            updateProp(other, BaseArtemisShip::shieldsRear)
            updateProp(other, BaseArtemisShip::shieldsRearMax)
            updateProp(other, BaseArtemisShip::impulse)
            updateProp(other, BaseArtemisShip::side)
        }
        super.updates(other)
    }

    /**
     * Returns true if this object contains any data.
     */
    override val hasData get() =
        super.hasData ||
            shieldsRear.hasValue ||
            shieldsRearMax.hasValue ||
            impulse.hasValue ||
            side.hasValue

    internal open class Dsl<T : BaseArtemisShip>(
        objectClass: KClass<T>
    ) : BaseArtemisShielded.Dsl<T>(objectClass) {
        var shieldsRear: Float = Float.NaN
        var shieldsRearMax: Float = Float.NaN
        var impulse: Float = Float.NaN
        var side: Byte = -1

        override fun updates(obj: T) {
            super.updates(obj)

            obj.shieldsRear.value = shieldsRear
            obj.shieldsRearMax.value = shieldsRearMax
            obj.impulse.value = impulse
            obj.side.value = side

            shieldsRear = Float.NaN
            shieldsRearMax = Float.NaN
            impulse = Float.NaN
            side = -1
        }
    }
}
