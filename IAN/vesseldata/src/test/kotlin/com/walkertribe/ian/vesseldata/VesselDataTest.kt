package com.walkertribe.ian.vesseldata

import com.walkertribe.ian.grid.writeToSntFile
import com.walkertribe.ian.util.FilePathResolver
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.io.File
import java.io.InputStreamReader
import korlibs.io.serialization.xml.Xml
import okio.Path.Companion.toOkioPath

class VesselDataTest :
    DescribeSpec({
        val inputStream = VesselData::class.java.getResourceAsStream("dat/vesselData.csv")
        val vessels =
            inputStream
                ?.use { input ->
                    InputStreamReader(input).useLines { lines ->
                        lines
                            .map { line ->
                                val attributes = line.split(",")
                                var index = 0

                                val id = attributes[index++].toInt()
                                val side = attributes[index++].toInt()
                                val faction = TestFaction.valueOf(attributes[index++])
                                val vesselName = attributes[index++]
                                val broadType = attributes[index++]

                                val productionCoefficient = attributes[index].toFloatOrNull()
                                if (productionCoefficient != null) {
                                    index++
                                }

                                val expectedAttributes = mutableSetOf(attributes[index++])

                                val bayCount: Int?
                                while (true) {
                                    val nextToken = attributes[index++]
                                    val count = nextToken.toIntOrNull()
                                    if (count == null) {
                                        expectedAttributes.add(nextToken)
                                    } else {
                                        bayCount = count.takeIf { it > 0 }
                                        break
                                    }
                                }

                                val ordnanceCounts = mutableListOf<Int>()
                                while (index < attributes.size) {
                                    ordnanceCounts.add(attributes[index++].toInt())
                                }

                                TestVessel(
                                    id,
                                    side,
                                    faction,
                                    vesselName,
                                    broadType,
                                    productionCoefficient,
                                    ordnanceCounts,
                                    bayCount,
                                    expectedAttributes,
                                )
                            }
                            .toList()
                    }
                }
                .orEmpty()

        val vesselData =
            VesselData.Loaded(
                TestFaction.entries.map { it.build() },
                vessels.map { it.build() to it.grid },
            )

        val tmpDir = tempdir()
        val tmpDirPath = tmpDir.toOkioPath()
        val datDir = File(tmpDir, "dat")
        datDir.mkdir()
        val datFile = File(datDir, "vesselData.xml")

        describe("VesselData") {
            describe("Construct factions") {
                withData(TestFaction.entries.toList()) {
                    it.test(vesselData.getFaction(it.ordinal))
                }
            }

            describe("Construct vessels") {
                withData(vessels.toList()) { it.test(vesselData[it.id], vesselData) }
            }

            describe("Load from vesselData.xml file") {
                it("Success") {
                    Arb.list(TestVessel.arbitrary(), 1..100).checkAll(iterations = 10) { vessels ->
                        val distinctVessels = vessels.distinctBy { it.id }

                        val vesselDataXml =
                            Xml("vessel_data") {
                                TestFaction.entries.forEach { node(it.serialize()) }
                                distinctVessels.forEach { node(it.serialize()) }
                            }
                        datFile.writeText(vesselDataXml.toString())

                        distinctVessels.forEach { vessel ->
                            val internalsFile = vessel.internalsFilePath ?: return@forEach
                            vessel.grid?.writeToSntFile(File(tmpDir, internalsFile))
                        }

                        val loadedData =
                            VesselData.load(FilePathResolver(tmpDirPath))
                                .shouldBeInstanceOf<VesselData.Loaded>()

                        loadedData.factions.keys shouldBeSameSizeAs TestFaction.entries
                        TestFaction.entries.forEach { test ->
                            test.test(loadedData.getFaction(test.ordinal))
                        }

                        loadedData.vesselKeys shouldBeSameSizeAs distinctVessels
                        distinctVessels.forEach { test ->
                            test.test(loadedData[test.id], loadedData)
                        }
                    }
                }

                describe("Error") {
                    describe("Parsing") {
                        datFile.writeText("<vessel_data><hullRace></hullRace></vessel_data>")

                        val data =
                            VesselData.load(FilePathResolver(tmpDirPath))
                                .shouldBeInstanceOf<VesselData.Error>()

                        it("Stores the error message") {
                            data.message.shouldNotBeNull() shouldBeEqual
                                missingAttribute("hullRace", "ID", "Integer")
                        }

                        it("Cannot retrieve factions") {
                            Arb.int().checkAll { id -> data.getFaction(id).shouldBeNull() }
                        }

                        it("Cannot retrieve vessels") {
                            Arb.int().checkAll { id -> data[id].shouldBeNull() }
                        }

                        it("Cannot retrieve grids") {
                            Arb.int().checkAll { hullId -> data.getGrid(hullId).shouldBeNull() }
                        }
                    }

                    describe("Arbitrary") {
                        val errors = mutableListOf<VesselData.Error>()

                        it("Stores the error message") {
                            Arb.string().checkAll { message ->
                                val error = VesselData.Error(message)
                                error.message.shouldNotBeNull() shouldBeEqual message
                                errors.add(error)
                            }
                        }

                        it("Cannot retrieve factions") {
                            Arb.int().checkAll { id ->
                                errors.forEach { it.getFaction(id).shouldBeNull() }
                            }
                        }

                        it("Cannot retrieve vessels") {
                            Arb.int().checkAll { id -> errors.forEach { it[id].shouldBeNull() } }
                        }
                    }
                }
            }
        }
    })
