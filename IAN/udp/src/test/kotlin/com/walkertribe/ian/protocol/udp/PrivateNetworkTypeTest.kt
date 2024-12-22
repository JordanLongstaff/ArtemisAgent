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

class PrivateNetworkTypeTest : DescribeSpec({
    describe("PrivateNetworkType") {
        val arbByte = Arb.nonNegativeInt(max = 0xFF)

        data class TestSuite(
            val testName: String,
            val firstByte: Int,
            val arbBytes: Arb<Triple<Int, Int, Int>>,
            val expectedType: PrivateNetworkType,
            val expectedBroadcastAddress: String,
        )

        val allTestSuites = arrayOf(
            TestSuite(
                "24-bit block",
                10,
                Arb.triple(arbByte, arbByte, arbByte),
                PrivateNetworkType.TWENTY_FOUR_BIT_BLOCK,
                "10.255.255.255",
            ),
            TestSuite(
                "20-bit block",
                172,
                Arb.triple(Arb.int(16..31), arbByte, arbByte),
                PrivateNetworkType.TWENTY_BIT_BLOCK,
                "172.31.255.255",
            ),
            TestSuite(
                "16-bit block",
                192,
                Arb.triple(Arb.of(168), arbByte, arbByte),
                PrivateNetworkType.SIXTEEN_BIT_BLOCK,
                "192.168.255.255",
            ),
        )
        allTestSuites.map {
            it.expectedType
        } shouldContainExactlyInAnyOrder PrivateNetworkType.entries

        val invalidTestSuites = listOf(
            "Too few bytes" to Arb.intArray(Arb.nonNegativeInt(3), arbByte),
            "Too many bytes" to Arb.intArray(Arb.int(5..UShort.MAX_VALUE.toInt()), arbByte),
            "Not a private address" to Arb.bind(
                Arb.pair(arbByte, arbByte).filter { (a, b) ->
                    a != 10 && (a != 172 || b !in 16..31) && (a != 192 || b != 168)
                },
                Arb.pair(arbByte, arbByte),
            ) { (a, b), (c, d) -> intArrayOf(a, b, c, d) },
        )

        allTestSuites.forEachIndexed { suiteIndex, suite ->
            val first = suite.firstByte
            describe(suite.testName) {
                withData(
                    nameFn = {
                        "${if (it.second) "Matches" else "Does not match"} ${it.first.testName}"
                    },
                    allTestSuites.mapIndexed { index, otherSuite ->
                        otherSuite to (index == suiteIndex)
                    },
                ) { (testSuite, shouldMatch) ->
                    suite.arbBytes.checkAll { (second, third, fourth) ->
                        val address = intArrayOf(first, second, third, fourth).joinToString(".")
                        testSuite.expectedType.match(address) shouldBeEqual shouldMatch
                    }
                }

                it("Defines correct network type") {
                    suite.arbBytes.checkAll { (second, third, fourth) ->
                        val address = intArrayOf(first, second, third, fourth).joinToString(".")
                        PrivateNetworkType.of(address).shouldNotBeNull() shouldBeEqual
                            suite.expectedType
                    }
                }

                it("Broadcast address: ${suite.expectedBroadcastAddress}") {
                    suite.expectedType.broadcastAddress shouldBeEqual suite.expectedBroadcastAddress
                }

                describe("Does not match invalid address") {
                    withData(nameFn = { it.first }, invalidTestSuites) {
                        it.second.checkAll { bytes ->
                            suite.expectedType.match(bytes.joinToString(".")).shouldBeFalse()
                        }
                    }
                }
            }
        }

        describe("Does not exist for invalid address") {
            withData(nameFn = { it.first }, invalidTestSuites) {
                it.second.checkAll { bytes ->
                    PrivateNetworkType.of(bytes.joinToString(".")).shouldBeNull()
                }
            }
        }
    }
})
