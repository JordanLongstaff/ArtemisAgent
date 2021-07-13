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

class DockedPacketTest : PacketTestSpec.Server<DockedPacket>(
    specName = "DockedPacket",
    packetType = TestPacketTypes.SIMPLE_EVENT,
    packetTypeName = CorePacketType.SIMPLE_EVENT,
) {
    private var dockID: Int = 0

    override val protocol: PacketTestProtocol<DockedPacket> = PacketTestProtocol()

    override val payloadGen: Gen<ByteReadPacket> = Arb.int().map {
        dockID = it

        buildPacket {
            writeIntLittleEndian(SimpleEventPacket.Subtype.DOCKED.toInt())
            writeIntLittleEndian(it)
        }
    }

    override suspend fun testType(packet: ArtemisPacket): DockedPacket =
        packet.shouldBeInstanceOf()

    override suspend fun testPayload(packet: DockedPacket) {
        packet.objectId shouldBeEqual dockID
    }
}
