package com.walkertribe.ian.iface

import com.walkertribe.ian.enums.AudioCommand
import com.walkertribe.ian.enums.CommsRecipientType
import com.walkertribe.ian.enums.EnemyMessage
import com.walkertribe.ian.enums.GameType
import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.protocol.ArtemisPacket
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.enum
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.bits.reverseByteOrder
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readIntLittleEndian
import io.mockk.called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify

class PacketWriterTest : DescribeSpec({
    val sendChannel = mockk<ByteWriteChannel>()
    val packetWriter = PacketWriter(sendChannel)

    afterTest { clearAllMocks() }
    afterSpec { unmockkAll() }

    describe("PacketWriter") {
        it("Writes packet correctly") {
            val ints = mutableListOf<Int>()
            val payloadSlot = slot<ByteReadPacket>()
            val extraSize = Int.SIZE_BYTES * 3

            val iterations = PropertyTesting.defaultIterationCount
            val expectedInts = iterations * 5

            coEvery { sendChannel.writeInt(capture(ints)) } just runs
            coEvery { sendChannel.writePacket(capture(payloadSlot)) } just runs
            every { sendChannel.flush() } just runs

            checkAll(
                iterations = iterations,
                Arb.int(),
                Arb.enum<GameType>(),
                Arb.float(),
                Arb.int(),
            ) { int, gameType, float, packetType ->
                packetWriter.start(packetType)
                    .writeInt(int)
                    .writeEnumAsInt(gameType)
                    .writeFloat(float)
                    .flush()

                ints shouldContainExactly listOf(
                    ArtemisPacket.HEADER,
                    ArtemisPacket.PREAMBLE_SIZE + extraSize,
                    Origin.CLIENT.toInt(),
                    0,
                    Int.SIZE_BYTES + extraSize,
                ).map(Int::reverseByteOrder)

                val packet = payloadSlot.captured

                packet.readIntLittleEndian() shouldBeEqual packetType
                packet.readIntLittleEndian() shouldBeEqual int
                packet.readIntLittleEndian() shouldBeEqual gameType.ordinal
                packet.readIntLittleEndian() shouldBeEqual float.toRawBits()

                ints.clear()
                packet.close()
            }

            coVerify(exactly = expectedInts) { sendChannel.writeInt(any()) }
            coVerify(exactly = iterations) { sendChannel.writePacket(any()) }
            verify(exactly = iterations) { sendChannel.flush() }

            confirmVerified(sendChannel)
        }

        describe("Throws when attempting to write before packet start") {
            it("Integer") {
                Arb.int().checkAll {
                    shouldThrow<IllegalStateException> { packetWriter.writeInt(it) }
                }

                verify { sendChannel wasNot called }

                confirmVerified(sendChannel)
            }

            describe("Enum") {
                it("Audio command") {
                    Exhaustive.enum<AudioCommand>().checkAll {
                        shouldThrow<IllegalStateException> { packetWriter.writeEnumAsInt(it) }
                    }

                    verify { sendChannel wasNot called }

                    confirmVerified(sendChannel)
                }

                it("Comms recipient type") {
                    Exhaustive.enum<CommsRecipientType>().checkAll {
                        shouldThrow<IllegalStateException> { packetWriter.writeEnumAsInt(it) }
                    }

                    verify { sendChannel wasNot called }

                    confirmVerified(sendChannel)
                }

                it("Enemy message") {
                    Exhaustive.enum<EnemyMessage>().checkAll {
                        shouldThrow<IllegalStateException> { packetWriter.writeEnumAsInt(it) }
                    }

                    verify { sendChannel wasNot called }

                    confirmVerified(sendChannel)
                }
            }

            it("Float") {
                Arb.float().checkAll {
                    shouldThrow<IllegalStateException> { packetWriter.writeFloat(it) }
                }

                verify { sendChannel wasNot called }

                confirmVerified(sendChannel)
            }
        }

        it("Throws when writing unsupported write packets") {
            shouldThrow<UnsupportedOperationException> { packetWriter.unsupported() }

            verify { sendChannel wasNot called }

            confirmVerified(sendChannel)
        }

        it("Throws when started twice") {
            checkAll(Arb.int(), Arb.int()) { type1, type2 ->
                shouldThrow<IllegalStateException> {
                    packetWriter.start(type1)
                    packetWriter.start(type2)
                }
            }

            verify { sendChannel wasNot called }

            confirmVerified(sendChannel)

            coEvery { sendChannel.writeInt(any()) } just runs
            coEvery { sendChannel.writePacket(any()) } just runs
            every { sendChannel.flush() } just runs
            packetWriter.flush()
        }

        it("Can close") {
            coEvery { sendChannel.close(any()) } returns true

            packetWriter.close()

            coVerify { sendChannel.close(any()) }

            confirmVerified(sendChannel)
        }
    }
})
