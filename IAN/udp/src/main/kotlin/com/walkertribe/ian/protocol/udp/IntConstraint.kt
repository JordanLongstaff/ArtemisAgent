package com.walkertribe.ian.protocol.udp

internal sealed interface IntConstraint {
    @JvmInline
    value class Equals(val value: Int) : IntConstraint {
        override fun check(int: Int): Boolean = int == value
    }

    class Range(val range: IntRange) : IntConstraint {
        override fun check(int: Int): Boolean = int in range
    }

    fun check(int: Int): Boolean
}
