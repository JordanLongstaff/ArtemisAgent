package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.protocol.core.PacketTestSpec

class ToggleRedAlertPacketTest :
    PacketTestSpec.Client<ToggleRedAlertPacket>(
        specName = "ToggleRedAlertPacket",
        fixtures = listOf(ToggleRedAlertPacketFixture),
    )
