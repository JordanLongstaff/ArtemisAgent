package artemis.agent.game.misc

import com.walkertribe.ian.util.JamCrc
import io.github.serpro69.kfaker.Faker
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CommsActionEntryTest {
    @Test
    fun propertiesTest() {
        val label = Faker().random.randomString()
        val entry = CommsActionEntry(label)
        val expectedHash = JamCrc.compute(label)

        Assertions.assertEquals(label, entry.label)
        Assertions.assertEquals(expectedHash, entry.hashCode())
    }
}
