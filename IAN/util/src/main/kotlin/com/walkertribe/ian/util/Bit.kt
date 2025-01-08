package com.walkertribe.ian.util

/** Interface for the bits in a bit field for an object. */
interface Bit {
    /**
     * Returns the index of this bit in a bit field, or -1 if the bit should not be included in the
     * given version of Artemis.
     */
    fun getIndex(version: Version): Int
}
