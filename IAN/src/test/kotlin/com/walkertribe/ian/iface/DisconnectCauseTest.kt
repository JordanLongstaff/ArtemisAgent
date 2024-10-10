package com.walkertribe.ian.iface

import com.walkertribe.ian.protocol.PacketException
import com.walkertribe.ian.util.version
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.io.IOException

class DisconnectCauseTest : DescribeSpec({
    describe("DisconnectCause") {
        it("IOError") {
            checkAll<String?> { message ->
                val ex = IOException(message)
                DisconnectCause.IOError(ex).exception shouldBeEqual ex
            }
        }

        it("PacketParseError") {
            checkAll(
                Arb.string().orNull(),
                Arb.int(),
                Arb.byteArray(
                    Arb.nonNegativeInt(max = UShort.MAX_VALUE.toInt()),
                    Arb.byte(),
                ),
            ) { str, packetType, payload ->
                val ex = PacketException(str, packetType, payload)
                DisconnectCause.PacketParseError(ex).exception shouldBeEqual ex
            }
        }

        it("UnsupportedVersion") {
            Arb.version().checkAll { version ->
                DisconnectCause.UnsupportedVersion(version).version shouldBeEqual version
            }
        }

        it("UnknownError") {
            checkAll<String?> { message ->
                val ex = RuntimeException(message)
                DisconnectCause.UnknownError(ex).throwable shouldBeEqual ex
            }
        }
    }
})
