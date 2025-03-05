package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.protocol.core.PacketTestSpec

class DockedPacketTest :
    PacketTestSpec.Server<DockedPacket>(
        specName = "DockedPacket",
        fixtures = listOf(DockedPacketFixture()),
    )
