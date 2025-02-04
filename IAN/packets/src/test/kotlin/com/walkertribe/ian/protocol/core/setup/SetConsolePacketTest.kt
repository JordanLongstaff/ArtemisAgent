package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.protocol.core.PacketTestSpec

class SetConsolePacketTest :
    PacketTestSpec.Client<SetConsolePacket>(
        specName = "SetConsolePacket",
        fixtures = SetConsolePacketFixture.ALL,
    )
