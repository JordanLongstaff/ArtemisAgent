package com.walkertribe.ian.enums

/**
 * The bridge consoles.
 *
 * @author rjwut
 */
@JvmInline
value class Console private constructor(val index: Int) {
    companion object {
        val MAIN_SCREEN = Console(0)
        val COMMUNICATIONS = Console(5)
        val SINGLE_SEAT_CRAFT = Console(6)
    }
}
