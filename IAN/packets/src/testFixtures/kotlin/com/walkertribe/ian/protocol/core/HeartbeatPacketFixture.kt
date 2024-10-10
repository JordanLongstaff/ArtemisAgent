package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.arbitrary.map
import io.kotest.property.exhaustive.of
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import kotlinx.io.readIntLe

interface HeartbeatPacketFixture {
    data object Client : PacketTestFixture.Client<HeartbeatPacket.Client>(
        packetType = TestPacketTypes.VALUE_INT,
        expectedPayloadSize = Int.SIZE_BYTES,
    ) {
        data object Data : PacketTestData.Client<HeartbeatPacket.Client>(HeartbeatPacket.Client) {
            override fun validatePayload(payload: Source) {
                payload.readIntLe() shouldBeEqual
                    ValueIntPacket.Subtype.CLIENT_HEARTBEAT.toInt()
            }
        }

        override val generator: Gen<Data> = Exhaustive.of(Data)
    }

    class Server(
        arbVersion: Arb<Version> = Arb.version(),
    ) : PacketTestFixture.Server<HeartbeatPacket.Server>(TestPacketTypes.HEARTBEAT) {
        data class Data(
            override val version: Version,
        ) : PacketTestData.Server<HeartbeatPacket.Server> {
            override fun buildPayload(): Source = buildPacket { }

            override fun validate(packet: HeartbeatPacket.Server) {
                // Nothing to validate
            }
        }

        override val generator: Gen<Data> = arbVersion.map(::Data)

        override suspend fun testType(packet: Packet.Server): HeartbeatPacket.Server =
            packet.shouldBeInstanceOf()
    }
}
