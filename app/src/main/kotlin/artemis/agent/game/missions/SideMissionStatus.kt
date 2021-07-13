package artemis.agent.game.missions

import androidx.annotation.ColorRes
import artemis.agent.R

enum class SideMissionStatus(
    @ColorRes val backgroundColor: Int
) {
    ALL_CLEAR(R.color.allyStatusBackgroundBlue),
    OVERTAKEN(R.color.allyStatusBackgroundOrange),
    DAMAGED(R.color.allyStatusBackgroundYellow);

    companion object {
        fun maxOf(a: SideMissionStatus, b: SideMissionStatus): SideMissionStatus =
            if (a.ordinal > b.ordinal) a else b
    }
}
