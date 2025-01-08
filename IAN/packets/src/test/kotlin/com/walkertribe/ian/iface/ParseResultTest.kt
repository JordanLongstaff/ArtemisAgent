package com.walkertribe.ian.iface

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketException
import com.walkertribe.ian.protocol.PacketTestListenerModule
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.checkAll
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class ParseResultTest :
    DescribeSpec({
        val mockListener = mockk<ListenerModule>()

        afterSpec { clearAllMocks() }

        describe("ParseResult") {
            describe("Fail") {
                val fails = mutableListOf<ParseResult.Fail>()

                it("Constructor") {
                    checkAll(
                        Arb.int(),
                        Arb.byteArray(Arb.nonNegativeInt(UShort.MAX_VALUE.toInt()), Arb.byte()),
                    ) { packetType, payload ->
                        val exception = PacketException(RuntimeException(), packetType, payload)
                        val fail = ParseResult.Fail(exception)
                        fail.exception shouldBeEqual exception
                        shouldThrow<PacketException> { throw fail.exception }
                        fails.add(fail)
                    }
                }

                it("Cannot add listeners") {
                    fails.forEach {
                        shouldThrow<IllegalStateException> {
                            it.addListeners(listOf(mockListener, PacketTestListenerModule))
                        }
                    }
                }
            }

            describe("Skip") {
                it("Cannot add listeners") {
                    shouldThrow<IllegalStateException> {
                        ParseResult.Skip.addListeners(
                            listOf(mockListener, PacketTestListenerModule)
                        )
                    }
                }
            }

            describe("Interesting") {
                it("With listeners: true") {
                    val result = ParseResult.Processing()
                    result.addListeners(listOf(mockListener))
                    result.isInteresting.shouldBeTrue()
                }

                it("Without listeners: false") {
                    val result = ParseResult.Fail(PacketException())
                    result.isInteresting.shouldBeFalse()
                }

                it("Copied from previous result") {
                    val previous = ParseResult.Processing()
                    previous.addListeners(listOf(mockListener))
                    val success = ParseResult.Success(mockk(), previous)
                    success.isInteresting.shouldBeTrue()
                }

                it("Can fire listeners") {
                    val mockPacket =
                        mockk<Packet.Server> { every { offerTo(any()) } answers { callOriginal() } }
                    val success = ParseResult.Success(mockPacket, ParseResult.Skip)
                    success.addListeners(listOf(PacketTestListenerModule))
                    success.fireListeners()
                    PacketTestListenerModule.packets shouldContain mockPacket
                }

                PacketTestListenerModule.packets.clear()
            }
        }
    })
