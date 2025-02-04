package com.walkertribe.ian.world

import com.walkertribe.ian.vesseldata.VesselDataObject

/**
 * An [ArtemisObject] which can have shields. Note that shield values can be negative.
 *
 * @author dhleong
 */
interface ArtemisShielded<T : ArtemisShielded<T>> : ArtemisObject<T>, VesselDataObject {
    /** The object's name. Unspecified: null */
    val name: Property.ObjectProperty<String>

    /**
     * Identifies the type of ship this is. This corresponds to the uniqueID attribute of vessel
     * elements in vesselData.xml. Unspecified: -1
     */
    val hullId: Property.IntProperty

    /** The strength of the forward shields. Unspecified: Float.NaN */
    val shieldsFront: Property.FloatProperty

    /** The maximum strength of the forward shields. Unspecified: Float.NaN */
    val shieldsFrontMax: Property.FloatProperty
}
