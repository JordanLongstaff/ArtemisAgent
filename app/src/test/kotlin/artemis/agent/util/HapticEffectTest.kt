package artemis.agent.util

import android.os.VibrationEffect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll

class HapticEffectTest :
    DescribeSpec({
        describe("HapticEffect") {
            mockkStatic(VibrationEffect::createPredefined)
            every { VibrationEffect.createPredefined(any()) } answers
                {
                    mockk<VibrationEffect> { every { describeContents() } returns firstArg() }
                }

            afterSpec {
                clearAllMocks()
                unmockkAll()
            }

            it("Two entries: TICK and CLICK") { HapticEffect.entries shouldHaveSize 2 }

            val durations = longArrayOf(50L, 100L)
            val effectIds = intArrayOf(VibrationEffect.EFFECT_TICK, VibrationEffect.EFFECT_CLICK)

            withData(HapticEffect.entries) { effect ->
                val expectedDuration = durations[effect.ordinal]

                it("Duration: $expectedDuration ms") {
                    effect.duration shouldBeEqual expectedDuration
                }

                it("Begets vibration effect") {
                    val vibration = effect.vibration.shouldBeInstanceOf<VibrationEffect>()
                    vibration.describeContents() shouldBeEqual effectIds[effect.ordinal]
                }
            }
        }
    })
