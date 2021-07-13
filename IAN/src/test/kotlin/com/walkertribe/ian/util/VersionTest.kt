package com.walkertribe.ian.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.constant
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.negativeInt
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.arbitrary.shuffle
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.arbitrary.triple
import io.kotest.property.checkAll
import io.kotest.property.forAll

class VersionTest : DescribeSpec({
    describe("Version") {
        val arbVersionParts = Arb.triple(
            Arb.nonNegativeInt(),
            Arb.nonNegativeInt(),
            Arb.nonNegativeInt(),
        )

        describe("Primary constructor") {
            it("Valid") {
                arbVersionParts.forAll { (major, minor, patch) ->
                    Version(major, minor, patch).toString() == "$major.$minor.$patch"
                }
            }

            describe("Invalid") {
                it("No arguments") {
                    shouldThrow<IllegalArgumentException> { Version() }
                }

                it("One argument") {
                    Arb.nonNegativeInt().checkAll {
                        shouldThrow<IllegalArgumentException> { Version(it) }
                    }
                }

                it("Negative parts") {
                    Arb.triple(
                        Arb.negativeInt(),
                        Arb.nonNegativeInt(),
                        Arb.nonNegativeInt(),
                    ).flatMap { (major, minor, patch) ->
                        Arb.shuffle(listOf(major, minor, patch))
                    }.checkAll { (major, minor, patch) ->
                        collect(
                            when {
                                major < 0 -> "Major"
                                minor < 0 -> "Minor"
                                patch < 0 -> "Patch"
                                else -> "Fail"
                            }
                        )
                        shouldThrow<IllegalArgumentException> { Version(major, minor, patch) }
                    }
                }
            }
        }

        describe("String constructor") {
            it("Valid") {
                arbVersionParts.map { (major, minor, patch) -> "$major.$minor.$patch" }.forAll {
                    Version(it).toString() == it
                }
            }

            describe("Invalid") {
                it("Empty string") {
                    shouldThrow<NumberFormatException> { Version("") }
                }

                it("One part") {
                    Arb.nonNegativeInt().checkAll {
                        shouldThrow<IllegalArgumentException> { Version(it.toString()) }
                    }
                }

                it("Negative parts") {
                    Arb.triple(
                        Arb.negativeInt(),
                        Arb.nonNegativeInt(),
                        Arb.nonNegativeInt(),
                    ).flatMap { (major, minor, patch) ->
                        Arb.shuffle(listOf(major, minor, patch))
                    }.checkAll { (major, minor, patch) ->
                        collect(
                            when {
                                major < 0 -> "Major"
                                minor < 0 -> "Minor"
                                patch < 0 -> "Patch"
                                else -> "Fail"
                            }
                        )
                        shouldThrow<IllegalArgumentException> { Version("$major.$minor.$patch") }
                    }
                }

                it("Non-integer parts") {
                    Arb.triple(
                        Arb.stringPattern("[A-Za-z]+"),
                        Arb.nonNegativeInt(),
                        Arb.nonNegativeInt(),
                    ).flatMap { (major, minor, patch) ->
                        Arb.shuffle(listOf(major, minor, patch))
                    }.checkAll { (major, minor, patch) ->
                        collect(
                            when {
                                major !is Int -> "Major"
                                minor !is Int -> "Minor"
                                patch !is Int -> "Patch"
                                else -> "Fail"
                            }
                        )
                        shouldThrow<NumberFormatException> { Version("$major.$minor.$patch") }
                    }
                }
            }
        }

        describe("Comparisons") {
            describe("Equal versions") {
                it("Equals") {
                    arbVersionParts.forAll { (major, minor, patch) ->
                        Version(major, minor, patch) == Version(major, minor, patch)
                    }
                }

                it("Less than or equal") {
                    arbVersionParts.forAll { (major, minor, patch) ->
                        Version(major, minor, patch) <= Version(major, minor, patch)
                    }
                }

                it("Greater than or equal") {
                    arbVersionParts.forAll { (major, minor, patch) ->
                        Version(major, minor, patch) >= Version(major, minor, patch)
                    }
                }
            }

            describe("Different versions") {
                val arbVersionPair = Arb.choice(
                    arbVersionParts.filter { it.first < Int.MAX_VALUE }.flatMap { partsA ->
                        Arb.triple(
                            Arb.int(min = partsA.first + 1),
                            Arb.nonNegativeInt(),
                            Arb.nonNegativeInt(),
                        ).map { partsB -> Pair(partsA, partsB) }
                    },
                    arbVersionParts.filter { it.second < Int.MAX_VALUE }.flatMap { partsA ->
                        Arb.triple(
                            Arb.constant(partsA.first),
                            Arb.int(min = partsA.second + 1),
                            Arb.nonNegativeInt(),
                        ).map { partsB -> Pair(partsA, partsB) }
                    },
                    arbVersionParts.filter { it.third < Int.MAX_VALUE }.flatMap { partsA ->
                        Arb.triple(
                            Arb.constant(partsA.first),
                            Arb.constant(partsA.second),
                            Arb.int(min = partsA.third + 1),
                        ).map { partsB -> Pair(partsA, partsB) }
                    },
                )

                it("Not equals") {
                    arbVersionPair.forAll { (partsA, partsB) ->
                        collect(
                            when {
                                partsA.first != partsB.first -> "Major"
                                partsA.second != partsB.second -> "Minor"
                                partsA.third != partsB.third -> "Patch"
                                else -> "Fail"
                            }
                        )

                        val versionA = Version(partsA.first, partsA.second, partsA.third)
                        val versionB = Version(partsB.first, partsB.second, partsB.third)
                        versionA != versionB && versionB != versionA
                    }
                }

                it("Less than") {
                    arbVersionPair.forAll { (partsA, partsB) ->
                        collect(
                            when {
                                partsA.first < partsB.first -> "Major"
                                partsA.second < partsB.second -> "Minor"
                                partsA.third < partsB.third -> "Patch"
                                else -> "Fail"
                            }
                        )

                        val versionA = Version(partsA.first, partsA.second, partsA.third)
                        val versionB = Version(partsB.first, partsB.second, partsB.third)
                        versionA < versionB
                    }
                }

                it("Less than or equal") {
                    arbVersionPair.forAll { (partsA, partsB) ->
                        collect(
                            when {
                                partsA.first <= partsB.first -> "Major"
                                partsA.second <= partsB.second -> "Minor"
                                partsA.third <= partsB.third -> "Patch"
                                else -> "Fail"
                            }
                        )

                        val versionA = Version(partsA.first, partsA.second, partsA.third)
                        val versionB = Version(partsB.first, partsB.second, partsB.third)
                        versionA <= versionB
                    }
                }

                it("Greater than") {
                    arbVersionPair.forAll { (partsA, partsB) ->
                        collect(
                            when {
                                partsB.first > partsA.first -> "Major"
                                partsB.second > partsA.second -> "Minor"
                                partsB.third > partsA.third -> "Patch"
                                else -> "Fail"
                            }
                        )

                        val versionA = Version(partsA.first, partsA.second, partsA.third)
                        val versionB = Version(partsB.first, partsB.second, partsB.third)
                        versionB > versionA
                    }
                }

                it("Greater than or equal") {
                    arbVersionPair.forAll { (partsA, partsB) ->
                        collect(
                            when {
                                partsB.first >= partsA.first -> "Major"
                                partsB.second >= partsA.second -> "Minor"
                                partsB.third >= partsA.third -> "Patch"
                                else -> "Fail"
                            }
                        )

                        val versionA = Version(partsA.first, partsA.second, partsA.third)
                        val versionB = Version(partsB.first, partsB.second, partsB.third)
                        versionB >= versionA
                    }
                }
            }
        }
    }
})
