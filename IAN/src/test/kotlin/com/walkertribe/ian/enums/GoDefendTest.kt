package com.walkertribe.ian.enums

import com.walkertribe.ian.world.ArtemisBase
import com.walkertribe.ian.world.ArtemisNpc
import com.walkertribe.ian.world.ArtemisPlayer
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.choose
import io.kotest.property.checkAll

class GoDefendTest :
    DescribeSpec({
        val arbObject =
            Arb.choose(
                3 to Arb.bind<ArtemisBase>(),
                4 to Arb.bind<ArtemisNpc>(),
                1 to Arb.bind<ArtemisPlayer>(),
            )

        it("GoDefend helper extension") {
            arbObject.checkAll { target ->
                collect(target.type)

                val goDefend = OtherMessage.GoDefend(target)
                goDefend.shouldBeInstanceOf<OtherMessage.GoDefend>()
                goDefend.targetID shouldBeEqual target.id
            }
        }
    })
