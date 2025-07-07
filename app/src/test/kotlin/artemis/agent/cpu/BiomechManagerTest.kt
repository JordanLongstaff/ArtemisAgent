package artemis.agent.cpu

import artemis.agent.game.biomechs.BiomechEntry
import artemis.agent.game.biomechs.BiomechRageStatus
import artemis.agent.userSettings
import com.walkertribe.ian.iface.CompositeListenerModule
import com.walkertribe.ian.iface.ListenerRegistry
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.ParseResult
import com.walkertribe.ian.protocol.core.PacketTestFixture.Companion.writePacketWithHeader
import com.walkertribe.ian.protocol.core.world.BiomechRagePacket
import com.walkertribe.ian.protocol.core.world.BiomechRagePacketFixture
import com.walkertribe.ian.world.ArtemisNpc
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.positiveInt
import io.kotest.property.checkAll
import io.ktor.utils.io.ByteChannel

class BiomechManagerTest :
    DescribeSpec({
        describe("BiomechManager") {
            val biomechManager = BiomechManager()

            describe("Initial state") {
                it("Enabled") { biomechManager.enabled.shouldBeTrue() }

                it("Not yet confirmed") { biomechManager.confirmed.shouldBeFalse() }

                it("No BioMechs") {
                    biomechManager.scanned.shouldBeEmpty()
                    biomechManager.unscanned.shouldBeEmpty()
                }

                it("No destroyed BioMechs") {
                    biomechManager.destroyedBiomechName.replayCache.shouldBeEmpty()
                }

                it("No next active BioMech") {
                    biomechManager.nextActiveBiomech.replayCache.shouldBeEmpty()
                }

                it("Neutral rage") {
                    biomechManager.rageStatus.value shouldBeEqual BiomechRageStatus.NEUTRAL
                }

                it("No sorting") {
                    biomechManager.sorter.apply {
                        sortByClassFirst.shouldBeFalse()
                        sortByStatus.shouldBeFalse()
                        sortByClassSecond.shouldBeFalse()
                        sortByName.shouldBeFalse()
                    }
                }

                it("Default freeze time") { biomechManager.freezeTime shouldBeEqual 220_000L }

                it("No updates") { biomechManager.hasUpdate.shouldBeFalse() }

                it("Inactive") { biomechManager.shouldFlash.shouldBeNull() }
            }

            it("Confirmed") {
                biomechManager.confirmed = true
                biomechManager.shouldFlash!!.shouldBeFalse()
            }

            describe("Responds to rage packets") {
                val neutral = BiomechRagePacketFixture(arbRage = Arb.of(0))
                val hostile = BiomechRagePacketFixture(arbRage = Arb.positiveInt())

                val listenerRegistry =
                    ListenerRegistry().apply {
                        register(CompositeListenerModule(biomechManager.listeners))
                    }
                val byteChannel = ByteChannel()
                val reader = PacketReader(byteChannel, listenerRegistry)

                suspend fun BiomechRagePacketFixture.testPacket(
                    iterations: Int = PropertyTesting.defaultIterationCount,
                    onPacket: (BiomechRagePacket) -> Unit = {},
                ) {
                    generator.checkAll(iterations = iterations) { data ->
                        val payload = data.buildPayload()
                        byteChannel.writePacketWithHeader(packetType, payload)

                        val result = reader.readPacket().shouldBeInstanceOf<ParseResult.Success>()
                        val packet = testType(result.packet)
                        onPacket(packet)
                    }
                }

                it("Neutral") {
                    neutral.testPacket(iterations = 1) { packet ->
                        biomechManager.onPacket(packet)
                        biomechManager.rageStatus.value shouldBeEqual BiomechRageStatus.NEUTRAL
                        biomechManager.hasUpdate.shouldBeFalse()
                    }
                }

                it("Hostile") {
                    hostile.testPacket { packet ->
                        biomechManager.onPacket(packet)
                        biomechManager.rageStatus.value shouldBeEqual BiomechRageStatus.HOSTILE
                        biomechManager.hasUpdate.shouldBeTrue()
                        biomechManager.shouldFlash!!.shouldBeTrue()
                    }
                }

                it("Reset update") {
                    biomechManager.resetUpdate()
                    biomechManager.hasUpdate.shouldBeFalse()
                    biomechManager.shouldFlash!!.shouldBeFalse()
                }

                it("Back to neutral") {
                    neutral.testPacket(iterations = 1) { packet ->
                        biomechManager.onPacket(packet)
                        biomechManager.rageStatus.value shouldBeEqual BiomechRageStatus.NEUTRAL
                        biomechManager.hasUpdate.shouldBeFalse()
                    }
                }

                it("Notify update") {
                    biomechManager.notifyUpdate()
                    biomechManager.hasUpdate.shouldBeTrue()
                    biomechManager.shouldFlash!!.shouldBeTrue()
                }

                it("Neutral again") {
                    neutral.testPacket(iterations = 1) { packet ->
                        biomechManager.onPacket(packet)
                        biomechManager.rageStatus.value shouldBeEqual BiomechRageStatus.NEUTRAL
                        biomechManager.hasUpdate.shouldBeTrue()
                    }
                }
            }

            describe("Update from settings") {
                val settings = userSettings {
                    biomechsEnabled = false
                    freezeDurationSeconds = 1
                    biomechSortClassFirst = true
                    biomechSortStatus = true
                    biomechSortClassSecond = true
                    biomechSortName = true
                }
                biomechManager.updateFromSettings(settings)

                it("Enabled") {
                    biomechManager.enabled.shouldBeFalse()
                    biomechManager.shouldFlash.shouldBeNull()
                }

                it("Freeze time") { biomechManager.freezeTime shouldBeEqual 1000L }

                describe("Sort settings") {
                    it("Class first") { biomechManager.sorter.sortByClassFirst.shouldBeTrue() }
                    it("Status") { biomechManager.sorter.sortByStatus.shouldBeTrue() }
                    it("Class second") { biomechManager.sorter.sortByClassSecond.shouldBeTrue() }
                    it("Name") { biomechManager.sorter.sortByName.shouldBeTrue() }
                }
            }

            describe("Revert settings") {
                biomechManager.enabled = true
                val settings = userSettings { biomechManager.revertSettings(this) }

                it("Enabled") { settings.biomechsEnabled.shouldBeTrue() }

                it("Freeze time") { settings.freezeDurationSeconds shouldBeEqual 1 }

                describe("Sort settings") {
                    it("Class first") { settings.biomechSortClassFirst.shouldBeTrue() }
                    it("Status") { settings.biomechSortStatus.shouldBeTrue() }
                    it("Class second") { settings.biomechSortClassSecond.shouldBeTrue() }
                    it("Name") { settings.biomechSortName.shouldBeTrue() }
                }
            }

            describe("Reset") {
                biomechManager.scanned.add(BiomechEntry(ArtemisNpc(0, 0L)))
                biomechManager.unscanned[0] = ArtemisNpc(0, 0L)
                biomechManager.reset()

                it("No longer confirmed") { biomechManager.confirmed.shouldBeFalse() }

                it("No more updates") { biomechManager.hasUpdate.shouldBeFalse() }

                it("Back to neutral") {
                    biomechManager.rageStatus.value shouldBeEqual BiomechRageStatus.NEUTRAL
                }

                it("No BioMechs") {
                    biomechManager.scanned.shouldBeEmpty()
                    biomechManager.unscanned.shouldBeEmpty()
                }
            }
        }
    })
