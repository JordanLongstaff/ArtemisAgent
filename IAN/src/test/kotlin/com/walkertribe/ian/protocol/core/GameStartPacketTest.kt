package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.enums.GameType
import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeIntLittleEndian

class GameStartPacketTest : PacketTestSpec.Server<GameStartPacket>(
    specName = "GameStartPacket",
    packetType = TestPacketTypes.START_GAME,
    packetTypeName = CorePacketType.START_GAME,
    needsListeners = false,
) {
    private var gameType: GameType = GameType.SIEGE
    private var difficulty: Int = 0

    override val protocol: PacketTestProtocol<GameStartPacket> = PacketTestProtocol()

    override val payloadGen: Gen<ByteReadPacket> get() = Arb.int().map {
        difficulty = it

        buildPacket {
            writeIntLittleEndian(difficulty)
            writeIntLittleEndian(gameType.ordinal)
        }
    }

    override suspend fun testType(packet: ArtemisPacket): GameStartPacket =
        packet.shouldBeInstanceOf()

    override suspend fun testPayload(packet: GameStartPacket) {
        packet.gameType shouldBeEqual gameType
    }

    override suspend fun DescribeSpecContainerScope.organizeTests(
        describeTests: suspend DescribeSpecContainerScope.() -> Unit,
    ) {
        GameType.entries.forEach {
            gameType = it

            describe("Game type: $it") {
                describeTests()
            }
        }
    }

    override suspend fun describeClientTests(
        scope: DescribeSpecContainerScope,
        client: ArtemisNetworkInterface,
        sendChannel: ByteWriteChannel,
        afterTest: () -> Unit
    ) {
        scope.describe(specName) {
            withData(nameFn = { "Game type: $it" }, GameType.entries) {
                gameType = it
                runClientTest(client, sendChannel)
                afterTest()
            }
        }
    }
}
