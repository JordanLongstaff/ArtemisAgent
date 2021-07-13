package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.IntelType
import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.printableAscii
import io.kotest.property.arbitrary.string
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeIntLittleEndian

class IntelPacketTest : PacketTestSpec.Server<IntelPacket>(
    specName = "IntelPacket",
    packetType = TestPacketTypes.OBJECT_TEXT,
    packetTypeName = CorePacketType.OBJECT_TEXT,
) {
    private var objectID: Int = 0
    private var intelType: IntelType = IntelType.RACE
    private var intel: String = ""

    override val protocol: PacketTestProtocol<IntelPacket> = PacketTestProtocol()

    override val payloadGen: Gen<ByteReadPacket> = Arb.bind(
        Arb.int(),
        Arb.enum<IntelType>(),
        Arb.string(codepoints = Codepoint.printableAscii()),
    ) { id, type, content ->
        objectID = id
        intelType = type
        intel = content

        buildPacket {
            writeIntLittleEndian(objectID)
            writeByte(intelType.ordinal.toByte())
            writeString(intel)
        }
    }

    override suspend fun testType(packet: ArtemisPacket): IntelPacket =
        packet.shouldBeInstanceOf()

    override suspend fun testPayload(packet: IntelPacket) {
        packet.id shouldBeEqual objectID
        packet.intelType shouldBeEqual intelType
        packet.intel.toString() shouldBeEqual intel
    }
}
