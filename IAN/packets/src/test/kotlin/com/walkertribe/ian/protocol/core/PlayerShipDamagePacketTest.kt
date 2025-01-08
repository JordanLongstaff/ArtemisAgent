package com.walkertribe.ian.protocol.core

class PlayerShipDamagePacketTest :
    PacketTestSpec.Server<PlayerShipDamagePacket>(
        specName = "PlayerShipDamagePacket",
        fixtures = listOf(PlayerShipDamagePacketFixture()),
    )
