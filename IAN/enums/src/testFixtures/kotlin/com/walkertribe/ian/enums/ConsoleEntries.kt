package com.walkertribe.ian.enums

private val allConsoles by lazy {
    sortedSetOf(
        comparator = compareBy { it.index },
        Console.MAIN_SCREEN,
        Console.COMMUNICATIONS,
        Console.SINGLE_SEAT_CRAFT,
    )
}

val Console.Companion.entries
    get() = allConsoles
