package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.util.Version
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.of
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readIntLittleEndian

class ActivateUpgradePacketTest : PacketTestSpec.Client<ActivateUpgradePacket>(
    specName = "ActivateUpgradePacket",
    packetType = TestPacketTypes.VALUE_INT,
    packetTypeName = CorePacketType.VALUE_INT,
    expectedPayloadSize = Int.SIZE_BYTES * 2,
) {
    private var expectedSubtype: Int = 0

    override suspend fun DescribeSpecContainerScope.organizeTests(
        describe: suspend DescribeSpecContainerScope.(Gen<ActivateUpgradePacket>) -> Unit,
    ) {
        val allTestCases = arrayOf(
            ActivateUpgradePacket.Old to ValueIntPacket.Subtype.ACTIVATE_UPGRADE_OLD,
            ActivateUpgradePacket.Current to ValueIntPacket.Subtype.ACTIVATE_UPGRADE_CURRENT,
        )
        allTestCases.size shouldBeEqual ActivateUpgradePacket::class.sealedSubclasses.size

        allTestCases.forEach { (packet, subtype) ->
            describe(packet.javaClass.simpleName) {
                expectedSubtype = subtype.toInt()
                describe(Exhaustive.of(packet))
            }
        }
    }

    override suspend fun runTest(packet: ActivateUpgradePacket, payload: ByteReadPacket) {
        payload.readIntLittleEndian() shouldBeEqual expectedSubtype
        payload.readIntLittleEndian() shouldBeEqual 8
    }

    override fun clientPacketGen(client: ArtemisNetworkInterface): Gen<ActivateUpgradePacket> {
        val packet = ActivateUpgradePacket(client.version)

        expectedSubtype = when (packet) {
            is ActivateUpgradePacket.Old -> ValueIntPacket.Subtype.ACTIVATE_UPGRADE_OLD
            is ActivateUpgradePacket.Current -> ValueIntPacket.Subtype.ACTIVATE_UPGRADE_CURRENT
        }.toInt()

        return Exhaustive.of(packet)
    }

    override suspend fun DescribeSpecContainerScope.describeMore() {
        it("Current packet version: after 2.3.1") {
            Arb.int(min = 2).checkAll {
                ActivateUpgradePacket(Version(2, 3, it))
                    .shouldBeInstanceOf<ActivateUpgradePacket.Current>()
            }

            checkAll(
                Arb.int(min = 4),
                Arb.nonNegativeInt(),
            ) { minor, patch ->
                ActivateUpgradePacket(Version(2, minor, patch))
                    .shouldBeInstanceOf<ActivateUpgradePacket.Current>()
            }
        }

        it("Old packet version: 2.3.1 and older") {
            for (patch in 0..1) {
                ActivateUpgradePacket(Version(2, 3, patch))
                    .shouldBeInstanceOf<ActivateUpgradePacket.Old>()
            }
        }
    }
}
