package artemis.agent.game.misc

import com.walkertribe.ian.enums.AudioCommand
import com.walkertribe.ian.protocol.core.comm.AudioCommandPacket
import io.github.serpro69.kfaker.Faker
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AudioEntryTest {
    @Test
    fun propertiesTest() {
        testEntry { entry, audioId, title ->
            Assertions.assertEquals(audioId, entry.audioId)
            Assertions.assertEquals(title, entry.title)
            Assertions.assertEquals(audioId, entry.hashCode())
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

                Assertions.assertEquals(audioId, packet.audioId)
                Assertions.assertEquals(expectedCommand, packet.command)
            }
        }
    }
}
