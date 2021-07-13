package artemis.agent.game

import androidx.annotation.ColorRes
import artemis.agent.R

enum class WarStatus(@ColorRes val backgroundColor: Int) {
    TENSION(R.color.connected),
    WARNING(R.color.heartbeatLost),
    DECLARED(R.color.failedToConnect)
}
