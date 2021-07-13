package com.walkertribe.ian.protocol

import com.walkertribe.ian.enums.Origin
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.checkAll
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.core.writeFully

class ArtemisPacketExceptionTest : DescribeSpec({
    describe("ArtemisPacketException") {
        val messages = mutableListOf<String>()

        it("Can be constructed from message") {
            checkAll<String> {
                val ex = ArtemisPacketException(it)
                val message = ex.message
                message.shouldNotBeNull()
                message shouldBeEqual it

                messages.add(it)
            }
        }

        it("Can be constructed from another exception") {
            messages.forEach {
                val ex = ArtemisPacketException(RuntimeException(it))
                val message = ex.message
                message.shouldNotBeNull()
                message shouldBeEqual it
            }
        }

        val exceptionDetails =
            mutableListOf<Pair<ArtemisPacketException, Triple<Origin, Int, ByteArray>>>()

        it("Can be constructed with additional details") {
            checkAll(
                Arb.enum<Origin>(),
                Arb.int(),
                Arb.byteArray(Arb.nonNegativeInt(UShort.MAX_VALUE.toInt()), Arb.byte()),
            ) { expectedOrigin, expectedPacketType, expectedPayload ->
                collect(expectedOrigin)

                val ex = ArtemisPacketException(
                    RuntimeException(),
                    expectedOrigin,
                    expectedPacketType,
                    buildPacket { writeFully(expectedPayload) }
                )

                val origin = ex.origin.shouldNotBeNull()
                origin shouldBeEqual expectedOrigin

                ex.packetType shouldBeEqual expectedPacketType

                val payload = ex.payload.shouldNotBeNull()
                val bytes = payload.copy().readBytes()
                bytes.toList() shouldContainExactly expectedPayload.toList()

                exceptionDetails.add(ex to Triple(origin, ex.packetType, bytes))
            }
        }

        it("Can be thrown") {
            shouldThrow<ArtemisPacketException> { throw ArtemisPacketException() }
        }

        describe("Unknown packet") {
            it("Can create") {
                exceptionDetails.forEach { (ex, details) ->
                    val (origin, packetType, payload) = details
                    val packet = ex.toUnknownPacket()
                    packet.shouldBeInstanceOf<RawPacket.Unknown>()
                    packet.origin shouldBeEqual origin
                    packet.type shouldBeEqual packetType
                    packet.payload.toList() shouldContainExactly payload.toList()
                }
            }

            it("Throws with no origin") {
                exceptionDetails.forEach { (exception, _) ->
                    val ex = ArtemisPacketException(
                        RuntimeException(),
                        null,
                        exception.packetType,
                        exception.payload,
                    )

                    shouldThrow<IllegalStateException> { ex.toUnknownPacket() }
                }
            }

            it("Throws with null payload") {
                exceptionDetails.forEach { (_, details) ->
                    val ex = ArtemisPacketException(
                        RuntimeException(),
                        details.first,
                        details.second,
                        null,
                    )

                    shouldThrow<IllegalStateException> { ex.toUnknownPacket() }
                }
            }
        }
    }
})
