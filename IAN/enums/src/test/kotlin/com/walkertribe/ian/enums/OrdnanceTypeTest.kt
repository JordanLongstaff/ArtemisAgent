package com.walkertribe.ian.enums

import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.choose
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.of
import io.kotest.property.forAll
import kotlin.time.Duration.Companion.minutes

class OrdnanceTypeTest : DescribeSpec({
    describe("OrdnanceType") {
        val validCodes = arrayOf(
            "trp",
            "nuk",
            "min",
            "emp",
            "shk",
            "bea",
            "pro",
            "tag",
        )

        validCodes.size shouldBeEqual OrdnanceType.size

        val legacyTypes = intArrayOf(1, 4, 6, 9, 8)
        legacyTypes.size shouldBeEqual OrdnanceType.BEACON.ordinal

        describe("Infer from three-letter code") {
            withData(
                nameFn = { (code, expectedOrdnanceType) -> "$code = $expectedOrdnanceType" },
                validCodes.zip(OrdnanceType.entries),
            ) { (code, expectedOrdnanceType) ->
                val actualOrdnanceType = OrdnanceType[code]
                actualOrdnanceType.shouldNotBeNull() shouldBeEqual expectedOrdnanceType
                actualOrdnanceType.code shouldBeEqual code
            }
        }

        it("Invalid code returns null") {
            Arb.string().filter { !validCodes.contains(it) }.forAll {
                val lengthClass = when(it.length) {
                    in 0..2 -> "Shorter than"
                    3 -> "Exactly"
                    else -> "Longer than"
                }
                collect("$lengthClass 3 characters")
                OrdnanceType[it] == null
            }
        }

        describe("Ordnance types by version") {
            val preSplitSize = OrdnanceType.BEACON.ordinal
            val splitVersion = "2.6.3"
            val arbVersionSinceSplit = Arb.choose(
                1 to Arb.version(major = 2, minor = 6, patchArb = Arb.int(min = 3)),
                3 to Arb.version(major = 2, minorRange = 7..9),
                96 to Arb.version(major = 2, minorArb = Arb.int(min = 10)),
            )

            it("Before $splitVersion: 5 ordnance types") {
                val preSplitList = OrdnanceType.entries.subList(0, preSplitSize)

                Arb.version(major = 2, minorRange = 3..5).checkAll { version ->
                    collect("${version.toString().substringBeforeLast('.')}.*")
                    OrdnanceType.countForVersion(version) shouldBeEqual preSplitSize
                    OrdnanceType.getAllForVersion(version).toList() shouldContainExactly
                        preSplitList
                }

                Exhaustive.of(0, 1, 2).checkAll { patch ->
                    collect("2.6.$patch")
                    val version = Version(2, 6, patch)
                    OrdnanceType.countForVersion(version) shouldBeEqual preSplitSize
                    OrdnanceType.getAllForVersion(version).toList() shouldContainExactly
                        preSplitList
                }
            }

            it("Since $splitVersion: 8 ordnance types") {
                arbVersionSinceSplit.checkAll { version ->
                    val parts = version.toString().split(".").map(String::toInt)
                    collect(
                        when (val minor = parts[1]) {
                            6 -> "2.6.3+"
                            in 7..9 -> "2.7-9.*"
                            else -> {
                                val minorLength = minor.toString().length
                                val minimum = Array(minorLength - 1) { "0" }
                                    .joinToString("", prefix = "1")
                                val maximum = Array(minorLength) { "9" }.joinToString("")
                                "2.$minimum-$maximum.*"
                            }
                        }
                    )

                    OrdnanceType.countForVersion(version) shouldBeEqual OrdnanceType.size
                    OrdnanceType.getAllForVersion(version).toList() shouldContainExactly
                        OrdnanceType.entries
                }
            }

            OrdnanceType.entries.forEach { ordnanceType ->
                describe(ordnanceType.toString()) {
                    if (ordnanceType < OrdnanceType.BEACON) {
                        it("Existed before $splitVersion") {
                            Arb.version(major = 2, minorRange = 3..5).forAll { version ->
                                collect("${version.toString().substringBeforeLast('.')}.*")
                                ordnanceType existsIn version
                            }

                            Exhaustive.of(0, 1, 2).forAll { patch ->
                                collect("2.6.$patch")
                                ordnanceType existsIn Version(2, 6, patch)
                            }
                        }

                        val legacyLabel = "Type ${legacyTypes[ordnanceType.ordinal]} $ordnanceType"
                        it("Was $legacyLabel") {
                            Arb.version(major = 2, minorRange = 3..5).forAll { version ->
                                collect("${version.toString().substringBeforeLast('.')}.*")
                                ordnanceType.getLabelFor(version) == legacyLabel
                            }

                            Exhaustive.of(0, 1, 2).forAll { patch ->
                                collect("2.6.$patch")
                                ordnanceType.getLabelFor(Version(2, 6, patch)) == legacyLabel
                            }
                        }
                    } else {
                        it("Did not exist before $splitVersion") {
                            Arb.version(major = 2, minorRange = 3..5).forAll { version ->
                                collect("${version.toString().substringBeforeLast('.')}.*")
                                !(ordnanceType existsIn version)
                            }

                            Exhaustive.of(0, 1, 2).forAll { patch ->
                                collect("2.6.$patch")
                                !(ordnanceType existsIn Version(2, 6, patch))
                            }
                        }
                    }

                    it("Exists since $splitVersion") {
                        arbVersionSinceSplit.forAll { version ->
                            val parts = version.toString().split(".").map(String::toInt)
                            collect(
                                when (val minor = parts[1]) {
                                    6 -> "2.6.3+"
                                    in 7..9 -> "2.7-9.*"
                                    else -> {
                                        val minorLength = minor.toString().length
                                        val minimum = Array(minorLength - 1) { "0" }
                                            .joinToString("", prefix = "1")
                                        val maximum = Array(minorLength) { "9" }.joinToString("")
                                        "2.$minimum-$maximum.*"
                                    }
                                }
                            )

                            ordnanceType existsIn version
                        }
                    }
                }
            }
        }

        describe("Build minutes") {
            val expectedBuildTimes = intArrayOf(3, 10, 4, 5, 10, 1, 1, 1)

            withData(
                nameFn = {
                    "${it.second}: ${it.first} minute${if (it.first == 1) "" else "s"}"
                },
                expectedBuildTimes.zip(OrdnanceType.entries),
            ) { (expectedTime, ordnanceType) ->
                ordnanceType.buildTime shouldBeEqual expectedTime.minutes.inWholeMilliseconds
            }
        }

        describe("Labels") {
            val latestVersion = Version.LATEST

            OrdnanceType.entries.forEach { ordnanceType ->
                describe(ordnanceType.toString()) {
                    val labels = listOfNotNull(
                        ordnanceType.toString(),
                        if (ordnanceType < OrdnanceType.BEACON) {
                            "Type ${legacyTypes[ordnanceType.ordinal]} $ordnanceType"
                        } else {
                            null
                        }
                    )

                    withData(nameFn = { "Has label: $it" }, labels) { label ->
                        ordnanceType.hasLabel(label).shouldBeTrue()
                    }

                    it("Label for latest version: ${labels[0]}") {
                        ordnanceType.getLabelFor(latestVersion) shouldBeEqual labels[0]
                    }

                    it("Arbitrary label: false") {
                        Arb.string().filterNot(labels::contains).forAll { !ordnanceType.hasLabel(it) }
                    }
                }
            }
        }
    }
})
