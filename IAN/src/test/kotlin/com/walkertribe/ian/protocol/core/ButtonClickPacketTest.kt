package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.iface.ArtemisNetworkInterface
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readIntLittleEndian

class ButtonClickPacketTest : PacketTestSpec.Client<ButtonClickPacket>(
    specName = "ButtonClickPacket",
    packetType = TestPacketTypes.VALUE_INT,
    packetTypeName = CorePacketType.VALUE_INT,
    expectedPayloadSize = Int.SIZE_BYTES * 3,
) {
    override suspend fun DescribeSpecContainerScope.organizeTests(
        describe: suspend DescribeSpecContainerScope.(Gen<ButtonClickPacket>) -> Unit,
    ) {
        describe("Primary constructor") {
            describe(Arb.bind())
        }

        describe("Label constructor") {
            describe(Arb.string().map(::ButtonClickPacket))
        }
    }

    override suspend fun runTest(packet: ButtonClickPacket, payload: ByteReadPacket) {
        payload.readIntLittleEndian() shouldBeEqual ValueIntPacket.Subtype.BUTTON_CLICK.toInt()
        payload.readIntLittleEndian() shouldBeEqual 0x0d
        payload.readIntLittleEndian() shouldBeEqual packet.hash
    }

    override fun clientPacketGen(client: ArtemisNetworkInterface): Gen<ButtonClickPacket> {
        return Arb.bind<ButtonClickPacket>()
    }
}
