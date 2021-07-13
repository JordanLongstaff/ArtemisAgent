package com.walkertribe.ian.util

import com.walkertribe.ian.protocol.core.CorePacketType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.checkAll

class JamCrcTest : DescribeSpec({
    describe("JamCrc") {
        it("Can compute for any string") {
            checkAll<String> { JamCrc.compute(it) }
        }

        describe("Computes expected hash for packet types") {
            withData(
                nameFn = { (string, hash) -> "'$string' = 0x$hash" },
                CorePacketType.CARRIER_RECORD to "9ad1f23b",
                CorePacketType.COMMS_BUTTON to "ca88f050",
                CorePacketType.COMMS_MESSAGE to "574c4c4b",
                CorePacketType.COMM_TEXT to "d672c35f",
                CorePacketType.CONNECTED to "e548e74a",
                CorePacketType.CONTROL_MESSAGE to "6aadc57f",
                CorePacketType.HEARTBEAT to "f5821226",
                CorePacketType.INCOMING_MESSAGE to "ae88e058",
                CorePacketType.OBJECT_BIT_STREAM to "80803df9",
                CorePacketType.OBJECT_DELETE to "cc5a3e30",
                CorePacketType.OBJECT_TEXT to "ee665279",
                CorePacketType.PLAIN_TEXT_GREETING to "6d04b3da",
                CorePacketType.SIMPLE_EVENT to "f754c8fe",
                CorePacketType.START_GAME to "3de66711",
                CorePacketType.VALUE_INT to "4c821d3c",
            ) { (string, expectedHash) ->
                JamCrc.compute(string) shouldBeEqual expectedHash.toLong(16).toInt()
            }
        }
    }
})
