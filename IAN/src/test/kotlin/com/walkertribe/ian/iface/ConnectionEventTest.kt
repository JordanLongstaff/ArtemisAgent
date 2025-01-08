package com.walkertribe.ian.iface

import com.walkertribe.ian.protocol.PacketException
import com.walkertribe.ian.util.Version
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.clearMocks
import io.mockk.mockk
import kotlin.reflect.KClass
import kotlinx.datetime.Clock
import kotlinx.io.IOException

class ConnectionEventTest :
    DescribeSpec({
        val connectionEvents = mutableListOf<ConnectionEvent>()
        val listenerModule =
            object : ConnectionEventListenerModule {
                override val acceptedTypes: Set<KClass<out ListenerArgument>> =
                    setOf(ConnectionEvent::class)

                override val connectionEventListeners: List<ListenerFunction<out ConnectionEvent>> =
                    listOf(ListenerFunction(ConnectionEvent::class, connectionEvents::add))

                override fun onPacket(arg: ListenerArgument) {
                    assert(false) { "onPacket should not be called" }
                }

                override fun onArtemisObject(arg: ListenerArgument) {
                    assert(false) { "onArtemisObject should not be called" }
                }
            }

        describe("ConnectionEvent") {
            beforeContainer { connectionEvents.clear() }

            describe("Success") {
                val events = mutableListOf<ConnectionEvent.Success>()

                it("Constructor") {
                    Arb.string().checkAll {
                        val startTime = Clock.System.now().toEpochMilliseconds()
                        val event = ConnectionEvent.Success(it)
                        val endTime = Clock.System.now().toEpochMilliseconds()

                        event.message shouldBeEqual it
                        event.timestamp shouldBeIn startTime..endTime

                        events.add(event)
                    }
                }

                it("Can offer to listener modules") {
                    events.forEach { it.offerTo(listenerModule) }
                    connectionEvents shouldContainExactly events
                }
            }

            describe("Disconnect") {
                arrayOf(
                        DisconnectCause.LocalDisconnect,
                        DisconnectCause.RemoteDisconnect,
                        DisconnectCause.IOError(IOException()),
                        DisconnectCause.PacketParseError(mockk<PacketException>()),
                        DisconnectCause.UnsupportedVersion(Version.LATEST),
                    )
                    .forEach { cause ->
                        describe(cause::class.simpleName ?: cause.toString()) {
                            lateinit var event: ConnectionEvent.Disconnect

                            it("Constructor") {
                                val startTime = Clock.System.now().toEpochMilliseconds()
                                event = ConnectionEvent.Disconnect(cause)
                                val endTime = Clock.System.now().toEpochMilliseconds()

                                event.cause shouldBeEqual cause
                                event.timestamp shouldBeIn startTime..endTime
                            }

                            it("Can offer to listener modules") {
                                event.offerTo(listenerModule)
                                connectionEvents.size shouldBeEqual 1
                                connectionEvents shouldContain event
                            }
                        }
                    }
            }

            arrayOf(ConnectionEvent.HeartbeatLost, ConnectionEvent.HeartbeatRegained).forEach {
                event ->
                describe(event.toString()) {
                    it("Can offer to listener modules") {
                        event.offerTo(listenerModule)
                        connectionEvents.size shouldBeEqual 1
                        connectionEvents shouldContain event
                    }
                }
            }
        }

        describe("ConnectionEventListenerModule") {
            connectionEvents.clear()
            val mockArgument = mockk<ListenerArgument>()

            it("Does not accept arguments that are not connection events") {
                listenerModule.onConnectionEvent(mockArgument)
                connectionEvents.shouldBeEmpty()
            }

            clearMocks(mockArgument)
        }
    })
