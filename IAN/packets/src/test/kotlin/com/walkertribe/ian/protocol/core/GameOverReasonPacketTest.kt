package com.walkertribe.ian.protocol.core

class GameOverReasonPacketTest :
    PacketTestSpec.Server<GameOverReasonPacket>(
        specName = "GameOverReasonPacket",
        fixtures = listOf(GameOverReasonPacketFixture()),
    )
