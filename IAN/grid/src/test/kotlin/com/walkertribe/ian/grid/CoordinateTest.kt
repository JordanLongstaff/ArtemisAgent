package com.walkertribe.ian.grid

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.comparables.shouldNotBeEqualComparingTo
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.types.shouldHaveSameHashCodeAs
import io.kotest.matchers.types.shouldNotHaveSameHashCodeAs
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.filter
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.bytes
import io.kotest.property.exhaustive.cartesianPairs
import io.kotest.property.exhaustive.filter
import io.kotest.property.exhaustive.ints
import io.kotest.property.forAll

class CoordinateTest :
    DescribeSpec({
        describe("Coordinate") {
            val exhaustiveXY = Exhaustive.bytes(from = 0, to = 4)
            val exhaustiveZ = Exhaustive.bytes(from = 0, to = 9)
            val exhaustiveIndices = Exhaustive.ints(0 until Coordinate.COUNT)

            fun exhaustiveOrderedPair(
                exhaustive: Exhaustive<Byte>,
                filterFn: (Pair<Byte, Byte>) -> Boolean,
            ) = Exhaustive.cartesianPairs(exhaustive, exhaustive).filter(filterFn)

            it("All coordinates cached") { Coordinate.ALL shouldHaveSize Coordinate.COUNT }

            it("Get by index") {
                exhaustiveIndices.checkAll { index ->
                    val x = index / 50
                    val y = index / 10 % 5
                    val z = index % 10

                    Coordinate(x.toByte(), y.toByte(), z.toByte()) shouldBeEqual
                        Coordinate.ALL[index]
                }
            }

            describe("Invalid coordinates") {
                val invalidXY = Arb.byte().filter { it !in 0 until 5 }
                val invalidZ = Arb.byte().filter { it !in 0 until 10 }

                it("X-coordinate") {
                    checkAll(invalidXY, exhaustiveXY, exhaustiveZ) { x, y, z ->
                        shouldThrow<IllegalArgumentException> { Coordinate(x, y, z) }
                    }
                }

                it("Y-coordinate") {
                    checkAll(exhaustiveXY, invalidXY, exhaustiveZ) { x, y, z ->
                        shouldThrow<IllegalArgumentException> { Coordinate(x, y, z) }
                    }
                }

                it("Z-coordinate") {
                    checkAll(exhaustiveXY, exhaustiveXY, invalidZ) { x, y, z ->
                        shouldThrow<IllegalArgumentException> { Coordinate(x, y, z) }
                    }
                }
            }

            it("Hash code") {
                Coordinate.ALL.forEach { coord -> coord.hashCode() shouldBeEqual coord.index }
            }

            describe("Comparisons") {
                describe("Equal coordinates") {
                    it("Equals") {
                        checkAll(exhaustiveXY, exhaustiveXY, exhaustiveZ) { xByte, yByte, zByte ->
                            val coordA = Coordinate(xByte, yByte, zByte)
                            val coordB = Coordinate(xByte, yByte, zByte)

                            coordA shouldBeEqual coordB
                            coordB shouldBeEqual coordA
                            coordA shouldBeEqualComparingTo coordB
                            coordB shouldBeEqualComparingTo coordA
                        }
                    }

                    it("Equal hash codes") {
                        exhaustiveIndices.checkAll { index ->
                            val coordA = Coordinate.ALL[index]
                            val coordB = Coordinate.ALL[index]

                            coordA shouldHaveSameHashCodeAs coordB
                            coordB shouldHaveSameHashCodeAs coordA
                        }
                    }

                    it("Less than or equal to") {
                        exhaustiveIndices.checkAll { index ->
                            val coordA = Coordinate.ALL[index]
                            val coordB = Coordinate.ALL[index]

                            coordA shouldBeLessThanOrEqualTo coordB
                            coordB shouldBeLessThanOrEqualTo coordA
                        }
                    }

                    it("Greater than or equal to") {
                        exhaustiveIndices.checkAll { index ->
                            val coordA = Coordinate.ALL[index]
                            val coordB = Coordinate.ALL[index]

                            coordA shouldBeGreaterThanOrEqualTo coordB
                            coordB shouldBeGreaterThanOrEqualTo coordA
                        }
                    }
                }

                describe("Different coordinates") {
                    it("Not equals") {
                        checkAll(exhaustiveIndices, exhaustiveIndices) { i, j ->
                            if (i != j) {
                                val coordA = Coordinate.ALL[i]
                                val coordB = Coordinate.ALL[j]

                                coordA shouldNotBeEqual coordB
                                coordB shouldNotBeEqual coordA
                                coordA shouldNotBeEqualComparingTo coordB
                                coordB shouldNotBeEqualComparingTo coordA
                            }
                        }
                    }

                    it("Different hash codes") {
                        checkAll(exhaustiveIndices, exhaustiveIndices) { i, j ->
                            if (i != j) {
                                val coordA = Coordinate.ALL[i]
                                val coordB = Coordinate.ALL[j]

                                coordA shouldNotHaveSameHashCodeAs coordB
                                coordB shouldNotHaveSameHashCodeAs coordA
                            }
                        }
                    }
                }

                describe("Less than") {
                    it("X-coordinate") {
                        forAll(
                            exhaustiveOrderedPair(exhaustiveXY) { (a, b) -> a < b },
                            exhaustiveXY,
                            exhaustiveXY,
                            exhaustiveZ,
                            exhaustiveZ,
                        ) { (x1, x2), y1, y2, z1, z2 ->
                            Coordinate(x1, y1, z1) < Coordinate(x2, y2, z2)
                        }
                    }

                    it("Y-coordinate") {
                        forAll(
                            exhaustiveXY,
                            exhaustiveOrderedPair(exhaustiveXY) { (a, b) -> a < b },
                            exhaustiveZ,
                            exhaustiveZ,
                        ) { x, (y1, y2), z1, z2 ->
                            Coordinate(x, y1, z1) < Coordinate(x, y2, z2)
                        }
                    }

                    it("Z-coordinate") {
                        forAll(
                            exhaustiveXY,
                            exhaustiveXY,
                            exhaustiveOrderedPair(exhaustiveZ) { (a, b) -> a < b },
                        ) { x, y, (z1, z2) ->
                            Coordinate(x, y, z1) < Coordinate(x, y, z2)
                        }
                    }
                }

                describe("Less than or equal") {
                    it("X-coordinate") {
                        forAll(
                            exhaustiveOrderedPair(exhaustiveXY) { (a, b) -> a < b },
                            exhaustiveXY,
                            exhaustiveXY,
                            exhaustiveZ,
                            exhaustiveZ,
                        ) { (x1, x2), y1, y2, z1, z2 ->
                            Coordinate(x1, y1, z1) <= Coordinate(x2, y2, z2)
                        }
                    }

                    it("Y-coordinate") {
                        forAll(
                            exhaustiveXY,
                            exhaustiveOrderedPair(exhaustiveXY) { (a, b) -> a < b },
                            exhaustiveZ,
                            exhaustiveZ,
                        ) { x, (y1, y2), z1, z2 ->
                            Coordinate(x, y1, z1) <= Coordinate(x, y2, z2)
                        }
                    }

                    it("Z-coordinate") {
                        forAll(
                            exhaustiveXY,
                            exhaustiveXY,
                            exhaustiveOrderedPair(exhaustiveZ) { (a, b) -> a <= b },
                        ) { x, y, (z1, z2) ->
                            Coordinate(x, y, z1) <= Coordinate(x, y, z2)
                        }
                    }
                }

                describe("Greater than") {
                    it("X-coordinate") {
                        forAll(
                            exhaustiveOrderedPair(exhaustiveXY) { (a, b) -> a > b },
                            exhaustiveXY,
                            exhaustiveXY,
                            exhaustiveZ,
                            exhaustiveZ,
                        ) { (x1, x2), y1, y2, z1, z2 ->
                            Coordinate(x1, y1, z1) > Coordinate(x2, y2, z2)
                        }
                    }

                    it("Y-coordinate") {
                        forAll(
                            exhaustiveXY,
                            exhaustiveOrderedPair(exhaustiveXY) { (a, b) -> a > b },
                            exhaustiveZ,
                            exhaustiveZ,
                        ) { x, (y1, y2), z1, z2 ->
                            Coordinate(x, y1, z1) > Coordinate(x, y2, z2)
                        }
                    }

                    it("Z-coordinate") {
                        forAll(
                            exhaustiveXY,
                            exhaustiveXY,
                            exhaustiveOrderedPair(exhaustiveZ) { (a, b) -> a > b },
                        ) { x, y, (z1, z2) ->
                            Coordinate(x, y, z1) > Coordinate(x, y, z2)
                        }
                    }
                }

                describe("Greater than or equal") {
                    it("X-coordinate") {
                        forAll(
                            exhaustiveOrderedPair(exhaustiveXY) { (a, b) -> a > b },
                            exhaustiveXY,
                            exhaustiveXY,
                            exhaustiveZ,
                            exhaustiveZ,
                        ) { (x1, x2), y1, y2, z1, z2 ->
                            Coordinate(x1, y1, z1) >= Coordinate(x2, y2, z2)
                        }
                    }

                    it("Y-coordinate") {
                        forAll(
                            exhaustiveXY,
                            exhaustiveOrderedPair(exhaustiveXY) { (a, b) -> a > b },
                            exhaustiveZ,
                            exhaustiveZ,
                        ) { x, (y1, y2), z1, z2 ->
                            Coordinate(x, y1, z1) >= Coordinate(x, y2, z2)
                        }
                    }

                    it("Z-coordinate") {
                        forAll(
                            exhaustiveXY,
                            exhaustiveXY,
                            exhaustiveOrderedPair(exhaustiveZ) { (a, b) -> a >= b },
                        ) { x, y, (z1, z2) ->
                            Coordinate(x, y, z1) >= Coordinate(x, y, z2)
                        }
                    }
                }
            }
        }
    })
