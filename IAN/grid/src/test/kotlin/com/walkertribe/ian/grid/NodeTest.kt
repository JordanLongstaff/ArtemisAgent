package com.walkertribe.ian.grid

import com.walkertribe.ian.enums.ShipSystem
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldHaveSameHashCodeAs
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.enum
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.cartesianPairs
import io.kotest.property.exhaustive.filter
import io.kotest.property.exhaustive.ints
import io.kotest.property.exhaustive.map

class NodeTest :
    DescribeSpec({
        describe("Node") {
            val exhaustiveCoords =
                Exhaustive.ints(0 until Coordinate.COUNT).map { Coordinate.ALL[it] }

            it("Constructor") {
                checkAll(exhaustiveCoords, Arb.enum<ShipSystem>()) { coord, system ->
                    Node(coord, system).toString() shouldStartWith system.toString()
                }
            }

            it("Hash code") {
                exhaustiveCoords.checkAll { coord ->
                    Arb.enum<ShipSystem>().checkAll(iterations = 1) { system ->
                        Node(coord, system) shouldHaveSameHashCodeAs coord
                    }
                }
            }

            describe("Equality") {
                it("Equals") {
                    checkAll(exhaustiveCoords, Arb.enum<ShipSystem>(), Arb.enum<ShipSystem>()) {
                        coord,
                        system1,
                        system2 ->
                        Node(coord, system1) shouldBeEqual Node(coord, system2)
                    }
                }

                it("Not equals") {
                    checkAll(
                        Exhaustive.cartesianPairs(exhaustiveCoords, exhaustiveCoords).filter {
                            (coord1, coord2) ->
                            coord1 != coord2
                        },
                        Arb.enum<ShipSystem>(),
                    ) { (coord1, coord2), system ->
                        Node(coord1, system) shouldNotBeEqual Node(coord2, system)
                    }
                }
            }
        }
    })
