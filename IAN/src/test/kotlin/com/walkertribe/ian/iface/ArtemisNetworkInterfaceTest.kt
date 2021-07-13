package com.walkertribe.ian.iface

import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.core.ActivateUpgradePacketTest
import com.walkertribe.ian.protocol.core.BayStatusPacketTest
import com.walkertribe.ian.protocol.core.ButtonClickPacketTest
import com.walkertribe.ian.protocol.core.EndGamePacketTest
import com.walkertribe.ian.protocol.core.GameOverReasonPacketTest
import com.walkertribe.ian.protocol.core.GameStartPacketTest
import com.walkertribe.ian.protocol.core.HeartbeatPacket
import com.walkertribe.ian.protocol.core.HeartbeatPacketTest
import com.walkertribe.ian.protocol.core.JumpEndPacketTest
import com.walkertribe.ian.protocol.core.PausePacketTest
import com.walkertribe.ian.protocol.core.PlayerShipDamagePacketTest
import com.walkertribe.ian.protocol.core.comm.AudioCommandPacketTest
import com.walkertribe.ian.protocol.core.comm.CommsButtonPacketTest
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacketTest
import com.walkertribe.ian.protocol.core.comm.CommsOutgoingPacketTest
import com.walkertribe.ian.protocol.core.comm.IncomingAudioPacketTest
import com.walkertribe.ian.protocol.core.comm.ToggleRedAlertPacketTest
import com.walkertribe.ian.protocol.core.setup.AllShipSettingsPacketTest
import com.walkertribe.ian.protocol.core.setup.ReadyPacketTest
import com.walkertribe.ian.protocol.core.setup.SetConsolePacketTest
import com.walkertribe.ian.protocol.core.setup.SetShipPacketTest
import com.walkertribe.ian.protocol.core.setup.VersionPacketTest
import com.walkertribe.ian.protocol.core.setup.WelcomePacketTest
import com.walkertribe.ian.protocol.core.world.BiomechRagePacketTest
import com.walkertribe.ian.protocol.core.world.DeleteObjectPacketTest
import com.walkertribe.ian.protocol.core.world.DockedPacketTest
import com.walkertribe.ian.protocol.core.world.IntelPacketTest
import com.walkertribe.ian.protocol.core.world.ObjectUpdatePacketTest
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeZero
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.choose
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.triple
import io.kotest.property.checkAll
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.close
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class ArtemisNetworkInterfaceTest : DescribeSpec({
    failfast = true

    beforeAny {
        TestListener.clear()
    }

    afterSpec {
        TestListener.clear()
    }

    describe("ArtemisNetworkInterface") {
        val loopbackAddress = "127.0.0.1"
        val port = 2010
        val testTimeout = 20.seconds
        lateinit var client: ArtemisNetworkInterface

        SelectorManager(Dispatchers.IO).use { selector ->
            aSocket(selector).tcp().bind(loopbackAddress, port).use { server ->
                lateinit var socket: Socket

                it("Can connect") {
                    eventually(5.seconds) {
                        val clientDeferred = async {
                            KtorArtemisNetworkInterface.connect(
                                host = loopbackAddress,
                                port = port,
                                timeoutMs = 1000L,
                                debugMode = true,
                            )
                        }

                        socket = server.accept()
                        client = clientDeferred.await().shouldNotBeNull()

                        client.addListener(TestListener)
                        client.setAutoSendHeartbeat(false)
                        client.start()
                    }
                }

                val sendChannel = socket.openWriteChannel(autoFlush = false)
                val readChannel = socket.openReadChannel()

                val allPackets = mutableMapOf<String, List<ArtemisPacket>>()

                describe("Can receive packets from server") {
                    val scope = this

                    arrayOf(
                        ObjectUpdatePacketTest(),
                        DeleteObjectPacketTest(),
                        GameStartPacketTest(),
                        PausePacketTest(),
                        CommsButtonPacketTest(),
                        IncomingAudioPacketTest(),
                        AllShipSettingsPacketTest(),
                        BayStatusPacketTest(),
                        EndGamePacketTest(),
                        GameOverReasonPacketTest(),
                        HeartbeatPacketTest.Server,
                        JumpEndPacketTest(),
                        PlayerShipDamagePacketTest(),
                        CommsIncomingPacketTest(),
                        BiomechRagePacketTest(),
                        DockedPacketTest(),
                        IntelPacketTest(),
                        WelcomePacketTest(),
                        VersionPacketTest(),
                    ).forEach { spec ->
                        spec.describeClientTests(scope, client, sendChannel) {
                            val list = allPackets[spec.specName].orEmpty()
                            allPackets[spec.specName] =
                                TestListener.calls(spec.protocol.packetClass) + list
                        }
                    }
                }

                describe("Can send packets from client") {
                    withData(
                        nameFn = { it.specName },
                        ReadyPacketTest(),
                        SetConsolePacketTest(),
                        SetShipPacketTest(),
                        AudioCommandPacketTest(),
                        CommsOutgoingPacketTest(),
                        ToggleRedAlertPacketTest(),
                        ActivateUpgradePacketTest(),
                        ButtonClickPacketTest(),
                        HeartbeatPacketTest.Client,
                    ) { spec ->
                        spec.runClientTest(this, client, readChannel)
                    }
                }

                describe("Cannot send server packet from client") {
                    withData(allPackets) { packets ->
                        client.send(packets)
                        readChannel.availableForRead.shouldBeZero()
                    }
                }

                it("Can lose heartbeat") {
                    GameStartPacketTest().runClientTest(client, sendChannel)

                    client.setTimeout(1L)
                    eventually(1.seconds) {
                        val events = TestListener.calls<ConnectionEvent.HeartbeatLost>()
                        events.size shouldBeEqual 1
                    }
                }

                it("Can regain heartbeat") {
                    client.setTimeout(1000L)
                    HeartbeatPacketTest.Server.runClientTest(client, sendChannel)

                    val events = TestListener.calls<ConnectionEvent.HeartbeatRegained>()
                    events.size shouldBeEqual 1
                }

                it("Sends heartbeats intermittently") {
                    client.setAutoSendHeartbeat(true)

                    repeat(3) {
                        eventually(60.seconds) {
                            withTimeout(10.seconds) {
                                HeartbeatPacketTest.Client.runClientTest(
                                    HeartbeatPacket.Client,
                                    readChannel
                                )
                            }
                        }
                    }
                }

                it("Can stop") {
                    client.stop()

                    eventually(1.seconds) {
                        val events = TestListener.calls<ConnectionEvent.Disconnect>()
                        events.size shouldBeEqual 1
                        events.forEach {
                            it.cause.shouldBeInstanceOf<DisconnectCause.LocalDisconnect>()
                        }
                    }
                }

                readChannel.cancel()
                sendChannel.close()

                it("Closes on remote disconnect") {
                    eventually(testTimeout * 5) {
                        val clientDeferred = async {
                            KtorArtemisNetworkInterface.connect(
                                host = loopbackAddress,
                                port = port,
                                timeoutMs = 1000L,
                            )
                        }

                        socket = server.accept()
                        client = clientDeferred.await().shouldNotBeNull()
                        client.setAutoSendHeartbeat(false)
                        client.start()
                        client.addListener(TestListener)

                        socket.dispose()

                        eventually(testTimeout) {
                            assertSoftly {
                                val events = TestListener.calls<ConnectionEvent.Disconnect>()
                                events.size shouldBeEqual 1
                                events.forEach {
                                    val (cause) =
                                        it.shouldBeInstanceOf<ConnectionEvent.Disconnect>()
                                    cause.shouldBeInstanceOf<DisconnectCause.RemoteDisconnect>()
                                }
                            }
                        }
                    }
                }

                it("Closes on read exception") {
                    var sender: ByteWriteChannel? = null

                    eventually(testTimeout) {
                        val clientDeferred = async {
                            KtorArtemisNetworkInterface.connect(
                                host = loopbackAddress,
                                port = port,
                                timeoutMs = 1000L,
                            )
                        }

                        socket = server.accept()
                        client = clientDeferred.await().shouldNotBeNull()
                        client.setAutoSendHeartbeat(false)
                        client.start()
                        client.addListener(TestListener)
                        TestListener.clear()

                        sender?.close()
                        sender = socket.openWriteChannel(autoFlush = false).apply {
                            val spec = VersionPacketTest()
                            spec.failures.forEach {
                                it.payloadGen.checkAll(1) { payload ->
                                    spec.prepareClient(this@apply, payload)
                                }
                            }
                        }

                        eventually(2.seconds) {
                            assertSoftly {
                                val events = TestListener.calls<ConnectionEvent.Disconnect>()
                                events.size shouldBeEqual 1
                                events.forEach {
                                    val (cause) =
                                        it.shouldBeInstanceOf<ConnectionEvent.Disconnect>()
                                    cause.shouldBeInstanceOf<DisconnectCause.PacketParseError>()
                                }
                            }
                        }
                    }

                    sender?.close()
                }

                describe("Closes on unsupported version") {
                    withData(
                        nameFn = { it.first },
                        "Too old" to Arb.choose(
                            3 to Arb.triple(Arb.of(2), Arb.int(0..2), Arb.nonNegativeInt()),
                            997 to Arb.triple(
                                Arb.of(0, 1),
                                Arb.nonNegativeInt(),
                                Arb.nonNegativeInt(),
                            ),
                        ),
                        "Beyond latest version" to Arb.choose(
                            1 to Arb.triple(Arb.of(2), Arb.of(8), Arb.int(min = 2)),
                            9 to Arb.triple(Arb.of(2), Arb.int(min = 9), Arb.nonNegativeInt()),
                            990 to Arb.triple(
                                Arb.int(min = 3),
                                Arb.nonNegativeInt(),
                                Arb.nonNegativeInt(),
                            ),
                        ),
                    ) { (_, versionArb) ->
                        val versionSpec = VersionPacketTest()

                        versionArb.checkAll(10) { (major, minor, patch) ->
                            versionSpec.majorVersion = major
                            versionSpec.minorVersion = minor
                            versionSpec.patchVersion = patch

                            eventually(testTimeout) {
                                var sender: ByteWriteChannel? = null

                                val result = withTimeoutOrNull(2.seconds) {
                                    val clientDeferred = async {
                                        KtorArtemisNetworkInterface.connect(
                                            host = loopbackAddress,
                                            port = port,
                                            timeoutMs = 1000L,
                                        )
                                    }

                                    socket = server.accept()
                                    client = clientDeferred.await().shouldNotBeNull()
                                    client.setAutoSendHeartbeat(false)
                                    client.start()
                                    client.addListener(TestListener)
                                    TestListener.clear()

                                    sender = socket.openWriteChannel(autoFlush = true).also {
                                        versionSpec.prepareClient(it, versionSpec.buildPacket())
                                    }

                                    eventually(5.seconds) {
                                        val events =
                                            TestListener.calls<ConnectionEvent.Disconnect>()
                                        events.size shouldBeEqual 1
                                        events.forEach {
                                            it.cause.shouldBeInstanceOf<DisconnectCause.UnsupportedServerVersion>()
                                        }
                                    }
                                }

                                sender?.close()
                                result.shouldNotBeNull()
                            }
                        }
                    }
                }
            }
        }

        it("Connection attempt can time out") {
            KtorArtemisNetworkInterface.connect(
                host = loopbackAddress,
                port = port,
            ).shouldBeNull()
        }

        it("Cannot connect to invalid address") {
            Arb.string(minSize = 1, codepoints = Codepoint.alphanumeric()).checkAll {
                KtorArtemisNetworkInterface.connect(
                    host = it,
                    port = port,
                    timeoutMs = 5000L,
                ).shouldBeNull()
            }
        }
    }
})
