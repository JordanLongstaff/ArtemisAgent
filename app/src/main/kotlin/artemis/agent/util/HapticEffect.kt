package artemis.agent.util

import android.os.Build
import android.os.VibrationEffect
import androidx.annotation.RequiresApi

private const val TICK_DURATION = 50L
private const val CLICK_DURATION = 100L

enum class HapticEffect(val duration: Long) {
    TICK(TICK_DURATION) {
        @delegate:RequiresApi(Build.VERSION_CODES.Q)
        override val vibration: VibrationEffect by lazy {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        }
    },
    CLICK(CLICK_DURATION) {
        @delegate:RequiresApi(Build.VERSION_CODES.Q)
        override val vibration: VibrationEffect by lazy {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        }
    };

    @get:RequiresApi(Build.VERSION_CODES.Q) abstract val vibration: VibrationEffect
}
