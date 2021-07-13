package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import com.walkertribe.ian.util.BoolState
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.exhaustive.of
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeIntLittleEndian

class PausePacketTest : PacketTestSpec.Server<PausePacket>(
    specName = "PausePacket",
    packetType = TestPacketTypes.SIMPLE_EVENT,
    packetTypeName = CorePacketType.SIMPLE_EVENT,
) {
    private lateinit var isPaused: BoolState

    override val protocol: PacketTestProtocol<PausePacket> = PacketTestProtocol()

    override val payloadGen: Gen<ByteReadPacket> get() = Exhaustive.of(
        buildPacket {
            writeIntLittleEndian(SimpleEventPacket.Subtype.PAUSE.toInt())
            writeIntLittleEndian(if (isPaused.booleanValue) 1 else 0)
        }
    )

    override suspend fun testType(packet: ArtemisPacket): PausePacket =
        packet.shouldBeInstanceOf()

    override suspend fun testPayload(packet: PausePacket) {
        packet.isPaused shouldBeEqual isPaused
    }

    override suspend fun DescribeSpecContainerScope.organizeTests(
        describeTests: suspend DescribeSpecContainerScope.() -> Unit,
    ) {
        arrayOf(BoolState.True, BoolState.False).forEach {
            describe("Paused: $it") {
                isPaused = it

                describeTests()
            }
        }
    }

    override suspend fun describeClientTests(
        scope: DescribeSpecContainerScope,
        client: ArtemisNetworkInterface,
        sendChannel: ByteWriteChannel,
        afterTest: () -> Unit,
    ) {
        scope.describe(specName) {
            withData(nameFn = { "Paused: $it" }, BoolState.True, BoolState.False) {
                isPaused = it
                runClientTest(client, sendChannel)
                afterTest()
            }
        }
    }
}
