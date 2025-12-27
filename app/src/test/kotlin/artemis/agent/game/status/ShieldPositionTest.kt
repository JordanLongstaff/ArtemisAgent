package artemis.agent.game.status

import artemis.agent.R
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual

class ShieldPositionTest :
    DescribeSpec({
        describe("ShieldPosition") {
            withData(
                nameFn = { it.first.name },
                ShieldPosition.FRONT to R.string.front_shield,
                ShieldPosition.REAR to R.string.rear_shield,
            ) { (position, expectedStringId) ->
                position.stringId shouldBeEqual expectedStringId
            }
        }
    })
