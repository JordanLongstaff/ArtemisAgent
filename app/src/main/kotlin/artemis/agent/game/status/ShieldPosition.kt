package artemis.agent.game.status

import androidx.annotation.StringRes
import artemis.agent.R

enum class ShieldPosition(@all:StringRes val stringId: Int) {
    FRONT(R.string.front_shield),
    REAR(R.string.rear_shield),
}
