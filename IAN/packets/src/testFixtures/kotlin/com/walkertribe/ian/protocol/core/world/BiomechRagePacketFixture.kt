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

class BiomechRagePacketFixture(
    arbVersion: Arb<Version> = Arb.version(),
) : PacketTestFixture.Server<BiomechRagePacket>(TestPacketTypes.SIMPLE_EVENT) {
    class Data internal constructor(
        override val version: Version,
        private val rage: Int,
    ) : PacketTestData.Server<BiomechRagePacket> {
        override fun buildPayload(): Source = buildPacket {
            writeIntLe(SimpleEventPacket.Subtype.BIOMECH_STANCE.toInt())
            writeIntLe(rage)
        }

        override fun validate(packet: BiomechRagePacket) {
            packet.rage shouldBeEqual rage
        }
    }

    override val generator: Gen<Data> = Arb.bind(arbVersion, Arb.int(), ::Data)

    override suspend fun testType(packet: Packet.Server): BiomechRagePacket =
        packet.shouldBeInstanceOf()
}
