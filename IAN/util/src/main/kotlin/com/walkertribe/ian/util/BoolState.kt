package com.walkertribe.ian.util

import kotlinx.io.Source
import kotlinx.io.readByteArray

/**
 * Represents the tri-state values of TRUE, FALSE and UNKNOWN, preserving the original values that
 * produced them in the bit stream.
 * @author rjwut
 */
sealed class BoolState(bytes: ByteArray?) {
    data object True : BoolState(byteArrayOf(1))
    data object False : BoolState(byteArrayOf(0))
    data object Unknown : BoolState(null)

    internal val boolValue: Boolean? = bytes?.hasNonZero

    /**
     * Returns true if this BoolState represents TRUE; false otherwise.
     */
    val booleanValue: Boolean get() = boolValue == true

    companion object {
        /**
         * Converts the given boolean value to a BoolState.
         */
        operator fun invoke(value: Boolean?): BoolState = when (value) {
            null -> Unknown
            true -> True
            else -> False
        }
    }
}

/**
 * Returns false if state is null or UNKNOWN; true otherwise.
 */
val BoolState?.isKnown get() = this?.boolValue != null

fun Source.readBoolState(byteCount: Int): BoolState = BoolState(readByteArray(byteCount).hasNonZero)

private val ByteArray.hasNonZero: Boolean get() = any { it != 0.toByte() }
