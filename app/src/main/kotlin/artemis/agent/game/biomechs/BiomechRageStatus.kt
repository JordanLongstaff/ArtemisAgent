package artemis.agent.game.biomechs

import androidx.annotation.ColorRes
import artemis.agent.R

enum class BiomechRageStatus(@ColorRes val color: Int) {
    NEUTRAL(R.color.biomechNeutral),
    HOSTILE(R.color.enemyRed);

    companion object {
        operator fun get(rage: Int): BiomechRageStatus = entries[rage.coerceAtMost(HOSTILE.ordinal)]
    }
}
