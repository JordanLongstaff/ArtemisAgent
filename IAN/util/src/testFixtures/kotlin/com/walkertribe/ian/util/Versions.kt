package com.walkertribe.ian.util

import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.nonNegativeInt

internal val defaultRange = 0..Int.MAX_VALUE
internal val defaultPart = Arb.nonNegativeInt()

fun Arb.Companion.version(): Arb<Version> = version(defaultPart)

fun Arb.Companion.version(
    majorRange: IntRange = defaultRange,
    minorRange: IntRange = defaultRange,
    patchRange: IntRange = defaultRange,
): Arb<Version> = version(Arb.int(majorRange), Arb.int(minorRange), Arb.int(patchRange))

fun Arb.Companion.version(
    majorArb: Arb<Int> = defaultPart,
    minorArb: Arb<Int> = defaultPart,
    patchArb: Arb<Int> = defaultPart,
): Arb<Version> =
    Arb.bind(majorArb, minorArb, patchArb) { major, minor, patch -> Version(major, minor, patch) }

fun Arb.Companion.version(
    major: Int,
    minorRange: IntRange = defaultRange,
    patchRange: IntRange = defaultRange,
): Arb<Version> = version(major, Arb.int(minorRange), Arb.int(patchRange))

fun Arb.Companion.version(
    major: Int,
    minorArb: Arb<Int> = defaultPart,
    patchArb: Arb<Int> = defaultPart,
): Arb<Version> = Arb.bind(minorArb, patchArb) { minor, patch -> Version(major, minor, patch) }

fun Arb.Companion.version(major: Int, minor: Int, patchRange: IntRange): Arb<Version> =
    version(major, minor, Arb.int(patchRange))

fun Arb.Companion.version(major: Int, minor: Int, patchArb: Arb<Int> = defaultPart): Arb<Version> =
    patchArb.map { patch -> Version(major, minor, patch) }
