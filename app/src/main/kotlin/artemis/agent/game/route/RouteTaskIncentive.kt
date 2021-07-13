package artemis.agent.game.route

import androidx.annotation.StringRes
import artemis.agent.R
import artemis.agent.game.ObjectEntry
import artemis.agent.game.allies.AllyStatus

enum class RouteTaskIncentive {
    NEEDS_ENERGY {
        override fun matches(ally: ObjectEntry.Ally): Boolean =
            ally.status == AllyStatus.NEED_ENERGY

        override fun getTextFor(ally: ObjectEntry.Ally): Int =
            R.string.reason_needs_energy
    },
    NEEDS_DAMCON {
        override fun matches(ally: ObjectEntry.Ally): Boolean =
            ally.status == AllyStatus.NEED_DAMCON

        override fun getTextFor(ally: ObjectEntry.Ally): Int =
            R.string.reason_needs_damcon
    },
    RESET_COMPUTER {
        override fun matches(ally: ObjectEntry.Ally): Boolean =
            ally.status == AllyStatus.MALFUNCTION

        override fun getTextFor(ally: ObjectEntry.Ally): Int =
            R.string.reason_malfunction
    },
    AMBASSADOR_PICKUP {
        override fun matches(ally: ObjectEntry.Ally): Boolean =
            ally.status == AllyStatus.AMBASSADOR || ally.status == AllyStatus.PIRATE_BOSS

        override fun getTextFor(ally: ObjectEntry.Ally): Int =
            if (ally.status == AllyStatus.PIRATE_BOSS) {
                R.string.reason_pirate_boss
            } else {
                R.string.reason_ambassador
            }
    },
    HOSTAGE {
        override fun matches(ally: ObjectEntry.Ally): Boolean =
            ally.status == AllyStatus.HOSTAGE

        override fun getTextFor(ally: ObjectEntry.Ally): Int =
            R.string.reason_hostage
    },
    COMMANDEERED {
        override fun matches(ally: ObjectEntry.Ally): Boolean =
            ally.status == AllyStatus.COMMANDEERED

        override fun getTextFor(ally: ObjectEntry.Ally): Int =
            R.string.reason_commandeered
    },
    HAS_ENERGY {
        override fun matches(ally: ObjectEntry.Ally): Boolean =
            ally.hasEnergy

        override fun getTextFor(ally: ObjectEntry.Ally): Int =
            R.string.reason_has_energy
    };

    abstract fun matches(ally: ObjectEntry.Ally): Boolean

    @StringRes
    abstract fun getTextFor(ally: ObjectEntry.Ally): Int
}
