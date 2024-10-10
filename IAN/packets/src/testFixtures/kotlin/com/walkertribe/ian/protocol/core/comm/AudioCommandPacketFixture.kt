package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.enums.AudioCommand
import com.walkertribe.ian.protocol.core.PacketTestData
import com.walkertribe.ian.protocol.core.PacketTestFixture
import com.walkertribe.ian.protocol.core.TestPacketTypes
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import kotlinx.io.Source
import kotlinx.io.readIntLe

class AudioCommandPacketFixture private constructor(
    audioCommand: AudioCommand,
) : PacketTestFixture.Client<AudioCommandPacket>(
    packetType = TestPacketTypes.CONTROL_MESSAGE,
    expectedPayloadSize = Int.SIZE_BYTES * 2,
) {
    class Data internal constructor(
        private val audioID: Int,
        private val audioCommand: AudioCommand,
    ) : PacketTestData.Client<AudioCommandPacket>(AudioCommandPacket(audioID, audioCommand)) {
        init {
            packet.audioId shouldBeEqual audioID
            packet.command shouldBeEqual audioCommand
        }

        override fun validatePayload(payload: Source) {
            payload.readIntLe() shouldBeEqual audioID
            payload.readIntLe() shouldBeEqual audioCommand.ordinal
        }
    }

    override val generator: Gen<Data> = Arb.int().map { Data(it, audioCommand) }
    override val specName: String = audioCommand.toString()

    companion object {
        val ALL = AudioCommand.entries.map(::AudioCommandPacketFixture)
    }
}
