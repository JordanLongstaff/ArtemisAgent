package com.walkertribe.ian.grid

import com.walkertribe.ian.enums.ShipSystem

class Node(internal val coord: Coordinate, val system: ShipSystem) {
    var damage: Float = -1f
        internal set

    override fun hashCode(): Int = coord.hashCode()

    override fun equals(other: Any?): Boolean =
        this === other || (other is Node && coord == other.coord)

    override fun toString(): String = "$system $coord"
}
