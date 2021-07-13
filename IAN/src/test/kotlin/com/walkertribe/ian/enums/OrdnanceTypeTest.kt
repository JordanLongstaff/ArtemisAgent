package com.walkertribe.ian.enums

import com.walkertribe.ian.util.Version
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.choose
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.of
import io.kotest.property.forAll

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

        describe("Infer from three-letter code") {
            withData(
                nameFn = { (code, expectedOrdnanceType) -> "$code = $expectedOrdnanceType" },
                validCodes.zip(OrdnanceType.entries),
            ) { (code, expectedOrdnanceType) ->
                val actualOrdnanceType = OrdnanceType[code]
                actualOrdnanceType.shouldNotBeNull()
                actualOrdnanceType shouldBeEqual expectedOrdnanceType
                actualOrdnanceType.code shouldBeEqual code
            }
        }

        it("Invalid code returns null") {
            Arb.string().filter { !validCodes.contains(it) }.forAll {
                collect("Length ${it.length}")
                OrdnanceType[it] == null
            }
        }

        describe("Ordnance types by version") {
            val preSplitSize = OrdnanceType.BEACON.ordinal
            val splitVersion = "2.6.3"
            val arbVersionSinceSplit = Arb.choose(
                1 to Arb.pair(Arb.of(6), Arb.int(min = 3)),
                3 to Arb.pair(Arb.int(7..9), Arb.nonNegativeInt()),
                96 to Arb.pair(Arb.int(min = 10), Arb.nonNegativeInt()),
            )

            it("Before $splitVersion: 5 ordnance types") {
                val preSplitList = OrdnanceType.entries.subList(0, preSplitSize)

                checkAll(Arb.int(3..5), Arb.nonNegativeInt()) { minor, patch ->
                    collect("2.$minor.*")
                    val version = Version(2, minor, patch)
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
                arbVersionSinceSplit.checkAll { (minor, patch) ->
                    collect(
                        when (minor) {
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

                    val version = Version(2, minor, patch)
                    OrdnanceType.countForVersion(version) shouldBeEqual OrdnanceType.size
                    OrdnanceType.getAllForVersion(version).toList() shouldContainExactly
                        OrdnanceType.entries
                }
            }

            OrdnanceType.entries.forEach { ordnanceType ->
                describe(ordnanceType.toString()) {
                    if (ordnanceType < OrdnanceType.BEACON) {
                        it("Existed before $splitVersion") {
                            forAll(Arb.int(3..5), Arb.nonNegativeInt()) { minor, patch ->
                                collect("2.$minor.*")
                                ordnanceType existsIn Version(2, minor, patch)
                            }

                            Exhaustive.of(0, 1, 2).forAll { patch ->
                                collect("2.6.$patch")
                                ordnanceType existsIn Version(2, 6, patch)
                            }
                        }

                        it("Was ${ordnanceType.alternateLabel}") {
                            forAll(Arb.int(3..5), Arb.nonNegativeInt()) { minor, patch ->
                                collect("2.$minor.*")
                                ordnanceType.getLabelFor(Version(2, minor, patch)) ==
                                    ordnanceType.alternateLabel
                            }

                            Exhaustive.of(0, 1, 2).forAll { patch ->
                                collect("2.6.$patch")
                                ordnanceType.getLabelFor(Version(2, 6, patch)) ==
                                    ordnanceType.alternateLabel
                            }
                        }
                    } else {
                        it("Did not exist before $splitVersion") {
                            forAll(Arb.int(3..5), Arb.nonNegativeInt()) { minor, patch ->
                                collect("2.$minor.*")
                                !(ordnanceType existsIn Version(2, minor, patch))
                            }

                            Exhaustive.of(0, 1, 2).forAll { patch ->
                                collect("2.6.$patch")
                                !(ordnanceType existsIn Version(2, 6, patch))
                            }
                        }
                    }

                    it("Exists since $splitVersion") {
                        arbVersionSinceSplit.forAll { (minor, patch) ->
                            collect(
                                when (minor) {
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

                            ordnanceType existsIn Version(2, minor, patch)
                        }
                    }
                }
            }
        }
    }
})
