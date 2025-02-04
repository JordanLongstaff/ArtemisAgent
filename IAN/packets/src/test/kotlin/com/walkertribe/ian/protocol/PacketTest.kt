package com.walkertribe.ian.protocol

import com.walkertribe.ian.iface.ListenerArgument
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.clearMocks
import io.mockk.mockk

class PacketTest :
    DescribeSpec({
        describe("Packet") {
            it("HEADER = 0xDEADBEEF") { Packet.HEADER shouldBeEqual 0xDEADBEEF.toInt() }

            it("PREAMBLE_SIZE = 24") { Packet.PREAMBLE_SIZE shouldBeEqual 24 }
        }

        describe("PacketListenerModule") {
            val mockArgument = mockk<ListenerArgument>()

            it("Does not accept arguments that are not packets") {
                PacketTestListenerModule.onPacket(mockArgument)
                PacketTestListenerModule.packets.shouldBeEmpty()
            }

            clearMocks(mockArgument)
        }
    })
