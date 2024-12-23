package com.walkertribe.ian.vesseldata

import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.util.Util.caretToNewline
import com.walkertribe.ian.util.Util.splitSpaceDelimited
import korlibs.io.serialization.xml.Xml

/**
 * Corresponds to the &lt;vessel&gt; element in vesselData.xml. Note that this
 * represents an entire class of ships, not an individual one.
 * @author rjwut
 */
@Suppress("LongParameterList")
class Vessel internal constructor(
    /**
     * Returns the Vessel's ID.
     */
    val id: Int,

    /**
     * Returns the Vessel's Faction ID.
     */
    val side: Int,

    /**
     * Returns this Vessel's name.
     */
    val name: String,

    broadType: String,

    /**
     * Returns the maximum capacity of storage of each ordnance type on this Vessel.
     */
    val ordnanceStorage: Map<OrdnanceType, Int>,

    /**
     * Returns the base production coefficient. This value affects how quickly
     * the base produces new ordnance.
     */
    val productionCoefficient: Float,

    /**
     * Returns the number of fighter bays on this Vessel.
     */
    val bayCount: Int,

    /**
     * Returns a short description of this Vessel.
     */
    val description: String?,
) {
    internal constructor(xml: Xml) : this(
        id = xml.requiredInt("uniqueID"),
        side = xml.requiredInt("side"),
        name = xml.requiredString("classname"),
        broadType = xml.str("broadType"),
        ordnanceStorage = xml["torpedo_storage"].associate {
            val code = it.requiredString("type")
            requireNotNull(OrdnanceType[code]) {
                "Invalid ordnance type code: $code"
            } to it.requiredInt("amount")
        },
        productionCoefficient = xml.child("production")?.float("coeff") ?: 0f,
        bayCount = xml.child("carrierload")?.int("baycount") ?: 0,
        description = xml.child("long_desc")?.run {
            str("text").caretToNewline()
        },
    )

    /**
     * Returns an array of this Vessel's attributes.
     */
    val attributes: Set<String> = build(broadType)

    /**
     * Returns the Faction to which this Vessel belongs.
     */
    fun getFaction(vesselData: VesselData): Faction? = vesselData.getFaction(side)

    /**
     * Returns true if this Vessel has all the given attributes; false
     * otherwise.
     */
    operator fun get(vararg attrs: String): Boolean = attrs.all(attributes::contains)

    /**
     * Returns true if this is a single-seat vessel.
     */
    val isSingleseat: Boolean get() = this[SINGLESEAT]

    private companion object {
        private const val SINGLESEAT = "singleseat"

        private fun build(broadType: String): Set<String> =
            broadType.lowercase().splitSpaceDelimited().toSet()
    }
}
