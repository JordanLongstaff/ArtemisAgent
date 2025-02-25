package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.world.Property
import com.walkertribe.ian.world.shouldBeUnspecified
import com.walkertribe.ian.world.shouldContainValue
import kotlinx.io.Sink
import kotlinx.io.writeFloatLe

internal infix fun Property.FloatProperty.shouldMatch(flag: Flag<Float>) {
    if (flag.enabled) {
        this shouldContainValue flag.value
    } else {
        this.shouldBeUnspecified()
    }
}

internal fun testFloatPropertyFlags(vararg pairs: Pair<Flag<Float>, Property.FloatProperty>) {
    pairs.forEach { (flag, property) -> property shouldMatch flag }
}

internal fun Sink.writeFloatFlags(firstFlag: Flag<Float>, vararg flags: Flag<Float>) {
    writeFloatFlag(firstFlag)
    writeFloatFlags(flags.asIterable())
}

internal fun Sink.writeFloatFlags(flags: Array<Flag<Float>>) {
    writeFloatFlags(flags.asIterable())
}

private fun Sink.writeFloatFlags(flags: Iterable<Flag<Float>>) {
    flags.forEach { writeFloatFlag(it) }
}

private fun Sink.writeFloatFlag(flag: Flag<Float>) {
    if (flag.enabled) {
        writeFloatLe(flag.value)
    }
}
