package com.walkertribe.ian.world

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.floats.shouldBeBetween
import io.kotest.matchers.floats.shouldBeNaN
import io.kotest.property.Arb
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.checkAll

class ShieldsTest :
    DescribeSpec({
        describe("Shields") {
            val shields = Shields(0L)

            describe("Initial state") {
                it("Percentage: NaN") { shields.percentage.shouldBeNaN() }

                it("Is damaged: false") { shields.isDamaged.shouldBeFalse() }
            }

            describe("Full strength") {
                val values = mutableListOf<Float>()
                Arb.numericFloat(min = 1f).checkAll { values.add(it) }

                it("Percentage: 100%") {
                    values.forEach { strength ->
                        shields.strength.value = strength
                        shields.maxStrength.value = strength
                        shields.percentage shouldBeEqual 1f
                    }
                }

                it("Is damaged: false") {
                    values.forEach { strength ->
                        shields.strength.value = strength
                        shields.maxStrength.value = strength
                        shields.isDamaged.shouldBeFalse()
                    }
                }
            }

            describe("Less than full strength") {
                val pairs = mutableListOf<Pair<Float, Float>>()

                Arb.numericFloat(min = 1f)
                    .flatMap { maxStrength ->
                        Arb.float(min = 0f, max = maxStrength * 0.98f).map { strength ->
                            strength to maxStrength
                        }
                    }
                    .checkAll { pairs.add(it) }

                it("Percentage") {
                    pairs.forEach { (strength, maxStrength) ->
                        shields.strength.value = strength
                        shields.maxStrength.value = maxStrength
                        shields.percentage.shouldBeBetween(0f, 1f, 0f)
                    }
                }

                it("Is damaged: true") {
                    pairs.forEach { (strength, maxStrength) ->
                        shields.strength.value = strength
                        shields.maxStrength.value = maxStrength
                        shields.isDamaged.shouldBeTrue()
                    }
                }
            }
        }
    })
