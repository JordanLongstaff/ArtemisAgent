package com.walkertribe.ian.enums

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.forAll

class OriginTest :
    DescribeSpec({
        describe("Origin") {
            it("Constructor") { Arb.int().forAll { Origin(it).value == it } }

            describe("Constants") {
                withData(
                    nameFn = { "${it.first} = ${it.third}" },
                    Triple("SERVER", Origin.SERVER, 1),
                    Triple("CLIENT", Origin.CLIENT, 2),
                ) { (_, expectedOrigin, value) ->
                    val origin = Origin(value)
                    origin shouldBeEqual expectedOrigin
                    origin.isValid.shouldBeTrue()
                }
            }

            it("Entry set contains only SERVER and CLIENT") {
                Origin.entries shouldContainExactly listOf(Origin.SERVER, Origin.CLIENT)
            }

            it("Invalid") {
                Arb.bind<Origin>()
                    .filter { it.value !in 1..2 }
                    .forAll {
                        collect(if (it.value > 2) "POSITIVE" else "NON-POSITIVE")
                        !it.isValid
                    }
            }
        }
    })
