package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.IntelType
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.PacketTestData
import com.walkertribe.ian.protocol.core.PacketTestFixture
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import kotlinx.io.writeIntLe

class IntelPacketFixture
private constructor(
    arbVersion: Arb<Version>,
    intelType: IntelType,
    arbId: Arb<Int> = Arb.int(),
    arbText: Arb<String> = Arb.string(),
) : PacketTestFixture.Server<IntelPacket>(TestPacketTypes.OBJECT_TEXT) {
    override val specName: String = "Intel type: ${normalize(intelType.name)}"

    class Data
    internal constructor(
        override val version: Version,
        private val objectID: Int,
        private val intelType: IntelType,
        private val intel: String,
    ) : PacketTestData.Server<IntelPacket> {
        override fun buildPayload(): Source = buildPacket {
            writeIntLe(objectID)
            writeByte(intelType.ordinal.toByte())
            writeString(intel)
        }

        override fun validate(packet: IntelPacket) {
            packet.id shouldBeEqual objectID
            packet.intelType shouldBeEqual intelType
            packet.intel shouldBeEqual intel
        }
    }

    override val generator: Gen<Data> =
        Arb.bind(arbVersion, arbId, arbText) { version, id, intel ->
            Data(version, objectID = id, intelType, intel)
        }

    override suspend fun testType(packet: Packet.Server): IntelPacket = packet.shouldBeInstanceOf()

    companion object {
        fun allFixtures(
            arbVersion: Arb<Version> = Arb.version(),
            arbId: Arb<Int> = Arb.int(),
            arbText: Arb<String> = Arb.string(),
        ): List<IntelPacketFixture> =
            IntelType.entries.map { IntelPacketFixture(arbVersion, it, arbId, arbText) }

        private fun normalize(str: String) =
            str.replace('_', ' ').let { it[0] + it.substring(1).lowercase() }
    }
}
