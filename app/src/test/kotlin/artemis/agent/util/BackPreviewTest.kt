package artemis.agent.util

import androidx.activity.BackEventCompat
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.positiveFloat
import io.kotest.property.checkAll

class BackPreviewTest :
    DescribeSpec({
        var previewed = false
        var reverted = false
        var beforePreview = false
        var closed = false

        val backPreview =
            object : BackPreview(true) {
                override fun beforePreview() {
                    super.beforePreview()
                    beforePreview = true
                }

                override fun preview() {
                    previewed = true
                }

                override fun revert() {
                    reverted = true
                }

                override fun close() {
                    super.close()
                    closed = true
                }
            }

        fun reset() {
            previewed = false
            reverted = false
            beforePreview = false
            closed = false
        }

        beforeTest { reset() }

        val swipeEdgeArb =
            Arb.of(BackEventCompat.EDGE_LEFT, BackEventCompat.EDGE_RIGHT, BackEventCompat.EDGE_NONE)

        describe("BackPreview") {
            it("On back started") {
                checkAll(Arb.float(), Arb.float(), Arb.float(0.0f, 1.0f), swipeEdgeArb) {
                    touchX,
                    touchY,
                    progress,
                    swipeEdge ->
                    reset()
                    backPreview.handleOnBackStarted(
                        BackEventCompat(touchX, touchY, progress, swipeEdge)
                    )

                    backPreview.isEnabled.shouldBeTrue()
                    beforePreview.shouldBeTrue()
                    previewed.shouldBeTrue()
                    reverted.shouldBeFalse()
                    closed.shouldBeFalse()
                }
            }

            describe("On back progressed") {
                withData(
                    nameFn = { "${it.first} preview" },
                    Triple("Activate", Arb.positiveFloat(includeNaNs = false), true),
                    Triple("Revert", Arb.numericFloat(max = 0.0f), false),
                ) { (_, progressArb, shouldPreview) ->
                    checkAll(Arb.float(), Arb.float(), progressArb, swipeEdgeArb) {
                        touchX,
                        touchY,
                        progress,
                        swipeEdge ->
                        reset()
                        backPreview.handleOnBackProgressed(
                            BackEventCompat(touchX, touchY, progress, swipeEdge)
                        )

                        backPreview.isEnabled.shouldBeTrue()
                        beforePreview.shouldBeFalse()
                        previewed shouldBeEqual shouldPreview
                        reverted shouldBeEqual !shouldPreview
                        closed.shouldBeFalse()
                    }
                }
            }

            it("On back cancelled") {
                backPreview.handleOnBackCancelled()

                backPreview.isEnabled.shouldBeTrue()
                beforePreview.shouldBeFalse()
                previewed.shouldBeFalse()
                reverted.shouldBeTrue()
                closed.shouldBeTrue()
            }

            it("On back ended") {
                backPreview.handleOnBackPressed()

                backPreview.isEnabled.shouldBeFalse()
                beforePreview.shouldBeFalse()
                previewed.shouldBeTrue()
                reverted.shouldBeFalse()
                closed.shouldBeTrue()
            }
        }
    })
