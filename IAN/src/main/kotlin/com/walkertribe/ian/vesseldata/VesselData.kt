package com.walkertribe.ian.vesseldata

import com.walkertribe.ian.util.PathResolver
import korlibs.io.serialization.xml.Xml

/**
 * Contains all the information extracted from the vesselData.xml file.
 * @author rjwut
 */
sealed interface VesselData {
    data class Loaded internal constructor(
        /**
         * Returns a List containing all the Factions.
         */
        val factions: Map<Int, Faction>,

        internal val vessels: Map<Int, Vessel>,
    ) : VesselData {
        internal constructor(factions: List<Faction>, vessels: List<Vessel>) : this(
            factions = factions.associateBy { it.id },
            vessels = vessels.associateBy { it.id },
        )

        internal constructor(xml: Xml) : this(
            factions = xml["hullRace"].map(::Faction),
            vessels = xml["vessel"].map(::Vessel),
        )

        override fun getFaction(id: Int): Faction? = factions[id]
        override fun get(id: Int): Vessel? = vessels[id]
    }

    @JvmInline
    value class Error internal constructor(val message: String?) : VesselData {
        override fun getFaction(id: Int): Faction? = null
        override fun get(id: Int): Vessel? = null
    }

    /**
     * Returns the Faction represented by the given faction ID. Note that if the server and client
     * vesselData.xml files are not identical, one may specify a faction ID that the other doesn't
     * have, which would result in this method returning null. Your code should handle this scenario
     * gracefully.
     */
    fun getFaction(id: Int): Faction?

    /**
     * Returns the Vessel represented by the given hull ID, or null if no Vessel has this ID. Note
     * that if the server and client vesselData.xml files are not identical, one may specify a hull
     * ID that the other doesn't have, which would result in this method returning null. Your code
     * should handle this scenario gracefully.
     */
    operator fun get(id: Int): Vessel?

    companion object {
        fun load(pathResolver: PathResolver): VesselData =
            pathResolver(PathResolver.DAT / "vesselData.xml") {
                try {
                    Loaded(Xml(readUtf8()))
                } catch (ex: IllegalArgumentException) {
                    Error(ex.message)
                }
            }
    }
}
