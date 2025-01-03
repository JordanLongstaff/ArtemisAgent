package com.walkertribe.ian.world

import com.walkertribe.ian.util.shouldBeUnknown
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainOnlyNulls
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull

class ArtemisDslTest : DescribeSpec({
    describe("ArtemisCreature") {
        it("isNotTyphon") {
            ArtemisCreature.Dsl.isNotTyphon.shouldBeUnknown()
        }
    }

    describe("ArtemisNpc") {
        it("isEnemy") {
            ArtemisNpc.Dsl.isEnemy.shouldBeUnknown()
        }

        it("isSurrendered") {
            ArtemisNpc.Dsl.isSurrendered.shouldBeUnknown()
        }

        it("isInNebula") {
            ArtemisNpc.Dsl.isInNebula.shouldBeUnknown()
        }

        it("scanBits") {
            ArtemisNpc.Dsl.scanBits.shouldBeNull()
        }
    }

    describe("ArtemisPlayer") {
        describe("Player") {
            it("shipIndex") {
                ArtemisPlayer.Dsl.Player.shipIndex shouldBeEqual Byte.MIN_VALUE
            }

            it("capitalShipID") {
                ArtemisPlayer.Dsl.Player.capitalShipID shouldBeEqual -1
            }

            it("alertStatus") {
                ArtemisPlayer.Dsl.Player.alertStatus.shouldBeNull()
            }

            it("dockingBase") {
                ArtemisPlayer.Dsl.Player.dockingBase shouldBeEqual -1
            }

            it("driveType") {
                ArtemisPlayer.Dsl.Player.driveType.shouldBeNull()
            }

            it("warp") {
                ArtemisPlayer.Dsl.Player.warp shouldBeEqual -1
            }
        }

        describe("Weapons") {
            it("ordnanceCounts") {
                ArtemisPlayer.Dsl.Weapons.ordnanceCounts.shouldBeEmpty()
            }

            describe("tubeStates") {
                it("Size: ${Artemis.MAX_TUBES}") {
                    ArtemisPlayer.Dsl.Weapons.tubeStates.size shouldBeEqual Artemis.MAX_TUBES
                }

                it("Contents: null") {
                    ArtemisPlayer.Dsl.Weapons.tubeStates.shouldContainOnlyNulls()
                }
            }

            describe("tubeContents") {
                it("Size: ${Artemis.MAX_TUBES}") {
                    ArtemisPlayer.Dsl.Weapons.tubeContents.size shouldBeEqual Artemis.MAX_TUBES
                }

                it("Contents: null") {
                    ArtemisPlayer.Dsl.Weapons.tubeStates.shouldContainOnlyNulls()
                }
            }
        }

        describe("Upgrades") {
            it("doubleAgentActive") {
                ArtemisPlayer.Dsl.Upgrades.doubleAgentActive.shouldBeUnknown()
            }

            it("doubleAgentCount") {
                ArtemisPlayer.Dsl.Upgrades.doubleAgentCount shouldBeEqual -1
            }

            it("doubleAgentSecondsLeft") {
                ArtemisPlayer.Dsl.Upgrades.doubleAgentSecondsLeft shouldBeEqual -1
            }
        }
    }
})
