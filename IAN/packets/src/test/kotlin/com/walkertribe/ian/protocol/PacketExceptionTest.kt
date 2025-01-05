package com.walkertribe.ian.protocol

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeZero
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.checkAll

class PacketExceptionTest :
    DescribeSpec({
        describe("PacketException") {
            val messages = mutableListOf<String>()

            it("Can be constructed empty") {
                val ex = PacketException()
                ex.cause.shouldBeNull()
                ex.message.shouldBeNull()
                ex.packetType.shouldBeZero()
                ex.payload.shouldBeNull()
            }

            it("Can be constructed from message") {
                checkAll<String> {
                    val ex = PacketException(it)
                    ex.message.shouldNotBeNull() shouldBeEqual it
                    ex.cause.shouldBeNull()
                    ex.packetType.shouldBeZero()
                    ex.payload.shouldBeNull()

                    messages.add(it)
                }
            }

            it("Can be constructed from another exception") {
                (messages + null).forEach {
                    val ex = PacketException(RuntimeException(it))
                    ex.message.shouldNotBeNull().shouldBeEqual(it ?: "RuntimeException")
                    ex.cause.shouldNotBeNull().shouldBeInstanceOf<RuntimeException>()
                    ex.packetType.shouldBeZero()
                    ex.payload.shouldBeNull()
                }
            }

            val exceptionDetails = mutableListOf<Triple<PacketException, Int, ByteArray>>()

            it("Can be constructed with additional details") {
                checkAll(
                    Arb.int(),
                    Arb.byteArray(Arb.nonNegativeInt(UShort.MAX_VALUE.toInt()), Arb.byte()),
                ) { expectedPacketType, expectedPayload ->
                    val ex =
                        PacketException(RuntimeException(), expectedPacketType, expectedPayload)

                    ex.packetType shouldBeEqual expectedPacketType

                    val payload = ex.payload.shouldNotBeNull()
                    payload.toList() shouldContainExactly expectedPayload.toList()

                    exceptionDetails.add(Triple(ex, ex.packetType, payload))
                }
            }

            it("Can be thrown") {
                exceptionDetails.forEach { shouldThrow<PacketException> { throw it.first } }
            }
        }
    })
