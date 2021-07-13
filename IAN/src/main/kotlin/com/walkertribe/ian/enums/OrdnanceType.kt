package com.walkertribe.ian.enums

import com.walkertribe.ian.util.Version

private const val ONE_MINUTE = 60_000L

/**
 * The types of ordnance that player ships can fire.
 * @author rjwut
 */
enum class OrdnanceType(
    /**
     * Returns the three-character code of this `OrdnanceType` (as used in vesselData.xml).
     */
    val code: String,
    private val label: String,
    buildMinutes: Int,
    private val minVersion: Version? = null,
    val alternateLabel: String = ""
) {
    TORPEDO(
        "trp",
        "Torpedo",
        3,
        alternateLabel = "Type 1 Homing"
    ),
    NUKE(
        "nuk",
        "Nuke",
        10,
        alternateLabel = "Type 4 Nuke"
    ),
    MINE(
        "min",
        "Mine",
        4,
        alternateLabel = "Type 6 Mine"
    ),
    EMP(
        "emp",
        "EMP",
        5,
        alternateLabel = "Type 9 EMP"
    ),
    PSHOCK(
        "shk",
        "Pshock",
        10,
        alternateLabel = "Type 8 Pshock"
    ),
    BEACON(
        "bea",
        "Beacon",
        1,
        Version.BEACON
    ),
    PROBE(
        "pro",
        "Probe",
        1,
        Version.BEACON
    ),
    TAG(
        "tag",
        "Tag",
        1,
        Version.BEACON
    );

    val buildTime: Long = ONE_MINUTE * buildMinutes

    override fun toString(): String = label

    fun getLabelFor(version: Version): String =
        if (version >= Version.BEACON) label else alternateLabel

    /**
     * Returns true if this `OrdnanceType` exists in the given version of Artemis, false
     * otherwise.
     */
    infix fun existsIn(version: Version): Boolean = minVersion?.let { it <= version } ?: true

    companion object {
        /**
         * Returns the full count of all `OrdnanceType`s.
         */
        val size = entries.size

        /**
         * Returns the number of `OrdnanceType`s that exist in the given version of
         * Artemis.
         */
        fun countForVersion(version: Version) = entries.count { it existsIn version }

        /**
         * Returns the array of `OrdnanceType`s that exist in the given version of
         * Artemis.
         */
        fun getAllForVersion(version: Version) = entries.filter { it existsIn version }.toTypedArray()

        /**
         * Returns the `OrdnanceType` corresponding to the given three-character
         * code (as used in vesselData.xml) or null if no such `OrdnanceType`
         * was found.
         */
        operator fun get(code: String): OrdnanceType? = entries.find { code == it.code }
    }
}
