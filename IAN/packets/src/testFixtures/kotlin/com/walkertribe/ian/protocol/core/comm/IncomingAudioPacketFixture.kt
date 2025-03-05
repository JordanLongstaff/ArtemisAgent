package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.enums.AudioMode
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.PacketTestData
import com.walkertribe.ian.protocol.core.PacketTestFixture
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.exhaustive.of
import io.ktor.utils.io.core.buildPacket
import kotlinx.io.Source
import kotlinx.io.writeIntLe

class IncomingAudioPacketFixture
private constructor(
    override val specName: String,
    arbVersion: Arb<Version>,
    audioModeGen: Gen<AudioMode>,
) : PacketTestFixture.Server<IncomingAudioPacket>(TestPacketTypes.INCOMING_MESSAGE) {
    class Data
    internal constructor(
        override val version: Version,
        private val audioID: Int,
        private val audioMode: AudioMode,
    ) : PacketTestData.Server<IncomingAudioPacket> {
        override fun buildPayload(): Source = buildPacket {
            writeIntLe(audioID)

            when (audioMode) {
                is AudioMode.Playing -> {
                    writeIntLe(1)
                }
                is AudioMode.Incoming -> {
                    writeIntLe(2)
                    writeString(audioMode.title)
                    writeString(audioMode.filename)
                }
            }
        }

        override fun validate(packet: IncomingAudioPacket) {
            packet.audioId shouldBeEqual audioID
            packet.audioMode shouldBeEqual audioMode
        }
    }

    override val generator: Gen<Data> =
        Arb.bind(genA = arbVersion, genB = Arb.int(), genC = audioModeGen, bindFn = ::Data)

    override suspend fun testType(packet: Packet.Server): IncomingAudioPacket =
        packet.shouldBeInstanceOf()

    companion object {
        fun allFixtures(
            arbVersion: Arb<Version> = Arb.version()
        ): List<IncomingAudioPacketFixture> =
            listOf(
                IncomingAudioPacketFixture("Playing", arbVersion, Exhaustive.of(AudioMode.Playing)),
                IncomingAudioPacketFixture("Incoming", arbVersion, Arb.bind<AudioMode.Incoming>()),
            )
    }
}
