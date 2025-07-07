package artemis.agent

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual

class NotificationChannelTagTest :
    DescribeSpec({
        describe("NotificationChannelTag") {
            val tags =
                listOf(
                    "game info",
                    "connection",
                    "game over",
                    "border war",
                    "deep strike",
                    "new mission",
                    "mission progress",
                    "mission completed",
                    "production",
                    "attack",
                    "destroyed",
                    "reanimate",
                    "perfidy",
                )

            withData(nameFn = { it.second }, NotificationChannelTag.entries.zip(tags)) {
                (id, expectedTag) ->
                id.tag shouldBeEqual expectedTag
            }
        }
    })
