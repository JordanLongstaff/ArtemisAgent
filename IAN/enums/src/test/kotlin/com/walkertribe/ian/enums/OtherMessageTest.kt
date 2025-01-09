package com.walkertribe.ian.enums

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

class OtherMessageTest :
    DescribeSpec({
        val noArgMessages =
            listOf(
                OtherMessage.Hail to 0,
                OtherMessage.TurnToHeading0 to 1,
                OtherMessage.TurnToHeading90 to 2,
                OtherMessage.TurnToHeading180 to 3,
                OtherMessage.TurnToHeading270 to 4,
                OtherMessage.TurnLeft10Degrees to 5,
                OtherMessage.TurnRight10Degrees to 6,
                OtherMessage.TurnLeft25Degrees to 15,
                OtherMessage.TurnRight25Degrees to 16,
                OtherMessage.AttackNearestEnemy to 7,
                OtherMessage.ProceedToYourDestination to 8,
            )

        describe("Other message") {
            withData(nameFn = { it.first.toString() }, noArgMessages) {
                it.first.recipientType shouldBeEqual CommsRecipientType.OTHER
            }

            it("GoDefend") {
                Arb.int().checkAll { id ->
                    val goDefend = OtherMessage.GoDefend(id)
                    goDefend.recipientType shouldBeEqual CommsRecipientType.OTHER
                    goDefend.targetID shouldBeEqual id
                }
            }
        }

        describe("ID") {
            withData(nameFn = { "${it.first} = ${it.second}" }, noArgMessages) {
                (message, expectedID) ->
                message.id shouldBeEqual expectedID
            }

            it("GoDefend = 9") {
                Arb.int().checkAll { id ->
                    val goDefend = OtherMessage.GoDefend(id)
                    goDefend.id shouldBeEqual 9
                }
            }
        }
    })
