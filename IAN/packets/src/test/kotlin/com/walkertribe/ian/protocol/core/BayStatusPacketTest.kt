package com.walkertribe.ian.protocol.core

class BayStatusPacketTest :
    PacketTestSpec.Server<BayStatusPacket>(
        specName = "BayStatusPacket",
        fixtures = BayStatusPacketFixture.ALL,
    )
