package artemis.agent.game.biomechs

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual

class BiomechRageStatusTest :
    DescribeSpec({
        val expectedStatuses =
            arrayOf(
                BiomechRageStatus.NEUTRAL,
                BiomechRageStatus.HOSTILE,
                BiomechRageStatus.HOSTILE,
                BiomechRageStatus.HOSTILE,
                BiomechRageStatus.HOSTILE,
            )

        describe("BiomechRageStatus") {
            describe("Status from rage") {
                expectedStatuses.forEachIndexed { index, status ->
                    it("$index = $status") { BiomechRageStatus[index] shouldBeEqual status }
                }
            }
        }
    })
