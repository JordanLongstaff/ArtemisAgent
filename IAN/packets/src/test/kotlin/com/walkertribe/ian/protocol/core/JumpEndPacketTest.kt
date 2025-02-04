package com.walkertribe.ian.protocol.core

class JumpEndPacketTest :
    PacketTestSpec.Server<JumpEndPacket>(
        specName = "JumpEndPacket",
        fixtures = listOf(JumpEndPacketFixture()),
    )
