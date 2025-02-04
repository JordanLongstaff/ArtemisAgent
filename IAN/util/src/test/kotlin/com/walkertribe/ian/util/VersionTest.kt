package com.walkertribe.ian.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldNotBeEqualComparingTo
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.negativeInt
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.arbitrary.shuffle
import io.kotest.property.arbitrary.triple
import io.kotest.property.checkAll
import io.kotest.property.forAll

class VersionTest :
    DescribeSpec({
        describe("Version") {
            val majorRange = 0..0xF
            val otherRange = 0..0x3FFF

            describe("Primary constructor") {
                it("Valid") {
                    Arb.version(majorRange, otherRange, otherRange).forAll { version ->
                        version.toString() == "${version.major}.${version.minor}.${version.patch}"
                    }
                }

                describe("Invalid") {
                    it("Negative parts") {
                        Arb.triple(Arb.negativeInt(), Arb.nonNegativeInt(), Arb.nonNegativeInt())
                            .flatMap { (major, minor, patch) ->
                                Arb.shuffle(listOf(major, minor, patch))
                            }
                            .checkAll { (major, minor, patch) ->
                                collect(
                                    when {
                                        major < 0 -> "Major"
                                        minor < 0 -> "Minor"
                                        patch < 0 -> "Patch"
                                        else -> "Fail"
                                    }
                                )
                                shouldThrow<IllegalArgumentException> {
                                    Version(major, minor, patch)
                                }
                            }
                    }
                }
            }

            describe("Constants") {
                withData(
                    nameFn = { "${it.first} == ${it.third}" },
                    Triple("ACCENT_COLOR", Version.ACCENT_COLOR, "2.4.0"),
                    Triple("COMM_FILTERS", Version.COMM_FILTERS, "2.6.0"),
                    Triple("BEACON", Version.BEACON, "2.6.3"),
                    Triple("NEBULA_TYPES", Version.NEBULA_TYPES, "2.7.0"),
                    Triple("MINIMUM", Version.MINIMUM, "2.3.0"),
                ) { (_, version, expected) ->
                    version.toString() shouldBeEqual expected
                }

                it("Latest does not precede minimum") { Version.MINIMUM <= Version.LATEST }
            }

            describe("Comparisons") {
                describe("Equal versions") {
                    it("Equals") {
                        Arb.version(majorRange, otherRange, otherRange).checkAll { versionA ->
                            val versionB = versionA.copy()
                            versionA shouldBeEqual versionB
                            versionB shouldBeEqual versionA
                            versionA shouldBeEqualComparingTo versionB
                            versionB shouldBeEqualComparingTo versionA
                        }
                    }

                    it("Equal hash codes") {
                        Arb.version(majorRange, otherRange, otherRange).forAll { versionA ->
                            val versionB = versionA.copy()
                            versionA.hashCode() == versionB.hashCode()
                        }
                    }

                    it("Less than or equal") {
                        Arb.version(majorRange, otherRange, otherRange).forAll { version ->
                            version <= version.copy()
                        }
                    }

                    it("Greater than or equal") {
                        Arb.version(majorRange, otherRange, otherRange).forAll { version ->
                            version >= version.copy()
                        }
                    }
                }

                describe("Different versions") {
                    val arbVersionPair =
                        Arb.choice(
                            Arb.version(
                                    majorRange = 0..0xE,
                                    minorRange = otherRange,
                                    patchRange = otherRange,
                                )
                                .flatMap { versionA ->
                                    Arb.version(
                                            majorRange = (versionA.major + 1)..0xF,
                                            minorRange = otherRange,
                                            patchRange = otherRange,
                                        )
                                        .map { versionB -> versionA to versionB }
                                },
                            Arb.version(
                                    majorRange = majorRange,
                                    minorRange = 0..0x3FFE,
                                    patchRange = otherRange,
                                )
                                .flatMap { versionA ->
                                    Arb.version(
                                            major = versionA.major,
                                            minorRange = (versionA.minor + 1)..0x3FFF,
                                            patchRange = otherRange,
                                        )
                                        .map { versionB -> versionA to versionB }
                                },
                            Arb.version(
                                    majorRange = majorRange,
                                    minorRange = otherRange,
                                    patchRange = 0..0x3FFE,
                                )
                                .flatMap { versionA ->
                                    Arb.version(
                                            major = versionA.major,
                                            minor = versionA.minor,
                                            patchRange = (versionA.patch + 1)..0x3FFF,
                                        )
                                        .map { versionB -> versionA to versionB }
                                },
                        )

                    it("Not equals") {
                        arbVersionPair.checkAll { (versionA, versionB) ->
                            collect(
                                when {
                                    versionA.major != versionB.major -> "Major"
                                    versionA.minor != versionB.minor -> "Minor"
                                    versionA.patch != versionB.patch -> "Patch"
                                    else -> "Fail"
                                }
                            )

                            versionA shouldNotBeEqual versionB
                            versionB shouldNotBeEqual versionA
                            versionA shouldNotBeEqualComparingTo versionB
                            versionB shouldNotBeEqualComparingTo versionA
                        }
                    }

                    it("Different hash codes") {
                        arbVersionPair.forAll { (versionA, versionB) ->
                            versionA.hashCode() != versionB.hashCode()
                        }
                    }

                    it("Less than") {
                        arbVersionPair.forAll { (versionA, versionB) ->
                            collect(
                                when {
                                    versionA.major < versionB.major -> "Major"
                                    versionA.minor < versionB.minor -> "Minor"
                                    versionA.patch < versionB.patch -> "Patch"
                                    else -> "Fail"
                                }
                            )

                            versionA < versionB
                        }
                    }

                    it("Less than or equal") {
                        arbVersionPair.forAll { (versionA, versionB) ->
                            collect(
                                when {
                                    versionA.major < versionB.major -> "Major"
                                    versionA.minor < versionB.minor -> "Minor"
                                    versionA.patch <= versionB.patch -> "Patch"
                                    else -> "Fail"
                                }
                            )

                            versionA <= versionB
                        }
                    }

                    it("Greater than") {
                        arbVersionPair.forAll { (versionA, versionB) ->
                            collect(
                                when {
                                    versionB.major > versionA.major -> "Major"
                                    versionB.minor > versionA.minor -> "Minor"
                                    versionB.patch > versionA.patch -> "Patch"
                                    else -> "Fail"
                                }
                            )

                            versionB > versionA
                        }
                    }

                    it("Greater than or equal") {
                        arbVersionPair.forAll { (versionA, versionB) ->
                            collect(
                                when {
                                    versionB.major > versionA.major -> "Major"
                                    versionB.minor > versionA.minor -> "Minor"
                                    versionB.patch >= versionA.patch -> "Patch"
                                    else -> "Fail"
                                }
                            )

                            versionB >= versionA
                        }
                    }
                }
            }
        }
    })
