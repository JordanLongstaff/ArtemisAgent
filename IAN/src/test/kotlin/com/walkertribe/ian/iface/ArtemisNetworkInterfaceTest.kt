package com.walkertribe.ian.iface

import com.walkertribe.ian.enums.GameType
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.ActivateUpgradePacketFixture
import com.walkertribe.ian.protocol.core.BayStatusPacket
import com.walkertribe.ian.protocol.core.BayStatusPacketFixture
import com.walkertribe.ian.protocol.core.ButtonClickPacketFixture
import com.walkertribe.ian.protocol.core.EndGamePacket
import com.walkertribe.ian.protocol.core.EndGamePacketFixture
import com.walkertribe.ian.protocol.core.GameOverReasonPacket
import com.walkertribe.ian.protocol.core.GameOverReasonPacketFixture
import com.walkertribe.ian.protocol.core.GameStartPacket
import com.walkertribe.ian.protocol.core.GameStartPacketFixture
import com.walkertribe.ian.protocol.core.HeartbeatPacket
import com.walkertribe.ian.protocol.core.HeartbeatPacketFixture
import com.walkertribe.ian.protocol.core.JumpEndPacket
import com.walkertribe.ian.protocol.core.JumpEndPacketFixture
import com.walkertribe.ian.protocol.core.PacketTestData
import com.walkertribe.ian.protocol.core.PacketTestFixture
import com.walkertribe.ian.protocol.core.PacketTestFixture.Companion.writePacketWithHeader
import com.walkertribe.ian.protocol.core.PausePacket
import com.walkertribe.ian.protocol.core.PausePacketFixture
import com.walkertribe.ian.protocol.core.PlayerShipDamagePacket
import com.walkertribe.ian.protocol.core.PlayerShipDamagePacketFixture
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.protocol.core.comm.AudioCommandPacketFixture
import com.walkertribe.ian.protocol.core.comm.CommsButtonPacket
import com.walkertribe.ian.protocol.core.comm.CommsButtonPacketFixture
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacketFixture
import com.walkertribe.ian.protocol.core.comm.CommsOutgoingPacketFixture
import com.walkertribe.ian.protocol.core.comm.IncomingAudioPacket
import com.walkertribe.ian.protocol.core.comm.IncomingAudioPacketFixture
import com.walkertribe.ian.protocol.core.comm.ToggleRedAlertPacketFixture
import com.walkertribe.ian.protocol.core.setup.AllShipSettingsPacket
import com.walkertribe.ian.protocol.core.setup.AllShipSettingsPacketFixture
import com.walkertribe.ian.protocol.core.setup.ReadyPacketFixture
import com.walkertribe.ian.protocol.core.setup.SetConsolePacketFixture
import com.walkertribe.ian.protocol.core.setup.SetShipPacketFixture
import com.walkertribe.ian.protocol.core.setup.VersionPacket
import com.walkertribe.ian.protocol.core.setup.VersionPacketFixture
import com.walkertribe.ian.protocol.core.setup.WelcomePacket
import com.walkertribe.ian.protocol.core.setup.WelcomePacketFixture
import com.walkertribe.ian.protocol.core.world.BiomechRagePacket
import com.walkertribe.ian.protocol.core.world.BiomechRagePacketFixture
import com.walkertribe.ian.protocol.core.world.DeleteObjectPacket
import com.walkertribe.ian.protocol.core.world.DeleteObjectPacketFixture
import com.walkertribe.ian.protocol.core.world.DockedPacket
import com.walkertribe.ian.protocol.core.world.DockedPacketFixture
import com.walkertribe.ian.protocol.core.world.IntelPacket
import com.walkertribe.ian.protocol.core.world.IntelPacketFixture
import com.walkertribe.ian.protocol.core.world.ObjectUpdatePacket
import com.walkertribe.ian.protocol.core.world.ObjectUpdatePacketFixture
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.eventuallyConfig
import io.kotest.assertions.retry
import io.kotest.assertions.throwables.shouldNotThrowAnyUnit
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.choose
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.cancel
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.readByteArray
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.withTimeout
import kotlinx.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ArtemisNetworkInterfaceTest :
    DescribeSpec({
        failfast = true

        beforeAny { TestListener.clear() }

        afterSpec {
            TestListener.clear()
            clearAllMocks()
            unmockkAll()
        }

        describe("ArtemisNetworkInterface") {
            val loopbackAddress = "127.0.0.1"
            val port = 2010
            val testTimeout = 1.minutes
            val client =
                KtorArtemisNetworkInterface(maxVersion = Version.DEFAULT).apply {
                    addListenerModule(TestListener.module)
                    setAutoSendHeartbeat(false)
                }
            val debugClient =
                KtorArtemisNetworkInterface(maxVersion = null).apply {
                    addListenerModule(TestListener.module)
                    setAutoSendHeartbeat(false)
                }
            val testDispatcher = UnconfinedTestDispatcher()

            SelectorManager(testDispatcher).use { selector ->
                aSocket(selector).tcp().bind(loopbackAddress, port).use { server ->
                    lateinit var socket: Socket

                    it("Can connect") {
                        eventually(5.seconds) {
                            val connectDeferred =
                                async(testDispatcher) {
                                    client.connect(
                                        host = loopbackAddress,
                                        port = port,
                                        timeoutMs = 1000L,
                                    )
                                }

                            socket = server.accept()
                            connectDeferred.await().shouldBeTrue()
                            client.startTime.shouldBeNull()
                        }
                    }

                    it("Cannot stop before starting") { shouldNotThrowAnyUnit { client.stop() } }

                    it("Can start") {
                        client.start()
                        client.startTime.shouldNotBeNull()
                    }

                    it("Cannot start a second time") {
                        val startTime = client.startTime.shouldNotBeNull()
                        client.start()
                        client.startTime.shouldNotBeNull() shouldBeEqual startTime
                    }

                    it("Can reconnect") {
                        eventually(5.seconds) {
                            val connectDeferred =
                                async(testDispatcher) {
                                    client.connect(
                                        host = loopbackAddress,
                                        port = port,
                                        timeoutMs = 1000L,
                                    )
                                }

                            socket = server.accept()
                            connectDeferred.await().shouldBeTrue()
                            client.startTime.shouldBeNull()
                            client.start()
                        }
                    }

                    val sendChannel = socket.openWriteChannel(autoFlush = false)
                    val readChannel = socket.openReadChannel()

                    suspend fun PacketTestFixture.Server<*>.testClient(
                        packetClass: KClass<out Packet.Server>,
                        shouldSendVersionPacket: Boolean,
                    ) {
                        TestListener.clear()
                        var packetCount = 0

                        generator.checkAll { data ->
                            if (shouldSendVersionPacket) {
                                val version = minOf(Version.DEFAULT, data.version)
                                val versionData = VersionPacketFixture.Data(0, 0f, version)
                                sendChannel.writePacketWithHeader(
                                    TestPacketTypes.CONNECTED,
                                    versionData.buildPayload(),
                                )
                            }

                            sendChannel.writePacketWithHeader(packetType, data.buildPayload())
                            packetCount++
                        }

                        eventually(testTimeout) {
                            TestListener.calls(packetClass) shouldHaveSize packetCount
                        }
                    }

                    suspend fun PacketTestFixture.Client<*>.testRead(
                        data: PacketTestData.Client<*>
                    ) {
                        data.validate(readPacket(readChannel), expectedPayloadSize, packetType)
                    }

                    suspend fun PacketTestFixture.Client<*>.testClient() {
                        generator.checkAll { data ->
                            eventually(
                                eventuallyConfig {
                                    duration = 5.minutes
                                    expectedExceptions = setOf(TimeoutCancellationException::class)
                                    includeFirst = false
                                }
                            ) {
                                withTimeout(5.seconds) {
                                    readChannel.readByteArray(readChannel.availableForRead)
                                    client.sendPacket(data.packet)
                                    testRead(data)
                                }
                            }
                        }
                    }

                    describe("Can receive packets from server") {
                        val clientArbVersion =
                            Arb.choose(999 to Arb.version(2, 3..7), 1 to Arb.version(2, 8, 0..1))

                        arrayOf(
                                Triple(
                                    "WelcomePacket",
                                    WelcomePacket::class,
                                    listOf(WelcomePacketFixture),
                                ),
                                Triple(
                                    "VersionPacket",
                                    VersionPacket::class,
                                    listOf(VersionPacketFixture(clientArbVersion)),
                                ),
                                Triple(
                                    "BiomechRagePacket",
                                    BiomechRagePacket::class,
                                    listOf(BiomechRagePacketFixture(clientArbVersion)),
                                ),
                                Triple(
                                    "DockedPacket",
                                    DockedPacket::class,
                                    listOf(DockedPacketFixture(clientArbVersion)),
                                ),
                                Triple(
                                    "EndGamePacket",
                                    EndGamePacket::class,
                                    listOf(EndGamePacketFixture(clientArbVersion)),
                                ),
                                Triple(
                                    "GameOverReasonPacket",
                                    GameOverReasonPacket::class,
                                    listOf(GameOverReasonPacketFixture(clientArbVersion)),
                                ),
                                Triple(
                                    "HeartbeatPacket.Server",
                                    HeartbeatPacket.Server::class,
                                    listOf(HeartbeatPacketFixture.Server(clientArbVersion)),
                                ),
                                Triple(
                                    "JumpEndPacket",
                                    JumpEndPacket::class,
                                    listOf(JumpEndPacketFixture(clientArbVersion)),
                                ),
                                Triple(
                                    "PlayerShipDamagePacket",
                                    PlayerShipDamagePacket::class,
                                    listOf(PlayerShipDamagePacketFixture(clientArbVersion)),
                                ),
                                Triple(
                                    "AllShipSettingsPacket",
                                    AllShipSettingsPacket::class,
                                    AllShipSettingsPacketFixture.ALL,
                                ),
                                Triple(
                                    "BayStatusPacket",
                                    BayStatusPacket::class,
                                    BayStatusPacketFixture.ALL,
                                ),
                                Triple(
                                    "CommsButtonPacket",
                                    CommsButtonPacket::class,
                                    CommsButtonPacketFixture.allFixtures(clientArbVersion),
                                ),
                                Triple(
                                    "CommsIncomingPacket",
                                    CommsIncomingPacket::class,
                                    CommsIncomingPacketFixture.ALL,
                                ),
                                Triple(
                                    "DeleteObjectPacket",
                                    DeleteObjectPacket::class,
                                    DeleteObjectPacketFixture.allFixtures(clientArbVersion),
                                ),
                                Triple(
                                    "GameStartPacket",
                                    GameStartPacket::class,
                                    GameStartPacketFixture.allFixtures(clientArbVersion),
                                ),
                                Triple(
                                    "IncomingAudioPacket",
                                    IncomingAudioPacket::class,
                                    IncomingAudioPacketFixture.allFixtures(clientArbVersion),
                                ),
                                Triple(
                                    "IntelPacket",
                                    IntelPacket::class,
                                    IntelPacketFixture.allFixtures(clientArbVersion),
                                ),
                                Triple(
                                    "PausePacket",
                                    PausePacket::class,
                                    PausePacketFixture.allFixtures(clientArbVersion),
                                ),
                                Triple(
                                    "ObjectUpdatePacket",
                                    ObjectUpdatePacket::class,
                                    ObjectUpdatePacketFixture.ALL,
                                ),
                            )
                            .forEachIndexed { index, (specName, packetClass, fixtures) ->
                                val addVersion = index > 1

                                if (
                                    fixtures.size == 1 ||
                                        (fixtures[0].groupName.isBlank() &&
                                            fixtures[0].specName.isBlank())
                                ) {
                                    it(specName) {
                                        fixtures.forEach { it.testClient(packetClass, addVersion) }
                                    }
                                } else {
                                    describe(specName) {
                                        fixtures
                                            .groupBy { fixture ->
                                                fixture.groupName.takeIf { it.isNotBlank() }
                                                    ?: fixture.specName
                                            }
                                            .forEach { (groupName, list) ->
                                                it(groupName) {
                                                    list.forEach {
                                                        it.testClient(packetClass, addVersion)
                                                    }
                                                }
                                            }
                                    }
                                }
                            }
                    }

                    describe("Can send packets from client") {
                        withData(
                            nameFn = { it.first },
                            "ActivateUpgradePacket" to ActivateUpgradePacketFixture.ALL,
                            "AudioCommandPacket" to AudioCommandPacketFixture.ALL,
                            "ButtonClickPacket" to ButtonClickPacketFixture.ALL,
                            "CommsOutgoingPacket" to CommsOutgoingPacketFixture.ALL,
                            "HeartbeatPacket.Client" to listOf(HeartbeatPacketFixture.Client),
                            "ReadyPacket" to listOf(ReadyPacketFixture),
                            "SetConsolePacket" to SetConsolePacketFixture.ALL,
                            "SetShipPacket" to SetShipPacketFixture.ALL,
                            "ToggleRedAlertPacket" to listOf(ToggleRedAlertPacketFixture),
                        ) { (_, fixtures) ->
                            if (fixtures.size == 1) {
                                fixtures[0].testClient()
                            } else {
                                withData(nameFn = { it.specName }, fixtures) { fixture ->
                                    fixture.testClient()
                                }
                            }
                        }
                    }

                    it("Can lose heartbeat") {
                        TestListener.clear()
                        val gameStartData =
                            GameStartPacketFixture.Data(client.version, GameType.SIEGE, 0)
                        sendChannel.writePacketWithHeader(
                            TestPacketTypes.START_GAME,
                            gameStartData.buildPayload(),
                        )

                        client.setTimeout(1L)
                        eventually(2.seconds) {
                            TestListener.calls<ConnectionEvent.HeartbeatLost>().shouldBeSingleton()
                        }
                    }

                    it("Can regain heartbeat") {
                        client.setTimeout(1000L)
                        retry(3, 15.seconds) {
                            TestListener.clear()
                            sendChannel.writePacketWithHeader(
                                TestPacketTypes.HEARTBEAT,
                                HeartbeatPacketFixture.Server.Data(client.version).buildPayload(),
                            )

                            eventually(5.seconds) {
                                TestListener.calls<ConnectionEvent.HeartbeatRegained>()
                                    .shouldBeSingleton()
                            }
                        }
                    }

                    it("Sends heartbeats intermittently") {
                        client.setAutoSendHeartbeat(true)

                        repeat(3) {
                            eventually(60.seconds) {
                                withTimeout(10.seconds) {
                                    HeartbeatPacketFixture.Client.testRead(
                                        HeartbeatPacketFixture.Client.Data
                                    )
                                }
                            }
                        }
                    }

                    it("Can stop") {
                        TestListener.clear()
                        client.sendJob.cancelAndJoin()
                        client.parseResultDispatchJob.cancelAndJoin()
                        client.connectionListenerJob.cancelAndJoin()

                        client.sendingChannel.send(HeartbeatPacket.Client)
                        client.parseResultsChannel.send(
                            ParseResult.Success(mockk<WelcomePacket>(), ParseResult.Skip)
                        )
                        client.connectionEventChannel.send(ConnectionEvent.Success(""))

                        client.stop()

                        eventually(1.seconds) {
                            TestListener.calls<ConnectionEvent.Disconnect>().shouldBeSingleton {
                                it.cause.shouldBeInstanceOf<DisconnectCause.LocalDisconnect>()
                            }

                            TestListener.calls<ConnectionEvent.Success>().shouldBeEmpty()
                            TestListener.calls<WelcomePacket>().shouldBeEmpty()
                        }
                    }

                    it("Sending channel cleared on stop") {
                        client.sendingChannel.tryReceive().isFailure.shouldBeTrue()
                    }

                    it("Parse results channel cleared on stop") {
                        client.parseResultsChannel.tryReceive().isFailure.shouldBeTrue()
                    }

                    it("Connection events channel cleared on stop") {
                        client.connectionEventChannel.tryReceive().isFailure.shouldBeTrue()
                    }

                    readChannel.cancel()
                    sendChannel.flushAndClose()

                    it("Closes on remote disconnect") {
                        client.setAutoSendHeartbeat(false)

                        eventually(testTimeout * 5) {
                            val connectDeferred =
                                async(testDispatcher) {
                                    client.connect(
                                        host = loopbackAddress,
                                        port = port,
                                        timeoutMs = 1000L,
                                    )
                                }

                            socket = server.accept()
                            connectDeferred.await().shouldBeTrue()
                            client.start()
                            TestListener.clear()

                            socket.dispose()

                            eventually(testTimeout) {
                                assertSoftly {
                                    TestListener.calls<ConnectionEvent.Disconnect>()
                                        .shouldBeSingleton {
                                            it.cause should
                                                beInstanceOf<DisconnectCause.RemoteDisconnect>()
                                        }
                                }
                            }
                        }
                    }

                    it("Closes on read exception") {
                        var sender: ByteWriteChannel? = null

                        eventually(testTimeout) {
                            val connectDeferred =
                                async(testDispatcher) {
                                    client.connect(
                                        host = loopbackAddress,
                                        port = port,
                                        timeoutMs = 1000L,
                                    )
                                }

                            socket = server.accept()
                            connectDeferred.await().shouldBeTrue()
                            client.start()
                            TestListener.clear()

                            sender?.flushAndClose()
                            sender =
                                socket.openWriteChannel(autoFlush = false).apply {
                                    writePacketWithHeader(TestPacketTypes.CONNECTED, buildPacket {})
                                }

                            eventually(2.seconds) {
                                assertSoftly {
                                    TestListener.calls<ConnectionEvent.Disconnect>()
                                        .shouldBeSingleton {
                                            it.cause should
                                                beInstanceOf<DisconnectCause.PacketParseError>()
                                        }
                                }
                            }
                        }

                        sender?.flushAndClose()
                    }

                    withData<Triple<String, Throwable, (DisconnectCause) -> Unit>>(
                        nameFn = { "Closes on ${it.first} error" },
                        Triple("write", IOException()) { cause: DisconnectCause ->
                            cause.shouldBeInstanceOf<DisconnectCause.IOError>()
                        },
                        Triple("unknown", RuntimeException()) { cause: DisconnectCause ->
                            cause.shouldBeInstanceOf<DisconnectCause.UnknownError>()
                        },
                    ) { (_, exception, testCause) ->
                        eventually(testTimeout) {
                            val connectDeferred =
                                async(testDispatcher) {
                                    client.connect(
                                        host = loopbackAddress,
                                        port = port,
                                        timeoutMs = 1000L,
                                    )
                                }

                            socket = server.accept()
                            connectDeferred.await().shouldBeTrue()
                            client.start()
                            TestListener.clear()

                            client.sendPacket(
                                mockk<Packet.Client> { every { writeTo(any()) } throws exception }
                            )

                            eventually(2.seconds) {
                                assertSoftly {
                                    TestListener.calls<ConnectionEvent.Disconnect>()
                                        .shouldBeSingleton { testCause(it.cause) }
                                }
                            }
                        }
                    }

                    describe("Closes on unsupported version") {
                        val clientSpy = spyk(client)

                        every { clientSpy.isRunning } returns true
                        every { clientSpy.startTime } returns null

                        val spyConnectDeferred =
                            async(testDispatcher) {
                                clientSpy.connect(
                                    host = loopbackAddress,
                                    port = port,
                                    timeoutMs = 1000L,
                                )
                            }

                        socket = server.accept()
                        spyConnectDeferred.await().shouldBeTrue()
                        clientSpy.start()

                        val spySender = socket.openWriteChannel(autoFlush = true)

                        val unsupportedTestCases =
                            listOf(
                                "Too old" to
                                    Arb.choose(3 to Arb.version(2, 0..2), 997 to Arb.version(0..1)),
                                "Beyond latest version" to
                                    Arb.choose(
                                        1 to Arb.version(2, 8, Arb.int(min = 2)),
                                        9 to Arb.version(2, Arb.int(min = 9)),
                                        990 to Arb.version(Arb.int(min = 3)),
                                    ),
                            )

                        withData(nameFn = { it.first }, unsupportedTestCases) { (_, versionArb) ->
                            val versionFixture = VersionPacketFixture(versionArb)

                            TestListener.clear()
                            var count = 0
                            versionFixture.generator.checkAll { data ->
                                spySender.writePacketWithHeader(
                                    TestPacketTypes.CONNECTED,
                                    data.buildPayload(),
                                )
                                count++
                            }

                            eventually(testTimeout) {
                                TestListener.calls<ConnectionEvent.Disconnect>().shouldBeSingleton {
                                    it.cause should
                                        beInstanceOf<DisconnectCause.UnsupportedVersion>()
                                }
                            }

                            verify(exactly = count) { clientSpy.stop() }
                        }

                        spySender.flushAndClose()
                        clearMocks(clientSpy)
                        clientSpy.stop()
                        clientSpy.dispose()
                        socket.dispose()

                        it("No upper bound in debug mode") {
                            val versionFixture =
                                VersionPacketFixture(unsupportedTestCases.last().second)

                            val connectDeferred =
                                async(testDispatcher) {
                                    debugClient.connect(
                                        host = loopbackAddress,
                                        port = port,
                                        timeoutMs = 1000L,
                                    )
                                }

                            socket = server.accept()
                            connectDeferred.await().shouldBeTrue()
                            debugClient.start()
                            TestListener.clear()

                            val sender = socket.openWriteChannel(autoFlush = true)

                            var count = 0
                            versionFixture.generator.checkAll { data ->
                                sender.writePacketWithHeader(
                                    TestPacketTypes.CONNECTED,
                                    data.buildPayload(),
                                )
                                count++
                            }

                            eventually(testTimeout) {
                                TestListener.calls<VersionPacket>() shouldHaveSize count
                            }

                            sender.flushAndClose()
                            debugClient.stop()
                            socket.dispose()
                        }
                    }
                }
            }

            it("Cannot restart without new connection") {
                client.start()
                client.startTime.shouldBeNull()
            }

            it("Connection attempt can time out") {
                client.connect(host = loopbackAddress, port = port, timeoutMs = 0L).shouldBeFalse()
            }

            it("Cannot connect to invalid address") {
                Arb.string(minSize = 1, codepoints = Codepoint.alphanumeric()).checkAll {
                    client.connect(host = it, port = port, timeoutMs = 5000L).shouldBeFalse()
                }
            }

            it("Can dispose") {
                client.dispose()
                debugClient.dispose()

                client.sendingChannel.tryReceive().isClosed.shouldBeTrue()
                client.parseResultsChannel.tryReceive().isClosed.shouldBeTrue()
                client.connectionEventChannel.tryReceive().isClosed.shouldBeTrue()
            }
        }
    })
