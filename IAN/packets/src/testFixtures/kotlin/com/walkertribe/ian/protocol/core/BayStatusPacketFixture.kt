package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import kotlinx.io.writeIntLe

class BayStatusPacketFixture
private constructor(
    override val specName: String,
    shouldWriteBayNumber: Boolean,
    versionArb: Arb<Version>,
) : PacketTestFixture.Server<BayStatusPacket>(TestPacketTypes.CARRIER_RECORD) {
    data class Bay(
        val id: Int,
        val bayNumber: Int,
        val name: String,
        val className: String,
        val refitTime: Int,
    )

    data class Data(
        override val version: Version,
        val shouldWriteBayNumber: Boolean,
        val bays: List<Bay>,
    ) : PacketTestData.Server<BayStatusPacket> {
        override fun buildPayload(): Source = buildPacket {
            bays.forEach { bay ->
                writeIntLe(bay.id)
                if (shouldWriteBayNumber) {
                    writeIntLe(bay.bayNumber)
                }
                writeString(bay.name)
                writeString(bay.className)
                writeIntLe(bay.refitTime)
            }
            writeIntLe(0)
        }

        override fun validate(packet: BayStatusPacket) {
            packet.fighterCount shouldBeEqual bays.size
        }
    }

    override val generator: Gen<Data> =
        Arb.bind(
            versionArb,
            Arb.list(
                Arb.bind(
                    genA = Arb.int().filter { it != 0 },
                    genB = Arb.int(),
                    genC = Arb.string(),
                    genD = Arb.string(),
                    genE = Arb.int(),
                ) { id, bayNumber, name, className, refitTime ->
                    Bay(id, bayNumber, name, className, refitTime)
                }
            ),
        ) { version, bays ->
            Data(version, shouldWriteBayNumber, bays)
        }

    override suspend fun testType(packet: Packet.Server): BayStatusPacket =
        packet.shouldBeInstanceOf()

    companion object {
        val ALL =
            listOf(
                BayStatusPacketFixture("Before version 2.6.0", false, Arb.version(2, 3..5)),
                BayStatusPacketFixture(
                    "Since version 2.6.0",
                    true,
                    Arb.version(2, Arb.int(min = 6)),
                ),
            )
    }
}
