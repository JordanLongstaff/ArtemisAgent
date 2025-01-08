package artemis.agent.game.route

import artemis.agent.R
import artemis.agent.game.ObjectEntry
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Test

class RouteTaskIncentiveTest {
    @Test
    fun matchesTest() {
        val allIncentives = RouteTaskIncentive.entries
        val allyEntry = allyEntry

        testData.forEach { data ->
            every { allyEntry.status } returns data.status
            every { allyEntry.hasEnergy } returns data.energy

            val matchingIncentives = allIncentives.filter { it.matches(allyEntry) }
            Assert.assertEquals(1, matchingIncentives.size)
            Assert.assertEquals(data.incentive, matchingIncentives.first())
        }
    }

    @Test
    fun textTest() {
        val allyEntry = allyEntry

        testData.forEach { data ->
            every { allyEntry.status } returns data.status
            every { allyEntry.hasEnergy } returns data.energy

            Assert.assertEquals(data.text, textMap[data.incentive.getTextFor(allyEntry)])
        }
    }

    private companion object {
        val allyEntry by lazy { mockk<ObjectEntry.Ally>() }

        @OptIn(ExperimentalSerializationApi::class)
        val testData by lazy {
            RouteTaskIncentive::class.java.getResourceAsStream("task-incentives.json")!!.use {
                Json.decodeFromStream<List<RouteTaskIncentiveTestData>>(it)
            }
        }

        val textMap by lazy {
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
        }

        @JvmStatic
        @AfterClass
        fun cleanup() {
            clearMocks(allyEntry)
        }
    }
}
