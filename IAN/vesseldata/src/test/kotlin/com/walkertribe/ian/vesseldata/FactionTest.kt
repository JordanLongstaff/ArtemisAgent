package com.walkertribe.ian.vesseldata

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.comparables.shouldNotBeEqualComparingTo
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldHaveSameHashCodeAs
import io.kotest.matchers.types.shouldNotHaveSameHashCodeAs
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import korlibs.io.serialization.xml.Xml
import kotlin.math.max
import kotlin.math.min

class FactionTest :
    DescribeSpec({
        describe("Faction") {
            val arbTaunt = Arb.bind<Taunt>()
            val arbThreeTaunts =
                Arb.bind(arbTaunt, arbTaunt, arbTaunt) { one, two, three ->
                    listOf(one, two, three)
                }

            it("Valid") {
                checkAll(
                    genA = Arb.int(),
                    genB = Arb.string(),
                    genC = Arb.enum<TestFactionAttributes>(),
                    genD = arbThreeTaunts,
                ) { id, name, attributes, taunts ->
                    val faction = Faction(id, name, keys = attributes.keys, taunts)
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
                            Faction(id, name, keys = "biomech", taunts = emptyList())
                        }
                    }
                }

                it("Enemy with no attributes") {
                    checkAll(Arb.int(), Arb.string()) { id, name ->
                        shouldThrow<IllegalArgumentException> {
                            Faction(id, name, keys = "enemy", taunts = emptyList())
                        }
                    }
                }

                it("WHALELOVER and WHALEHATER") {
                    checkAll(Arb.int(), Arb.string()) { id, name ->
                        shouldThrow<IllegalArgumentException> {
                            Faction(
                                id,
                                name,
                                keys = "enemy loner whalelover whalehater",
                                taunts = emptyList(),
                            )
                        }
                    }
                }

                it("Non-player JUMPMASTER") {
                    checkAll(Arb.int(), Arb.string()) { id, name ->
                        shouldThrow<IllegalArgumentException> {
                            Faction(id, name, keys = "enemy loner jumpmaster", taunts = emptyList())
                        }
                    }
                }
            }

            describe("Comparisons") {
                describe("Equal IDs") {
                    it("Equals") {
                        checkAll(
                            genA = Arb.int(),
                            genB = Arb.string(),
                            genC = Arb.string(),
                            genD = Arb.string(),
                            genE = Arb.string(),
                        ) { id, name1, name2, immunity, text ->
                            val faction1 =
                                Faction(id, name = name1, keys = "player", taunts = emptyList())
                            val faction2 =
                                Faction(
                                    id,
                                    name = name2,
                                    keys = "player",
                                    taunts = listOf(Taunt(immunity, text)),
                                )
                            faction1 shouldBeEqual faction2
                            faction2 shouldBeEqual faction1
                            faction1 shouldBeEqualComparingTo faction2
                            faction2 shouldBeEqualComparingTo faction1
                        }
                    }

                    it("Equal hash codes") {
                        checkAll(
                            genA = Arb.int(),
                            genB = Arb.string(),
                            genC = Arb.string(),
                            genD = Arb.string(),
                            genE = Arb.string(),
                        ) { id, name1, name2, immunity, text ->
                            Faction(
                                id,
                                name = name1,
                                keys = "player",
                                taunts = emptyList(),
                            ) shouldHaveSameHashCodeAs
                                Faction(
                                    id,
                                    name = name2,
                                    keys = "player",
                                    taunts = listOf(Taunt(immunity, text)),
                                )
                        }
                    }

                    it("Less than or equal") {
                        checkAll(
                            genA = Arb.int(),
                            genB = Arb.string(),
                            genC = Arb.string(),
                            genD = Arb.string(),
                            genE = Arb.string(),
                        ) { id, name1, name2, immunity, text ->
                            Faction(
                                id,
                                name = name1,
                                keys = "player",
                                taunts = emptyList(),
                            ) shouldBeLessThanOrEqualTo
                                Faction(
                                    id,
                                    name = name2,
                                    keys = "player",
                                    taunts = listOf(Taunt(immunity, text)),
                                )
                        }
                    }

                    it("Greater than or equal") {
                        checkAll(
                            genA = Arb.int(),
                            genB = Arb.string(),
                            genC = Arb.string(),
                            genD = Arb.string(),
                            genE = Arb.string(),
                        ) { id, name1, name2, immunity, text ->
                            Faction(
                                id,
                                name = name1,
                                keys = "player",
                                taunts = emptyList(),
                            ) shouldBeGreaterThanOrEqualTo
                                Faction(
                                    id,
                                    name = name2,
                                    keys = "player",
                                    taunts = listOf(Taunt(immunity, text)),
                                )
                        }
                    }
                }

                describe("Different IDs") {
                    val arbIDPair =
                        Arb.int().flatMap { first ->
                            Arb.int()
                                .filter { it != first }
                                .map { second -> min(first, second) to max(first, second) }
                        }

                    it("Not equals") {
                        checkAll(arbIDPair, Arb.string()) { (id1, id2), name ->
                            val faction1 =
                                Faction(id = id1, name, keys = "player", taunts = emptyList())
                            val faction2 =
                                Faction(id = id2, name, keys = "player", taunts = emptyList())
                            faction1 shouldNotBeEqual faction2
                            faction2 shouldNotBeEqual faction1
                            faction1 shouldNotBeEqualComparingTo faction2
                            faction2 shouldNotBeEqualComparingTo faction1
                        }
                    }

                    it("Different hash codes") {
                        checkAll(arbIDPair, Arb.string()) { (id1, id2), name ->
                            Faction(
                                id = id1,
                                name,
                                keys = "player",
                                taunts = emptyList(),
                            ) shouldNotHaveSameHashCodeAs
                                Faction(id = id2, name, keys = "player", taunts = emptyList())
                        }
                    }

                    it("Less than") {
                        checkAll(arbIDPair, Arb.string()) { (id1, id2), name ->
                            Faction(
                                id = id1,
                                name,
                                keys = "player",
                                taunts = emptyList(),
                            ) shouldBeLessThan
                                Faction(id = id2, name, keys = "player", taunts = emptyList())
                        }
                    }

                    it("Less than or equal to") {
                        checkAll(arbIDPair, Arb.string()) { (id1, id2), name ->
                            Faction(
                                id = id1,
                                name,
                                keys = "player",
                                taunts = emptyList(),
                            ) shouldBeLessThanOrEqualTo
                                Faction(id = id2, name, keys = "player", taunts = emptyList())
                        }
                    }

                    it("Greater than") {
                        checkAll(arbIDPair, Arb.string()) { (id1, id2), name ->
                            Faction(
                                id = id2,
                                name,
                                keys = "player",
                                taunts = emptyList(),
                            ) shouldBeGreaterThan
                                Faction(id = id1, name, keys = "player", taunts = emptyList())
                        }
                    }

                    it("Greater than or equal to") {
                        checkAll(arbIDPair, Arb.string()) { (id1, id2), name ->
                            Faction(
                                id = id2,
                                name,
                                keys = "player",
                                taunts = emptyList(),
                            ) shouldBeGreaterThanOrEqualTo
                                Faction(id = id1, name, keys = "player", taunts = emptyList())
                        }
                    }
                }
            }

            describe("XML") {
                describe("Success") {
                    withData(TestFaction.entries.toList()) { it.test(Faction(it.serialize())) }
                }

                describe("Error") {
                    withData(
                        nameFn = { "Missing ${it.first}" },
                        Triple("ID", "name" to "A", "Integer"),
                        Triple("name", "ID" to "0", "String"),
                    ) { (missing, included, type) ->
                        val exception =
                            shouldThrow<IllegalArgumentException> {
                                Faction(
                                    Xml(
                                        """<hullRace ${included.first}="${included.second}"></hullRace>"""
                                    )
                                )
                            }
                        exception.message.shouldNotBeNull() shouldBeEqual
                            missingAttribute("hullRace", missing, type)
                    }
                }
            }
        }
    })
