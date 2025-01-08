package artemis.agent.game.misc

import com.walkertribe.ian.enums.AudioCommand
import com.walkertribe.ian.protocol.core.comm.AudioCommandPacket
import io.github.serpro69.kfaker.Faker
import org.junit.Assert
import org.junit.Test

class AudioEntryTest {
    @Test
    fun propertiesTest() {
        testEntry { entry, audioId, title ->
            Assert.assertEquals(audioId, entry.audioId)
            Assert.assertEquals(title, entry.title)
            Assert.assertEquals(audioId, entry.hashCode())
        }
    }

    @Test
    fun playPacketTest() {
        testPacketType(AudioCommand.PLAY) { playPacket }
    }

    @Test
    fun dismissPacketTest() {
        testPacketType(AudioCommand.DISMISS) { dismissPacket }
    }

    private companion object {
        val faker by lazy { Faker() }

        fun testEntry(test: (AudioEntry, Int, String) -> Unit) {
            val audioId = faker.random.nextInt()
            val title = faker.random.randomString()
            val entry = AudioEntry(audioId, title)
            test(entry, audioId, title)
        }

        fun testPacketType(
            expectedCommand: AudioCommand,
            getPacket: AudioEntry.() -> AudioCommandPacket,
        ) {
            testEntry { entry, audioId, _ ->
                val packet = entry.getPacket()

                Assert.assertEquals(audioId, packet.audioId)
                Assert.assertEquals(expectedCommand, packet.command)
            }
        }
    }
}
