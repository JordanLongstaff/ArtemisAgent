package com.walkertribe.ian.vesseldata

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import korlibs.io.serialization.xml.Xml

enum class TestFaction(
    val factionName: String,
    val attributes: TestFactionAttributes,
    val taunts: List<Taunt>,
) {
    TSN(
        "TSN",
        TestFactionAttributes.PLAYER,
        listOf(
            Taunt(
                "is proud of his laid back crew with lax discipline",
                "Your crew is a sloppy bunch of disrespectful layabouts!",
            ),
            Taunt(
                "has no living family",
                "Your father is so stupid he thinks planets are tiny plans!",
            ),
            Taunt(
                "is decorated for strategic thinking and is not interested in offering " +
                    "a challenge in battle",
                "You chicken-hearted cowards! I've had more challenging battles with a space bug!",
            ),
        ),
    ),
    TERRAN(
        "Terran",
        TestFactionAttributes.FRIENDLY,
        listOf(
            Taunt(
                "is proud of his laid back crew with lax discipline",
                "Your crew is a sloppy bunch of disrespectful layabouts!",
            ),
            Taunt(
                "has no living family",
                "Your father is so stupid he thinks planets are tiny plans!",
            ),
            Taunt(
                "is decorated for strategic thinking and is not interested in offering " +
                    "a challenge in battle",
                "You chicken-hearted cowards! I've had more challenging battles with a space bug!",
            ),
        ),
    ),
    KRALIEN(
        "Kralien",
        TestFactionAttributes.ENEMY_STANDARD,
        listOf(
            Taunt(
                "does not practice the Kralien religion faithfully",
                "Hey wormface! Can I borrow your Holy Scroll of Amborax? " +
                    "I need to wipe my stinky feet!",
            ),
            Taunt(
                "complained to Kralien High Command that Kralien ships are too weak",
                "You call that a warship? I could crush that toy with my bare hands.",
            ),
            Taunt("is unmarried", "You're so ugly that your wife will thank me for killing you!"),
        ),
    ),
    ARVONIAN(
        "Arvonian",
        TestFactionAttributes.ENEMY_SUPPORT_WHALELOVER,
        listOf(
            Taunt(
                "openly dislikes the Arvonian Royal Family",
                "Queen Marah looks like a hideous pustule and smells like a flatulent Space Whale.",
            ),
            Taunt("is unmarried", "Your husband dresses like a Situlan scum slug!"),
            Taunt(
                "does not care about space whales",
                "I'll kill you later, Arvonian. Right now I'm enjoying a bowl of space whale soup.",
            ),
        ),
    ),
    TORGOTH(
        "Torgoth",
        TestFactionAttributes.ENEMY_SUPPORT_WHALEHATER,
        listOf(
            Taunt(
                "runs a tidy ship and has no insecurity about hygiene",
                "Do us all a favor and take a bath, you fungus-infested pachyderm!",
            ),
            Taunt(
                "has already mourned his dead father and will not react to taunts about him",
                "Your father is a Space Whale!",
            ),
            Taunt(
                "has the highest Torgoth efficiency rating and will not react to taunts " +
                    "about his ship's condition",
                "That broken down rust bucket guarantees victory for Earth.",
            ),
        ),
    ),
    SKARAAN(
        "Skaraan",
        TestFactionAttributes.ENEMY_LONER_HASSPECIALS,
        listOf(
            Taunt("has no living family", "Your father is a flea-bitten Laparian mule!"),
            Taunt(
                "does not care about his ship's appearance",
                "Your ship is so ugly my eyeballs burn just looking at it!",
            ),
            Taunt(
                "often disagrees with Skaraan corporate leadership",
                "Your corporate bosses are dimwitted, money-losing morons!",
            ),
        ),
    ),
    BIOMECH(
        "BioMech",
        TestFactionAttributes.ENEMY_LONER_BIOMECH,
        listOf(
            Taunt("smnewi4f5jH#5ld3@(#k", "Scramble signal 1"),
            Taunt("Mk40gH(*HGl45gj3p;32ll0-", "Scramble signal 2"),
            Taunt("NNRnjh3h49@98%^km%%%m", "Scramble signal 3"),
        ),
    ),
    XIMNI(
        "Ximni",
        TestFactionAttributes.PLAYER_JUMPMASTER,
        listOf(
            Taunt(
                "has no visible mutations on his face",
                "The deadliest weapon on your ship is your ugly, mutant face!",
            ),
            Taunt(
                "is a decorated hero who ignores taunts about her bravery",
                "Only a coward hides in a massive battlewagon like that!",
            ),
            Taunt(
                "is a human who has never even seen the Ximni homeworld",
                "The only thing that smells worse than dragon breath is the Ximni home planet!",
            ),
        ),
    ),
    PIRATE(
        "Pirate",
        TestFactionAttributes.PLAYER_JUMPMASTER,
        listOf(
            Taunt(
                "is proud of his laid back crew with lax discipline",
                "Your crew is a sloppy bunch of disrespectful layabouts!",
            ),
            Taunt("isn't familiar with Terran animals", "You're nothing but pirhana bait!"),
            Taunt(
                "has a well maintained personal appearance",
                "Your breath could kill a space whale!",
            ),
        ),
    );

    val isEnemy: Boolean
        get() = attributes.keys.contains("enemy")

    override fun toString(): String = factionName

    fun build(): Faction = Faction(ordinal, factionName, attributes.keys, taunts)

    suspend fun test(faction: Faction?) {
        faction.shouldNotBeNull()
        faction.name shouldBeEqual factionName

        val expectedAttributes = attributes.expected.toSet()
        faction.attributes shouldContainExactly expectedAttributes

        expectedAttributes.forEach { faction[it].shouldBeTrue() }

        Arb.string().filterNot(expectedAttributes::contains).checkAll {
            faction[it].shouldBeFalse()
        }

        faction.taunts shouldContainExactly taunts
    }

    fun serialize(): Xml =
        Xml("hullRace", "ID" to ordinal, "name" to factionName, "keys" to attributes.keys) {
            taunts.forEach { node("taunt", "immunity" to it.immunity, "text" to it.text) }
        }
}
