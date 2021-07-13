package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.SimpleEventPacket
import com.walkertribe.ian.protocol.core.TestPacketTypes
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeIntLittleEndian

class BiomechRagePacketTest : PacketTestSpec.Server<BiomechRagePacket>(
    specName = "BiomechRagePacket",
    packetType = TestPacketTypes.SIMPLE_EVENT,
    packetTypeName = CorePacketType.SIMPLE_EVENT,
) {
    private var biomechRage: Int = 0

    override val protocol: PacketTestProtocol<BiomechRagePacket> = PacketTestProtocol()

    override val payloadGen: Gen<ByteReadPacket> = Arb.int().map {
        biomechRage = it

        buildPacket {
            writeIntLittleEndian(SimpleEventPacket.Subtype.BIOMECH_STANCE.toInt())
            writeIntLittleEndian(biomechRage)
        }
    }

    override suspend fun testType(packet: ArtemisPacket): BiomechRagePacket =
        packet.shouldBeInstanceOf()

    override suspend fun testPayload(packet: BiomechRagePacket) {
        packet.rage shouldBeEqual biomechRage
    }
}
