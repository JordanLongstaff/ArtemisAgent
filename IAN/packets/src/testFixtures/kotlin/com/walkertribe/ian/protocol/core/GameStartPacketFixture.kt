package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.enums.GameType
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import kotlinx.io.writeIntLe

class GameStartPacketFixture private constructor(arbVersion: Arb<Version>, gameType: GameType) :
    PacketTestFixture.Server<GameStartPacket>(TestPacketTypes.START_GAME) {
    override val specName: String = "Game type: $gameType"

    data class Data(override val version: Version, val gameType: GameType, val difficulty: Int) :
        PacketTestData.Server<GameStartPacket> {
        override fun buildPayload(): Source = buildPacket {
            writeIntLe(difficulty)
            writeIntLe(gameType.ordinal)
        }

        override fun validate(packet: GameStartPacket) {
            packet.gameType shouldBeEqual gameType
        }
    }

    override val generator: Gen<Data> =
        Arb.bind(arbVersion, Arb.int()) { version, difficulty ->
            Data(version, gameType, difficulty)
        }

    override suspend fun testType(packet: Packet.Server): GameStartPacket =
        packet.shouldBeInstanceOf()

    companion object {
        fun allFixtures(arbVersion: Arb<Version> = Arb.version()): List<GameStartPacketFixture> =
            GameType.entries.map { GameStartPacketFixture(arbVersion, it) }
    }
}
