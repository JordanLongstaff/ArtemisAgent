package com.walkertribe.ian.protocol

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.protocol.core.CorePacketType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

class PacketHashTest : DescribeSpec({
    describe("Packet") {
        describe("Named type") {
            withData(
                nameFn = { (type, hash) -> "'$type' = 0x$hash" },
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
            ) { (type, expectedHash) ->
                checkAll(Arb.enum<Origin>(), Arb.byte()) { origin, subtype ->
                    Packet(
                        origin = origin,
                        type = type,
                        subtype = subtype,
                    ).getHash() shouldBeEqual expectedHash.toLong(16).toInt()
                }
            }
        }

        it("Hashed type") {
            checkAll(
                Arb.enum<Origin>(),
                Arb.int().filter { it != 0 },
                Arb.byte(),
            ) { origin, hash, subtype ->
                Packet(
                    origin = origin,
                    hash = hash,
                    subtype = subtype,
                ).getHash() shouldBeEqual hash
            }
        }

        it("Throws with empty type and no hash") {
            checkAll(Arb.enum<Origin>(), Arb.byte()) { origin, subtype ->
                shouldThrow<ArtemisPacketException> {
                    Packet(origin = origin, subtype = subtype).getHash()
                }
            }
        }
    }
})
