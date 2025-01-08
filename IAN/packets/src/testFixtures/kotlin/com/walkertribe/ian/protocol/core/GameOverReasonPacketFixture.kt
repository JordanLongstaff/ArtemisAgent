package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import kotlinx.io.writeIntLe

class GameOverReasonPacketFixture(arbVersion: Arb<Version> = Arb.version()) :
    PacketTestFixture.Server<GameOverReasonPacket>(TestPacketTypes.SIMPLE_EVENT) {
    class Data internal constructor(override val version: Version, private val text: List<String>) :
        PacketTestData.Server<GameOverReasonPacket> {
        override fun buildPayload(): Source = buildPacket {
            writeIntLe(SimpleEventPacket.Subtype.GAME_OVER_REASON.toInt())
            text.forEach { str -> writeString(str) }
        }

        override fun validate(packet: GameOverReasonPacket) {
            packet.text shouldContainExactly text
        }
    }

    override val generator: Gen<Data> = Arb.bind(arbVersion, Arb.list(Arb.string()), ::Data)

    override suspend fun testType(packet: Packet.Server): GameOverReasonPacket =
        packet.shouldBeInstanceOf()
}
