package com.walkertribe.ian.util

object Util {
    private const val CARET = '^'
    private const val NEWLINE = '\n'

    /** Split the given String consisting of space-separated tokens into a List of String tokens. */
    fun String.splitSpaceDelimited(): List<String> = split(" ").filter(String::isNotEmpty)

    /** Reverses splitSpaceDelimited(). */
    fun Collection<*>.joinSpaceDelimited(): String = joinToString(" ")

    /** Converts carets (which Artemis uses for line breaks) to newline characters. */
    fun String.caretToNewline(): String = replace(CARET, NEWLINE)

    /** Converts the given byte to a hex String. */
    @OptIn(ExperimentalStdlibApi::class) fun Byte.toHex(): String = this.toHexString()
}
