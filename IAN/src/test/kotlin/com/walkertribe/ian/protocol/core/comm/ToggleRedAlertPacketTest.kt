package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.protocol.core.ValueIntPacket
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.exhaustive.of
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readIntLittleEndian

class ToggleRedAlertPacketTest : PacketTestSpec.Client<ToggleRedAlertPacket>(
    specName = "ToggleRedAlertPacket",
    packetType = TestPacketTypes.VALUE_INT,
    packetTypeName = CorePacketType.VALUE_INT,
    expectedPayloadSize = Int.SIZE_BYTES * 2,
) {
    override suspend fun DescribeSpecContainerScope.organizeTests(
        describe: suspend DescribeSpecContainerScope.(Gen<ToggleRedAlertPacket>) -> Unit,
    ) {
        describe(Exhaustive.of(ToggleRedAlertPacket()))
    }

    override suspend fun runTest(packet: ToggleRedAlertPacket, payload: ByteReadPacket) {
        payload.readIntLittleEndian() shouldBeEqual ValueIntPacket.Subtype.TOGGLE_RED_ALERT.toInt()
        payload.readIntLittleEndian() shouldBeEqual 0
    }

    override fun clientPacketGen(client: ArtemisNetworkInterface): Gen<ToggleRedAlertPacket> {
        return Exhaustive.of(ToggleRedAlertPacket())
    }
}
