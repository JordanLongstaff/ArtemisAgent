package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.protocol.core.PacketTestSpec

class ReadyPacketTest :
    PacketTestSpec.Client<ReadyPacket>(
        specName = "ReadyPacket",
        fixtures = listOf(ReadyPacketFixture),
    )
