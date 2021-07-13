package com.walkertribe.ian.world

import com.walkertribe.ian.vesseldata.Vessel
import com.walkertribe.ian.vesseldata.VesselData
import kotlin.reflect.KClass

/**
 * Base implementation of a shielded world object.
 */
abstract class BaseArtemisShielded(
    id: Int,
    timestamp: Long
) : BaseArtemisObject(id, timestamp), ArtemisShielded {
    override val hullId = Property.IntProperty(timestamp)
    override val shieldsFront = Property.FloatProperty(timestamp)
    override val shieldsFrontMax = Property.FloatProperty(timestamp)
    override val name = Property.ObjectProperty<CharSequence>(timestamp)

    override val nameString: String? get() = name.value?.toString()

    override fun getVessel(vesselData: VesselData): Vessel? =
        if (hullId.hasValue) vesselData[hullId.value] else null

    override fun updates(other: ArtemisObject) {
        super.updates(other)
        if (other is BaseArtemisShielded) {
            updateProp(other, BaseArtemisShielded::name)
            updateProp(other, BaseArtemisShielded::hullId)
            updateProp(other, BaseArtemisShielded::shieldsFront)
            updateProp(other, BaseArtemisShielded::shieldsFrontMax)
        }
    }

    /**
     * Returns true if this object contains any data.
     */
    override val hasData: Boolean get() =
        super.hasData ||
            name.hasValue ||
            hullId.hasValue ||
            shieldsFront.hasValue ||
            shieldsFrontMax.hasValue

    internal open class Dsl<T : BaseArtemisShielded>(
        objectClass: KClass<T>,
    ) : BaseArtemisObject.Dsl<T>(objectClass) {
        var name: CharSequence? = null
        var hullId: Int = -1
        var shieldsFront: Float = Float.NaN
        var shieldsFrontMax: Float = Float.NaN

        override fun updates(obj: T) {
            super.updates(obj)

            obj.name.value = name
            obj.hullId.value = hullId
            obj.shieldsFront.value = shieldsFront
            obj.shieldsFrontMax.value = shieldsFrontMax

            name = null
            hullId = -1
            shieldsFront = Float.NaN
            shieldsFrontMax = Float.NaN
        }
    }
}
