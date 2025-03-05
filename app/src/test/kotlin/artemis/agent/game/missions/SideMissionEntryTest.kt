package artemis.agent.game.missions

import artemis.agent.game.ObjectEntry
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.next
import io.kotest.property.arbs.cars
import io.kotest.property.checkAll

class SideMissionEntryTest :
    DescribeSpec({
        val arbAlly = Arb.bind<ObjectEntry.Ally>()
        val arbReward = Arb.enum<RewardType>()

        fun create(payout: RewardType = arbReward.next()): SideMissionEntry =
            SideMissionEntry(
                source = arbAlly.next(),
                destination = arbAlly.next(),
                payout = payout,
                timestamp = System.currentTimeMillis(),
            )

        describe("SideMissionEntry") {
            describe("State") {
                val entry = create()

                it("Not started") { entry.isStarted.shouldBeFalse() }

                it("Starts when associated with a ship name") {
                    Arb.cars().checkAll { ship ->
                        entry.associatedShipName = ship.value
                        entry.isStarted.shouldBeTrue()
                    }
                }

                it("Not completed") { entry.isCompleted.shouldBeFalse() }

                it("Completes when timestamp is set") {
                    Arb.long(max = Long.MAX_VALUE - 1).checkAll { timestamp ->
                        entry.completionTimestamp = timestamp
                        entry.isCompleted.shouldBeTrue()
                    }
                }
            }

            describe("Initial payout") {
                withData(RewardType.entries) { rewardType ->
                    val entry = create(rewardType)

                    val expectedPayouts = IntArray(RewardType.entries.size)
                    entry.rewards shouldBeSameSizeAs expectedPayouts

                    expectedPayouts[rewardType.ordinal] = 1
                    entry.rewards.zip(expectedPayouts).shouldForAll { (actual, expected) ->
                        actual shouldBeEqual expected
                    }
                }
            }
        }
    })
