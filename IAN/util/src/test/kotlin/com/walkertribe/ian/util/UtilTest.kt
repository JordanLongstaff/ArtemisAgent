package com.walkertribe.ian.util

import com.walkertribe.ian.util.Util.caretToNewline
import com.walkertribe.ian.util.Util.joinSpaceDelimited
import com.walkertribe.ian.util.Util.splitSpaceDelimited
import com.walkertribe.ian.util.Util.toHex
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.alphanumeric
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.bytes
import io.kotest.property.exhaustive.ints
import io.kotest.property.exhaustive.map

class UtilTest :
    DescribeSpec({
        val hexChars = "0123456789abcdef"

        describe("Split space delimited") {
            it("Tokens") {
                Arb.string()
                    .filter { it.isNotBlank() }
                    .checkAll { string ->
                        val s = string.trim().replace(Regex("\\s+"), " ")

                        val tokens = string.splitSpaceDelimited()
                        val tokenCount = s.count { it == ' ' } + 1
                        tokens.size shouldBeEqual tokenCount

                        val spaceIndices = s.indices.filter { s[it] == ' ' } + listOf(s.length)
                        val tokenLengths =
                            spaceIndices.mapIndexed { i, index ->
                                index - 1 - if (i == 0) -1 else spaceIndices[i - 1]
                            }
                        tokens.map { it.length } shouldContainExactly tokenLengths

                        var startIndex = 0
                        tokens.forEachIndexed { index, token ->
                            token shouldBeEqual s.substring(startIndex, spaceIndices[index])
                            startIndex += tokenLengths[index] + 1
                        }

                        tokens.joinToString(" ") shouldBeEqual s
                    }
            }

            it("Blank string") {
                Exhaustive.ints(0..1000)
                    .map { String(CharArray(it) { ' ' }) }
                    .checkAll { string -> string.splitSpaceDelimited().shouldBeEmpty() }
            }
        }

        describe("Join space delimited") {
            it("Empty collection") { emptyList<String>().joinSpaceDelimited() shouldBeEqual "" }

            it("Non-empty collection") {
                Arb.list(Arb.string(codepoints = Codepoint.alphanumeric()), 1..100).checkAll {
                    tokens ->
                    val joined = tokens.joinSpaceDelimited()

                    val totalLength = tokens.sumOf { it.length } + tokens.size - 1
                    joined.length shouldBeEqual totalLength.coerceAtLeast(0)

                    var startIndex = 0
                    tokens.forEach { token ->
                        if (startIndex > 0) {
                            joined[startIndex - 1] shouldBeEqual ' '
                        }

                        val endIndex = startIndex + token.length
                        joined.substring(startIndex, endIndex) shouldBeEqual token
                        startIndex = endIndex + 1
                    }

                    joined.split(' ') shouldContainExactly tokens
                }
            }
        }

        it("Caret to newline") {
            Arb.string().checkAll { str ->
                val caretToNewline = str.caretToNewline()
                str.length shouldBeEqual caretToNewline.length
                str.zip(caretToNewline).forEach { (inChar, outChar) ->
                    outChar shouldBeEqual if (inChar == '^') '\n' else inChar
                }
            }
        }

        it("Byte to hex string") {
            Exhaustive.bytes().checkAll { byte ->
                val expectedHexA = hexChars[(byte.toInt() shr 4) and 0xf]
                val expectedHexB = hexChars[byte.toInt() and 0xf]
                val hex = "$expectedHexA$expectedHexB"

                byte.toHex() shouldBeEqual hex
                hex.toInt(16).toByte() shouldBeEqual byte
            }
        }
    })
