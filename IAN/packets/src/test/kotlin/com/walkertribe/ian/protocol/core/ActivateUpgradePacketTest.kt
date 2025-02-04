package com.walkertribe.ian.protocol.core

class ActivateUpgradePacketTest :
    PacketTestSpec.Client<ActivateUpgradePacket>(
        specName = "ActivateUpgradePacket",
        fixtures = ActivateUpgradePacketFixture.ALL,
    )
