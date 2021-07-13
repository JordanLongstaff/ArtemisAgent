package com.walkertribe.ian.vesseldata

interface VesselDataObject {
    /**
     * Returns the Vessel object corresponding to this object's hull ID in the
     * given VesselData object. If the hull ID is unspecified or vesselData.xml
     * contains no Vessel with that ID, returns null.
     */
    fun getVessel(vesselData: VesselData): Vessel?
}
