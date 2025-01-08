@file:Suppress("MagicNumber")

package com.walkertribe.ian.enums

import com.walkertribe.ian.util.Version
import kotlin.time.Duration.Companion.minutes

/**
 * The types of ordnance that player ships can fire.
 *
 * @author rjwut
 */
enum class OrdnanceType(
    /** Returns the three-character code of this `OrdnanceType` (as used in vesselData.xml). */
    val code: String,
    sentenceCase: Boolean = true,
    buildMinutes: Int,
    legacyType: Int? = null,
) {
    TORPEDO(code = "trp", buildMinutes = 3, legacyType = 1),
    NUKE(code = "nuk", buildMinutes = 10, legacyType = 4),
    MINE(code = "min", buildMinutes = 4, legacyType = 6),
    EMP(code = "emp", sentenceCase = false, buildMinutes = 5, legacyType = 9),
    PSHOCK(code = "shk", buildMinutes = 10, legacyType = 8),
    BEACON(code = "bea", buildMinutes = 1),
    PROBE(code = "pro", buildMinutes = 1),
    TAG(code = "tag", buildMinutes = 1);

    val buildTime: Long by lazy { buildMinutes.minutes.inWholeMilliseconds }

    private val label: String =
        if (sentenceCase) name.let { it[0] + it.substring(1).lowercase() } else name

    private val alternateLabel: String? = legacyType?.let { "Type $it $label" }

    override fun toString(): String = label

    fun getLabelFor(version: Version): String =
        alternateLabel?.takeIf { version < Version.BEACON } ?: label

    fun hasLabel(lookupLabel: String): Boolean =
        label == lookupLabel || alternateLabel == lookupLabel

    /**
     * Returns true if this `OrdnanceType` exists in the given version of Artemis, false otherwise.
     */
    infix fun existsIn(version: Version): Boolean =
        alternateLabel != null || version >= Version.BEACON

    companion object {
        /** Returns the full count of all [OrdnanceType]s. */
        val size = entries.size

        /** Returns the number of [OrdnanceType]s that exist in the given version of Artemis. */
        fun countForVersion(version: Version) = entries.count { it existsIn version }

        /** Returns the array of [OrdnanceType]s that exist in the given version of Artemis. */
        fun getAllForVersion(version: Version) =
            entries.filter { it existsIn version }.toTypedArray()

        /**
         * Returns the [OrdnanceType] corresponding to the given three-character code (as used in
         * `vesselData.xml`) or `null` if no such [OrdnanceType] was found.
         */
        operator fun get(code: String): OrdnanceType? = entries.find { code == it.code }
    }
}
