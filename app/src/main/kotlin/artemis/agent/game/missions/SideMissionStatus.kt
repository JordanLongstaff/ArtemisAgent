package artemis.agent.game.missions

import androidx.annotation.ColorRes
import artemis.agent.R

enum class SideMissionStatus(@field:ColorRes val backgroundColor: Int) {
    ALL_CLEAR(R.color.allyStatusBackgroundBlue),
    OVERTAKEN(R.color.allyStatusBackgroundOrange),
    DAMAGED(R.color.allyStatusBackgroundYellow),
}
