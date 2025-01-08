package artemis.agent.game.missions

import artemis.agent.game.ObjectEntry
import io.github.serpro69.kfaker.Faker
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class SideMissionEntryTest {
    @Test
    fun startedTest() {
        val entry = create()
        Assertions.assertFalse(entry.isStarted)

        entry.associatedShipName = faker.name.nameWithMiddle()
        Assertions.assertTrue(entry.isStarted)
    }

    @Test
    fun completedTest() {
        val entry = create()
        Assertions.assertFalse(entry.isCompleted)

        entry.completionTimestamp = faker.random.nextLong(Long.MAX_VALUE)
        Assertions.assertTrue(entry.isCompleted)
    }

    @ParameterizedTest
    @EnumSource(RewardType::class)
    fun startingRewardTest(firstPayout: RewardType) {
        val entry = create(firstPayout)

        val startingRewards = IntArray(RewardType.entries.size)
        startingRewards[firstPayout.ordinal] = 1

        Assertions.assertArrayEquals(startingRewards, entry.rewards)
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
