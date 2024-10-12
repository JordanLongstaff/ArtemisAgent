package com.walkertribe.ian.util

import korlibs.io.lang.UTF8
import korlibs.io.lang.toByteArray

/**
 * An implementation of the JamCRC algorithm. These checksums are used to allow Artemis to send an
 * integer instead of a full string.
 * @author rjwut
 */
object JamCrc {
    private const val INITIAL_VALUE = 0.inv()
    private const val POLYNOMIAL = 0x04c11db7
    private const val TABLE_SIZE = 256
    private const val BYTE_MASK = 0xff
    private const val INT_MSB = 1 shl 31
    private const val TOP_BYTE_SHIFT = Int.SIZE_BITS - Byte.SIZE_BITS

    private val TABLE = IntArray(TABLE_SIZE) {
        var refl = reflect(it, Byte.SIZE_BITS) shl TOP_BYTE_SHIFT
        repeat(Byte.SIZE_BITS) {
            refl = refl shl 1 xor if (refl and INT_MSB == 0) 0 else POLYNOMIAL
        }
        reflect(refl, Int.SIZE_BITS)
    }

    /**
     * Computes a checksum for the given byte array.
     */
    private fun compute(bytes: ByteArray): Int = bytes.fold(INITIAL_VALUE) { crc, b ->
        crc ushr Byte.SIZE_BITS xor TABLE[crc and BYTE_MASK xor (b.toInt() and BYTE_MASK)]
    }

    /**
     * Computes a checksum for the UTF-8 representation of the given string. Note that even though
     * strings are transmitted in UTF-16LE in the Artemis protocol, checksums are computed in UTF-8.
     * Weirdness.
     */
    fun compute(str: String): Int = compute(str.toByteArray(UTF8))

    private fun reflect(ref: Int, ch: Int): Int = BooleanArray(ch) {
        ref and (1 shl it) != 0
    }.fold(0) { value, bit ->
        value shl 1 or if (bit) 1 else 0
    }
}
