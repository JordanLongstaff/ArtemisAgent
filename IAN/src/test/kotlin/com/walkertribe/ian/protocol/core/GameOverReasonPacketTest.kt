package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeIntLittleEndian

class GameOverReasonPacketTest : PacketTestSpec.Server<GameOverReasonPacket>(
    specName = "GameOverReasonPacket",
    packetType = TestPacketTypes.SIMPLE_EVENT,
    packetTypeName = CorePacketType.SIMPLE_EVENT,
    needsListeners = false,
) {
    private lateinit var text: List<String>

    override val protocol: PacketTestProtocol<GameOverReasonPacket> = PacketTestProtocol()

    override val payloadGen: Gen<ByteReadPacket> = Arb.list(Arb.string()).map {
        text = it

        buildPacket {
            writeIntLittleEndian(
                SimpleEventPacket.Subtype.GAME_OVER_REASON.toInt()
            )
            text.forEach { str -> writeString(str) }
        }
    }

    override suspend fun testType(packet: ArtemisPacket): GameOverReasonPacket =
        packet.shouldBeInstanceOf()

    override suspend fun testPayload(packet: GameOverReasonPacket) {
        val readText = packet.text.map { it.toString() }
        readText shouldContainExactly text
    }
}
