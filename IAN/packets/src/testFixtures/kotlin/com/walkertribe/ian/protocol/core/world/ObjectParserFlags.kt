package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.AlertStatus
import com.walkertribe.ian.enums.DriveType
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.enums.TubeState
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.map
import io.ktor.utils.io.core.writeText
import kotlinx.io.Sink
import kotlinx.io.writeFloatLe
import kotlinx.io.writeIntLe
import kotlinx.io.writeShortLe

internal typealias PositionFlags = FlagByte<Float, Float, Float, *, *, *, *, *>

internal typealias BaseFlags1 = FlagByte<String, Float, Float, Int, Int, Float, Float, Float>

internal typealias BaseFlags2 = FlagByte<Float, Float, Float, Float, Byte, Byte, *, *>

internal typealias CreatureFlags1 = FlagByte<Float, Float, Float, String, Float, Float, Float, Int>

internal typealias CreatureFlags2 = FlagByte<Int, Int, Int, Int, Int, Int, Float, Float>

internal typealias CreatureFlags3 = FlagByte<Byte, Int, *, *, *, *, *, *>

internal typealias NpcFlags1 = FlagByte<String, Float, Float, Float, Float, Int, Int, Float>

internal typealias NpcFlags2 = FlagByte<Float, Float, Float, Float, Float, Float, Byte, out Number>

internal typealias NpcFlags2Old = FlagByte<Float, Float, Float, Float, Float, Float, Byte, Short>

internal typealias NpcFlags2New = FlagByte<Float, Float, Float, Float, Float, Float, Byte, Byte>

internal typealias NpcFlags3 = FlagByte<Float, Float, Float, Float, Short, Byte, Int, Int>

internal typealias NpcFlags4 = FlagByte<Int, Int, Int, Byte, Byte, Byte, Byte, Float>

internal typealias NpcFlags5Old = FlagByte<Float, Float, Float, Float, Float, Float, Float, Float>

internal typealias NpcFlags5New = FlagByte<Float, Float, Byte, Byte, Float, Float, Float, Float>

internal typealias NpcFlags6Old = FlagByte<Float, Float, Float, Float, Float, Float, Float, *>

internal typealias NpcFlags6New = FlagByte<Float, Float, Float, Float, Float, Float, Float, Float>

internal typealias NpcFlags7 = FlagByte<Float, *, *, *, *, *, *, *>

internal typealias PlayerFlags1 = FlagByte<Int, Float, Float, Float, Float, Byte, Byte, Float>

internal typealias PlayerFlags2 = FlagByte<Short, Int, Int, Float, Float, Float, Float, Float>

internal typealias PlayerFlags3 =
    FlagByte<Float, Float, out Number, String, Float, Float, Float, Float>

internal typealias PlayerFlags3Old =
    FlagByte<Float, Float, Short, String, Float, Float, Float, Float>

internal typealias PlayerFlags3New =
    FlagByte<Float, Float, Byte, String, Float, Float, Float, Float>

internal typealias PlayerFlags4 = FlagByte<Int, AlertStatus, Float, Byte, Byte, Byte, Int, Int>

internal typealias PlayerFlags5 = FlagByte<DriveType, Int, Float, Byte, Float, Byte, Int, Byte>

internal typealias PlayerFlags6 = FlagByte<Int, Float, Float, Byte, Byte, *, *, *>

internal typealias UpgradesByteFlags = FlagByte<Byte, Byte, Byte, Byte, Byte, Byte, Byte, Byte>

internal typealias UpgradesShortFlags =
    FlagByte<Short, Short, Short, Short, Short, Short, Short, Short>

internal typealias UpgradesEndFlags = FlagByte<Short, Short, Short, Short, *, *, *, *>

internal typealias WeaponsV1Flags1 = FlagByte<Byte, Byte, Byte, Byte, Byte, Byte, Float, Float>

internal typealias WeaponsV1Flags2 =
    FlagByte<Float, Float, Float, Float, TubeState, TubeState, TubeState, TubeState>

internal typealias WeaponsV1Flags3 =
    FlagByte<
        TubeState,
        TubeState,
        OrdnanceType,
        OrdnanceType,
        OrdnanceType,
        OrdnanceType,
        OrdnanceType,
        OrdnanceType,
    >

internal typealias WeaponsV2Flags1 = FlagByte<Byte, Byte, Byte, Byte, Byte, Byte, Byte, Byte>

internal typealias WeaponsV2Flags2 =
    FlagByte<Float, Float, Float, Float, Float, Float, TubeState, TubeState>

internal typealias WeaponsV2Flags3 =
    FlagByte<
        TubeState,
        TubeState,
        TubeState,
        TubeState,
        OrdnanceType,
        OrdnanceType,
        OrdnanceType,
        OrdnanceType,
    >

internal typealias WeaponsV2Flags4 = FlagByte<OrdnanceType, OrdnanceType, *, *, *, *, *, *>

internal typealias EngineeringFloatFlags =
    FlagByte<Float, Float, Float, Float, Float, Float, Float, Float>

internal typealias EngineeringByteFlags = FlagByte<Byte, Byte, Byte, Byte, Byte, Byte, Byte, Byte>

internal typealias AnomalyFlags = FlagByte<Float, Float, Float, Int, Int, Int, Byte, Byte>

internal typealias AsteroidFlags = FlagByte<Float, Float, Float, *, *, *, *, *>

internal typealias NebulaFlags = FlagByte<Float, Float, Float, Float, Float, Float, Byte, *>

internal typealias TorpedoFlags = FlagByte<Float, Float, Float, Float, Float, Float, Int, Int>

internal typealias GenericMeshFlags1 = FlagByte<Float, Float, Float, Int, Int, Int, Float, Float>

internal typealias GenericMeshFlags2 =
    FlagByte<Float, Float, Float, Float, String, String, String, Float>

internal typealias GenericMeshFlags3 =
    FlagByte<Byte, Float, Float, Float, Float, Float, Float, Byte>

internal typealias GenericMeshFlags4 = FlagByte<String, String, Int, *, *, *, *, *>

internal typealias DroneFlags1 = FlagByte<Int, Float, Float, Float, Float, Float, Float, Int>

internal typealias DroneFlags2 = FlagByte<Float, *, *, *, *, *, *, *>

internal data class Flag<T>(val enabled: Boolean, val value: T)

internal data class FlagByte<T1, T2, T3, T4, T5, T6, T7, T8>(
    val flag1: Flag<T1>,
    val flag2: Flag<T2>,
    val flag3: Flag<T3>,
    val flag4: Flag<T4>,
    val flag5: Flag<T5>,
    val flag6: Flag<T6>,
    val flag7: Flag<T7>,
    val flag8: Flag<T8>,
) {
    val byteValue: Byte by lazy {
        var value = 0

        arrayOf(flag1, flag2, flag3, flag4, flag5, flag6, flag7, flag8).forEachIndexed { index, flag
            ->
            if (flag.enabled) {
                value += 1 shl index
            }
        }

        value.toByte()
    }
}

internal typealias AnyFlagByte = FlagByte<*, *, *, *, *, *, *, *>

internal val dummy = Flag(false, 0)

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

internal fun Sink.writeFloatFlags(vararg flags: Flag<Float>) {
    flags.forEach {
        if (it.enabled) {
            writeFloatLe(it.value)
        }
    }
}

internal fun Sink.writeIntFlags(vararg flags: Flag<Int>) {
    flags.forEach {
        if (it.enabled) {
            writeIntLe(it.value)
        }
    }
}

internal fun <E : Enum<E>> Sink.writeEnumFlags(vararg flags: Flag<E>) {
    flags.forEach {
        if (it.enabled) {
            writeByte(it.value.ordinal.toByte())
        }
    }
}

internal fun Sink.writeByteFlags(vararg flags: Flag<Byte>) {
    flags.forEach {
        if (it.enabled) {
            writeByte(it.value)
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
