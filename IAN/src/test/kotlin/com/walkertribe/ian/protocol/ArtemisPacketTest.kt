package com.walkertribe.ian.protocol

import com.walkertribe.ian.iface.PacketWriter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual

class ArtemisPacketTest : DescribeSpec({
    class InvalidPacket : BaseArtemisPacket() {
        override fun writePayload(writer: PacketWriter) {
            writer.unsupported()
        }
    }

    describe("ArtemisPacket") {
        it("Must have @Packet annotation") {
            shouldThrow<ArtemisPacketException> { InvalidPacket() }
        }

        describe("Companion") {
            it("HEADER = 0xDEADBEEF") {
                ArtemisPacket.HEADER shouldBeEqual 0xDEADBEEF.toInt()
            }

            it("PREAMBLE_SIZE = 24") {
                ArtemisPacket.PREAMBLE_SIZE shouldBeEqual 24
            }
        }
    }
})
