package com.walkertribe.ian.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.booleanArray
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.negativeInt
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.arbitrary.positiveInt
import io.kotest.property.checkAll
import io.ktor.utils.io.core.buildPacket
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalKotest::class)
class BitFieldTest :
    DescribeSpec({
        describe("BitField").config(timeout = 12.minutes) {
            val arbBitField =
                Arb.booleanArray(Arb.nonNegativeInt(UShort.MAX_VALUE.toInt()), Arb.boolean())

            describe("Primary constructor") {
                withData(
                    nameFn = { "Works with $it extra bit${if (it == 1) "" else "s"}" },
                    0 until Byte.SIZE_BITS,
                ) { extraBits ->
                    Arb.positiveInt(UShort.MAX_VALUE.toInt()).checkAll { byteCount ->
                        val bitCount = (byteCount - 1) * Byte.SIZE_BITS + extraBits

                        val bitField = BitField(bitCount)
                        bitField.byteCount shouldBeEqual byteCount
                        for (i in 0 until bitCount) {
                            bitField[i].shouldBeFalse()
                        }
                    }
                }

                it("Can manually set bits") {
                    arbBitField.checkAll { bits ->
                        val bitField = BitField(bits.size)

                        for (i in bits.indices) {
                            bitField[i] = bits[i]
                        }

                        for (i in bits.indices) {
                            bitField[i] shouldBeEqual bits[i]
                        }
                    }
                }

                it("Throws with negative size") {
                    Arb.negativeInt().checkAll {
                        shouldThrow<IllegalArgumentException> { BitField(it) }
                    }
                }

                it("Cannot set bits at negative indices") {
                    checkAll(Arb.positiveInt(UShort.MAX_VALUE.toInt()), Arb.negativeInt()) {
                        bitCount,
                        index ->
                        val bitField = BitField(bitCount)
                        bitField[index] = true
                        bitField[index].shouldBeFalse()
                    }
                }

                it("Cannot set bits at indices beyond bit count") {
                    checkAll(
                        Arb.nonNegativeInt(UShort.MAX_VALUE.toInt()).flatMap { bitCount ->
                            Arb.int(min = bitCount).map { bitCount to it }
                        }
                    ) { (bitCount, index) ->
                        val bitField = BitField(bitCount)
                        bitField[index] = true
                        bitField[index].shouldBeFalse()
                    }
                }
            }

            it("Read from packet") {
                arbBitField.checkAll { bits ->
                    val byteCount = countBytes(bits.size)
                    val paddedBits = bits.copyOf(byteCount * Byte.SIZE_BITS)

                    val packet = buildPacket {
                        for (eightBits in paddedBits.toList().chunked(Byte.SIZE_BITS)) {
                            val byte =
                                eightBits
                                    .foldRight(0) { bit, acc -> (acc shl 1) or if (bit) 1 else 0 }
                                    .toByte()
                            writeByte(byte)
                        }
                    }
                    val bitField = packet.readBitField(bits.size)

                    for (i in bits.indices) {
                        bitField[i] shouldBeEqual bits[i]
                    }
                }
            }
        }
    })
