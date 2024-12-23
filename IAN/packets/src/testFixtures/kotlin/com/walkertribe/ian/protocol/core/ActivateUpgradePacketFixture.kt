package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.map
import io.kotest.property.exhaustive.of
import kotlinx.io.Source
import kotlinx.io.readIntLe

sealed class ActivateUpgradePacketFixture private constructor(
    override val specName: String,
    private val packet: ActivateUpgradePacket,
    expectedSubtype: Byte,
    private val versionGen: Gen<Version>,
) : PacketTestFixture.Client<ActivateUpgradePacket>(
    packetType = TestPacketTypes.VALUE_INT,
    expectedPayloadSize = Int.SIZE_BYTES * 2,
) {
    class Data internal constructor(
        packet: ActivateUpgradePacket,
        private val expectedSubtype: Int,
    ) : PacketTestData.Client<ActivateUpgradePacket>(packet) {
        override fun validatePayload(payload: Source) {
            payload.readIntLe() shouldBeEqual expectedSubtype
            payload.readIntLe() shouldBeEqual DOUBLE_AGENT_VALUE
        }
    }

    data object Old : ActivateUpgradePacketFixture(
        "Old packet version: 2.3.1 and older",
        ActivateUpgradePacket.Old,
        ValueIntPacket.Subtype.ACTIVATE_UPGRADE_OLD,
        Exhaustive.of(0, 1).map { Version(major = 2, minor = 3, patch = it) },
    )

    data object Current : ActivateUpgradePacketFixture(
        "Current packet version: after 2.3.1",
        ActivateUpgradePacket.Current,
        ValueIntPacket.Subtype.ACTIVATE_UPGRADE_CURRENT,
        Arb.choice(
            Arb.version(major = 2, minor = 3, patchArb = Arb.int(min = 3)),
            Arb.version(major = 2, minorArb = Arb.int(min = 4)),
        ),
    )

    override val generator: Gen<Data> = Exhaustive.of(Data(packet, expectedSubtype.toInt()))

    override suspend fun describeMore(scope: DescribeSpecContainerScope) {
        scope.it("Correct subtype chosen according to version") {
            versionGen.checkAll {
                ActivateUpgradePacket(it) shouldBeEqual packet
            }
        }
    }

    companion object {
        private const val DOUBLE_AGENT_VALUE = 8

        val ALL = listOf(Current, Old)
    }
}
