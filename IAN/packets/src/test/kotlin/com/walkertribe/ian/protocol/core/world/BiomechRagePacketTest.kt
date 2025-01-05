package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.protocol.core.PacketTestSpec

class BiomechRagePacketTest :
    PacketTestSpec.Server<BiomechRagePacket>(
        specName = "BiomechRagePacket",
        fixtures = listOf(BiomechRagePacketFixture()),
    )
