package artemis.agent

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.WithDataTestName
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual

class UserSettingsTest :
    DescribeSpec({
        describe("UserSettings") {
            describe("Defaults") {
                val settings = UserSettingsSerializer.defaultValue

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
        }
    })
