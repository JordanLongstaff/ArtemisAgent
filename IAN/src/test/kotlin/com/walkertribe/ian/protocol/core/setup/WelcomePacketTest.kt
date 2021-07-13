package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.core.writeIntLittleEndian

class WelcomePacketTest : PacketTestSpec.Server<WelcomePacket>(
    specName = "WelcomePacket",
    packetType = TestPacketTypes.PLAIN_TEXT_GREETING,
    packetTypeName = CorePacketType.PLAIN_TEXT_GREETING,
    needsListeners = false,
) {
    private var message: String = ""

    override val protocol: PacketTestProtocol<WelcomePacket> = PacketTestProtocol()

    override val payloadGen: Gen<ByteReadPacket> = Arb.string().map { message ->
        this.message = message

        buildPacket {
            writeIntLittleEndian(message.length)
            writeFully(Charsets.US_ASCII.encode(message))
        }
    }

    override suspend fun testType(packet: ArtemisPacket): WelcomePacket =
        packet.shouldBeInstanceOf()

    override suspend fun testPayload(packet: WelcomePacket) {
        packet.message shouldBeEqual message
    }
}
