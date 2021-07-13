package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.exhaustive.of
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.readIntLittleEndian

class HeartbeatPacketTest : DescribeSpec() {
    object Client : PacketTestSpec.Client<HeartbeatPacket.Client>(
        specName = "HeartbeatPacket.Client",
        packetType = TestPacketTypes.VALUE_INT,
        packetTypeName = CorePacketType.VALUE_INT,
        expectedPayloadSize = Int.SIZE_BYTES,
        autoIncludeTests = false,
    ) {
        override suspend fun DescribeSpecContainerScope.organizeTests(
            describe: suspend DescribeSpecContainerScope.(Gen<HeartbeatPacket.Client>) -> Unit,
        ) {
            describe(Exhaustive.of(HeartbeatPacket.Client))
        }

        override suspend fun runTest(packet: HeartbeatPacket.Client, payload: ByteReadPacket) {
            payload.readIntLittleEndian() shouldBeEqual
                ValueIntPacket.Subtype.CLIENT_HEARTBEAT.toInt()
        }

        override fun clientPacketGen(client: ArtemisNetworkInterface): Gen<HeartbeatPacket.Client> {
            return Exhaustive.of(HeartbeatPacket.Client)
        }
    }

    object Server : PacketTestSpec.Server<HeartbeatPacket.Server>(
        specName = "HeartbeatPacket.Server",
        packetType = TestPacketTypes.HEARTBEAT,
        packetTypeName = CorePacketType.HEARTBEAT,
        needsListeners = false,
        autoIncludeTests = false,
    ) {
        override val protocol: PacketTestProtocol<HeartbeatPacket.Server> = PacketTestProtocol()

        override val payloadGen: Gen<ByteReadPacket> = Exhaustive.of(buildPacket { })

        override suspend fun testType(packet: ArtemisPacket): HeartbeatPacket.Server =
            packet.shouldBeInstanceOf()
    }

    init {
        include(Client.tests())
        include(Server.tests())
    }
}
