package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.protocol.Packet
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.property.Gen
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.bits.reverseByteOrder
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.core.writeText
import io.ktor.utils.io.readInt
import io.ktor.utils.io.readPacket
import io.ktor.utils.io.writeInt
import io.ktor.utils.io.writePacket
import kotlinx.coroutines.launch
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.writeIntLe

sealed class PacketTestFixture<T : Packet>(val packetType: Int) {
    abstract class Client<T : Packet.Client>(packetType: Int, val expectedPayloadSize: Int) :
        PacketTestFixture<T>(packetType) {
        abstract val generator: Gen<PacketTestData.Client<T>>

        private val expectedHeader: List<Int> by lazy {
            listOf(
                Packet.HEADER,
                expectedPayloadSize + Packet.PREAMBLE_SIZE,
                Origin.CLIENT.value,
                0,
                expectedPayloadSize + Int.SIZE_BYTES,
            )
        }

        suspend fun readPacket(channel: ByteReadChannel): Source {
            val header = List(NUM_HEADER_INTS) { channel.readInt().reverseByteOrder() }
            header shouldContainExactly expectedHeader
            return channel.readPacket(header.last())
        }
    }

    abstract class Server<T : Packet.Server>(
        packetType: Int,
        val recognizeObjectListeners: Boolean = false,
    ) : PacketTestFixture<T>(packetType) {
        abstract val generator: Gen<PacketTestData.Server<T>>

        abstract suspend fun testType(packet: Packet.Server): T

        open fun afterTest(data: PacketTestData.Server<T>) {}
    }

    open val specName: String = ""
    open val groupName: String = ""

    open suspend fun describeMore(scope: DescribeSpecContainerScope) {}

    companion object {
        private const val NUM_HEADER_INTS = 5

        suspend fun ByteWriteChannel.writePacketWithHeader(packetType: Int, payload: Source) {
            val payloadSize = payload.remaining.toInt()

            writeInt(Packet.HEADER.reverseByteOrder())
            writeInt((payloadSize + Packet.PREAMBLE_SIZE).reverseByteOrder())
            writeInt(Origin.SERVER.value.reverseByteOrder())
            writeInt(0)
            writeInt((payloadSize + Int.SIZE_BYTES).reverseByteOrder())
            writeInt(packetType.reverseByteOrder())

            payload.use { writePacket(it) }
            flush()
        }

        fun <F : PacketTestFixture<*>> DescribeSpecContainerScope.organizeTests(
            fixtures: List<F>,
            describeTests: suspend DescribeSpecContainerScope.(F) -> Unit,
        ) = launch {
            fixtures
                .groupBy { it.groupName }
                .forEach { (groupName, list) ->
                    if (groupName.isBlank()) {
                        listTests(list, describeTests)
                    } else {
                        describe(groupName) { listTests(list, describeTests) }
                    }
                }
        }

        private fun <F : PacketTestFixture<*>> DescribeSpecContainerScope.listTests(
            fixtures: List<F>,
            describeTests: suspend DescribeSpecContainerScope.(F) -> Unit,
        ) = launch {
            if (fixtures.size == 1) {
                val fixture = fixtures[0]
                describeTests(fixture)
            } else {
                fixtures.forEach { fixture ->
                    describe(fixture.specName) { describeTests(fixture) }
                }
            }
        }

        fun Sink.writeString(str: String) {
            writeIntLe(str.length + 1)
            writeText(str, charset = Charsets.UTF_16LE)
            writeShort(0)
        }
    }
}
