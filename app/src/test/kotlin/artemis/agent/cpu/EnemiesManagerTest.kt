package artemis.agent.cpu

import artemis.agent.game.enemies.EnemyCaptainStatus
import artemis.agent.game.enemies.EnemyEntry
import artemis.agent.game.enemies.TauntStatus
import artemis.agent.userSettings
import com.walkertribe.ian.enums.IntelType
import com.walkertribe.ian.iface.CompositeListenerModule
import com.walkertribe.ian.iface.ListenerRegistry
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.ParseResult
import com.walkertribe.ian.protocol.core.PacketTestFixture.Companion.writePacketWithHeader
import com.walkertribe.ian.protocol.core.world.IntelPacket
import com.walkertribe.ian.protocol.core.world.IntelPacketFixture
import com.walkertribe.ian.util.FilePathResolver
import com.walkertribe.ian.vesseldata.Taunt
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.world.ArtemisNpc
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import io.ktor.utils.io.ByteChannel
import java.io.File
import okio.Path.Companion.toOkioPath

class EnemiesManagerTest :
    DescribeSpec({
        describe("EnemiesManager") {
            val enemiesManager = EnemiesManager()

            val tmpDir = tempdir()
            val datDir = File(tmpDir, "dat")
            datDir.mkdir()
            val datFile = File(datDir, "vesselData.xml")
            datFile.writeText(EnemyEntry::class.java.getResource("vesselData.xml")!!.readText())

            val vesselData = VesselData.load(FilePathResolver(tmpDir.toOkioPath()))
            val faction = vesselData.getFaction(1)!!
            val vessel = vesselData[1]!!
            val enemy = EnemyEntry(ArtemisNpc(0, 0L), vessel, faction, vesselData)

            describe("Initial state") {
                it("Enabled") { enemiesManager.enabled.shouldBeTrue() }

                it("No enemies") {
                    enemiesManager.allEnemies.shouldBeEmpty()
                    enemiesManager.nameIndex.shouldBeEmpty()
                    enemiesManager.displayedEnemies.replayCache.shouldBeEmpty()
                }

                it("Cannot get enemy by name") {
                    checkAll<String> { name -> enemiesManager.getEnemyByName(name).shouldBeNull() }
                }

                it("No selection") {
                    enemiesManager.selection.value.shouldBeNull()
                    enemiesManager.selectionIndex.value shouldBeEqual -1
                }

                it("No sorting") {
                    enemiesManager.sorter.apply {
                        sortBySurrendered.shouldBeFalse()
                        sortByFaction.shouldBeFalse()
                        sortByFactionReversed.shouldBeFalse()
                        sortByName.shouldBeFalse()
                        sortByDistance.shouldBeFalse()
                    }
                }

                it("No categories") { enemiesManager.categories.value.shouldBeEmpty() }

                it("No taunts") { enemiesManager.taunts.value.shouldBeEmpty() }

                it("No intel") { enemiesManager.intel.value.shouldBeNull() }

                it("Showing taunt statuses") { enemiesManager.showTauntStatuses.shouldBeTrue() }

                it("Showing intel") { enemiesManager.showIntel.shouldBeTrue() }

                it("Ineffective taunts disabled") {
                    enemiesManager.disableIneffectiveTaunts.shouldBeTrue()
                }

                it("Max surrender distance") {
                    enemiesManager.maxSurrenderDistance.shouldNotBeNull()
                }

                it("No updates") { enemiesManager.hasUpdate.shouldBeFalse() }

                it("Inactive") { enemiesManager.shouldFlash.shouldBeNull() }

                it("No destroyed enemies") {
                    enemiesManager.destroyedEnemyName.replayCache.shouldBeEmpty()
                }

                it("No perfidious enemies") { enemiesManager.perfidy.replayCache.shouldBeEmpty() }
            }

            describe("Add enemy") {
                enemiesManager.addEnemy(enemy, "Test")

                it("Exists in records") { enemiesManager.allEnemies.shouldContain(0, enemy) }

                it("Exists in name index") { enemiesManager.nameIndex.shouldContain("Test", 0) }

                it("Get by name") {
                    enemiesManager.getEnemyByName("Test").shouldNotBeNull() shouldBe enemy
                }

                it("Active") { enemiesManager.shouldFlash.shouldBeFalse() }

                it("Has update") {
                    enemiesManager.hasUpdate = true
                    enemiesManager.shouldFlash.shouldBeTrue()
                }
            }

            describe("Refresh taunts") {
                it("Without selection") {
                    enemiesManager.refreshTaunts()
                    enemiesManager.taunts.value.shouldBeEmpty()
                }

                it("With selection") {
                    enemiesManager.selection.value = enemy
                    enemiesManager.refreshTaunts()
                    enemiesManager.taunts.value shouldHaveSize Taunt.COUNT
                }
            }

            describe("Responds to intel packets") {
                val easilyOffended = "easily offended."
                val bombastic = "bombastic."

                val arbIntel =
                    Arb.bind(Arb.int(0..2), Arb.of(easilyOffended, bombastic, null)) {
                        tauntIndex,
                        status ->
                        val (immunity) = enemy.faction.taunts[tauntIndex]
                        val suffix = status?.let { ", and is $it" } ?: "."
                        "Intel: The captain $immunity$suffix"
                    }

                val intelTypeCount = IntelType.entries.size
                val intelFixtures =
                    List(2) { index ->
                            IntelPacketFixture.allFixtures(
                                arbId = Arb.of(index),
                                arbText = arbIntel,
                            )
                        }
                        .flatten()

                val wrongIntel = intelFixtures[0]
                val correctIntel = intelFixtures[IntelType.LEVEL_2_SCAN.ordinal]
                val wrongId = intelFixtures[intelTypeCount + IntelType.LEVEL_2_SCAN.ordinal]

                val listenerRegistry =
                    ListenerRegistry().apply {
                        register(CompositeListenerModule(enemiesManager.listeners))
                    }
                val byteChannel = ByteChannel()
                val reader = PacketReader(byteChannel, listenerRegistry)

                suspend fun IntelPacketFixture.testPacket(
                    iterations: Int = PropertyTesting.defaultIterationCount,
                    onPacket: (IntelPacket) -> Unit = {},
                ) {
                    generator.checkAll(iterations = iterations) { data ->
                        val payload = data.buildPayload()
                        byteChannel.writePacketWithHeader(packetType, payload)

                        val result = reader.readPacket().shouldBeInstanceOf<ParseResult.Success>()
                        val packet = testType(result.packet)
                        onPacket(packet)
                    }
                }

                it("Wrong intel") {
                    wrongIntel.testPacket(iterations = 1) { packet ->
                        enemiesManager.onIntel(packet)
                        enemy.intel.shouldBeNull()
                    }
                }

                it("Wrong ID") {
                    wrongId.testPacket(iterations = 1) { packet ->
                        enemiesManager.onIntel(packet)
                        enemy.intel.shouldBeNull()
                    }
                }

                it("Valid intel") {
                    correctIntel.testPacket { packet ->
                        enemy.tauntStatuses.fill(TauntStatus.UNUSED)
                        enemiesManager.onIntel(packet)

                        val intel = enemy.intel.shouldNotBeNull()
                        intel shouldBe packet.intel
                        val status =
                            if (intel.endsWith(easilyOffended)) {
                                enemy.tauntStatuses.shouldContainOnly(TauntStatus.UNUSED)
                                EnemyCaptainStatus.EASILY_OFFENDED
                            } else {
                                enemy.tauntStatuses.shouldContainAll(
                                    TauntStatus.UNUSED,
                                    TauntStatus.INEFFECTIVE,
                                )
                                EnemyCaptainStatus.NORMAL
                            }
                        enemy.captainStatus shouldBe status
                    }
                }
            }

            describe("Update from settings") {
                val settings = userSettings {
                    enemiesEnabled = false
                    enemySortSurrendered = true
                    enemySortFaction = true
                    enemySortFactionReversed = true
                    enemySortName = true
                    enemySortDistance = true
                    surrenderRangeEnabled = false
                    showTauntStatuses = false
                    showEnemyIntel = false
                    disableIneffectiveTaunts = false
                }
                enemiesManager.updateFromSettings(settings)

                it("Enabled") { enemiesManager.enabled.shouldBeFalse() }

                it("Inactive") { enemiesManager.shouldFlash.shouldBeNull() }

                it("Not showing taunt statuses") {
                    enemiesManager.showTauntStatuses.shouldBeFalse()
                }

                it("Not showing intel") { enemiesManager.showIntel.shouldBeFalse() }

                it("Ineffective taunts enabled") {
                    enemiesManager.disableIneffectiveTaunts.shouldBeFalse()
                }

                describe("Sort settings") {
                    it("Surrendered") { enemiesManager.sorter.sortBySurrendered.shouldBeTrue() }
                    it("Faction") { enemiesManager.sorter.sortByFaction.shouldBeTrue() }
                    it("Faction reversed") {
                        enemiesManager.sorter.sortByFactionReversed.shouldBeTrue()
                    }
                    it("Name") { enemiesManager.sorter.sortByName.shouldBeTrue() }
                    it("Distance") { enemiesManager.sorter.sortByDistance.shouldBeTrue() }
                }

                describe("Max surrender distance") {
                    it("Disabled") { enemiesManager.maxSurrenderDistance.shouldBeNull() }

                    it("Enabled") {
                        val otherSettings = userSettings {
                            surrenderRangeEnabled = true
                            surrenderRange = 10000f
                        }
                        enemiesManager.updateFromSettings(otherSettings)
                        enemiesManager.maxSurrenderDistance.shouldNotBeNull() shouldBeEqual 10000f
                    }

                    enemiesManager.updateFromSettings(settings)
                }
            }

            describe("Revert settings") {
                enemiesManager.enabled = true
                enemiesManager.showIntel = true
                enemiesManager.showTauntStatuses = true
                enemiesManager.disableIneffectiveTaunts = true
                enemiesManager.maxSurrenderDistance = 100f
                val settings = userSettings { enemiesManager.revertSettings(this) }

                it("Enabled") { settings.enemiesEnabled.shouldBeTrue() }

                it("Showing taunt statuses") { settings.showTauntStatuses.shouldBeTrue() }

                it("Showing intel") { settings.showEnemyIntel.shouldBeTrue() }

                it("Ineffective taunts disabled") {
                    settings.disableIneffectiveTaunts.shouldBeTrue()
                }

                describe("Sort settings") {
                    it("Surrendered") { settings.enemySortSurrendered.shouldBeTrue() }
                    it("Faction") { settings.enemySortFaction.shouldBeTrue() }
                    it("Faction reversed") { settings.enemySortFactionReversed.shouldBeTrue() }
                    it("Name") { settings.enemySortName.shouldBeTrue() }
                    it("Distance") { settings.enemySortDistance.shouldBeTrue() }
                }

                describe("Max surrender distance") {
                    it("Enabled") {
                        settings.surrenderRangeEnabled.shouldBeTrue()
                        settings.surrenderRange shouldBeEqual 100f
                    }

                    it("Disabled") {
                        enemiesManager.maxSurrenderDistance = null
                        val otherSettings = userSettings { enemiesManager.revertSettings(this) }
                        otherSettings.surrenderRangeEnabled.shouldBeFalse()
                    }
                }
            }

            describe("Reset") {
                enemiesManager.reset()

                it("No enemies") {
                    enemiesManager.allEnemies.shouldBeEmpty()
                    enemiesManager.nameIndex.shouldBeEmpty()
                }

                it("No selection") { enemiesManager.selection.value.shouldBeNull() }
            }
        }
    })
