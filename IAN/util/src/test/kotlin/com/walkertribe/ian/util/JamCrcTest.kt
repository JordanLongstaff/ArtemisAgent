package com.walkertribe.ian.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.checkAll

class JamCrcTest :
    DescribeSpec({
        describe("JamCrc") {
            it("Can compute for any string") { checkAll<String> { JamCrc.compute(it) } }

            describe("Computes expected hash for packet types") {
                withData(
                    nameFn = { (string, hash) -> "'$string' = 0x$hash" },
                    "carrierRecord" to "9ad1f23b",
                    "commsButton" to "ca88f050",
                    "commsMessage" to "574c4c4b",
                    "commText" to "d672c35f",
                    "connected" to "e548e74a",
                    "controlMessage" to "6aadc57f",
                    "heartbeat" to "f5821226",
                    "incomingMessage" to "ae88e058",
                    "objectBitStream" to "80803df9",
                    "objectDelete" to "cc5a3e30",
                    "objectText" to "ee665279",
                    "plainTextGreeting" to "6d04b3da",
                    "simpleEvent" to "f754c8fe",
                    "startGame" to "3de66711",
                    "valueInt" to "4c821d3c",
                ) { (string, expectedHash) ->
                    JamCrc.compute(string) shouldBeEqual expectedHash.toLong(16).toInt()
                }
            }
        }
    })
