package com.walkertribe.ian.protocol.core

class PausePacketTest :
    PacketTestSpec.Server<PausePacket>(
        specName = "PausePacket",
        fixtures = PausePacketFixture.allFixtures(),
    )
