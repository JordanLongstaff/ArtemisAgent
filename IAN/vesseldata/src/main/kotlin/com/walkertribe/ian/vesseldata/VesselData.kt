package com.walkertribe.ian.vesseldata

import com.walkertribe.ian.grid.Grid
import com.walkertribe.ian.util.PathResolver
import korlibs.io.serialization.xml.Xml
import kotlinx.io.IOException

/**
 * Contains all the information extracted from the vesselData.xml file.
 *
 * @author rjwut
 */
sealed interface VesselData {
    @ConsistentCopyVisibility
    data class Loaded
    internal constructor(
        /** Returns a List containing all the Factions. */
        val factions: Map<Int, Faction>,
        internal val vessels: Map<Int, Vessel>,
        internal val grids: Map<Int, Grid>,
    ) : VesselData {
        internal constructor(
            factions: List<Faction>,
            vessels: List<Pair<Vessel, Grid?>>,
        ) : this(
            factions = factions.associateBy { it.id },
            vessels = vessels.associate { (vessel, _) -> vessel.id to vessel },
            grids = vessels.mapNotNull { (vessel, grid) -> grid?.let { vessel.id to it } }.toMap(),
        )

        internal constructor(
            xml: Xml,
            pathResolver: PathResolver,
        ) : this(
            factions = xml["hullRace"].map(::Faction),
            vessels =
                xml["vessel"].map { xml ->
                    val vessel = Vessel(xml)
                    vessel to vessel.internalsFilePath?.let { Grid(pathResolver, it) }
                },
        )

        override fun getFaction(id: Int): Faction? = factions[id]

        override fun get(id: Int): Vessel? = vessels[id]

        override fun getGrid(hullId: Int): Grid? = grids[hullId]
    }

    @JvmInline
    value class Error internal constructor(val message: String?) : VesselData {
        override fun getFaction(id: Int): Faction? = null

        override fun get(id: Int): Vessel? = null

        override fun getGrid(hullId: Int): Grid? = null
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

    fun getGrid(hullId: Int): Grid?

    companion object {
        fun load(pathResolver: PathResolver): VesselData =
            try {
                pathResolver(PathResolver.DAT / "vesselData.xml") {
                    Loaded(Xml(readUtf8()), pathResolver)
                }
            } catch (ex: IllegalArgumentException) {
                Error(ex.message)
            } catch (ex: IOException) {
                Error(ex.message)
            }
    }
}
