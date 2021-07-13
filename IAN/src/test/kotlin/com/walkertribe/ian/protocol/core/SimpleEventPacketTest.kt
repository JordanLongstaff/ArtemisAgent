package com.walkertribe.ian.protocol.core

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual

class SimpleEventPacketTest : DescribeSpec({
    describe("SimpleEventPacket") {
        describe("Subtypes") {
            withData<Triple<String, Byte, Byte>>(
                nameFn = { "${it.first} = ${it.third}" },
                Triple(
                    "PausePacket",
                    SimpleEventPacket.Subtype.PAUSE,
                    0x04,
                ),
                Triple(
                    "PlayerShipDamagePacket",
                    SimpleEventPacket.Subtype.PLAYER_SHIP_DAMAGE,
                    0x05,
                ),
                Triple(
                    "EndGamePacket",
                    SimpleEventPacket.Subtype.END_GAME,
                    0x06,
                ),
                Triple(
                    "JumpEndPacket",
                    SimpleEventPacket.Subtype.JUMP_END,
                    0x0d,
                ),
                Triple(
                    "AllShipSettingsPacket",
                    SimpleEventPacket.Subtype.SHIP_SETTINGS,
                    0x0f,
                ),
                Triple(
                    "GameOverReasonPacket",
                    SimpleEventPacket.Subtype.GAME_OVER_REASON,
                    0x14,
                ),
                Triple(
                    "BiomechRagePacket",
                    SimpleEventPacket.Subtype.BIOMECH_STANCE,
                    0x19,
                ),
                Triple(
                    "DockedPacket",
                    SimpleEventPacket.Subtype.DOCKED,
                    0x1a,
                ),
            ) {
                it.second shouldBeEqual it.third
            }
        }
    }
})
