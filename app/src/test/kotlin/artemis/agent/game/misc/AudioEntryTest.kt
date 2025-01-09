package artemis.agent.game.misc

import com.walkertribe.ian.enums.AudioCommand
import com.walkertribe.ian.protocol.core.comm.AudioCommandPacket
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class AudioEntryTest :
    DescribeSpec({
        suspend fun testAudioEntry(test: (AudioEntry, Int, String) -> Unit) {
            checkAll(Arb.int(), Arb.string()) { audioId, title ->
                test(AudioEntry(audioId, title), audioId, title)
            }
        }

        suspend fun testPacketType(
            command: AudioCommand,
            getPacket: AudioEntry.() -> AudioCommandPacket,
        ) {
            testAudioEntry { entry, audioId, _ ->
                val packet = entry.getPacket()
                packet.audioId shouldBeEqual audioId
                packet.command shouldBeEqual command
            }
        }

        describe("AudioEntry") {
            describe("Properties") {
                it("Audio ID") {
                    testAudioEntry { entry, audioId, _ -> entry.audioId shouldBeEqual audioId }
                }

                it("Title") {
                    testAudioEntry { entry, _, title -> entry.title shouldBeEqual title }
                }

                it("Hash code") {
                    testAudioEntry { entry, audioId, _ -> entry.hashCode() shouldBeEqual audioId }
                }
            }

            it("Play packet") { testPacketType(AudioCommand.PLAY) { playPacket } }

            it("Dismiss packet") { testPacketType(AudioCommand.DISMISS) { dismissPacket } }
        }
    })
