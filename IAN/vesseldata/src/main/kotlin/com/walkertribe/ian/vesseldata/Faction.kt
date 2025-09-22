package com.walkertribe.ian.vesseldata

import com.walkertribe.ian.util.Util.splitSpaceDelimited
import korlibs.io.serialization.xml.Xml

/**
 * Corresponds to the &lt;hullRace&gt; element in vesselData.xml.
 *
 * @author rjwut
 */
class Faction
internal constructor(
    /** Returns the faction's ID. */
    val id: Int,

    /** Returns the faction's name. */
    val name: String,
    keys: String,

    /** Returns a list of taunts used against this Faction. */
    val taunts: List<Taunt>,
) : Comparable<Faction> {
    /** Returns a Set containing the FactionAttributes that correspond to this Faction. */
    val attributes: Set<String> = build(keys)

    internal constructor(
        xml: Xml
    ) : this(
        id = xml.requiredInt("ID"),
        name = xml.requiredString("name"),
        keys = xml.str("keys"),
        taunts = xml["taunt"].map(::Taunt),
    )

    /** Returns true if this Faction has all the given FactionAttributes; false otherwise. */
    operator fun get(vararg attrs: String): Boolean = attrs.all(attributes::contains)

    override fun hashCode(): Int = id

    override fun equals(other: Any?): Boolean = other is Faction && id == other.id

    override fun compareTo(other: Faction): Int = compareValuesBy(this, other) { it.id }

    companion object {
        // stance
        const val PLAYER = "player" // contains player-controlled vessels
        const val FRIENDLY = "friendly" // contains allied vessels
        const val ENEMY = "enemy" // contains hostile vessels
        val ALL_STANCES = arrayOf(PLAYER, FRIENDLY, ENEMY)

        // fleet organization
        const val STANDARD = "standard" // forms the core of fleets
        const val SUPPORT = "support" // accompanies STANDARD enemies
        const val LONER = "loner" // flies alone outside the fleet
        private val ALL_ENEMY_ATTRIBUTES = arrayOf(STANDARD, SUPPORT, LONER)

        // behaviour
        const val BIOMECH = "biomech" // hive mind, consumes asteroids/anomalies
        const val JUMPMASTER =
            "jumpmaster" // has reduced jump cooldown and combat jump (PLAYER only)
        const val WHALEHATER = "whalehater" // gets distracted with hunting whales
        const val WHALELOVER = "whalelover" // attack anyone who shoots whales
        private val ALL_WHALE_AFFINITIES = listOf(WHALEHATER, WHALELOVER)

        private fun build(keys: String): Set<String> =
            keys
                .splitSpaceDelimited()
                .apply {
                    require(ALL_STANCES.any(this::contains)) {
                        "Must have at least one of PLAYER, FRIENDLY, or ENEMY"
                    }
                    require(!contains(ENEMY) || ALL_ENEMY_ATTRIBUTES.any(this::contains)) {
                        "ENEMY must have at least one of STANDARD, SUPPORT, or LONER"
                    }
                    require(!containsAll(ALL_WHALE_AFFINITIES)) {
                        "WHALEHATER and WHALELOVER are mutually exclusive"
                    }
                    require(contains(PLAYER) || !contains(JUMPMASTER)) {
                        "JUMPMASTER must be a PLAYER"
                    }
                }
                .toSet()
    }
}
