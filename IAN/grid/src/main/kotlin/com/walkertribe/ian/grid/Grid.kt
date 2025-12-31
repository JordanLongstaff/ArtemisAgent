package com.walkertribe.ian.grid

import com.walkertribe.ian.enums.ShipSystem
import com.walkertribe.ian.util.PathResolver
import okio.IOException
import okio.Path.Companion.toPath

class Grid internal constructor(nodes: List<Node>) {
    internal val nodeMap: Map<Coordinate, Node> = nodes.associateBy { it.coord }
    internal val nodeSystemMap: Map<ShipSystem, List<Node>> = nodes.groupBy { it.system }

    constructor(
        pathResolver: PathResolver,
        path: String,
    ) : this(
        try {
            pathResolver(path.toPath()) {
                Coordinate.ALL.mapNotNull { coord ->
                    skip(SKIP_BEFORE)
                    ShipSystem[readIntLe()]?.let { Node(coord, it) }.also { skip(SKIP_AFTER) }
                }
            }
        } catch (_: IOException) {
            emptyList()
        }
    )

    operator fun get(x: Int, y: Int, z: Int): Node? =
        nodeMap[Coordinate(x.toByte(), y.toByte(), z.toByte())]

    fun applyDamage(damage: Damage) {
        nodeMap[damage.coord]?.damage = damage.damage
    }

    fun clearDamage() {
        nodeMap.values.forEach { it.damage = 0f }
    }

    fun getNodesBySystem(system: ShipSystem): List<Node> = nodeSystemMap[system].orEmpty()

    fun getDamageBySystem(system: ShipSystem): Double =
        getNodesBySystem(system)
            .map { it.damage.coerceAtLeast(0f) }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0

    companion object {
        private const val SKIP_BEFORE = Float.SIZE_BYTES * 3L
        private const val SKIP_AFTER = Int.SIZE_BYTES * 4L
    }
}
