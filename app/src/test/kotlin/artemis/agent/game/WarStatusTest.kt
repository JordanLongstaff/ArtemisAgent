package artemis.agent.game

import artemis.agent.R
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual

class WarStatusTest :
    DescribeSpec({
        describe("WarStatus") {
            val colors = listOf(R.color.connected, R.color.heartbeatLost, R.color.failedToConnect)

            withData(
                nameFn = { (status) -> status.name.let { it[0] + it.substring(1).lowercase() } },
                WarStatus.entries.zip(colors),
            ) { (status, color) ->
                status.backgroundColor shouldBeEqual color
            }
        }
    })
