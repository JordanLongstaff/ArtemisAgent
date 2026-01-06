package com.walkertribe.ian.grid

class Coordinate private constructor(val x: Int, val y: Int, val z: Int) : Comparable<Coordinate> {
    val index: Int by lazy { computeIndex(x, y, z) }

    override fun hashCode(): Int = index

    override fun compareTo(other: Coordinate): Int = compareValuesBy(this, other) { it.index }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Coordinate && index == other.index)

    override fun toString(): String = "Coordinate #${index + 1}: ($x, $y, $z)"

    companion object {
        private const val MAX_XY: Byte = 5
        private const val MAX_Z: Byte = 10
        const val COUNT = MAX_XY * MAX_XY * MAX_Z

        val ALL: List<Coordinate> by lazy {
            List(COUNT) {
                val x = it / MAX_Z / MAX_XY
                val y = it / MAX_Z % MAX_XY
                val z = it % MAX_Z
                Coordinate(x, y, z)
            }
        }

        operator fun invoke(x: Byte, y: Byte, z: Byte): Coordinate =
            ALL[computeIndex(x.toInt(), y.toInt(), z.toInt())]

        private fun computeIndex(x: Int, y: Int, z: Int): Int {
            require(x in 0 until MAX_XY) { "X-coordinate out of range: $x" }
            require(y in 0 until MAX_XY) { "Y-coordinate out of range: $y" }
            require(z in 0 until MAX_Z) { "Z-coordinate out of range: $z" }
            return x * MAX_XY * MAX_Z + y * MAX_Z + z
        }
    }
}
