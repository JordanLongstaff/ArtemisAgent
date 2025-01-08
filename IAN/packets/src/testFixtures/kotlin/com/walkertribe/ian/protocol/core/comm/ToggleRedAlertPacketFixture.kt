package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.protocol.core.PacketTestData
import com.walkertribe.ian.protocol.core.PacketTestFixture
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.protocol.core.ValueIntPacket
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.exhaustive.of
import kotlinx.io.Source
import kotlinx.io.readIntLe

data object ToggleRedAlertPacketFixture :
    PacketTestFixture.Client<ToggleRedAlertPacket>(
        packetType = TestPacketTypes.VALUE_INT,
        expectedPayloadSize = Int.SIZE_BYTES * 2,
    ) {
    data object Data : PacketTestData.Client<ToggleRedAlertPacket>(ToggleRedAlertPacket()) {
        override fun validatePayload(payload: Source) {
            payload.readIntLe() shouldBeEqual ValueIntPacket.Subtype.TOGGLE_RED_ALERT.toInt()
            payload.readIntLe() shouldBeEqual 0
        }
    }

    override val generator: Gen<Data> = Exhaustive.of(Data)
}
