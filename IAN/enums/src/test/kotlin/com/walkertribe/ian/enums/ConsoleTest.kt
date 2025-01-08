package com.walkertribe.ian.enums

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual

class ConsoleTest :
    DescribeSpec({
        describe("Console") {
            describe("Constants") {
                withData(
                    nameFn = { "${it.first} = ${it.third}" },
                    Triple("MAIN_SCREEN", Console.MAIN_SCREEN, 0),
                    Triple("COMMUNICATIONS", Console.COMMUNICATIONS, 5),
                    Triple("SINGLE_SEAT_CRAFT", Console.SINGLE_SEAT_CRAFT, 6),
                ) { (_, console, expectedValue) ->
                    console.index shouldBeEqual expectedValue
                }
            }
        }
    })
