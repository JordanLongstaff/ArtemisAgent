package artemis.agent

import androidx.annotation.RawRes

enum class SoundEffect(@RawRes val soundId: Int) {
    BEEP_1(R.raw.beep1),
    BEEP_2(R.raw.beep2),
    CONFIRMATION(R.raw.confirmation),
    CONNECTED(R.raw.connected),
    DISCONNECTED(R.raw.disconnected),
    HEARTBEAT_LOST(R.raw.heartbeat_lost)
}
