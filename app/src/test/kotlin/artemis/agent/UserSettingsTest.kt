package artemis.agent

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.engine.names.WithDataTestName
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.file.shouldNotBeEmpty

class UserSettingsTest :
    DescribeSpec({
        describe("UserSettings") {
            val expectedLatestVersion = 3

            describe("Defaults") {
                val settings = UserSettingsSerializer.defaultValue

                it("Latest version") { settings.version shouldBeEqual expectedLatestVersion }

                it("Vessel data location") {
                    settings.vesselDataLocation shouldBeEqual
                        UserSettingsOuterClass.UserSettings.VesselDataLocation
                            .VESSEL_DATA_LOCATION_DEFAULT
                }

                it("Server port") { settings.serverPort shouldBeEqual 2010 }

                it("Show network info") { settings.showNetworkInfo.shouldBeTrue() }

                describe("Recent address limit") {
                    it("Value") { settings.recentAddressLimit shouldBeEqual 20 }

                    it("Enabled") { settings.recentAddressLimitEnabled.shouldBeFalse() }
                }

                it("Update interval") { settings.updateInterval shouldBeEqual 50 }

                it("Connection timeout") { settings.connectionTimeoutSeconds shouldBeEqual 9 }

                it("Server timeout") { settings.serverTimeoutSeconds shouldBeEqual 15 }

                it("Scan timeout") { settings.scanTimeoutSeconds shouldBeEqual 5 }

                it("Always scan publicly") { settings.alwaysScanPublic.shouldBeFalse() }

                it("Theme") {
                    settings.theme shouldBeEqual
                        UserSettingsOuterClass.UserSettings.Theme.THEME_DEFAULT
                }

                it("Three-digit directions") { settings.threeDigitDirections.shouldBeTrue() }

                it("Sound volume") { settings.soundVolume shouldBeEqual 50 }

                it("Mute") { settings.soundMuted.shouldBeFalse() }

                it("Haptics") { settings.hapticsEnabled.shouldBeTrue() }

                describe("Missions") {
                    describe("Enabled") {
                        it("All") { settings.missionsEnabled.shouldBeTrue() }

                        it("Battery charges") { settings.displayRewardBattery.shouldBeTrue() }
                        it("Extra coolant") { settings.displayRewardCoolant.shouldBeTrue() }
                        it("Nuke torpedoes") { settings.displayRewardNukes.shouldBeTrue() }
                        it("Production speed") { settings.displayRewardProduction.shouldBeTrue() }
                        it("Shield enhancements") { settings.displayRewardShield.shouldBeTrue() }
                    }

                    describe("Dismissal") {
                        it("Enabled") { settings.completedMissionDismissalEnabled.shouldBeTrue() }
                        it("Seconds") { settings.completedMissionDismissalSeconds shouldBeEqual 3 }
                    }
                }

                describe("Allies") {
                    it("Enabled") { settings.alliesEnabled.shouldBeTrue() }

                    describe("Sorting") {
                        it("Class first") { settings.allySortClassFirst.shouldBeFalse() }
                        it("Energy first") { settings.allySortEnergyFirst.shouldBeFalse() }
                        it("Status") { settings.allySortStatus.shouldBeFalse() }
                        it("Class second") { settings.allySortClassSecond.shouldBeFalse() }
                        it("Name") { settings.allySortName.shouldBeFalse() }
                    }

                    it("Manually return from commands") {
                        settings.allyCommandManualReturn.shouldBeFalse()
                    }

                    it("Show destroyed allies") { settings.showDestroyedAllies.shouldBeTrue() }
                }

                describe("Enemies") {
                    it("Enabled") { settings.enemiesEnabled.shouldBeTrue() }

                    describe("Sorting") {
                        it("Surrender status") { settings.enemySortSurrendered.shouldBeFalse() }
                        it("Faction") { settings.enemySortFaction.shouldBeFalse() }
                        it("Faction reversed") { settings.enemySortFactionReversed.shouldBeFalse() }
                        it("Name") { settings.enemySortName.shouldBeFalse() }
                        it("Distance") { settings.enemySortDistance.shouldBeFalse() }
                    }

                    describe("Max surrender range") {
                        it("Enabled") { settings.surrenderRangeEnabled.shouldBeTrue() }
                        it("Value") { settings.surrenderRange shouldBeEqual 5000f }
                    }

                    describe("Surrender bursts") {
                        it("Count") { settings.surrenderBurstCount shouldBeEqual 1 }
                        it("Interval") { settings.surrenderBurstInterval shouldBeEqual 500 }
                    }

                    it("Show enemy intel") { settings.showEnemyIntel.shouldBeTrue() }
                    it("Show taunt statuses") { settings.showTauntStatuses.shouldBeTrue() }
                    it("Disable ineffective taunts") {
                        settings.disableIneffectiveTaunts.shouldBeTrue()
                    }
                }

                describe("Biomechs") {
                    it("Enabled") { settings.biomechsEnabled.shouldBeTrue() }

                    describe("Sorting") {
                        it("Class first") { settings.biomechSortClassFirst.shouldBeFalse() }
                        it("Status") { settings.biomechSortStatus.shouldBeFalse() }
                        it("Class second") { settings.biomechSortClassSecond.shouldBeFalse() }
                        it("Name") { settings.biomechSortName.shouldBeFalse() }
                    }

                    it("Freeze duration") { settings.freezeDurationSeconds shouldBeEqual 220 }
                }

                describe("Routing") {
                    it("Enabled") { settings.routingEnabled.shouldBeTrue() }

                    describe("Incentives") {
                        it("Missions") { settings.routeMissions.shouldBeTrue() }
                        it("Needs DamCon") { settings.routeNeedsDamcon.shouldBeTrue() }
                        it("Needs energy") { settings.routeNeedsEnergy.shouldBeTrue() }
                        it("Has energy") { settings.routeHasEnergy.shouldBeTrue() }
                        it("Malfunction") { settings.routeMalfunction.shouldBeTrue() }
                        it("Ambassador") { settings.routeAmbassador.shouldBeTrue() }
                        it("Hostage") { settings.routeHostage.shouldBeTrue() }
                        it("Commandeered") { settings.routeCommandeered.shouldBeTrue() }
                    }

                    describe("Avoidances") {
                        data class AvoidanceSetting(
                            val name: String,
                            val enabled: Boolean,
                            val clearance: Float,
                            val defaultClearance: Float,
                        ) : WithDataTestName {
                            fun test() {
                                enabled.shouldBeTrue()
                                clearance shouldBeEqual defaultClearance
                            }

                            override fun dataTestName() = name
                        }

                        withData(
                            AvoidanceSetting(
                                "Black holes",
                                settings.avoidBlackHoles,
                                settings.blackHoleClearance,
                                500f,
                            ),
                            AvoidanceSetting(
                                "Mines",
                                settings.avoidMines,
                                settings.mineClearance,
                                1000f,
                            ),
                            AvoidanceSetting(
                                "Typhon",
                                settings.avoidTyphon,
                                settings.typhonClearance,
                                3000f,
                            ),
                        ) {
                            it.test()
                        }
                    }
                }
            }

            describe("Migrations") {
                val settings = UserSettingsSerializer.defaultValue

                describe("Newer version") {
                    val newMigration = UserSettingsSerializer.Migration(1000) { serverPort = 3000 }

                    it("Should migrate") { newMigration.shouldMigrate(settings).shouldBeTrue() }

                    it("Does migrate") {
                        val newSettings = newMigration.migrate(settings)
                        newSettings.version shouldBeEqual 1000
                        newSettings.serverPort shouldBeEqual 3000
                    }
                }

                describe("Older version") {
                    val skippedMigration = UserSettingsSerializer.Migration(-1) {}

                    it("Should not migrate") {
                        skippedMigration.shouldMigrate(settings).shouldBeFalse()
                    }
                }

                describe("Static helper function") {
                    it("Should contain next version") {
                        UserSettingsSerializer.Migration.migration {}.version shouldBeEqual
                            expectedLatestVersion + 1
                    }
                }
            }

            describe("Serialization") {
                val tempFile = tempfile()

                it("Write to stream") {
                    val settings = UserSettingsSerializer.defaultValue
                    tempFile.outputStream().use { UserSettingsSerializer.writeTo(settings, it) }
                    tempFile.shouldNotBeEmpty()
                }

                it("Read from stream") {
                    val settings =
                        tempFile.inputStream().use { UserSettingsSerializer.readFrom(it) }
                    val defaultSettings = UserSettingsSerializer.defaultValue

                    settings.vesselDataLocation shouldBeEqual defaultSettings.vesselDataLocation
                    settings.serverPort shouldBeEqual defaultSettings.serverPort
                    settings.showNetworkInfo shouldBeEqual defaultSettings.showNetworkInfo
                    settings.recentAddressLimit shouldBeEqual defaultSettings.recentAddressLimit
                    settings.recentAddressLimitEnabled shouldBeEqual
                        defaultSettings.recentAddressLimitEnabled
                    settings.updateInterval shouldBeEqual defaultSettings.updateInterval
                    settings.connectionTimeoutSeconds shouldBeEqual
                        defaultSettings.connectionTimeoutSeconds
                    settings.serverTimeoutSeconds shouldBeEqual defaultSettings.serverTimeoutSeconds
                    settings.scanTimeoutSeconds shouldBeEqual defaultSettings.scanTimeoutSeconds
                    settings.alwaysScanPublic shouldBeEqual defaultSettings.alwaysScanPublic
                    settings.theme shouldBeEqual defaultSettings.theme
                    settings.threeDigitDirections shouldBeEqual defaultSettings.threeDigitDirections
                    settings.soundVolume shouldBeEqual defaultSettings.soundVolume
                    settings.soundMuted shouldBeEqual defaultSettings.soundMuted
                    settings.hapticsEnabled shouldBeEqual defaultSettings.hapticsEnabled
                    settings.missionsEnabled shouldBeEqual defaultSettings.missionsEnabled
                    settings.displayRewardBattery shouldBeEqual defaultSettings.displayRewardBattery
                    settings.displayRewardCoolant shouldBeEqual defaultSettings.displayRewardCoolant
                    settings.displayRewardNukes shouldBeEqual defaultSettings.displayRewardNukes
                    settings.displayRewardProduction shouldBeEqual
                        defaultSettings.displayRewardProduction
                    settings.displayRewardShield shouldBeEqual defaultSettings.displayRewardShield
                    settings.completedMissionDismissalEnabled shouldBeEqual
                        defaultSettings.completedMissionDismissalEnabled
                    settings.completedMissionDismissalSeconds shouldBeEqual
                        defaultSettings.completedMissionDismissalSeconds
                    settings.alliesEnabled shouldBeEqual defaultSettings.alliesEnabled
                    settings.allySortClassFirst shouldBeEqual defaultSettings.allySortClassFirst
                    settings.allySortStatus shouldBeEqual defaultSettings.allySortStatus
                    settings.allySortClassSecond shouldBeEqual defaultSettings.allySortClassSecond
                    settings.allySortName shouldBeEqual defaultSettings.allySortName
                    settings.allySortEnergyFirst shouldBeEqual defaultSettings.allySortEnergyFirst
                    settings.allyCommandManualReturn shouldBeEqual
                        defaultSettings.allyCommandManualReturn
                    settings.showDestroyedAllies shouldBeEqual defaultSettings.showDestroyedAllies
                    settings.enemiesEnabled shouldBeEqual defaultSettings.enemiesEnabled
                    settings.enemySortFaction shouldBeEqual defaultSettings.enemySortFaction
                    settings.enemySortFactionReversed shouldBeEqual
                        defaultSettings.enemySortFactionReversed
                    settings.enemySortName shouldBeEqual defaultSettings.enemySortName
                    settings.enemySortDistance shouldBeEqual defaultSettings.enemySortDistance
                    settings.enemySortSurrendered shouldBeEqual defaultSettings.enemySortSurrendered
                    settings.surrenderRange shouldBeEqual defaultSettings.surrenderRange
                    settings.surrenderRangeEnabled shouldBeEqual
                        defaultSettings.surrenderRangeEnabled
                    settings.surrenderBurstCount shouldBeEqual defaultSettings.surrenderBurstCount
                    settings.surrenderBurstInterval shouldBeEqual
                        defaultSettings.surrenderBurstInterval
                    settings.showEnemyIntel shouldBeEqual defaultSettings.showEnemyIntel
                    settings.showTauntStatuses shouldBeEqual defaultSettings.showTauntStatuses
                    settings.disableIneffectiveTaunts shouldBeEqual
                        defaultSettings.disableIneffectiveTaunts
                    settings.biomechsEnabled shouldBeEqual defaultSettings.biomechsEnabled
                    settings.biomechSortClassFirst shouldBeEqual
                        defaultSettings.biomechSortClassFirst
                    settings.biomechSortStatus shouldBeEqual defaultSettings.biomechSortStatus
                    settings.biomechSortClassSecond shouldBeEqual
                        defaultSettings.biomechSortClassSecond
                    settings.biomechSortName shouldBeEqual defaultSettings.biomechSortName
                    settings.freezeDurationSeconds shouldBeEqual
                        defaultSettings.freezeDurationSeconds
                    settings.routingEnabled shouldBeEqual defaultSettings.routingEnabled
                    settings.routeMissions shouldBeEqual defaultSettings.routeMissions
                    settings.routeNeedsDamcon shouldBeEqual defaultSettings.routeNeedsDamcon
                    settings.routeNeedsEnergy shouldBeEqual defaultSettings.routeNeedsEnergy
                    settings.routeHasEnergy shouldBeEqual defaultSettings.routeHasEnergy
                    settings.routeMalfunction shouldBeEqual defaultSettings.routeMalfunction
                    settings.routeAmbassador shouldBeEqual defaultSettings.routeAmbassador
                    settings.routeHostage shouldBeEqual defaultSettings.routeHostage
                    settings.routeCommandeered shouldBeEqual defaultSettings.routeCommandeered
                    settings.avoidBlackHoles shouldBeEqual defaultSettings.avoidBlackHoles
                    settings.avoidMines shouldBeEqual defaultSettings.avoidMines
                    settings.avoidTyphon shouldBeEqual defaultSettings.avoidTyphon
                    settings.blackHoleClearance shouldBeEqual defaultSettings.blackHoleClearance
                    settings.mineClearance shouldBeEqual defaultSettings.mineClearance
                    settings.typhonClearance shouldBeEqual defaultSettings.typhonClearance
                }
            }
        }
    })
