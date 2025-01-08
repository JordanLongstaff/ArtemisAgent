package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.protocol.core.PacketTestSpec

class CommsIncomingPacketTest :
    PacketTestSpec.Server<CommsIncomingPacket>(
        specName = "CommsIncomingPacket",
        fixtures = CommsIncomingPacketFixture.ALL,
    )
