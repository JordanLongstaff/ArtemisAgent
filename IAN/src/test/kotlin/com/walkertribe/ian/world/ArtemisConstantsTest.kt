package com.walkertribe.ian.world

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual

class ArtemisConstantsTest : DescribeSpec({
    describe("Constants") {
        it("MAX_TUBES == 6") {
            Artemis.MAX_TUBES shouldBeEqual 6
        }

        it("MAX_WARP == 4") {
            Artemis.MAX_WARP shouldBeEqual 4.toByte()
        }

        it("SHIP_COUNT == 8") {
            Artemis.SHIP_COUNT shouldBeEqual 8
        }

        it("SYSTEM_COUNT == 8") {
            Artemis.SYSTEM_COUNT shouldBeEqual 8
        }

        it("MAP_SIZE == 100,000") {
            Artemis.MAP_SIZE shouldBeEqual 100000
        }
    }
})
