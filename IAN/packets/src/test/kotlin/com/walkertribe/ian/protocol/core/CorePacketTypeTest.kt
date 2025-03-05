package com.walkertribe.ian.protocol.core

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlin.reflect.full.memberProperties

class CorePacketTypeTest :
    DescribeSpec({
        describe("CorePacketType") {
            CorePacketType::class.memberProperties.forEach { property ->
                var uppercase = false
                val expectedValue =
                    property.name.toCharArray().joinToString("") { ch ->
                        when {
                            uppercase -> {
                                uppercase = false
                                ch.toString()
                            }
                            ch == '_' -> {
                                uppercase = true
                                ""
                            }
                            else -> ch.lowercase()
                        }
                    }

                it("${property.name} = \"$expectedValue\"") {
                    property.getter.call().shouldNotBeNull() shouldBeEqual expectedValue
                }
            }
        }
    })
