package com.walkertribe.ian.protocol.core

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import kotlinx.io.Source
import kotlinx.io.readIntLe

sealed class ButtonClickPacketFixture
private constructor(override val specName: String, arbPacket: Arb<ButtonClickPacket>) :
    PacketTestFixture.Client<ButtonClickPacket>(
        packetType = TestPacketTypes.VALUE_INT,
        expectedPayloadSize = PAYLOAD_SIZE,
    ) {
    class Data internal constructor(packet: ButtonClickPacket) :
        PacketTestData.Client<ButtonClickPacket>(packet) {
        override fun validatePayload(payload: Source) {
            payload.readIntLe() shouldBeEqual ValueIntPacket.Subtype.BUTTON_CLICK.toInt()
            payload.readIntLe() shouldBeEqual 0x0d
            payload.readIntLe() shouldBeEqual packet.hash
        }
    }

    data object Hash : ButtonClickPacketFixture("Primary constructor", Arb.bind())

    data object Label :
        ButtonClickPacketFixture("Label constructor", Arb.string().map(::ButtonClickPacket))

    override val generator: Gen<Data> = arbPacket.map(::Data)

    companion object {
        private const val PAYLOAD_SIZE = Int.SIZE_BYTES * 3

        val ALL = listOf(Hash, Label)
    }
}
