package com.walkertribe.ian.grid

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.types.shouldHaveSameHashCodeAs
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.negativeFloat
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.cartesianPairs
import io.kotest.property.exhaustive.filter
import io.kotest.property.exhaustive.ints
import io.kotest.property.exhaustive.map

class DamageTest :
    DescribeSpec({
        describe("Damage") {
            val exhaustiveCoords =
                Exhaustive.ints(0 until Coordinate.COUNT).map { Coordinate.ALL[it] }

            it("Constructor") {
                checkAll(exhaustiveCoords, Arb.numericFloat(min = 0f)) { coord, value ->
                    shouldNotThrow<IllegalArgumentException> { Damage(coord, value) }
                }
            }

            it("Hash code") {
                exhaustiveCoords.checkAll { coord ->
                    Arb.numericFloat(min = 0f).checkAll(iterations = 1) { value ->
                        Damage(coord, value) shouldHaveSameHashCodeAs coord
                    }
                }
            }

            describe("Equality") {
                it("Equals") {
                    checkAll(
                        exhaustiveCoords,
                        Arb.numericFloat(min = 0f),
                        Arb.numericFloat(min = 0f),
                    ) { coord, value1, value2 ->
                        Damage(coord, value1) shouldBeEqual Damage(coord, value2)
                    }
                }

                it("Not equals") {
                    checkAll(
                        Exhaustive.cartesianPairs(exhaustiveCoords, exhaustiveCoords).filter {
                            (coord1, coord2) ->
                            coord1 != coord2
                        },
                        Arb.numericFloat(min = 0f),
                    ) { (coord1, coord2), value ->
                        Damage(coord1, value) shouldNotBeEqual Damage(coord2, value)
                    }
                }
            }

            it("Invalid damage") {
                checkAll(exhaustiveCoords, Arb.negativeFloat()) { coord, value ->
                    shouldThrow<IllegalArgumentException> { Damage(coord, value) }
                }
            }
        }
    })
