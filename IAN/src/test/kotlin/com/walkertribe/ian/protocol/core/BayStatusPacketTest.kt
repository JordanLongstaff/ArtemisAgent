package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import com.walkertribe.ian.util.Version
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.arbitrary.string
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeIntLittleEndian

class BayStatusPacketTest : PacketTestSpec.Server<BayStatusPacket>(
    specName = "BayStatusPacket",
    packetType = TestPacketTypes.CARRIER_RECORD,
    packetTypeName = CorePacketType.CARRIER_RECORD,
) {
    data class Bay(
        val id: Int,
        val bayNumber: Int,
        val name: String,
        val className: String,
        val refitTime: Int,
    )

    private lateinit var bays: List<Bay>
    private var shouldWriteBayNumber: Boolean = true

    override var version: Version = super.version

    override val protocol: PacketTestProtocol<BayStatusPacket> = PacketTestProtocol()

    override val payloadGen: Gen<ByteReadPacket> get() = Arb.bind(
        if (shouldWriteBayNumber) Arb.int(min = 6) else Arb.int(3..5),
        Arb.nonNegativeInt(),
        Arb.list(
            Arb.bind(
                Arb.int().filter { it != 0 },
                Arb.int(),
                Arb.string(),
                Arb.string(),
                Arb.int(),
            ) { id, bayNumber, name, className, refitTime ->
                Bay(id, bayNumber, name, className, refitTime)
            },
        ),
    ) { major, patch, bayList ->
        version = Version(2, major, patch)
        bays = bayList

        buildPacket {
            bays.forEach {
                writeIntLittleEndian(it.id)
                if (shouldWriteBayNumber) {
                    writeIntLittleEndian(it.bayNumber)
                }
                writeString(it.name)
                writeString(it.className)
                writeIntLittleEndian(it.refitTime)
            }
            writeIntLittleEndian(0)
        }
    }

    override suspend fun testType(packet: ArtemisPacket): BayStatusPacket =
        packet.shouldBeInstanceOf()

    override suspend fun testPayload(packet: BayStatusPacket) {
        packet.fighterCount shouldBeEqual bays.size
    }

    override suspend fun DescribeSpecContainerScope.organizeTests(
        describeTests: suspend DescribeSpecContainerScope.() -> Unit,
    ) {
        arrayOf(
            Pair("Before version 2.6.0", false),
            Pair("Since version 2.6.0", true),
        ).forEach { (name, shouldWriteNumber) ->
            describe(name) {
                shouldWriteBayNumber = shouldWriteNumber

                describeTests()
            }
        }
    }
}
