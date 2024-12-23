package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.iface.ListenerRegistry
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.PacketWriter
import com.walkertribe.ian.iface.ParseResult
import com.walkertribe.ian.iface.TestListener
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketTestListenerModule
import com.walkertribe.ian.protocol.core.PacketTestFixture.Companion.organizeTests
import com.walkertribe.ian.protocol.core.PacketTestFixture.Companion.writePacketWithHeader
import com.walkertribe.ian.world.ArtemisObjectTestModule
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.annotation.Ignored
import io.kotest.core.factory.TestFactory
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.describeSpec
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Gen
import io.kotest.property.checkAll
import io.ktor.utils.io.ByteChannel
import io.mockk.clearAllMocks
import io.mockk.unmockkAll
import kotlinx.io.Source

@Ignored
sealed class PacketTestSpec<T : Packet>(
    val specName: String,
    open val fixtures: List<PacketTestFixture<T>>,
    autoIncludeTests: Boolean = true,
) : DescribeSpec() {
    init {
        if (autoIncludeTests) {
            @Suppress("LeakingThis")
            include(tests())
        }

        finalizeSpec {
            clearAllMocks()
            unmockkAll()
        }
    }

    abstract class Client<T : Packet.Client>(
        specName: String,
        final override val fixtures: List<PacketTestFixture.Client<T>>,
        autoIncludeTests: Boolean = true,
    ) : PacketTestSpec<T>(specName, fixtures, autoIncludeTests) {
        open suspend fun DescribeSpecContainerScope.describeMore() { }

        override fun tests(): TestFactory = describeSpec {
            val sendChannel = ByteChannel()
            val writer = PacketWriter(sendChannel)

            finalizeSpec {
                writer.close()
            }

            describe(specName) {
                organizeTests(fixtures) { fixture ->
                    it("Can write to PacketWriter") {
                        fixture.generator.checkAll { data ->
                            data.packet.writeTo(writer)
                            writer.flush()

                            val payload = fixture.readPacket(sendChannel)
                            data.validate(payload, fixture.expectedPayloadSize, fixture.packetType)

                            packets.add(data.packet)
                        }
                    }

                    it("Packet type matches") {
                        packets.forEach { it.type shouldBeEqual fixture.packetType }
                    }

                    packets.clear()
                    fixture.describeMore(this)
                }

                describeMore()
            }
        }
    }

    abstract class Server<T : Packet.Server>(
        specName: String,
        final override val fixtures: List<PacketTestFixture.Server<T>>,
        private val failures: List<Failure> = listOf(),
        private val isRequired: Boolean = false,
        autoIncludeTests: Boolean = true,
    ) : PacketTestSpec<T>(specName, fixtures, autoIncludeTests) {
        abstract class Failure(val packetType: Int, val testName: String) {
            abstract val payloadGen: Gen<Source>
        }

        private val expectedBehaviour by lazy { if (isRequired) "parse even" else "skip" }

        private val emptyListenerRegistry by lazy { ListenerRegistry() }
        private val testListenerRegistry by lazy {
            ListenerRegistry().apply { register(PacketTestListenerModule) }
        }
        private val objectListenerRegistry by lazy {
            ListenerRegistry().apply { register(ArtemisObjectTestModule) }
        }

        open suspend fun DescribeSpecContainerScope.describeMore() { }

        override fun tests(): TestFactory = describeSpec {
            describe(specName) {
                val expectedBehaviour = if (isRequired) "parse even" else "skip"
                val emptyListenerRegistry = ListenerRegistry()
                val testListenerRegistry = ListenerRegistry().apply {
                    register(PacketTestListenerModule)
                }
                val objectListenerRegistry = ListenerRegistry().apply {
                    register(ArtemisObjectTestModule)
                }

                organizeTests(fixtures) { fixture ->
                    PacketTestListenerModule.packets.clear()

                    val objectListenerBehaviour =
                        if (fixture.recognizeObjectListeners) "parse with" else "ignore"

                    val testCases = listOfNotNull(
                        Triple(
                            "Can read from PacketReader",
                            testListenerRegistry,
                            true,
                        ),
                        Triple(
                            "Will $expectedBehaviour without listeners",
                            emptyListenerRegistry,
                            isRequired,
                        ),
                        if (isRequired) null else Triple(
                            "Will $objectListenerBehaviour object listeners",
                            objectListenerRegistry,
                            fixture.recognizeObjectListeners,
                        ),
                    )

                    withData(nameFn = { it.first }, testCases) {
                        runTest(fixture, it.second, it.third)
                    }

                    it("Can offer to listener modules") {
                        packets.forEach(testListenerRegistry::offer)
                        PacketTestListenerModule.packets shouldContainExactly packets
                    }

                    packets.clear()

                    fixture.describeMore(this)
                }

                PacketTestListenerModule.packets.clear()

                describeMore()
                describeFailures()
            }
        }

        private suspend fun runTest(
            fixture: PacketTestFixture.Server<T>,
            listenerRegistry: ListenerRegistry,
            expectPacket: Boolean,
        ) {
            val readChannel = ByteChannel()
            val reader = PacketReader(readChannel, listenerRegistry)

            fixture.generator.checkAll { data ->
                reader.version = data.version
                readChannel.writePacketWithHeader(fixture.packetType, data.buildPayload())

                if (expectPacket) {
                    val result = shouldNotThrowAny { reader.readPacket() }
                    result.shouldBeInstanceOf<ParseResult.Success>()

                    val packet = fixture.testType(result.packet)
                    data.validate(packet)
                    packets.add(packet)
                    fixture.afterTest(data)
                } else {
                    reader.readPacket().shouldBeInstanceOf<ParseResult.Skip>()
                }
            }

            reader.close()
        }

        private suspend fun DescribeSpecContainerScope.describeFailures() {
            if (failures.isNotEmpty()) {
                val readChannel = ByteChannel()
                val reader = PacketReader(
                    readChannel,
                    ListenerRegistry().apply { register(TestListener.module) }
                )

                withData(nameFn = { it.testName }, failures) {
                    it.payloadGen.checkAll { payload ->
                        readChannel.writePacketWithHeader(it.packetType, payload)

                        val result = reader.readPacket()
                        result.shouldBeInstanceOf<ParseResult.Fail>()
                    }
                }

                reader.close()
            }
        }
    }

    protected val packets = mutableListOf<T>()

    abstract fun tests(): TestFactory
}
