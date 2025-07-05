package com.walkertribe.ian.util

import kotlinx.io.Source
import kotlinx.io.readByteArray

/**
 * Provides easy reading and writing of bits in a bit field. The bytes are little-endian, so in the
 * event that the final byte is not completely utilized, it will be the most significant bits that
 * are left unused. The bits in a bit field are commonly represented as enums in IAN, but [BitField]
 * does not require this.
 *
 * Note that Artemis will never fully utilize the entire bit field. If the number of bits actually
 * used is divisible by eight, Artemis will add another (unused) byte to the end of the field. So a
 * field with eight bits will be two bytes wide instead of one like you would expect, and the second
 * byte will always be `0x00`.
 *
 * @author rjwut
 */
class BitField(private val bitCount: Int) {
    private val bytes: ByteArray =
        ByteArray(
            countBytes(
                bitCount.also {
                    require(it >= 0) { "Bit field cannot have a negative number of bits" }
                }
            )
        )

    /** Returns the number of bytes in this BitField. */
    val byteCount: Int by lazy { bytes.size }

    /**
     * Creates a BitField large enough to accommodate the enumerated bits, and stores the indicated
     * bytes in it.
     */
    internal constructor(bitCount: Int, packet: Source) : this(bitCount) {
        packet.readByteArray(byteCount).copyInto(this.bytes)
    }

    /** Returns true if the indicated bit is 1, false if it's 0. */
    operator fun get(bitIndex: Int): Boolean =
        bitIndex in 0 until bitCount &&
            bytes[bitIndex shr BYTE_INDEX_SHIFT].toInt() and (1 shl bitIndex % Byte.SIZE_BITS) != 0

    /** If value is true, the indicated bit is set to 1; otherwise, it's set to 0. */
    operator fun set(bitIndex: Int, value: Boolean) {
        if (bitIndex !in 0 until bitCount) return
        val byteIndex = bitIndex / Byte.SIZE_BITS
        val bit = 1 shl bitIndex % Byte.SIZE_BITS
        val mask = bit xor BYTE_MASK
        val shiftedValue = if (value) bit else 0
        bytes[byteIndex] = (bytes[byteIndex].toInt() and mask or shiftedValue).toByte()
    }

    private companion object {
        private const val BYTE_MASK = 0xff
        private const val BYTE_INDEX_SHIFT = 3
    }
}

/**
 * Returns the number of bytes required to store the given number of bits in a [BitField]. Note that
 * Artemis allocates an extra, unused byte whenever there are no leftover bits in the last byte.
 */
fun countBytes(bitCount: Int): Int = bitCount / Byte.SIZE_BITS + 1

fun Source.readBitField(bitCount: Int): BitField = BitField(bitCount, this)
