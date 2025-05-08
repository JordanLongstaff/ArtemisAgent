package com.walkertribe.ian.vesseldata

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.pair
import io.kotest.property.checkAll

class VesselNameTest :
    DescribeSpec({
        describe("Vessel full name") {
            it("No vessel") {
                checkAll<Int> { TestObject(it).getVessel(VesselData.Empty).shouldBeNull() }
            }

            describe("Vessel found") {
                withData<Collection<TestFaction>>(
                    nameFn = { "With${if (it.isEmpty()) "out" else ""} faction" },
                    TestFaction.entries,
                    emptyList(),
                ) { factions ->
                    val hasFaction = factions.isNotEmpty()
                    TestVessel.arbitrary()
                        .flatMap { vessel ->
                            Arb.pair(
                                Arb.vesselData(
                                    factions = factions,
                                    vessels = Arb.of(vessel),
                                    numVessels = 1..1,
                                ),
                                Arb.of(vessel.id),
                            )
                        }
                        .checkAll { (vesselData, hullId) ->
                            val obj = TestObject(hullId)
                            val vessel = obj.getVessel(vesselData).shouldNotBeNull()
                            val faction = vessel.getFaction(vesselData)
                            val factionName =
                                if (hasFaction) "${faction.shouldNotBeNull().name} " else ""
                            obj.getFullName(vesselData) shouldBe "${factionName}${vessel.name}"
                        }
                }
            }
        }
    })

private class TestObject(val hullId: Int) : VesselDataObject {
    override fun getVessel(vesselData: VesselData): Vessel? = vesselData[hullId]
}
