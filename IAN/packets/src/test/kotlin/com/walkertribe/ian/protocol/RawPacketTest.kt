package com.walkertribe.ian.protocol

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.checkAll

class RawPacketTest :
    DescribeSpec({
        describe("RawPacket") {
            it("Constructor") {
                checkAll(
                    Arb.int(),
                    Arb.byteArray(Arb.nonNegativeInt(UShort.MAX_VALUE.toInt()), Arb.byte()),
                ) { packetType, payload ->
                    Packet.Raw(packetType, payload)
                }
            }
        }
    })
