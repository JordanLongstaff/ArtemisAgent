package artemis.agent.util

import android.os.VibrationEffect
import androidx.annotation.RequiresApi

private const val Q = 29
private const val TICK_DURATION = 50L
private const val CLICK_DURATION = 100L

enum class HapticEffect(val duration: Long) {
    TICK(TICK_DURATION) {
        @delegate:RequiresApi(Q)
        override val vibration: VibrationEffect by lazy {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        }
    },
    CLICK(CLICK_DURATION) {
        @delegate:RequiresApi(Q)
        override val vibration: VibrationEffect by lazy {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        }
    };

    @get:RequiresApi(Q) abstract val vibration: VibrationEffect
}
