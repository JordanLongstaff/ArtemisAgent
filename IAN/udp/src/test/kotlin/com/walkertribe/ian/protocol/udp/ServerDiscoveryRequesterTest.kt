package com.walkertribe.ian.protocol.udp

import io.kotest.assertions.retry
import io.kotest.assertions.retryConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.ipAddressV4
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.positiveInt
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.core.writeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.io.Source
import kotlinx.io.writeShortLe

class ServerDiscoveryRequesterTest : DescribeSpec({
    failfast = true

    describe("ServerDiscoveryRequester") {
        val loopbackAddress = "127.0.0.1"
        val testServers = Arb.list(
            Arb.bind(
                Arb.ipAddressV4(),
                Arb.string(1..10000),
            ) { ip, hostName -> Server(ip, hostName) },
        ).next()

        lateinit var packet: Source
        val discoveredServers = mutableListOf<Server>()
        var onQuitCalled = false

        val listener = object : ServerDiscoveryRequester.Listener {
            override suspend fun onDiscovered(server: Server) {
                discoveredServers.add(server)
            }

            override suspend fun onQuit() {
                onQuitCalled = true
            }
        }

        val retryConfig = retryConfig {
            maxRetry = 10
        }

        it("Throws when initialized with invalid timeout") {
            Arb.long(max = 0L).checkAll {
                shouldThrow<IllegalArgumentException> {
                    ServerDiscoveryRequester(listener = listener, timeoutMs = it)
                }
            }
        }

        val requester = ServerDiscoveryRequester(listener = listener, timeoutMs = 500L)

        val localAddress = InetSocketAddress(loopbackAddress, ServerDiscoveryRequester.PORT)

        SelectorManager(Dispatchers.IO).use { selector ->
            aSocket(selector).udp().bind(localAddress).use { socket ->
                var requesterJob: Job? = null
                lateinit var datagram: Datagram

                it("Datagram was sent through UDP") {
                    retry(retryConfig) {
                        requesterJob?.join()
                        requesterJob = launch { requester.run(loopbackAddress) }

                        datagram = socket.receive()
                        packet = datagram.packet
                        packet.shouldNotBeNull()
                    }
                }

                it("Datagram contains ENQ byte") {
                    packet.exhausted().shouldBeFalse()
                    val enq = packet.readByte()
                    enq shouldBeEqual Server.ENQ
                    packet.exhausted().shouldBeTrue()
                }

                it("Discovered all broadcasting servers") {
                    retry(retryConfig) {
                        discoveredServers.clear()
                        requesterJob?.join()
                        requesterJob = launch { requester.run(loopbackAddress) }

                        datagram = socket.receive()
                        testServers.forEach { (ip, hostName) ->
                            socket.send(
                                Datagram(
                                    packet = buildPacket {
                                        writeByte(Server.ACK)

                                        writeShortLe(ip.length.toShort())
                                        writeText(ip)

                                        writeShortLe(hostName.length.toShort())
                                        writeText(hostName)

                                        writeByte(0)
                                    },
                                    address = datagram.address
                                )
                            )
                        }

                        requesterJob?.join()
                        discoveredServers shouldContainExactly testServers
                    }
                }

                it("Ignores data not beginning with ACK byte") {
                    retry(retryConfig) {
                        discoveredServers.clear()
                        requesterJob?.join()
                        requesterJob = launch { requester.run(loopbackAddress) }

                        datagram = socket.receive()

                        checkAll(
                            Arb.byte().filter { it != Server.ACK },
                            Arb.byteArray(
                                Arb.positiveInt(max = 100),
                                Arb.byte(),
                            ),
                        ) { firstByte, otherBytes ->
                            socket.send(
                                Datagram(
                                    packet = buildPacket {
                                        writeByte(firstByte)
                                        writeFully(otherBytes)
                                    },
                                    address = datagram.address
                                )
                            )
                        }

                        requesterJob?.join()
                        discoveredServers.shouldBeEmpty()
                    }
                }
            }
        }

        it("Quit after timeout") {
            onQuitCalled.shouldBeTrue()
        }
    }
})
