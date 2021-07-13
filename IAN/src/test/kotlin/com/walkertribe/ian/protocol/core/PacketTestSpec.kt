package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.iface.ListenerRegistry
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.iface.ParseResult
import com.walkertribe.ian.iface.TestListener
import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import com.walkertribe.ian.util.Version
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Ignored
import io.kotest.core.factory.TestFactory
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.describeSpec
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.longs.shouldBeZero
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Gen
import io.kotest.property.PropertyContext
import io.kotest.property.checkAll
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.bits.reverseByteOrder
import io.ktor.utils.io.core.BytePacketBuilder
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.EOFException
import io.ktor.utils.io.core.readIntLittleEndian
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.core.writeIntLittleEndian
import io.ktor.utils.io.core.writeShort
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readIntLittleEndian
import io.ktor.utils.io.writeIntLittleEndian
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

@Ignored
sealed class PacketTestSpec<T : ArtemisPacket>(
    val specName: String,
    val packetType: Int,
    val packetTypeName: String,
    private val expectedOrigin: Origin,
    autoIncludeTests: Boolean = true,
) : DescribeSpec() {
    init {
        if (autoIncludeTests) {
            @Suppress("LeakingThis")
            include(tests())
        }
    }

    abstract class Client<T : ArtemisPacket>(
        specName: String,
        packetType: Int,
        packetTypeName: String,
        private val expectedPayloadSize: Int,
        autoIncludeTests: Boolean = true,
    ) : PacketTestSpec<T>(specName, packetType, packetTypeName, Origin.CLIENT, autoIncludeTests) {
        private val expectedHeader: List<Int> by lazy {
            listOf(
                ArtemisPacket.HEADER,
                expectedPayloadSize + ArtemisPacket.PREAMBLE_SIZE,
                Origin.CLIENT.toInt(),
                0,
                expectedPayloadSize + Int.SIZE_BYTES,
            )
        }

        abstract suspend fun runTest(packet: T, payload: ByteReadPacket)

        abstract suspend fun DescribeSpecContainerScope.organizeTests(
            describe: suspend DescribeSpecContainerScope.(Gen<T>) -> Unit,
        )

        open suspend fun DescribeSpecContainerScope.describeMore() { }

        abstract fun clientPacketGen(client: ArtemisNetworkInterface): Gen<T>

        override fun tests(): TestFactory = describeSpec {
            val sendChannel = mockk<ByteWriteChannel>()
            val writer = PacketWriter(sendChannel)

            val ints = mutableListOf<Int>()
            val payloadSlot = slot<ByteReadPacket>()

            coEvery { sendChannel.writeInt(capture(ints)) } just runs
            coEvery { sendChannel.writePacket(capture(payloadSlot)) } just runs
            every { sendChannel.flush() } just runs

            afterSpec {
                every { sendChannel.close(any()) } returns true

                writer.close()

                clearAllMocks()
                unmockkAll()
            }

            describe(specName) {
                organizeTests { generator ->
                    it("Can write to PacketWriter") {
                        generator.checkAll { packet ->
                            packet.writeTo(writer)
                            writer.flush()

                            ints shouldContainExactly expectedHeader.map(Int::reverseByteOrder)

                            val payload = payloadSlot.captured
                            payload.remaining.toInt() shouldBeEqual
                                expectedPayloadSize + Int.SIZE_BYTES
                            payload.readIntLittleEndian() shouldBeEqual packetType

                            runTest(packet, payload)
                            payload.remaining.shouldBeZero()

                            ints.clear()
                            payload.close()
                            packets.add(packet)
                        }
                    }

                    describePropertiesTests()
                    packets.clear()
                }

                describeMore()
            }
        }

        open suspend fun runClientTest(
            scope: ContainerScope,
            client: ArtemisNetworkInterface,
            readChannel: ByteReadChannel,
        ) {
            clientPacketGen(client).checkAll { packet ->
                eventually(CLIENT_CONFIG) {
                    withTimeout(CLIENT_TIMEOUT.seconds) {
                        readChannel.readAvailable(byteArrayOf())
                        client.send(packet)
                        runClientTest(packet, readChannel)
                    }
                }
            }
        }

        suspend fun runClientTest(packet: T, readChannel: ByteReadChannel) {
            expectedHeader.forEach {
                readChannel.readIntLittleEndian() shouldBeEqual it
            }

            val payload = readChannel.readPacket(expectedHeader.last())
            payload.readIntLittleEndian() shouldBeEqual packetType
            runTest(packet, payload)
        }
    }

    abstract class Server<T : ArtemisPacket>(
        specName: String,
        packetType: Int,
        packetTypeName: String,
        private val needsListeners: Boolean = true,
        autoIncludeTests: Boolean = true,
    ) : PacketTestSpec<T>(specName, packetType, packetTypeName, Origin.SERVER, autoIncludeTests) {
        abstract class Failure(val testName: String) {
            abstract val payloadGen: Gen<ByteReadPacket>
        }

        open val version: Version = ArtemisNetworkInterface.LATEST_VERSION

        open val failures: List<Failure> = listOf()

        abstract val protocol: PacketTestProtocol<T>

        abstract val payloadGen: Gen<ByteReadPacket>

        abstract suspend fun testType(packet: ArtemisPacket): T

        open suspend fun testPayload(packet: T) { }

        open suspend fun DescribeSpecContainerScope.organizeTests(
            describeTests: suspend DescribeSpecContainerScope.() -> Unit,
        ) {
            describeTests()
        }

        open suspend fun generateTest(
            client: ArtemisNetworkInterface,
            runTest: suspend (ByteReadPacket) -> Unit,
        ) {
            payloadGen.checkAll { runTest(it) }
        }

        open fun collect(context: PropertyContext) { }

        open suspend fun DescribeSpecContainerScope.describeMore(
            readChannel: ByteReadChannel,
        ) { }

        override fun tests(): TestFactory = describeSpec {
            val readChannel = mockk<ByteReadChannel>()

            describe(specName) {
                val expectedBehaviour = if (needsListeners) "skip" else "parse even"

                organizeTests {
                    withData(
                        nameFn = { it.first },
                        Triple(
                            "Can read from PacketReader",
                            TestListener.registry,
                            false,
                        ),
                        Triple(
                            "Will $expectedBehaviour without listeners",
                            ListenerRegistry(),
                            needsListeners,
                        ),
                    ) { (_, listenerRegistry, shouldSkip) ->
                        val reader = PacketReader(
                            readChannel,
                            protocol,
                            listenerRegistry,
                        )

                        payloadGen.checkAll { payload ->
                            this@Server.collect(this)

                            reader.version = version
                            readChannel.prepareMockPacket(payload, packetType)

                            if (shouldSkip) {
                                shouldThrow<EOFException> { reader.readPacket() }
                            } else {
                                val result = reader.readPacket()
                                result.shouldBeInstanceOf<ParseResult.Success>()

                                val packet = testType(result.packet)
                                testPayload(packet)
                                packets.add(packet)
                            }
                        }
                    }

                    it("Cannot write to PacketWriter") {
                        val writer = mockk<PacketWriter> {
                            justRun { unsupported() }
                            every { start(any()) } returns this
                        }

                        packets.forEach { it.writeTo(writer) }

                        verify(exactly = packets.size) { writer.unsupported() }
                    }

                    describePropertiesTests()
                    packets.clear()
                }

                describeMore(readChannel)
                describeFailures(readChannel)
            }
        }

        suspend fun prepareClient(sendChannel: ByteWriteChannel, payload: ByteReadPacket) {
            val payloadSize = payload.remaining.toInt()

            sendChannel.writeIntLittleEndian(ArtemisPacket.HEADER)
            sendChannel.writeIntLittleEndian(payloadSize + ArtemisPacket.PREAMBLE_SIZE)
            sendChannel.writeIntLittleEndian(Origin.SERVER.toInt())
            sendChannel.writeIntLittleEndian(0)
            sendChannel.writeIntLittleEndian(payloadSize + Int.SIZE_BYTES)
            sendChannel.writeIntLittleEndian(packetType)

            sendChannel.writePacket(payload)
            sendChannel.flush()
        }

        suspend fun runClientTest(
            client: ArtemisNetworkInterface,
            sendChannel: ByteWriteChannel,
        ) {
            var count = 0

            generateTest(client) {
                prepareClient(sendChannel, it)

                count++
            }

            eventually(SERVER_TIMEOUT.seconds) {
                val packets = TestListener.calls<ArtemisPacket>()
                packets.size shouldBeEqual count
                packets.forEach { testType(it) }
            }
        }

        open suspend fun describeClientTests(
            scope: DescribeSpecContainerScope,
            client: ArtemisNetworkInterface,
            sendChannel: ByteWriteChannel,
            afterTest: () -> Unit,
        ) {
            scope.it(specName) {
                runClientTest(client, sendChannel)
                afterTest()
            }
        }

        private suspend fun DescribeSpecContainerScope.describeFailures(
            readChannel: ByteReadChannel,
        ) {
            if (failures.isNotEmpty()) {
                val reader = PacketReader(
                    readChannel,
                    protocol,
                    TestListener.registry,
                )

                withData(nameFn = { it.testName }, failures) {
                    it.payloadGen.checkAll { payload ->
                        readChannel.prepareMockPacket(payload, packetType)

                        val result = reader.readPacket()
                        result.shouldBeInstanceOf<ParseResult.Fail>()
                    }
                }
            }
        }
    }

    protected val packets = mutableListOf<T>()

    protected suspend fun DescribeSpecContainerScope.describePropertiesTests() {
        describe("Packet properties") {
            it("Origin: $expectedOrigin") {
                packets.forEach { it.origin shouldBeEqual expectedOrigin }
            }

            it("Type: $packetTypeName") {
                packets.forEach { it.type shouldBeEqual packetType }
            }
        }
    }

    abstract fun tests(): TestFactory

    companion object {
        const val SERVER_TIMEOUT = 10
        private const val CLIENT_TIMEOUT = 30
        private val CLIENT_CONFIG = eventuallyConfig {
            duration = CLIENT_TIMEOUT.seconds * 10
            expectedExceptions = setOf(TimeoutCancellationException::class)
            includeFirst = false
        }

        fun ByteReadChannel.prepareMockPacket(payload: ByteReadPacket, packetType: Int) {
            clearMocks(this)

            val payloadSize = payload.remaining.toInt()
            coEvery { readInt() } returnsMany listOf(
                ArtemisPacket.HEADER,
                payloadSize + ArtemisPacket.PREAMBLE_SIZE,
                Origin.SERVER.toInt(),
                0,
                payloadSize + Int.SIZE_BYTES,
                packetType,
            ).map(Int::reverseByteOrder) andThenThrows EOFException()
            coEvery { readPacket(payloadSize) } returns payload
        }

        fun BytePacketBuilder.writeString(str: String) {
            writeIntLittleEndian(str.length + 1)
            writeFully(Charsets.UTF_16LE.encode(str))
            writeShort(0)
        }
    }
}
