package artemis.agent.game.route

import artemis.agent.R
import artemis.agent.game.ObjectEntry
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@OptIn(ExperimentalSerializationApi::class)
class RouteTaskIncentiveTest :
    DescribeSpec({
        val mockAlly = mockk<ObjectEntry.Ally>()

        val testData =
            RouteTaskIncentive::class.java.getResourceAsStream("task-incentives.json")!!.use {
                Json.decodeFromStream<List<RouteTaskIncentiveTestData>>(it)
            }

        val textMap =
            mapOf(
                R.string.reason_ambassador to "pick up ambassador",
                R.string.reason_commandeered to "liberate commandeered ship",
                R.string.reason_has_energy to "receive energy",
                R.string.reason_hostage to "rescue hostages",
                R.string.reason_malfunction to "reset computer",
                R.string.reason_needs_damcon to "transfer DamCon personnel",
                R.string.reason_needs_energy to "deliver energy",
                R.string.reason_pirate_boss to "pick up boss",
            )

        afterSpec { clearMocks(mockAlly) }

        describe("RouteTaskIncentive") {
            describe("Text") {
                testData.forEach { data ->
                    val incentive = data.incentive
                    val text = data.text

                    every { mockAlly.status } returns data.status
                    every { mockAlly.hasEnergy } returns data.energy

                    it(text) {
                        textMap[incentive.getTextFor(mockAlly)].shouldNotBeNull() shouldBeEqual text
                    }
                }
            }

            describe("Matches") {
                RouteTaskIncentive.entries.forEach { incentive ->
                    describe(incentive.name) {
                        testData.forEach { data ->
                            val expectedMatch = data.incentive == incentive
                            it("${if (expectedMatch) "M" else "No m"}atch: ${data.text}") {
                                every { mockAlly.status } returns data.status
                                every { mockAlly.hasEnergy } returns data.energy

                                incentive.matches(mockAlly) shouldBeEqual expectedMatch
                            }
                        }
                    }
                }
            }
        }
    })
