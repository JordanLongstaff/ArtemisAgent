package artemis.agent.game.missions

import artemis.agent.game.ObjectEntry
import io.github.serpro69.kfaker.Faker
import org.junit.Assert
import org.junit.Test

class SideMissionEntryTest {
    @Test
    fun startedTest() {
        val entry = create()
        Assert.assertFalse(entry.isStarted)

        entry.associatedShipName = faker.name.nameWithMiddle()
        Assert.assertTrue(entry.isStarted)
    }

    @Test
    fun completedTest() {
        val entry = create()
        Assert.assertFalse(entry.isCompleted)

        entry.completionTimestamp = faker.random.nextLong(Long.MAX_VALUE)
        Assert.assertTrue(entry.isCompleted)
    }

    @Test
    fun startingRewardTest() {
        repeat(RewardType.entries.size) {
            val entry = create(RewardType.entries[it])

            val startingRewards = IntArray(RewardType.entries.size)
            startingRewards[it] = 1

            Assert.assertArrayEquals(startingRewards, entry.rewards)
        }
    }

    private companion object {
        val faker by lazy { Faker() }

        fun create(payout: RewardType = faker.random.nextEnum()): SideMissionEntry =
            SideMissionEntry(
                source = faker.randomProvider.randomClassInstance<ObjectEntry.Ally>(),
                destination = faker.randomProvider.randomClassInstance<ObjectEntry.Ally>(),
                payout = payout,
                timestamp = System.currentTimeMillis(),
            )
    }
}
