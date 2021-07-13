package com.walkertribe.ian.iface

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.protocol.ArtemisPacketException
import com.walkertribe.ian.util.Version
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.errors.IOException

class ListenerRegistryTest : DescribeSpec({
    afterSpec {
        TestListener.clear()
    }

    describe("ListenerRegistry") {
        lateinit var registry: ListenerRegistry

        it("Can create") {
            registry = ListenerRegistry()
        }

        it("Starts with no listener functions") {
            registry.listeningFor(ListenerArgument::class).shouldBeEmpty()
        }

        it("Registering an object with no listener functions does nothing") {
            registry.register(object { })
            registry.listeningFor(ListenerArgument::class).shouldBeEmpty()
        }

        it("Registering an object with a listener function registers that function") {
            registry.register(TestListener)
            registry.listeningFor(ListenerArgument::class).size shouldBeEqual 1
        }

        it("Can fire connection events") {
            var expectedCalls = 0
            Arb.choice(
                Arb.bind<ConnectionEvent.Success>(),
                Arb.bind<ConnectionEvent.HeartbeatLost>(),
                Arb.bind<ConnectionEvent.HeartbeatRegained>(),
                Arb.choice(
                    Arb.of(DisconnectCause.LocalDisconnect),
                    Arb.of(DisconnectCause.RemoteDisconnect),
                    Arb.of(DisconnectCause.IOError(IOException())),
                    Arb.bind(
                        Arb.string().orNull(),
                        Arb.enum<Origin>().orNull(),
                        Arb.int(),
                        Arb.byteArray(
                            Arb.nonNegativeInt(max = UShort.MAX_VALUE.toInt()),
                            Arb.byte(),
                        ),
                    ) { str, origin, packetType, payload ->
                        DisconnectCause.PacketParseError(
                            ArtemisPacketException(
                                str,
                                origin,
                                packetType,
                                buildPacket { writeFully(payload) },
                            )
                        )
                    },
                    Arb.bind(
                        Arb.nonNegativeInt(),
                        Arb.nonNegativeInt(),
                        Arb.nonNegativeInt(),
                    ) { major, minor, patch ->
                        DisconnectCause.UnsupportedServerVersion(Version(major, minor, patch))
                    }
                ).map { ConnectionEvent.Disconnect(it) },
            ).checkAll {
                registry.fire(it)
                TestListener.calls<ConnectionEvent>().size shouldBeEqual ++expectedCalls
            }
        }

        it("Can clear") {
            registry.clear()
            registry.listeningFor(ListenerArgument::class).shouldBeEmpty()
        }

        describe("Cannot register") {
            withData(
                TestListener.Invalid.NonPublicClass,
                TestListener.Invalid.NonPublicFunction,
                TestListener.Invalid.NonUnitFunction,
                TestListener.Invalid.NoArguments,
                TestListener.Invalid.MultipleArguments,
                TestListener.Invalid.ParameterType,
            ) {
                shouldThrow<IllegalArgumentException> {
                    registry.register(it)
                }
            }
        }
    }
})
