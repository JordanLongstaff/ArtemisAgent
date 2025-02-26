package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.world.Property
import com.walkertribe.ian.world.shouldBeUnspecified
import com.walkertribe.ian.world.shouldContainValue
import kotlinx.io.Sink

internal infix fun Property.ByteProperty.shouldMatch(flag: Flag<Byte>) {
    shouldMatch(flag, -1)
}

internal fun Property.ByteProperty.shouldMatch(flag: Flag<Byte>, unknownValue: Byte) {
    if (flag.enabled) {
        this shouldContainValue flag.value
    } else {
        this.shouldBeUnspecified(unknownValue)
    }
}

internal fun testBytePropertyFlags(pairs: Iterable<Pair<Flag<Byte>, Property.ByteProperty>>) {
    pairs.forEach { (flag, property) -> property shouldMatch flag }
}

internal fun testBytePropertyFlags(
    vararg triples: Triple<Flag<Byte>, Property.ByteProperty, Byte?>
) {
    triples.forEach { (flag, property, unknownValue) ->
        if (unknownValue != null) {
            property.shouldMatch(flag, unknownValue)
        } else {
            property shouldMatch flag
        }
    }
}

internal fun Sink.writeByteFlags(firstFlag: Flag<Byte>, vararg flags: Flag<Byte>) {
    writeByteFlag(firstFlag)
    writeByteFlags(flags.iterator())
}

internal fun Sink.writeByteFlags(flags: Array<Flag<Byte>>) {
    writeByteFlags(flags.iterator())
}

private fun Sink.writeByteFlags(flags: Iterator<Flag<Byte>>) {
    flags.forEach { writeByteFlag(it) }
}

private fun Sink.writeByteFlag(flag: Flag<Byte>) {
    if (flag.enabled) {
        writeByte(flag.value)
    }
}
