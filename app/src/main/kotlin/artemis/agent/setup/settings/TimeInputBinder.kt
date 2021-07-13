package artemis.agent.setup.settings

import android.view.View
import artemis.agent.databinding.SecondsInputBinding

abstract class TimeInputBinder(
    binding: SecondsInputBinding,
    private val includeMinutes: Boolean = false,
    private val minimumSeconds: Int = 0,
) {
    private var binding: SecondsInputBinding? = binding

    var timeInSeconds: Int = -1
        set(seconds) {
            if (field == seconds) return
            field = seconds

            val ones = seconds % TEN
            val withoutOnes = seconds / TEN

            binding?.apply {
                secondsOne.text = ones.toString()
                secondsOneDownButton.setOnClickListener {
                    onSecondsChange(minimumSeconds.coerceAtLeast(seconds - 1))
                }
                secondsTenDownButton.setOnClickListener {
                    onSecondsChange(minimumSeconds.coerceAtLeast(seconds - TEN))
                }

                if (includeMinutes) {
                    val tens = withoutOnes % SIX
                    val mins = withoutOnes / SIX

                    minutes.visibility = View.VISIBLE
                    minutesDownButton.visibility = View.VISIBLE
                    minutesUpButton.visibility = View.VISIBLE
                    colon.visibility = View.VISIBLE

                    secondsTen.text = tens.toString()
                    minutes.text = mins.toString()

                    secondsOneUpButton.setOnClickListener {
                        onSecondsChange(MAX_WITH_MINUTES.coerceAtMost(seconds + 1))
                    }
                    secondsTenUpButton.setOnClickListener {
                        onSecondsChange(MAX_WITH_MINUTES.coerceAtMost(seconds + TEN))
                    }
                    minutesUpButton.setOnClickListener {
                        onSecondsChange(MAX_WITH_MINUTES.coerceAtMost(seconds + SIXTY))
                    }
                    minutesDownButton.setOnClickListener {
                        onSecondsChange(minimumSeconds.coerceAtLeast(seconds - SIXTY))
                    }
                } else {
                    minutes.visibility = View.GONE
                    minutesDownButton.visibility = View.GONE
                    minutesUpButton.visibility = View.GONE
                    colon.visibility = View.GONE

                    secondsTen.text = withoutOnes.toString()
                    secondsOneUpButton.setOnClickListener {
                        onSecondsChange(MAX_WITHOUT_MINUTES.coerceAtMost(seconds + 1))
                    }
                    secondsTenUpButton.setOnClickListener {
                        onSecondsChange(MAX_WITHOUT_MINUTES.coerceAtMost(seconds + TEN))
                    }
                }
            }
        }

    abstract fun onSecondsChange(seconds: Int)

    fun destroy() {
        binding = null
    }

    private companion object {
        const val MAX_WITHOUT_MINUTES = 99
        const val MAX_WITH_MINUTES = 599

        const val SIX = 6
        const val TEN = 10
        const val SIXTY = 60
    }
}
