package com.walkertribe.ian.util

import korlibs.io.lang.UTF16_LE
import korlibs.io.lang.toString

/**
 * Stores Artemis's UTF-16 null-terminated strings, preserving any "garbage" data that may follow
 * the null. This ensures that string reading and writing is symmetrical; in other words, that the
 * bytes written always exactly match the bytes read.
 * @author rjwut
 */
class NullTerminatedString(bytes: ByteArray) : Comparable<NullTerminatedString>, CharSequence {
    private val str: String

    /**
     * Returns the "garbage" bytes. This returns an empty array if there were no garbage bytes.
     */
    private val garbage: ByteArray

    /**
     * Reads a string from the given byte array.
     */
    init {
        var i = 0

        // find the null
        while (i < bytes.size) {
            if (bytes[i] == 0.toByte() && bytes[i + 1] == 0.toByte()) {
                break
            }
            i += 2
        }
        require(i < bytes.size) { "No null found for null-terminated string" }
        str = bytes.copyOfRange(0, i).toString(UTF16_LE)
        garbage = if (i < bytes.size - 2) {
            bytes.copyOfRange(i + 2, bytes.size) // null was early
        } else {
            byteArrayOf()
        }
    }

    override fun toString(): String = str

    /**
     * Returns the length of this NullTerminatedString in characters, excluding the null and any
     * garbage data.
     */
    override val length: Int get() = str.length

    private val garbageLength: Int get() = garbage.size

    override fun get(index: Int): Char = str[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
        str.subSequence(startIndex, endIndex)

    override fun equals(other: Any?): Boolean =
        this === other || (other is NullTerminatedString && str == other.str)

    override fun hashCode(): Int = str.hashCode()

    override fun compareTo(other: NullTerminatedString): Int =
        str.compareTo(other.str).takeUnless { it == 0 }
            ?: garbageLength.compareTo(other.garbageLength).takeUnless { it == 0 }
            ?: garbage.zip(other.garbage).map { (a, b) -> a.compareTo(b) }.firstOrNull { it != 0 }
            ?: 0
}
