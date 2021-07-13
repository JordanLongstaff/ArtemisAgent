package com.walkertribe.ian.protocol

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketWriter
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.checkAll
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.reflect.full.primaryConstructor

class RawPacketTest : DescribeSpec({
    afterSpec {
        clearAllMocks()
        unmockkAll()
    }

    describe("RawPacket") {
        arrayOf(
            RawPacket.Unknown::class,
            RawPacket.Unparsed::class,
        ).forEach { packetClass ->
            describe(packetClass.java.simpleName) {
                val packets = mutableListOf<RawPacket>()

                it("Can construct") {
                    val constructor = packetClass.primaryConstructor.shouldNotBeNull()

                    checkAll(
                        Arb.enum<Origin>(),
                        Arb.int(),
                        Arb.byteArray(Arb.nonNegativeInt(UShort.MAX_VALUE.toInt()), Arb.byte()),
                    ) { origin, packetType, payload ->
                        packets.add(constructor.call(origin, packetType, payload))
                    }
                }

                it("Cannot write to PacketWriter") {
                    val writer = mockk<PacketWriter>(relaxed = true)

                    packets.forEach { it.writeTo(writer) }

                    verify(exactly = packets.size) { writer.unsupported() }

                    confirmVerified(writer)
                }
            }
        }
    }
})
