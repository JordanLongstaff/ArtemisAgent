package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.enums.AudioMode
import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeIntLittleEndian

class IncomingAudioPacketTest : PacketTestSpec.Server<IncomingAudioPacket>(
    specName = "IncomingAudioPacket",
    packetType = TestPacketTypes.INCOMING_MESSAGE,
    packetTypeName = CorePacketType.INCOMING_MESSAGE,
) {
    private var audioID: Int = 0
    private lateinit var audioMode: AudioMode
    private lateinit var audioModeGen: Gen<AudioMode>

    override val protocol: PacketTestProtocol<IncomingAudioPacket> = PacketTestProtocol()

    override val payloadGen: Gen<ByteReadPacket> get() = Arb.bind(Arb.int(), audioModeGen) { id, mode ->
        audioID = id
        audioMode = mode

        buildPacket {
            writeIntLittleEndian(audioID)

            when (mode) {
                is AudioMode.Playing -> {
                    writeIntLittleEndian(1)
                }
                is AudioMode.Incoming -> {
                    writeIntLittleEndian(2)
                    writeString(mode.title.toString())
                    writeString(mode.filename.toString())
                }
            }
        }
    }

    override val failures: List<Failure> = listOf(
        object : Failure("Fails to parse invalid audio mode") {
            override val payloadGen: Gen<ByteReadPacket> = Arb.bind(
                Arb.int(),
                Arb.int().filter { it !in 1..2 },
            ) { id, mode ->
                buildPacket {
                    writeIntLittleEndian(id)
                    writeIntLittleEndian(mode)
                }
            }
        },
        object : Failure("Fails to parse incoming audio without title") {
            override val payloadGen: Gen<ByteReadPacket> = Arb.int().map {
                buildPacket {
                    writeIntLittleEndian(it)
                    writeIntLittleEndian(2)
                }
            }
        },
        object : Failure("Fails to parse incoming audio without filename") {
            override val payloadGen: Gen<ByteReadPacket> = Arb.bind(
                Arb.int(),
                Arb.string(),
            ) { id, title ->
                buildPacket {
                    writeIntLittleEndian(id)
                    writeIntLittleEndian(2)
                    writeString(title)
                }
            }
        },
    )

    override suspend fun testType(packet: ArtemisPacket): IncomingAudioPacket =
        packet.shouldBeInstanceOf()

    override suspend fun testPayload(packet: IncomingAudioPacket) {
        packet.audioId shouldBeEqual audioID

        when (val mode = audioMode) {
            is AudioMode.Playing -> {
                packet.audioMode.shouldBeInstanceOf<AudioMode.Playing>()
            }
            is AudioMode.Incoming -> {
                val readMode = packet.audioMode
                readMode.shouldBeInstanceOf<AudioMode.Incoming>()
                readMode.title.toString() shouldBeEqual mode.title.toString()
                readMode.filename.toString() shouldBeEqual mode.filename.toString()
            }
        }
    }

    override suspend fun DescribeSpecContainerScope.organizeTests(
        describeTests: suspend DescribeSpecContainerScope.() -> Unit,
    ) {
        allTestCases.forEach { (name, gen) ->
            describe(name) {
                audioModeGen = gen

                describeTests()
            }
        }
    }

    override suspend fun describeClientTests(
        scope: DescribeSpecContainerScope,
        client: ArtemisNetworkInterface,
        sendChannel: ByteWriteChannel,
        afterTest: () -> Unit
    ) {
        scope.describe(specName) {
            withData(nameFn = { it.first }, allTestCases) {
                audioModeGen = it.second
                runClientTest(client, sendChannel)
                afterTest()
            }
        }
    }

    private companion object {
        val allTestCases = listOf(
            "Playing" to Arb.of(AudioMode.Playing),
            "Incoming" to Arb.bind(
                Arb.string(),
                Arb.string(),
            ) { title, filename -> AudioMode.Incoming(title, filename) },
        )
    }
}
