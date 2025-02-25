package com.walkertribe.ian.protocol.core.world

import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.map

private fun <T> Arb.Companion.flag(arb: Arb<T>): Arb<Flag<T>> = Arb.bind(Arb.boolean(), arb, ::Flag)

internal fun <T> Arb.Companion.flags(arb: Arb<T>): Arb<FlagByte<T, *, *, *, *, *, *, *>> =
    Arb.flag(arb).map { FlagByte(it, dummy, dummy, dummy, dummy, dummy, dummy, dummy) }

internal fun <T1, T2> Arb.Companion.flags(
    arb1: Arb<T1>,
    arb2: Arb<T2>,
): Arb<FlagByte<T1, T2, *, *, *, *, *, *>> =
    Arb.bind(Arb.flag(arb1), Arb.flag(arb2)) { flag1, flag2 ->
        FlagByte(flag1, flag2, dummy, dummy, dummy, dummy, dummy, dummy)
    }

internal fun <T1, T2, T3> Arb.Companion.flags(
    arb1: Arb<T1>,
    arb2: Arb<T2>,
    arb3: Arb<T3>,
): Arb<FlagByte<T1, T2, T3, *, *, *, *, *>> =
    Arb.bind(Arb.flag(arb1), Arb.flag(arb2), Arb.flag(arb3)) { flag1, flag2, flag3 ->
        FlagByte(flag1, flag2, flag3, dummy, dummy, dummy, dummy, dummy)
    }

internal fun <T1, T2, T3, T4> Arb.Companion.flags(
    arb1: Arb<T1>,
    arb2: Arb<T2>,
    arb3: Arb<T3>,
    arb4: Arb<T4>,
): Arb<FlagByte<T1, T2, T3, T4, *, *, *, *>> =
    Arb.bind(Arb.flag(arb1), Arb.flag(arb2), Arb.flag(arb3), Arb.flag(arb4)) {
        flag1,
        flag2,
        flag3,
        flag4 ->
        FlagByte(flag1, flag2, flag3, flag4, dummy, dummy, dummy, dummy)
    }

internal fun <T1, T2, T3, T4, T5> Arb.Companion.flags(
    arb1: Arb<T1>,
    arb2: Arb<T2>,
    arb3: Arb<T3>,
    arb4: Arb<T4>,
    arb5: Arb<T5>,
): Arb<FlagByte<T1, T2, T3, T4, T5, *, *, *>> =
    Arb.bind(Arb.flag(arb1), Arb.flag(arb2), Arb.flag(arb3), Arb.flag(arb4), Arb.flag(arb5)) {
        flag1,
        flag2,
        flag3,
        flag4,
        flag5 ->
        FlagByte(flag1, flag2, flag3, flag4, flag5, dummy, dummy, dummy)
    }

internal fun <T1, T2, T3, T4, T5, T6> Arb.Companion.flags(
    arb1: Arb<T1>,
    arb2: Arb<T2>,
    arb3: Arb<T3>,
    arb4: Arb<T4>,
    arb5: Arb<T5>,
    arb6: Arb<T6>,
): Arb<FlagByte<T1, T2, T3, T4, T5, T6, *, *>> =
    Arb.bind(
        Arb.flag(arb1),
        Arb.flag(arb2),
        Arb.flag(arb3),
        Arb.flag(arb4),
        Arb.flag(arb5),
        Arb.flag(arb6),
    ) { flag1, flag2, flag3, flag4, flag5, flag6 ->
        FlagByte(flag1, flag2, flag3, flag4, flag5, flag6, dummy, dummy)
    }

internal fun <T1, T2, T3, T4, T5, T6, T7> Arb.Companion.flags(
    arb1: Arb<T1>,
    arb2: Arb<T2>,
    arb3: Arb<T3>,
    arb4: Arb<T4>,
    arb5: Arb<T5>,
    arb6: Arb<T6>,
    arb7: Arb<T7>,
): Arb<FlagByte<T1, T2, T3, T4, T5, T6, T7, *>> =
    Arb.bind(
        Arb.flag(arb1),
        Arb.flag(arb2),
        Arb.flag(arb3),
        Arb.flag(arb4),
        Arb.flag(arb5),
        Arb.flag(arb6),
        Arb.flag(arb7),
    ) { flag1, flag2, flag3, flag4, flag5, flag6, flag7 ->
        FlagByte(flag1, flag2, flag3, flag4, flag5, flag6, flag7, dummy)
    }

internal fun <T1, T2, T3, T4, T5, T6, T7, T8> Arb.Companion.flags(
    arb1: Arb<T1>,
    arb2: Arb<T2>,
    arb3: Arb<T3>,
    arb4: Arb<T4>,
    arb5: Arb<T5>,
    arb6: Arb<T6>,
    arb7: Arb<T7>,
    arb8: Arb<T8>,
): Arb<FlagByte<T1, T2, T3, T4, T5, T6, T7, T8>> =
    Arb.bind(
        Arb.flag(arb1),
        Arb.flag(arb2),
        Arb.flag(arb3),
        Arb.flag(arb4),
        Arb.flag(arb5),
        Arb.flag(arb6),
        Arb.flag(arb7),
        Arb.flag(arb8),
    ) { flag1, flag2, flag3, flag4, flag5, flag6, flag7, flag8 ->
        FlagByte(flag1, flag2, flag3, flag4, flag5, flag6, flag7, flag8)
    }
