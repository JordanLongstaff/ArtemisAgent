package artemis.agent.game.allies

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AllyStatusTest {
    @Test
    fun sortIndexTest() {
        testCases.forEach { Assertions.assertEquals(it.expectedSortIndex, it.allyStatus.sortIndex) }
    }

    @Test
    fun pirateAllyStatusTest() {
        testPirateSensitiveEquivalents(true)
    }

    @Test
    fun nonPirateAllyStatusTest() {
        testPirateSensitiveEquivalents(false)
    }

    private companion object {
        @OptIn(ExperimentalSerializationApi::class)
        val testCases by lazy {
            AllyStatus::class.java.getResourceAsStream("ally-statuses.json")!!.use {
                Json.decodeFromStream<List<AllyStatusTestCase>>(it)
            }
        }

        fun testPirateSensitiveEquivalents(isPirate: Boolean) {
            testCases.forEach {
                Assertions.assertEquals(
                    it.getStatusForPirate(isPirate),
                    it.allyStatus.getPirateSensitiveEquivalent(isPirate),
                )
            }
        }
    }
}
