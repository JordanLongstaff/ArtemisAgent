package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.protocol.core.PacketTestSpec

class IntelPacketTest :
    PacketTestSpec.Server<IntelPacket>(
        specName = "IntelPacket",
        fixtures = IntelPacketFixture.allFixtures(),
    )
