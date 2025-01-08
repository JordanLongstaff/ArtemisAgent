package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.protocol.core.PacketTestSpec

class WelcomePacketTest :
    PacketTestSpec.Server<WelcomePacket>(
        specName = "WelcomePacket",
        fixtures = listOf(WelcomePacketFixture),
    )
