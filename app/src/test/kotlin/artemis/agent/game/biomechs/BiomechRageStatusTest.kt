package artemis.agent.game.biomechs

import org.junit.Assert
import org.junit.Test

class BiomechRageStatusTest {
    private companion object {
        val expectedStatuses =
            arrayOf(
                BiomechRageStatus.NEUTRAL,
                BiomechRageStatus.HOSTILE,
                BiomechRageStatus.HOSTILE,
                BiomechRageStatus.HOSTILE,
                BiomechRageStatus.HOSTILE,
            )
    }

    @Test
    fun statusFromRageTest() {
        repeat(expectedStatuses.size) {
            Assert.assertEquals(expectedStatuses[it], BiomechRageStatus[it])
        }
    }
}
