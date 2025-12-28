package com.walkertribe.ian.vesseldata

import io.kotest.property.Arb
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.set
import kotlin.enums.enumEntries

private const val DEFAULT_MAX_VESSELS = 100

fun Arb.Companion.vesselData(
    factions: Collection<TestFaction> = enumEntries(),
    vessels: Arb<TestVessel> = TestVessel.arbitrary(),
    numVessels: IntRange = 0..DEFAULT_MAX_VESSELS,
): Arb<VesselData> =
    Arb.set(vessels, numVessels).map { vesselSet ->
        VesselData.Loaded(
            factions = factions.map(TestFaction::build),
            vessels = vesselSet.map { it.build() to it.grid },
        )
    }

val VesselData.Companion.Empty by lazy { VesselData.Loaded(emptyMap(), emptyMap(), emptyMap()) }

val VesselData.Loaded.vesselKeys
    get() = vessels.keys
