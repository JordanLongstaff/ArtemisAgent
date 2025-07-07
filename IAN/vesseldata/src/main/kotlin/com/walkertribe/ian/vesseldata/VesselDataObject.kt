package com.walkertribe.ian.vesseldata

import com.walkertribe.ian.util.Util.joinSpaceDelimited

interface VesselDataObject {
    /**
     * Returns the Vessel object corresponding to this object's hull ID in the given VesselData
     * object. If the hull ID is unspecified or vesselData.xml contains no Vessel with that ID,
     * returns null.
     */
    fun getVessel(vesselData: VesselData): Vessel?

    /** Returns the faction and model name of this object's vessel. */
    fun getFullName(vesselData: VesselData): String? =
        getVessel(vesselData)?.run {
            listOfNotNull(getFaction(vesselData)?.name, name).joinSpaceDelimited()
        }
}
