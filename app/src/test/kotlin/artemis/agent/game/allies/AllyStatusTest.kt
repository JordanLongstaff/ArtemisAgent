package artemis.agent.game.allies

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@OptIn(ExperimentalSerializationApi::class)
class AllyStatusTest :
    DescribeSpec({
        val testCases =
            AllyStatus::class.java.getResourceAsStream("ally-statuses.json")!!.use {
                Json.decodeFromStream<List<AllyStatusTestCase>>(it)
            }

        describe("AllyStatus") {
            describe("Sort index") {
                testCases.forEach { test ->
                    val status = test.allyStatus
                    val expectedSortIndex = test.expectedSortIndex
                    it("$status: $expectedSortIndex") {
                        status.sortIndex shouldBeEqual expectedSortIndex
                    }
                }
            }

            describe("Pirate-sensitive equivalents") {
                it("Companion") {
                    val expected =
                        listOf(
                            AllyStatus.AMBASSADOR,
                            AllyStatus.PIRATE_BOSS,
                            AllyStatus.CONTRABAND,
                            AllyStatus.PIRATE_SUPPLIES,
                            AllyStatus.SECURE_DATA,
                            AllyStatus.PIRATE_DATA,
                        )
                    AllyStatus.PIRATE_SENSITIVE shouldContainExactly expected
                }

                listOf("Is pirate" to true, "Is not pirate" to false).forEach { (name, isPirate) ->
                    describe(name) {
                        testCases.forEach { test ->
                            val expected = test.getStatusForPirate(isPirate)
                            it("${test.allyStatus} -> $expected") {
                                test.allyStatus.getPirateSensitiveEquivalent(isPirate) shouldBeEqual
                                    expected
                            }
                        }
                    }
                }
            }
        }
    })
