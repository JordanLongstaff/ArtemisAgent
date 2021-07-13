package com.walkertribe.ian.util

/**
 * Version number handling class. This handles semantic versioning
 * (major.minor.patch), and can interpret float version numbers for backwards
 * compatibility. For robustness and to avoid duplication of code, it can handle
 * an arbitrary number of parts in the version number, not just three.
 * @author rjwut
 */
class Version(private vararg val parts: Int) : Comparable<Version> {
    private val hash: Int = parts.contentHashCode()

    /**
     * Constructs a Version from integer parts, with the most significant part
     * first. This constructor can be used to create both modern and legacy
     * version numbers. Note that this constructor only accepts two or more
     * parts, as the JVM insists on calling Version(float) if you only provide
     * one part.
     */
    init {
        require(parts.size >= MIN_PARTS) { "Version must have at least two parts" }
        require(parts.all { it >= 0 }) { "Negative version numbers not allowed" }
    }

    /**
     * Constructs a Version from a String. This constructor can be used to
     * create both modern and legacy version numbers.
     */
    constructor(version: String) : this(*version.split(".").map(String::toInt).toIntArray())

    override fun equals(other: Any?): Boolean =
        this === other || (other is Version && this.compareTo(other) == 0)

    override fun hashCode(): Int = hash

    override fun toString(): String = parts.joinToString(".")

    /**
     * Compares this Version against the given one. If the two Version objects
     * don't have the same number of parts, the absent parts are treated as zero
     * (e.g.: 2.1 is the same as 2.1.0).
     */
    override operator fun compareTo(other: Version): Int {
        val partCount = parts.size.coerceAtLeast(other.parts.size)
        for (i in 0 until partCount) {
            val c = getPart(parts, i) - getPart(other.parts, i)
            if (c != 0) {
                return c
            }
        }
        return 0
    }

    companion object {
        private const val MIN_PARTS = 2

        val ACCENT_COLOR = Version(2, 4, 0)
        val COMM_FILTERS = Version(2, 6, 0)
        val BEACON = Version(2, 6, 3)
        val NEBULA_TYPES = Version(2, 7, 0)

        /**
         * Returns the indicated part value for the given array of parts, or 0 if
         * the index is greater than that of the last part.
         */
        private fun getPart(parts: IntArray, index: Int): Int =
            if (parts.size > index) parts[index] else 0
    }
}
