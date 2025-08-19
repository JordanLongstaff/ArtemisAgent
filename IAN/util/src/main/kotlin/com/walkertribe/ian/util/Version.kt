package com.walkertribe.ian.util

/**
 * Version number handling class. Version numbers are defined with semantic versioning
 * (`major.minor.patch`).
 *
 * @author rjwut
 */
data class Version(val major: Int, val minor: Int, val patch: Int) : Comparable<Version> {
    private val hash: Int by lazy {
        major.and(MAJOR_MASK).shl(MAJOR_SHIFT) or
            minor.and(PART_MASK).shl(MINOR_SHIFT) or
            patch.and(PART_MASK)
    }

    /**
     * Constructs a Version from integer parts, with the most significant part first. This
     * constructor can be used to create both modern and legacy version numbers. Note that this
     * constructor only accepts two or more parts, as the JVM insists on calling Version(float) if
     * you only provide one part.
     */
    init {
        require(major >= 0 && minor >= 0 && patch >= 0) { "Negative version numbers not allowed" }
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Version && this.compareTo(other) == 0)

    override fun hashCode(): Int = hash

    override fun toString(): String = "$major.$minor.$patch"

    /**
     * Compares this Version against the given one. If the two Version objects don't have the same
     * number of parts, the absent parts are treated as zero (e.g.: 2.1 is the same as 2.1.0).
     */
    override operator fun compareTo(other: Version): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    companion object {
        private const val MAJOR_MASK = 0x000F
        private const val PART_MASK = 0x3FFF
        private const val MINOR_SHIFT = 14
        private const val MAJOR_SHIFT = MINOR_SHIFT * 2

        val MINIMUM = Version(2, 3, 0)
        val DEFAULT = Version(2, 8, 1)

        val ACCENT_COLOR = Version(2, 4, 0)
        val COMM_FILTERS = Version(2, 6, 0)
        val BEACON = Version(2, 6, 3)
        val NEBULA_TYPES = Version(2, 7, 0)
    }
}
