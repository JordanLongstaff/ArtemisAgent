package com.walkertribe.ian.iface

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.checkAll
import io.kotest.property.forAll
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.bits.reverseByteOrder
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.errors.EOFException
import io.ktor.utils.io.readIntLittleEndian
import io.mockk.called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class PacketReaderTest : DescribeSpec({
    val readChannel = mockk<ByteReadChannel>()
    val listenerRegistry = mockk<ListenerRegistry>()
    val emptyBytePacket = ByteReadPacket(byteArrayOf())

    afterTest {
        clearAllMocks()
        emptyBytePacket.close()
    }

    val packetReader = PacketReader(readChannel, listenerRegistry)

    describe("PacketReader") {
        it("Skips unknown packets") {
            var iterations = 0

            checkAll(
                Arb.nonNegativeInt(max = Int.MAX_VALUE - Packet.PREAMBLE_SIZE),
                Arb.int(),
            ) { payloadSize, packetType ->
                coEvery { readChannel.readInt() } returnsMany listOf(
                    Packet.HEADER,
                    payloadSize + Packet.PREAMBLE_SIZE,
                    Origin.SERVER.value,
                    0,
                    payloadSize + Int.SIZE_BYTES,
                    packetType,
                ).map(Int::reverseByteOrder) andThenThrows EOFException()
                coEvery { readChannel.readPacket(payloadSize) } returns emptyBytePacket

                shouldThrow<EOFException> { packetReader.readPacket() }

                coVerify { readChannel.readPacket(any()) }

                iterations++
            }

            coVerify(exactly = iterations * 7) { readChannel.readIntLittleEndian() }
            verify { listenerRegistry wasNot called }

            confirmVerified(readChannel, listenerRegistry)
        }

        it("No bits") {
            Arb.int().forAll { !packetReader.has(it) }
        }

        describe("Parse error") {
            it("Invalid header") {
                var iterations = 0

                Arb.int().filter { it != Packet.HEADER }.checkAll {
                    coEvery { readChannel.readInt() } returns it.reverseByteOrder()

                    shouldThrow<PacketException> { packetReader.readPacket() }

                    iterations++
                }

                coVerify(exactly = iterations) { readChannel.readIntLittleEndian() }
                verify { listenerRegistry wasNot called }

                confirmVerified(readChannel, listenerRegistry)
            }

            it("Invalid length") {
                var iterations = 0

                Arb.int(max = Packet.PREAMBLE_SIZE - 1).checkAll {
                    coEvery { readChannel.readInt() } returnsMany listOf(
                        Packet.HEADER,
                        it,
                    ).map(Int::reverseByteOrder)

                    shouldThrow<PacketException> { packetReader.readPacket() }

                    iterations++
                }

                coVerify(exactly = iterations * 2) { readChannel.readIntLittleEndian() }
                verify { listenerRegistry wasNot called }

                confirmVerified(readChannel, listenerRegistry)
            }

            it("Unknown origin") {
                var iterations = 0

                Arb.int().filter { it !in 1..2 }.checkAll {
                    coEvery { readChannel.readInt() } returnsMany listOf(
                        Packet.HEADER,
                        Packet.PREAMBLE_SIZE,
                        it,
                        0,
                        Int.SIZE_BYTES,
                        0,
                    ).map(Int::reverseByteOrder)
                    coEvery { readChannel.readPacket(0) } returns emptyBytePacket

                    shouldThrow<PacketException> { packetReader.readPacket() }

                    iterations++
                }

                coVerify(exactly = iterations * 6) { readChannel.readIntLittleEndian() }
                coVerify(exactly = iterations) { readChannel.readPacket(any()) }
                verify { listenerRegistry wasNot called }

                confirmVerified(readChannel, listenerRegistry)
            }

            it("Cannot read client packets") {
                coEvery { readChannel.readInt() } returnsMany listOf(
                    Packet.HEADER,
                    Packet.PREAMBLE_SIZE,
                    Origin.CLIENT.value,
                    0,
                    Int.SIZE_BYTES,
                    0,
                ).map(Int::reverseByteOrder)
                coEvery { readChannel.readPacket(0) } returns emptyBytePacket

                shouldThrow<PacketException> { packetReader.readPacket() }

                coVerify { readChannel.readIntLittleEndian() }
                coVerify { readChannel.readPacket(any()) }
                verify { listenerRegistry wasNot called }

                confirmVerified(readChannel, listenerRegistry)
            }

            it("Non-empty padding") {
                var iterations = 0

                Arb.int().filter { it != 0 }.checkAll {
                    coEvery { readChannel.readInt() } returnsMany listOf(
                        Packet.HEADER,
                        Packet.PREAMBLE_SIZE,
                        Origin.SERVER.value,
                        it,
                        Int.SIZE_BYTES,
                        0,
                    ).map(Int::reverseByteOrder)
                    coEvery { readChannel.readPacket(0) } returns emptyBytePacket

                    shouldThrow<PacketException> { packetReader.readPacket() }

                    iterations++
                }

                coVerify(exactly = iterations * 6) { readChannel.readIntLittleEndian() }
                coVerify(exactly = iterations) { readChannel.readPacket(any()) }
                verify { listenerRegistry wasNot called }

                confirmVerified(readChannel, listenerRegistry)
            }

            it("Packet length discrepancy") {
                var iterations = 0

                Arb.int().flatMap { payloadSize ->
                    Arb.nonNegativeInt(max = Int.MAX_VALUE - Packet.PREAMBLE_SIZE).filter {
                        it != payloadSize - Int.SIZE_BYTES
                    }.map { Pair(it, payloadSize) }
                }.checkAll { (payloadSize1, payloadSize2) ->
                    coEvery { readChannel.readInt() } returnsMany listOf(
                        Packet.HEADER,
                        payloadSize1 + Packet.PREAMBLE_SIZE,
                        Origin.SERVER.value,
                        0,
                        payloadSize2,
                        0,
                    ).map(Int::reverseByteOrder)
                    coEvery { readChannel.readPacket(payloadSize1) } returns emptyBytePacket

                    shouldThrow<PacketException> { packetReader.readPacket() }

                    coVerify { readChannel.readPacket(any()) }

                    iterations++
                }

                coVerify(exactly = iterations * 6) { readChannel.readIntLittleEndian() }
                verify { listenerRegistry wasNot called }

                confirmVerified(readChannel, listenerRegistry)
            }
        }

        it("Can close") {
            every { readChannel.cancel(any()) } returns true

            packetReader.close()

            coVerify { readChannel.cancel(any()) }

            confirmVerified(readChannel)
        }
    }
})
