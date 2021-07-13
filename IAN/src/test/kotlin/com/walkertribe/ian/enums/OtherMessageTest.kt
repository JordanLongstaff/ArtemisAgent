package com.walkertribe.ian.enums

import com.walkertribe.ian.world.ArtemisBase
import com.walkertribe.ian.world.ArtemisNpc
import com.walkertribe.ian.world.ArtemisPlayer
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.choose
import io.kotest.property.checkAll

class OtherMessageTest : DescribeSpec({
    val noArgMessages = listOf(
        OtherMessage.Hail,
        OtherMessage.TurnToHeading0,
        OtherMessage.TurnToHeading90,
        OtherMessage.TurnToHeading180,
        OtherMessage.TurnToHeading270,
        OtherMessage.TurnLeft10Degrees,
        OtherMessage.TurnRight10Degrees,
        OtherMessage.TurnLeft25Degrees,
        OtherMessage.TurnRight25Degrees,
        OtherMessage.AttackNearestEnemy,
        OtherMessage.ProceedToYourDestination,
    )

    val arbObject = Arb.choose(
        3 to Arb.bind<ArtemisBase>(),
        4 to Arb.bind<ArtemisNpc>(),
        1 to Arb.bind<ArtemisPlayer>(),
    )

    describe("Other message") {
        withData(nameFn = { it.toString() }, noArgMessages) {
            it.recipientType shouldBeEqual CommsRecipientType.OTHER
        }

        it("GoDefend") {
            arbObject.checkAll {
                collect(it.type)
                OtherMessage.GoDefend(it).recipientType shouldBeEqual CommsRecipientType.OTHER
            }
        }
    }

    describe("No argument") {
        withData(nameFn = { it.toString() }, noArgMessages) {
            it.argument shouldBeEqual CommsMessage.NO_ARG
        }
    }

    describe("Object ID argument") {
        it("GoDefend") {
            arbObject.checkAll {
                collect(it.type)
                OtherMessage.GoDefend(it).argument shouldBeEqual it.id
            }
        }
    }
})
