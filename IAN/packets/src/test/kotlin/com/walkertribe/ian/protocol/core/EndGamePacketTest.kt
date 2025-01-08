package com.walkertribe.ian.protocol.core

class EndGamePacketTest :
    PacketTestSpec.Server<EndGamePacket>(
        specName = "EndGamePacket",
        fixtures = listOf(EndGamePacketFixture()),
    )
