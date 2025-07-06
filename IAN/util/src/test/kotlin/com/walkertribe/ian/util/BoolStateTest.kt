package com.walkertribe.ian.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.positiveInt
import io.kotest.property.checkAll
import io.ktor.utils.io.core.buildPacket

class BoolStateTest :
    DescribeSpec({
        describe("BoolState") {
            describe("From value") {
                withData(
                    nameFn = { (value, boolState) ->
                        @Suppress("NullableToStringCall") "$value: $boolState"
                    },
                    true to BoolState.True,
                    false to BoolState.False,
                    null to BoolState.Unknown,
                ) { (value, expectedBoolState) ->
                    BoolState(value) shouldBeEqual expectedBoolState
                }
            }

            describe("Get boolean value") {
                withData(
                    nameFn = { (boolState, value) -> "$boolState -> $value" },
                    BoolState.True to true,
                    BoolState.False to false,
                    BoolState.Unknown to false,
                ) { (boolState, expectedBooleanValue) ->
                    boolState.booleanValue shouldBeEqual expectedBooleanValue
                }
            }

            describe("Is known") {
                withData(
                    nameFn = { (boolState, known) ->
                        val prefix = if (known) "" else "un"
                        "${boolState?.toString() ?: "Null"} is ${prefix}known"
                    },
                    BoolState.True to true,
                    BoolState.False to true,
                    BoolState.Unknown to false,
                    null to false,
                ) { (boolState, expectedToBeKnown) ->
                    boolState.isKnown shouldBeEqual expectedToBeKnown
                }
            }

            describe("Read from packet") {
                withData(
                    nameFn = { it.first.toString() },
                    BoolState.True to 1.toByte(),
                    BoolState.False to 0.toByte(),
                ) { (boolState, value) ->
                    Arb.positiveInt(max = UShort.MAX_VALUE.toInt()).checkAll { count ->
                        buildPacket { repeat(count) { writeByte(value) } }
                            .use { packet -> packet.readBoolState(count) shouldBeEqual boolState }
                    }
                }
            }
        }
    })
