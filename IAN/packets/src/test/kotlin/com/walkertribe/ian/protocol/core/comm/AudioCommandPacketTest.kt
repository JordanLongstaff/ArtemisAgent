package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.protocol.core.PacketTestSpec

class AudioCommandPacketTest :
    PacketTestSpec.Client<AudioCommandPacket>(
        specName = "AudioCommandPacket",
        fixtures = AudioCommandPacketFixture.ALL,
    )
