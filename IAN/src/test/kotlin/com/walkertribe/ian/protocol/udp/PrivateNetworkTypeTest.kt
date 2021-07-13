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
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.triple
import io.kotest.property.checkAll

class PrivateNetworkTypeTest : DescribeSpec({
    describe("PrivateNetworkType") {
        data class TestSuite(
            val testName: String,
            val firstByte: Byte,
            val arbBytes: Arb<Triple<Byte, Byte, Byte>>,
            val expectedType: PrivateNetworkType,
            val expectedMatches: Triple<Boolean, Boolean, Boolean>,
        )

        val allTestSuites = arrayOf(
            TestSuite(
                "24-bit block",
                10,
                Arb.triple(Arb.byte(), Arb.byte(), Arb.byte()),
                PrivateNetworkType.TWENTY_FOUR_BIT_BLOCK,
                Triple(true, false, false),
            ),
            TestSuite(
                "20-bit block",
                -44,
                Arb.triple(Arb.byte(min = 16, max = 31), Arb.byte(), Arb.byte()),
                PrivateNetworkType.TWENTY_BIT_BLOCK,
                Triple(false, true, false),
            ),
            TestSuite(
                "16-bit block",
                -64,
                Arb.triple(Arb.of(-88), Arb.byte(), Arb.byte()),
                PrivateNetworkType.SIXTEEN_BIT_BLOCK,
                Triple(false, false, true),
            ),
        )
        allTestSuites.map {
            it.expectedType
        } shouldContainExactlyInAnyOrder PrivateNetworkType.entries

        val invalidTestSuites = listOf(
            "Too few bytes" to Arb.byteArray(Arb.nonNegativeInt(3), Arb.byte()),
            "Too many bytes" to Arb.byteArray(Arb.int(5..UShort.MAX_VALUE.toInt()), Arb.byte()),
            "Not a private address" to Arb.bind(
                Arb.pair(Arb.byte(), Arb.byte()).filter { (a, b) ->
                    a.toInt() != 10 &&
                        (a.toInt() != -44 || b !in 16..31) &&
                        (a.toInt() != -64 || b.toInt() != -88)
                },
                Arb.pair(Arb.byte(), Arb.byte()),
            ) { (a, b), (c, d) -> byteArrayOf(a, b, c, d) },
        )

        allTestSuites.forEach { suite ->
            describe(suite.testName) {
                withData(
                    nameFn = {
                        "${if (it.second) "Matches" else "Does not match"} ${it.first.testName}"
                    },
                    allTestSuites.zip(suite.expectedMatches.toList()),
                ) { (testSuite, shouldMatch) ->
                    suite.arbBytes.checkAll { (second, third, fourth) ->
                        val bytes = byteArrayOf(suite.firstByte, second, third, fourth)
                        testSuite.expectedType.match(bytes) shouldBeEqual shouldMatch
                    }
                }

                it("Defines correct network type") {
                    suite.arbBytes.checkAll { (second, third, fourth) ->
                        val bytes = byteArrayOf(suite.firstByte, second, third, fourth)
                        val networkType = PrivateNetworkType(bytes).shouldNotBeNull()
                        networkType shouldBeEqual suite.expectedType
                    }
                }

                describe("Does not match invalid address") {
                    withData(nameFn = { it.first }, invalidTestSuites) {
                        it.second.checkAll { bytes ->
                            suite.expectedType.match(bytes).shouldBeFalse()
                        }
                    }
                }
            }
        }

        describe("Does not exist for invalid address") {
            withData(nameFn = { it.first }, invalidTestSuites) {
                it.second.checkAll { bytes ->
                    PrivateNetworkType(bytes).shouldBeNull()
                }
            }
        }
    }
})
