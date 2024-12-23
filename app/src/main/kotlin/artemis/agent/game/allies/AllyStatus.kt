package artemis.agent.game.allies

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import artemis.agent.R

enum class AllyStatus(
    val sortIndex: AllySortIndex,
    @StringRes val description: Int,
    @ColorRes val backgroundColor: Int
) {
    HOSTAGE(
        AllySortIndex.HOSTAGE,
        R.string.ally_status_hostage,
        R.color.allyStatusBackgroundOrange,
    ),
    COMMANDEERED(
        AllySortIndex.COMMANDEERED,
        R.string.ally_status_commandeered_not_in_nebula,
        R.color.allyStatusBackgroundOrange,
    ),
    COMMANDEERED_NEBULA(
        AllySortIndex.COMMANDEERED,
        R.string.ally_status_commandeered_in_nebula,
        R.color.allyStatusBackgroundOrange,
    ),
    NEED_ENERGY(
        AllySortIndex.NEED_ENERGY,
        R.string.ally_status_needs_energy,
        R.color.allyStatusBackgroundYellow,
    ),
    NEED_DAMCON(
        AllySortIndex.NEED_DAMCON,
        R.string.ally_status_needs_damcon,
        R.color.allyStatusBackgroundYellow,
    ),
    MALFUNCTION(
        AllySortIndex.MALFUNCTION,
        R.string.ally_status_malfunction,
        R.color.allyStatusBackgroundYellow,
    ),
    FLYING_BLIND(
        AllySortIndex.FLYING_BLIND,
        R.string.ally_status_blind,
        R.color.allyStatusBackgroundYellow,
    ),
    AMBASSADOR(
        AllySortIndex.AMBASSADOR,
        R.string.ally_status_ambassador_for_tsn,
        R.color.allyStatusBackgroundYellow,
    ),
    PIRATE_BOSS(
        AllySortIndex.AMBASSADOR,
        R.string.ally_status_ambassador_for_pirate,
        R.color.allyStatusBackgroundYellow,
    ),
    CONTRABAND(
        AllySortIndex.CONTRABAND,
        R.string.ally_status_contraband_for_tsn,
        R.color.allyStatusBackgroundYellow,
    ),
    PIRATE_SUPPLIES(
        AllySortIndex.CONTRABAND,
        R.string.ally_status_contraband_for_pirate,
        R.color.allyStatusBackgroundYellow,
    ),
    SECURE_DATA(
        AllySortIndex.SECURE_DATA,
        R.string.ally_status_data_for_tsn,
        R.color.allyStatusBackgroundYellow,
    ),
    PIRATE_DATA(
        AllySortIndex.SECURE_DATA,
        R.string.ally_status_data_for_pirate,
        R.color.allyStatusBackgroundRed,
    ),
    NORMAL(
        AllySortIndex.NORMAL,
        R.string.ally_status_normal,
        R.color.allyStatusBackgroundBlue,
    ),
    REWARD(
        AllySortIndex.NORMAL,
        R.string.ally_status_reward,
        R.color.allyStatusBackgroundBlue,
    ),
    REPAIRING(
        AllySortIndex.NORMAL,
        R.string.ally_status_repairing,
        R.color.allyStatusBackgroundBlue,
    ),
    FIGHTER_TRAP(
        AllySortIndex.TRAP,
        R.string.ally_status_fighter_trap,
        R.color.allyStatusBackgroundRed,
    ),
    MINE_TRAP(
        AllySortIndex.TRAP,
        R.string.ally_status_mine_trap,
        R.color.allyStatusBackgroundRed,
    );

    fun getPirateSensitiveEquivalent(isPirate: Boolean): AllyStatus {
        if (this < AMBASSADOR || this > PIRATE_DATA) return this

        val index = ordinal - AMBASSADOR.ordinal
        return PIRATE_SENSITIVE[if (isPirate) index or PIRATE_FLAG else index and NOT_PIRATE_FLAG]
    }

    private companion object {
        val PIRATE_SENSITIVE = entries.subList(
            AMBASSADOR.ordinal,
            NORMAL.ordinal
        )

        const val PIRATE_FLAG = 1
        const val NOT_PIRATE_FLAG = 1.inv()
    }
}
