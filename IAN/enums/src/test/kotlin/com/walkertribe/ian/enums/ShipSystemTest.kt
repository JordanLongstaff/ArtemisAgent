package com.walkertribe.ian.enums

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

class ShipSystemTest :
    DescribeSpec({
        describe("ShipSystem") {
            describe("Values") {
                ShipSystem.entries.forEachIndexed { index, system ->
                    describe(system.name) { system.value shouldBeEqual index - 1 }
                }
            }

            describe("Get by value") {
                describe("Valid") {
                    withData(ShipSystem.entries) { system ->
                        ShipSystem[system.ordinal - 1].shouldNotBeNull() shouldBeEqual system
                    }
                }

                it("Invalid") {
                    Arb.int().filter { it !in -1..<8 }.checkAll { ShipSystem[it].shouldBeNull() }
                }
            }
        }
    })
