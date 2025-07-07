package artemis.agent.util

import artemis.agent.R
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual

class SoundEffectTest :
    DescribeSpec({
        describe("SoundEffect") {
            val expectedResources =
                intArrayOf(
                    R.raw.beep1,
                    R.raw.beep2,
                    R.raw.confirmation,
                    R.raw.connected,
                    R.raw.disconnected,
                    R.raw.heartbeat_lost,
                )

            withData(SoundEffect.entries) { soundEffect ->
                soundEffect.soundId shouldBeEqual expectedResources[soundEffect.ordinal]
            }
        }
    })
