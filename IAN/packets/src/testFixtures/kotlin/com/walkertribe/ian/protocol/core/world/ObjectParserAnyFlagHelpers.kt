package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.world.Property
import com.walkertribe.ian.world.shouldBeUnspecified
import com.walkertribe.ian.world.shouldContainValue
import io.ktor.utils.io.core.writeText
import kotlinx.io.Sink
import kotlinx.io.writeIntLe

internal infix fun <V : Any> Property.ObjectProperty<V>.shouldMatch(flag: Flag<V>) {
    if (flag.enabled) {
        this shouldContainValue flag.value
    } else {
        this.shouldBeUnspecified()
    }
}

internal fun <E : Enum<E>> Sink.writeEnumFlags(firstFlag: Flag<E>, vararg flags: Flag<E>) {
    writeEnumFlag(firstFlag)
    writeEnumFlags(flags.iterator())
}

internal fun <E : Enum<E>> Sink.writeEnumFlags(flags: Array<Flag<E>>) {
    writeEnumFlags(flags.iterator())
}

private fun <E : Enum<E>> Sink.writeEnumFlags(flags: Iterator<Flag<E>>) {
    flags.forEach { writeEnumFlag(it) }
}

private fun <E : Enum<E>> Sink.writeEnumFlag(flag: Flag<E>) {
    if (flag.enabled) {
        writeByte(flag.value.ordinal.toByte())
    }
}

internal fun Sink.writeStringFlags(vararg flags: Flag<String>) {
    flags.forEach {
        if (it.enabled) {
            val str = it.value
            writeIntLe(str.length + 1)
            writeText(str, charset = Charsets.UTF_16LE)
            writeShort(0)
        }
    }
}
