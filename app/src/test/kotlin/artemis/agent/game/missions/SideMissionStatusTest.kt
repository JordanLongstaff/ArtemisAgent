package artemis.agent.game.missions

import artemis.agent.R
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual

class SideMissionStatusTest :
    DescribeSpec({
        describe("SideMissionStatus") {
            val expectedRes =
                intArrayOf(
                    R.color.allyStatusBackgroundBlue,
                    R.color.allyStatusBackgroundOrange,
                    R.color.allyStatusBackgroundYellow,
                )

            withData(SideMissionStatus.entries) { status ->
                status.backgroundColor shouldBeEqual expectedRes[status.ordinal]
            }
        }
    })
