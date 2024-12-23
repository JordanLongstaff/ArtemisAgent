package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.PacketTestData
import com.walkertribe.ian.protocol.core.PacketTestFixture
import com.walkertribe.ian.protocol.core.SimpleEventPacket
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import kotlinx.io.writeIntLe

class DockedPacketFixture(
    arbVersion: Arb<Version> = Arb.version(),
) : PacketTestFixture.Server<DockedPacket>(TestPacketTypes.SIMPLE_EVENT) {
    class Data internal constructor(
        override val version: Version,
        private val dockID: Int,
    ) : PacketTestData.Server<DockedPacket> {
        override fun buildPayload(): Source = buildPacket {
            writeIntLe(SimpleEventPacket.Subtype.DOCKED.toInt())
            writeIntLe(dockID)
        }

        override fun validate(packet: DockedPacket) {
            packet.objectId shouldBeEqual dockID
        }
    }

    override val generator: Gen<Data> = Arb.bind(arbVersion, Arb.int(), ::Data)

    override suspend fun testType(packet: Packet.Server): DockedPacket =
        packet.shouldBeInstanceOf()
}
