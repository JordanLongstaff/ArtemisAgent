package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.world.Property
import com.walkertribe.ian.world.shouldBeUnspecified
import com.walkertribe.ian.world.shouldContainValue
import kotlinx.io.Sink
import kotlinx.io.writeIntLe
import kotlinx.io.writeShortLe

internal infix fun Property.BoolProperty.shouldMatch(flag: Flag<out Number>) {
    if (flag.enabled) {
        this shouldContainValue (flag.value.toInt() != 0)
    } else {
        this.shouldBeUnspecified()
    }
}

internal infix fun Property.IntProperty.shouldMatch(flag: Flag<out Number>) {
    if (flag.enabled) {
        this shouldContainValue flag.value.toInt()
    } else {
        this.shouldBeUnspecified()
    }
}

internal fun testBoolPropertyFlags(vararg pairs: Pair<Flag<out Number>, Property.BoolProperty>) {
    pairs.forEach { (flag, property) -> property shouldMatch flag }
}

internal fun testIntPropertyFlags(vararg pairs: Pair<Flag<Int>, Property.IntProperty>) {
    pairs.forEach { (flag, property) -> property shouldMatch flag }
}

internal fun Sink.writeIntFlags(vararg flags: Flag<Int>) {
    flags.forEach {
        if (it.enabled) {
            writeIntLe(it.value)
        }
    }
}

internal fun Sink.writeShortFlags(vararg flags: Flag<Short>) {
    flags.forEach {
        if (it.enabled) {
            writeShortLe(it.value)
        }
    }
}
