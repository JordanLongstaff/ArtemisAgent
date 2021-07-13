package com.walkertribe.ian.iface

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.ArtemisPacketException
import com.walkertribe.ian.protocol.CompositeProtocol
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.property.Arb
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.checkAll
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
import io.mockk.unmockkAll
import io.mockk.verify

class PacketReaderTest : DescribeSpec({
    val readChannel = mockk<ByteReadChannel>()
    val listenerRegistry = mockk<ListenerRegistry>()

    afterTest { clearAllMocks() }
    afterSpec { unmockkAll() }

    val packetReader = PacketReader(
        readChannel,
        CompositeProtocol(),
        listenerRegistry,
    )

    val emptyBytePacket = ByteReadPacket(byteArrayOf())

    describe("PacketReader") {
        it("Skips unknown packets") {
            val expectedCalls = 7 * PropertyTesting.defaultIterationCount

            checkAll(
                Arb.nonNegativeInt(max = Int.MAX_VALUE - ArtemisPacket.PREAMBLE_SIZE),
                Arb.int(),
            ) { payloadSize, packetType ->
                coEvery { readChannel.readInt() } returnsMany listOf(
                    ArtemisPacket.HEADER,
                    payloadSize + ArtemisPacket.PREAMBLE_SIZE,
                    Origin.SERVER.toInt(),
                    0,
                    payloadSize + Int.SIZE_BYTES,
                    packetType,
                ).map(Int::reverseByteOrder) andThenThrows EOFException()
                coEvery { readChannel.readPacket(payloadSize) } returns emptyBytePacket

                shouldThrow<EOFException> { packetReader.readPacket() }

                coVerify { readChannel.readPacket(payloadSize) }
            }

            coVerify(exactly = expectedCalls) { readChannel.readIntLittleEndian() }
            verify { listenerRegistry wasNot called }

            confirmVerified(readChannel, listenerRegistry)
        }

        describe("Parse error") {
            it("Invalid header") {
                Arb.int().filter { it != ArtemisPacket.HEADER }.checkAll {
                    coEvery { readChannel.readInt() } returns it.reverseByteOrder()

                    shouldThrow<ArtemisPacketException> { packetReader.readPacket() }
                }

                coVerify(exactly = PropertyTesting.defaultIterationCount) {
                    readChannel.readIntLittleEndian()
                }
                verify { listenerRegistry wasNot called }

                confirmVerified(readChannel, listenerRegistry)
            }

            it("Invalid length") {
                Arb.int(max = ArtemisPacket.PREAMBLE_SIZE - 1).checkAll {
                    coEvery { readChannel.readInt() } returnsMany listOf(
                        ArtemisPacket.HEADER,
                        it,
                    ).map(Int::reverseByteOrder)

                    shouldThrow<ArtemisPacketException> { packetReader.readPacket() }
                }

                coVerify(exactly = PropertyTesting.defaultIterationCount * 2) {
                    readChannel.readIntLittleEndian()
                }
                verify { listenerRegistry wasNot called }

                confirmVerified(readChannel, listenerRegistry)
            }

            it("Unknown origin") {
                val iterations = PropertyTesting.defaultIterationCount

                Arb.int().filter { it !in 1..2 }.checkAll {
                    coEvery { readChannel.readInt() } returnsMany listOf(
                        ArtemisPacket.HEADER,
                        ArtemisPacket.PREAMBLE_SIZE,
                        it,
                        0,
                        Int.SIZE_BYTES,
                        0,
                    ).map(Int::reverseByteOrder)
                    coEvery { readChannel.readPacket(0) } returns emptyBytePacket

                    shouldThrow<ArtemisPacketException> { packetReader.readPacket() }
                }

                coVerify(exactly = iterations * 6) { readChannel.readIntLittleEndian() }
                coVerify(exactly = iterations) { readChannel.readPacket(0) }
                verify { listenerRegistry wasNot called }

                confirmVerified(readChannel, listenerRegistry)
            }

            it("Cannot read client packets") {
                coEvery { readChannel.readInt() } returnsMany listOf(
                    ArtemisPacket.HEADER,
                    ArtemisPacket.PREAMBLE_SIZE,
                    Origin.CLIENT.toInt(),
                    0,
                    Int.SIZE_BYTES,
                    0,
                ).map(Int::reverseByteOrder)
                coEvery { readChannel.readPacket(0) } returns emptyBytePacket

                shouldThrow<ArtemisPacketException> { packetReader.readPacket() }

                coVerify { readChannel.readIntLittleEndian() }
                coVerify { readChannel.readPacket(0) }
                verify { listenerRegistry wasNot called }

                confirmVerified(readChannel, listenerRegistry)
            }

            it("Non-empty padding") {
                val iterations = PropertyTesting.defaultIterationCount

                Arb.int().filter { it != 0 }.checkAll {
                    coEvery { readChannel.readInt() } returnsMany listOf(
                        ArtemisPacket.HEADER,
                        ArtemisPacket.PREAMBLE_SIZE,
                        Origin.SERVER.toInt(),
                        it,
                        Int.SIZE_BYTES,
                        0,
                    ).map(Int::reverseByteOrder)
                    coEvery { readChannel.readPacket(0) } returns emptyBytePacket

                    shouldThrow<ArtemisPacketException> { packetReader.readPacket() }
                }

                coVerify(exactly = iterations * 6) { readChannel.readIntLittleEndian() }
                coVerify(exactly = iterations) { readChannel.readPacket(0) }
                verify { listenerRegistry wasNot called }

                confirmVerified(readChannel, listenerRegistry)
            }

            it("Packet length discrepancy") {
                val iterations = PropertyTesting.defaultIterationCount

                Arb.int().flatMap { payloadSize ->
                    Arb.nonNegativeInt(max = Int.MAX_VALUE - ArtemisPacket.PREAMBLE_SIZE).filter {
                        it != payloadSize - Int.SIZE_BYTES
                    }.map { Pair(it, payloadSize) }
                }.checkAll { (payloadSize1, payloadSize2) ->
                    coEvery { readChannel.readInt() } returnsMany listOf(
                        ArtemisPacket.HEADER,
                        payloadSize1 + ArtemisPacket.PREAMBLE_SIZE,
                        Origin.SERVER.toInt(),
                        0,
                        payloadSize2,
                        0,
                    ).map(Int::reverseByteOrder)
                    coEvery { readChannel.readPacket(payloadSize1) } returns emptyBytePacket

                    shouldThrow<ArtemisPacketException> { packetReader.readPacket() }

                    coVerify { readChannel.readPacket(payloadSize1) }
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
