package com.walkertribe.ian.protocol.core

class GameStartPacketTest :
    PacketTestSpec.Server<GameStartPacket>(
        specName = "GameStartPacket",
        fixtures = GameStartPacketFixture.allFixtures(),
    )
