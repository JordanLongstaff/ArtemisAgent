package com.walkertribe.ian.grid

import com.walkertribe.ian.enums.ShipSystem
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.orNull
import java.io.File

private const val NULL_SYSTEM = -2
private const val BLOCK_SIZE_INTS = 8
private const val SYSTEM_OFFSET = 3

fun arbitraryShipSystemsList(
    systemGen: Gen<ShipSystem?> = Arb.enum<ShipSystem>().orNull()
): List<ShipSystem?> =
    systemGen.generate(RandomSource.default()).take(Coordinate.COUNT).map { it.value }.toList()

fun arbitraryNodesList(systemGen: Gen<ShipSystem?> = Arb.enum<ShipSystem>().orNull()) =
    Coordinate.ALL.zip(arbitraryShipSystemsList(systemGen)).mapNotNull { (coord, system) ->
        system?.let { Node(coord, it) }
    }

fun Grid.Companion.arbitrary(systemGen: Gen<ShipSystem?> = Arb.enum<ShipSystem>()): Grid =
    Grid(arbitraryNodesList(systemGen))

fun Grid.writeToSntFile(path: File): Grid {
    val sntValues =
        Coordinate.ALL.flatMap { coord ->
                buildList {
                    repeat(BLOCK_SIZE_INTS) { add(0) }
                    this[SYSTEM_OFFSET] = nodeMap[coord]?.system?.value ?: NULL_SYSTEM
                }
            }
            .flatMap { num -> List(Int.SIZE_BYTES) { i -> num.shr(i * Byte.SIZE_BITS).toByte() } }
            .toByteArray()

    path.writeBytes(sntValues)
    return this
}
