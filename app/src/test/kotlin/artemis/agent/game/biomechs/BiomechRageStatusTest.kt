package artemis.agent.game.biomechs

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource

class BiomechRageStatusTest {
    @ParameterizedTest
    @CsvFileSource(resources = ["/artemis/agent/game/biomechs/rage-status.csv"])
    fun statusFromRageTest(index: Int, expectedStatus: BiomechRageStatus) {
        Assertions.assertEquals(expectedStatus, BiomechRageStatus[index])
    }
}
