package com.walkertribe.ian.world

/** Base implementation for ships (player or NPC). */
abstract class BaseArtemisShip<T : BaseArtemisShip<T>>(id: Int, timestamp: Long) :
    BaseArtemisShielded<T>(id, timestamp) {
    /** The aft shields. */
    val shieldsRear = Shields(timestamp)

    /**
     * Impulse setting, as a value from 0 (all stop) and 1 (full impulse). Unspecified: Float.NaN
     */
    val impulse = Property.FloatProperty(timestamp)

    /**
     * The side the ship is on. Ships on the same side are friendly to one another. Unspecified: -1
     */
    val side = Property.ByteProperty(timestamp)

    /** Returns true if this object contains any data. */
    override val hasData
        get() = super.hasData || shieldsRear.hasData || impulse.hasValue || side.hasValue

    override fun updates(other: T) {
        shieldsRear updates other.shieldsRear
        impulse updates other.impulse
        side updates other.side

        super.updates(other)
    }

    abstract class Dsl<T : BaseArtemisShip<T>> : BaseArtemisShielded.Dsl<T>() {
        var shieldsRear: Float = Float.NaN
        var shieldsRearMax: Float = Float.NaN
        var impulse: Float = Float.NaN
        var side: Byte = -1

        override fun updates(obj: T) {
            super.updates(obj)

            obj.shieldsRear.strength.value = shieldsRear
            obj.shieldsRear.maxStrength.value = shieldsRearMax
            obj.impulse.value = impulse
            obj.side.value = side

            shieldsRear = Float.NaN
            shieldsRearMax = Float.NaN
            impulse = Float.NaN
            side = -1
        }
    }
}
