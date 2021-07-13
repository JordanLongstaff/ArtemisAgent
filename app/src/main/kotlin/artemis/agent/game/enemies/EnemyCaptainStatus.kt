package artemis.agent.game.enemies

import androidx.annotation.StringRes
import artemis.agent.R

enum class EnemyCaptainStatus(@StringRes val description: Int) {
    NO_INTEL(R.string.enemy_status_no_intel),
    NORMAL(R.string.enemy_status_normal),
    COWARDLY(R.string.enemy_status_cowardly),
    BRAVE(R.string.enemy_status_brave),
    EASILY_OFFENDED(R.string.enemy_status_easily_offended),
    DUPLICITOUS(R.string.enemy_status_duplicitous),
}
