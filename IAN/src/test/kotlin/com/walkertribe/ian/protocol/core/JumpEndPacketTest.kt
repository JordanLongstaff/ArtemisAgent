package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.exhaustive.of
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeIntLittleEndian

class JumpEndPacketTest : PacketTestSpec.Server<JumpEndPacket>(
    specName = "JumpEndPacket",
    packetType = TestPacketTypes.SIMPLE_EVENT,
    packetTypeName = CorePacketType.SIMPLE_EVENT,
) {
    override val protocol: PacketTestProtocol<JumpEndPacket> = PacketTestProtocol()

    override val payloadGen: Gen<ByteReadPacket> = Exhaustive.of(
        buildPacket {
            writeIntLittleEndian(SimpleEventPacket.Subtype.JUMP_END.toInt())
        }
    )

    override suspend fun testType(packet: ArtemisPacket): JumpEndPacket =
        packet.shouldBeInstanceOf()
}
