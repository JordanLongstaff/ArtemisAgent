package artemis.agent.game.misc

import com.walkertribe.ian.util.JamCrc
import io.github.serpro69.kfaker.Faker
import org.junit.Assert
import org.junit.Test

class CommsActionEntryTest {
    @Test
    fun propertiesTest() {
        val label = Faker().random.randomString()
        val entry = CommsActionEntry(label)
        val expectedHash = JamCrc.compute(label)

        Assert.assertEquals(label, entry.label)
        Assert.assertEquals(expectedHash, entry.hashCode())
    }
}
