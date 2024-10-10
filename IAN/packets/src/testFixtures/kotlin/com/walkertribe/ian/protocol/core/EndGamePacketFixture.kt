package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.map
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import kotlinx.io.writeIntLe

class EndGamePacketFixture(
    arbVersion: Arb<Version> = Arb.version(),
) : PacketTestFixture.Server<EndGamePacket>(TestPacketTypes.SIMPLE_EVENT) {
    class Data internal constructor(
        override val version: Version,
    ) : PacketTestData.Server<EndGamePacket> {
        override fun buildPayload(): Source = buildPacket {
            writeIntLe(SimpleEventPacket.Subtype.END_GAME.toInt())
        }

        override fun validate(packet: EndGamePacket) {
            // Nothing to validate
        }
    }

    override val generator: Gen<Data> = arbVersion.map(::Data)

    override suspend fun testType(packet: Packet.Server): EndGamePacket =
        packet.shouldBeInstanceOf()
}
