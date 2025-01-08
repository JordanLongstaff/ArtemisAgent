package com.walkertribe.ian.protocol.core

import io.kotest.core.spec.style.DescribeSpec

class HeartbeatPacketTest : DescribeSpec() {
    object Client :
        PacketTestSpec.Client<HeartbeatPacket.Client>(
            specName = "HeartbeatPacket.Client",
            fixtures = listOf(HeartbeatPacketFixture.Client),
            autoIncludeTests = false,
        )

    object Server :
        PacketTestSpec.Server<HeartbeatPacket.Server>(
            specName = "HeartbeatPacket.Server",
            fixtures = listOf(HeartbeatPacketFixture.Server()),
            autoIncludeTests = false,
        )

    init {
        include(Client.tests())
        include(Server.tests())
    }
}
