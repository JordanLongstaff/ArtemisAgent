package com.walkertribe.ian.util

import korlibs.io.lang.UTF8
import korlibs.io.lang.toByteArray

/**
 * An implementation of the JamCRC algorithm. These checksums are used to allow
 * Artemis to send an int instead of a full string.
 * @author rjwut
 */
object JamCrc {
    private const val INITIAL_VALUE = 0.inv()
    private const val POLYNOMIAL = 0x04c11db7
    private val TABLE = IntArray(256) {
        var refl = reflect(it, 8.toByte()) shl 24
        repeat(8) {
            refl = refl shl 1 xor if (refl and (1 shl 31) == 0) 0 else POLYNOMIAL
        }
        reflect(refl, 32.toByte())
    }

    /**
     * Computes a checksum for the given byte array.
     */
    fun compute(bytes: ByteArray): Int = bytes.fold(INITIAL_VALUE) { crc, b ->
        crc ushr 8 xor TABLE[crc and 0xff xor (b.toInt() and 0xff)]
    }

    /**
     * Computes a checksum for the UTF-8 representation of the given string.
     * Note that even though strings are transmitted in UTF-16LE in the Artemis
     * protocol, checksums are computed in UTF-8. Weirdness.
     */
    fun compute(str: CharSequence): Int = compute(str.toString().toByteArray(UTF8))

    private fun reflect(ref: Int, ch: Byte): Int = (0 until ch).map {
        ref and (1 shl it) != 0
    }.fold(0) { value, bit ->
        value shl 1 or if (bit) 1 else 0
    }
}
