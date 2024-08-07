package com.walkertribe.ian.protocol.core.setup

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
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFloatLittleEndian
import io.ktor.utils.io.core.writeIntLittleEndian

data class VersionPacketFixture(
    private val arbVersion: Arb<Version> = Arb.version(),
) : PacketTestFixture.Server<VersionPacket>(TestPacketTypes.CONNECTED) {
    data class Data(
        val unknownInt: Int,
        val legacyFloat: Float,
        val packetVersion: Version,
    ) : PacketTestData.Server<VersionPacket> {
        override val version: Version get() = Version.LATEST

        override fun buildPayload(): ByteReadPacket = buildPacket {
            writeIntLittleEndian(unknownInt)
            writeFloatLittleEndian(legacyFloat)
            writeIntLittleEndian(packetVersion.major)
            writeIntLittleEndian(packetVersion.minor)
            writeIntLittleEndian(packetVersion.patch)
        }

        override fun validate(packet: VersionPacket) {
            packet.version shouldBeEqual packetVersion
        }
    }

    override val generator: Gen<Data> = Arb.bind(Arb.int(), Arb.float(), arbVersion, ::Data)

    override suspend fun testType(packet: Packet.Server): VersionPacket =
        packet.shouldBeInstanceOf()
}
