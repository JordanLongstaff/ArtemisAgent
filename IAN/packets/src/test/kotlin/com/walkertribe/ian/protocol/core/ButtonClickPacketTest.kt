package com.walkertribe.ian.protocol.core

class ButtonClickPacketTest :
    PacketTestSpec.Client<ButtonClickPacket>(
        specName = "ButtonClickPacket",
        fixtures = ButtonClickPacketFixture.ALL,
    )
