package artemis.agent

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.checkAll

class NotificationInfoTest :
    DescribeSpec({
        describe("NotificationInfo") {
            describe("Constructor") {
                it("Default argument for ongoing") {
                    checkAll<NotificationChannelTag, String, String> { channel, title, message ->
                        val notificationInfo = NotificationInfo(channel, title, message)
                        notificationInfo.channel shouldBeEqual channel
                        notificationInfo.title shouldBeEqual title
                        notificationInfo.message shouldBeEqual message
                        notificationInfo.ongoing.shouldBeFalse()
                    }
                }

                it("Explicit argument for ongoing") {
                    checkAll<NotificationChannelTag, String, String, Boolean> {
                        channel,
                        title,
                        message,
                        ongoing ->
                        val notificationInfo = NotificationInfo(channel, title, message, ongoing)
                        notificationInfo.channel shouldBeEqual channel
                        notificationInfo.title shouldBeEqual title
                        notificationInfo.message shouldBeEqual message
                        notificationInfo.ongoing shouldBeEqual ongoing
                    }
                }
            }
        }
    })
