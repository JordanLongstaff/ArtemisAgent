package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.enums.DriveType
import com.walkertribe.ian.vesseldata.Empty
import com.walkertribe.ian.vesseldata.TestVessel
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.vesseldata.vesselData
import com.walkertribe.ian.world.EPSILON
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.floats.shouldBeNaN
import io.kotest.matchers.floats.shouldBeWithinPercentageOf
import io.kotest.matchers.floats.shouldNotBeNaN
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.negativeFloat
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class ShipTest :
    DescribeSpec({
        describe("Ship") {
            val ships = mutableListOf<Ship>()

            describe("Constructor") {
                it("With no accent colour") {
                    checkAll(Arb.string().orNull(), Arb.int(), Arb.enum<DriveType>()) {
                        name,
                        shipType,
                        driveType ->
                        collect(if (name == null) "Does not have name" else "Has name")

                        val ship = Ship(name, shipType, driveType)
                        ship.accentColor.shouldBeNaN()
                        ship.hue.shouldBeNaN()
                        ships.add(ship)
                    }
                }

                it("With specified accent colour") {
                    checkAll(
                        genA = Arb.string().orNull(),
                        genB = Arb.int(),
                        genC = Arb.numericFloat(min = 0f, max = 1f),
                        genD = Arb.enum<DriveType>(),
                    ) { name, shipType, accentColor, drive ->
                        collect(if (name == null) "Does not have name" else "Has name")

                        val ship = Ship(name, shipType, drive, accentColor)
                        ship.accentColor.shouldNotBeNaN()
                        ship.hue.shouldNotBeNaN()
                        ship.hue.shouldBeWithinPercentageOf(accentColor * Ship.HUE_RANGE, EPSILON)
                        ships.add(ship)
                    }
                }
            }

            describe("Cannot construct with accent colour out of range") {
                withData(
                    nameFn = { it.first },
                    "Too low" to Arb.negativeFloat(),
                    "Too high" to Arb.numericFloat(min = 1.001f),
                ) { (_, arbAccentColor) ->
                    checkAll(
                        genA = Arb.string().orNull(),
                        genB = Arb.int(),
                        genC = arbAccentColor,
                        genD = Arb.enum<DriveType>(),
                    ) { name, shipType, accentColor, drive ->
                        shouldThrow<IllegalArgumentException> {
                            Ship(name, shipType, drive, accentColor)
                        }
                    }
                }
            }

            describe("Vessel") {
                it("Null if not found in vessel data") {
                    ships.forEach { it.getVessel(VesselData.Empty).shouldBeNull() }
                }

                it("Retrieved from vessel data if found") {
                    TestVessel.arbitrary()
                        .flatMap { vessel ->
                            Arb.pair(
                                Arb.vesselData(
                                    factions = emptyList(),
                                    vessels = Arb.of(vessel),
                                    numVessels = 1..1,
                                ),
                                Arb.bind(Arb.string().orNull(), Arb.enum<DriveType>()) { name, drive
                                    ->
                                    Ship(name, shipType = vessel.id, drive)
                                },
                            )
                        }
                        .checkAll { (vesselData, ship) ->
                            ship.getVessel(vesselData).shouldNotBeNull()
                        }
                }
            }

            describe("Full name") {
                it("No vessel") {
                    ships.forEach { it.getFullName(VesselData.Empty).shouldBeNull() }
                }

                it("Retrieved from vessel data if found") {
                    TestVessel.arbitrary()
                        .flatMap { vessel ->
                            Arb.pair(
                                Arb.vesselData(vessels = Arb.of(vessel), numVessels = 1..1),
                                Arb.bind(Arb.string().orNull(), Arb.enum<DriveType>()) { name, drive
                                    ->
                                    Ship(name, shipType = vessel.id, drive)
                                },
                            )
                        }
                        .checkAll { (vesselData, ship) ->
                            val vessel = ship.getVessel(vesselData).shouldNotBeNull()
                            val faction = vessel.getFaction(vesselData).shouldNotBeNull()
                            ship.getFullName(vesselData) shouldBe "${faction.name} ${vessel.name}"
                        }
                }
            }
        }
    })
