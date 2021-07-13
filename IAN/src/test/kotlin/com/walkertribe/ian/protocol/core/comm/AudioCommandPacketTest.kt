package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.enums.AudioCommand
import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readIntLittleEndian

class AudioCommandPacketTest : PacketTestSpec.Client<AudioCommandPacket>(
    specName = "AudioCommandPacket",
    packetType = TestPacketTypes.CONTROL_MESSAGE,
    packetTypeName = CorePacketType.CONTROL_MESSAGE,
    expectedPayloadSize = Int.SIZE_BYTES * 2,
) {
    private var clientPacketCommand: AudioCommand = AudioCommand.PLAY

    override suspend fun DescribeSpecContainerScope.organizeTests(
        describe: suspend DescribeSpecContainerScope.(Gen<AudioCommandPacket>) -> Unit,
    ) {
        AudioCommand.entries.forEach { command ->
            describe(command.name) {
                describe(Arb.int().map { AudioCommandPacket(it, command) })
            }
        }
    }

    override suspend fun runTest(packet: AudioCommandPacket, payload: ByteReadPacket) {
        payload.readIntLittleEndian() shouldBeEqual packet.audioId
        payload.readIntLittleEndian() shouldBeEqual packet.command.ordinal
    }

    override suspend fun runClientTest(
        scope: ContainerScope,
        client: ArtemisNetworkInterface,
        readChannel: ByteReadChannel,
    ) {
        scope.withData(AudioCommand.entries) { command ->
            clientPacketCommand = command
            super.runClientTest(this, client, readChannel)
        }
    }

    override fun clientPacketGen(client: ArtemisNetworkInterface): Gen<AudioCommandPacket> {
        return Arb.int().map { AudioCommandPacket(it, clientPacketCommand) }
    }
}
