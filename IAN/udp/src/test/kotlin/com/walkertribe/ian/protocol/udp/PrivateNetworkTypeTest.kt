package com.walkertribe.ian.protocol.udp

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.intArray
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.triple
import io.kotest.property.checkAll

class PrivateNetworkTypeTest :
    DescribeSpec({
        describe("PrivateNetworkType") {
            val arbByte = Arb.nonNegativeInt(max = 0xFF)

            data class TestSuite(
                val testName: String,
                val firstByte: Int,
                val arbBytes: Arb<Triple<Int, Int, Int>>,
                val expectedType: PrivateNetworkType,
                val expectedBroadcastAddress: String,
            )

            val allTestSuites =
                arrayOf(
                    TestSuite(
                        testName = "24-bit block",
                        firstByte = 10,
                        arbBytes = Arb.triple(arbByte, arbByte, arbByte),
                        expectedType = PrivateNetworkType.TWENTY_FOUR_BIT_BLOCK,
                        expectedBroadcastAddress = "10.255.255.255",
                    ),
                    TestSuite(
                        testName = "20-bit block",
                        firstByte = 172,
                        arbBytes = Arb.triple(Arb.int(16..31), arbByte, arbByte),
                        expectedType = PrivateNetworkType.TWENTY_BIT_BLOCK,
                        expectedBroadcastAddress = "172.31.255.255",
                    ),
                    TestSuite(
                        testName = "16-bit block",
                        firstByte = 192,
                        arbBytes = Arb.triple(Arb.of(168), arbByte, arbByte),
                        expectedType = PrivateNetworkType.SIXTEEN_BIT_BLOCK,
                        expectedBroadcastAddress = "192.168.255.255",
                    ),
                )
            allTestSuites.map { it.expectedType } shouldContainExactlyInAnyOrder
                PrivateNetworkType.entries

            val invalidTestSuites =
                listOf(
                    "Too few bytes" to Arb.intArray(Arb.nonNegativeInt(3), arbByte),
                    "Too many bytes" to Arb.intArray(Arb.int(5..UShort.MAX_VALUE.toInt()), arbByte),
                    "Not a private address" to
                        Arb.bind(
                            Arb.pair(arbByte, arbByte).filter { (a, b) ->
                                a != 10 && (a != 172 || b !in 16..31) && (a != 192 || b != 168)
                            },
                            Arb.pair(arbByte, arbByte),
                        ) { (a, b), (c, d) ->
                            intArrayOf(a, b, c, d)
                        },
                )

            allTestSuites.forEachIndexed { suiteIndex, suite ->
                val leading = suite.firstByte
                describe(suite.testName) {
                    withData(
                        nameFn = {
                            "${if (it.second) "Matches" else "Does not match"} ${it.first.testName}"
                        },
                        allTestSuites.mapIndexed { index, otherSuite ->
                            otherSuite to (index == suiteIndex)
                        },
                    ) { (testSuite, shouldMatch) ->
                        suite.arbBytes.checkAll { (first, second, third) ->
                            val address =
                                intArrayOf(leading, first, second, third).joinToString(".")
                            testSuite.expectedType.match(address) shouldBeEqual shouldMatch
                        }
                    }

                    it("Defines correct network type") {
                        suite.arbBytes.checkAll { (first, second, third) ->
                            val address =
                                intArrayOf(leading, first, second, third).joinToString(".")
                            PrivateNetworkType.of(address).shouldNotBeNull() shouldBeEqual
                                suite.expectedType
                        }
                    }

                    describe("Does not match invalid address") {
                        withData(nameFn = { it.first }, invalidTestSuites) { (_, arbParts) ->
                            arbParts.checkAll { parts ->
                                suite.expectedType.match(parts.joinToString(".")).shouldBeFalse()
                            }
                        }
                    }
                }
            }

            describe("Does not exist for invalid address") {
                withData(nameFn = { it.first }, invalidTestSuites) { (_, arbParts) ->
                    arbParts.checkAll { parts ->
                        PrivateNetworkType.of(parts.joinToString(".")).shouldBeNull()
                    }
                }
            }
        }
    })
