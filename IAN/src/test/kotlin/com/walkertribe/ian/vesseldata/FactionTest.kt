package com.walkertribe.ian.vesseldata

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import korlibs.io.serialization.xml.Xml

class FactionTest : DescribeSpec({
    describe("Faction") {
        val arbTaunt = Arb.bind<Taunt>()
        val arbThreeTaunts = Arb.bind(arbTaunt, arbTaunt, arbTaunt) { one, two, three ->
            listOf(one, two, three)
        }

        it("Valid") {
            checkAll(
                Arb.int(),
                Arb.string(),
                Arb.enum<TestFactionAttributes>(),
                arbThreeTaunts,
            ) { id, name, attributes, taunts ->
                val faction = Faction(id, name, attributes.keys, taunts)
                faction.id shouldBeEqual id
                faction.name shouldBeEqual name
                faction.attributes shouldContainExactly attributes.expected.toSet()
                faction.taunts shouldContainExactly taunts
            }
        }

        describe("Invalid") {
            it("Not player, friendly or enemy") {
                checkAll(Arb.int(), Arb.string()) { id, name ->
                    shouldThrow<IllegalArgumentException> {
                        Faction(id, name, "biomech", listOf())
                    }
                }
            }

            it("Enemy with no attributes") {
                checkAll(Arb.int(), Arb.string()) { id, name ->
                    shouldThrow<IllegalArgumentException> {
                        Faction(id, name, "enemy", listOf())
                    }
                }
            }

            it("WHALELOVER and WHALEHATER") {
                checkAll(Arb.int(), Arb.string()) { id, name ->
                    shouldThrow<IllegalArgumentException> {
                        Faction(id, name, "enemy loner whalelover whalehater", listOf())
                    }
                }
            }

            it("Non-player JUMPMASTER") {
                checkAll(Arb.int(), Arb.string()) { id, name ->
                    shouldThrow<IllegalArgumentException> {
                        Faction(id, name, "enemy loner jumpmaster", listOf())
                    }
                }
            }
        }

        describe("XML") {
            describe("Success") {
                withData(TestFaction.entries.toList()) {
                    it.test(Faction(it.serialize()))
                }
            }

            describe("Error") {
                withData(
                    nameFn = { "Missing ${it.first}" },
                    Triple(
                        "ID",
                        "name" to "A",
                        "Integer",
                    ),
                    Triple(
                        "name",
                        "ID" to "0",
                        "String",
                    ),
                ) { (missing, included, type) ->
                    val exception = shouldThrow<IllegalArgumentException> {
                        Faction(
                            Xml("""<hullRace ${included.first}="${included.second}"></hullRace>""")
                        )
                    }
                    exception.message.shouldNotBeNull() shouldBeEqual
                        missingAttribute("hullRace", missing, type)
                }
            }
        }
    }
})
