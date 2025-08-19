package artemis.agent.cpu

import com.walkertribe.ian.enums.AudioMode
import com.walkertribe.ian.iface.CompositeListenerModule
import com.walkertribe.ian.iface.ListenerRegistry
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.ParseResult
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.PacketTestFixture
import com.walkertribe.ian.protocol.core.PacketTestFixture.Companion.writePacketWithHeader
import com.walkertribe.ian.protocol.core.comm.CommsButtonPacketFixture
import com.walkertribe.ian.protocol.core.comm.IncomingAudioPacketFixture
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import io.ktor.utils.io.ByteChannel

class MiscManagerTest :
    DescribeSpec({
        describe("MiscManager") {
            val miscManager = MiscManager()

            val listenerRegistry =
                ListenerRegistry().apply {
                    register(CompositeListenerModule(miscManager.listeners))
                }
            val byteChannel = ByteChannel()
            val reader = PacketReader(byteChannel, listenerRegistry)

            val (playing, incomingAudio) = IncomingAudioPacketFixture.allFixtures()
            val (remove, create, removeAll) =
                CommsButtonPacketFixture.allFixtures(arbLabels = Arb.of("Test"))

            suspend fun <T : Packet.Server> PacketTestFixture.Server<T>.testPacket(
                iterations: Int = PropertyTesting.defaultIterationCount,
                onPacket: (T) -> Unit = {},
            ) {
                generator.checkAll(iterations = iterations) { data ->
                    val payload = data.buildPayload()
                    byteChannel.writePacketWithHeader(packetType, payload)

                    val result = reader.readPacket().shouldBeInstanceOf<ParseResult.Success>()
                    val packet = testType(result.packet)
                    onPacket(packet)
                }
            }

            describe("Initial state") {
                it("Actions do not exist") { miscManager.actionsExist.value.shouldBeFalse() }

                it("Audio does not exist") { miscManager.audioExists.value.shouldBeFalse() }

                it("Action list empty") { miscManager.actions.value.shouldBeEmpty() }

                it("Audio list empty") { miscManager.audio.value.shouldBeEmpty() }

                it("Showing actions") { miscManager.showingAudio.value.shouldBeFalse() }

                it("Inactive") { miscManager.shouldFlash.shouldBeNull() }
            }

            describe("Comms buttons") {
                it("Create") {
                    create.testPacket(iterations = 1) { packet ->
                        miscManager.onPacket(packet)
                        miscManager.actionsExist.value.shouldBeTrue()
                        miscManager.actions.value.shouldBeSingleton {
                            it.label shouldBeEqual "Test"
                        }
                        miscManager.hasUpdate.shouldBeTrue()
                        miscManager.shouldFlash.shouldBeTrue()
                    }
                }

                it("Remove") {
                    remove.testPacket(iterations = 2) { packet ->
                        miscManager.onPacket(packet)
                        miscManager.actionsExist.value.shouldBeTrue()
                        miscManager.actions.value.shouldBeEmpty()
                        miscManager.hasUpdate.shouldBeTrue()
                        miscManager.shouldFlash.shouldBeTrue()
                    }
                }

                it("Reset update") {
                    miscManager.resetUpdate()
                    miscManager.hasUpdate.shouldBeFalse()
                    miscManager.shouldFlash.shouldBeFalse()
                }

                it("Remove All") {
                    create.testPacket(iterations = 1) { packet -> miscManager.onPacket(packet) }

                    removeAll.testPacket(iterations = 1) { packet ->
                        miscManager.onPacket(packet)
                        miscManager.actionsExist.value.shouldBeTrue()
                        miscManager.actions.value.shouldBeEmpty()
                        miscManager.hasUpdate.shouldBeFalse()
                        miscManager.shouldFlash.shouldBeFalse()
                    }
                }

                it("Reset") {
                    miscManager.reset()
                    miscManager.actionsExist.value.shouldBeFalse()
                    miscManager.audioExists.value.shouldBeFalse()
                    miscManager.actions.value.shouldBeEmpty()
                    miscManager.audio.value.shouldBeEmpty()
                    miscManager.hasUpdate.shouldBeFalse()
                    miscManager.shouldFlash.shouldBeNull()
                }
            }

            describe("Incoming audio") {
                it("Does not respond to Playing") {
                    playing.testPacket { packet ->
                        miscManager.onPacket(packet)
                        miscManager.audioExists.value.shouldBeFalse()
                        miscManager.audio.value.shouldBeEmpty()
                        miscManager.hasUpdate.shouldBeFalse()
                        miscManager.shouldFlash.shouldBeNull()
                    }
                }

                it("Responds to Incoming") {
                    incomingAudio.testPacket(iterations = 1) { packet ->
                        miscManager.onPacket(packet)
                        val audioMode = packet.audioMode.shouldBeInstanceOf<AudioMode.Incoming>()

                        miscManager.audioExists.value.shouldBeTrue()
                        miscManager.audio.value.shouldBeSingleton {
                            it.audioId shouldBeEqual packet.audioId
                            it.title shouldBeEqual audioMode.title
                        }
                        miscManager.hasUpdate.shouldBeTrue()
                        miscManager.shouldFlash.shouldBeTrue()
                    }
                }

                it("Reset") {
                    miscManager.reset()
                    miscManager.actionsExist.value.shouldBeFalse()
                    miscManager.audioExists.value.shouldBeFalse()
                    miscManager.actions.value.shouldBeEmpty()
                    miscManager.audio.value.shouldBeEmpty()
                    miscManager.hasUpdate.shouldBeFalse()
                    miscManager.shouldFlash.shouldBeNull()
                }
            }
        }
    })
