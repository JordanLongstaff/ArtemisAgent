package artemis.agent.cpu

import android.util.Log
import artemis.agent.AgentViewModel
import artemis.agent.game.ObjectEntry
import artemis.agent.game.route.RouteEntry
import com.walkertribe.ian.enums.DriveType
import com.walkertribe.ian.world.Artemis
import com.walkertribe.ian.world.ArtemisBlackHole
import com.walkertribe.ian.world.ArtemisCreature
import com.walkertribe.ian.world.ArtemisMine
import com.walkertribe.ian.world.ArtemisObject
import com.walkertribe.ian.world.ArtemisShielded
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

/**
 * Handles all routing calculations among potential destinations and waypoints. The algorithm to
 * calculate the optimal path accounts for dangerous obstacles that must be avoided. The algorithm
 * to determine the optimal ordering of waypoints is implemented using an ant colony optimization.
 */
internal class RoutingGraph(
    private val viewModel: AgentViewModel,
    private val source: ArtemisShielded<*>,
) {
    // Set of all waypoints to visit and their precedence over other waypoints
    private val paths = ConcurrentHashMap<ObjectEntry<*>, CopyOnWriteArraySet<ObjectEntry<*>>>()

    // Pre-calculated costs from one waypoint to another
    private var costs = ConcurrentHashMap<Int, Float>()

    // Current minimum route cost
    private var minimumCost: Float = Float.POSITIVE_INFINITY

    // All objects to avoid - mines, black holes and Typhons
    private val objectsToAvoid =
        ConcurrentHashMap<ArtemisObject<*>, CopyOnWriteArraySet<ArtemisObject<*>>>()

    // Pheromone matrix for the ant colony
    private var pheromones = ConcurrentHashMap<Int, Double>()

    // Pheromone trail for first solution - this controls pheromone decay
    private var firstPheromone: Double? = null

    /** Clears all data from the current route calculation. */
    fun resetGraph() {
        paths.clear()
    }

    /**
     * Adds a waypoint to the route, optionally adding a second waypoint with the former taking
     * precedence over the latter.
     */
    fun addPath(src: ObjectEntry<*>, dst: ObjectEntry<*>? = null) {
        // Get the current set of waypoints over which the first waypoint has precedence
        // If it wasn't registered, do so and increment the size counter
        val targets = paths.getOrPut(src) { CopyOnWriteArraySet<ObjectEntry<*>>() }

        // If there's a second waypoint, add it to the aforementioned set
        dst?.also(targets::add)
    }

    /**
     * Removes all waypoints that have no precedence over other waypoints, but are preceded by some
     * other waypoint. This improves accuracy and efficiency in the eventual route calculation.
     */
    fun purgePaths() {
        val destinations = mutableSetOf<ObjectEntry<*>>()
        paths.values.forEach(destinations::addAll)
        destinations.forEach {
            val laterPoints = paths[it] ?: return@forEach
            if (laterPoints.isEmpty()) paths.remove(it)
        }
    }

    /** Tests a previous route to see if it still traverses all necessary paths. */
    fun testRoute(previousRoute: List<RouteEntry>?) {
        val currentNodes = paths.keys.toMutableSet()
        previousRoute?.onEach { (point) ->
            currentNodes.remove(point)
            paths[point]
                ?.filter { target ->
                    paths[target]?.isNotEmpty() == true ||
                        currentNodes.none { otherNode ->
                            paths[otherNode]?.contains(target) == true
                        }
                }
                ?.also(currentNodes::addAll)
        }
        if (currentNodes.isNotEmpty()) {
            minimumCost = Float.POSITIVE_INFINITY
        }
    }

    /** Pre-process the list of objects to avoid based on user settings. */
    fun preprocessObjectsToAvoid() {
        // Start by fetching all of the objects and their neighbours
        val nearObjects = viewModel.findNeighbors()

        // Then spread out search for nearby objects to form "clusters" in which proximity becomes
        // transitive - this is important for minefields etc.
        val clusterSets = mutableListOf<CopyOnWriteArraySet<ArtemisObject<*>>>()
        while (nearObjects.isNotEmpty()) {
            // Grab an object from the set - exit loop if none are left
            val firstKey = nearObjects.keys.first()

            // Remove object from the set to avoid processing it repeatedly
            val openSet = nearObjects.remove(firstKey)

            // If this object already belongs to a cluster, skip it
            if (openSet == null || clusterSets.any { it.contains(firstKey) }) continue

            // Start a new cluster beginning with this object
            val newSet = CopyOnWriteArraySet<ArtemisObject<*>>()
            newSet.add(firstKey)

            while (openSet.isNotEmpty()) {
                // Find a nearby object - if none left, finalize cluster
                val nextKey = openSet.first()

                // Add all of its neighbours to the cluster
                nearObjects[nextKey]?.also { openSet.addAll(it.filterNot(newSet::contains)) }

                // Add it to the cluster as well
                newSet.add(nextKey)
                openSet.remove(nextKey)
            }

            clusterSets.add(newSet)
        }

        // Finally, map every object to the cluster that contains it
        clusterSets.forEach { objSet -> objSet.forEach { obj -> objectsToAvoid[obj] = objSet } }
    }

    /** Removes an obstacle from the set of objects to avoid once it is deleted. */
    fun removeObstacle(obj: ArtemisObject<*>) {
        objectsToAvoid.remove(obj)
    }

    /**
     * Pre-process all route costs between every pair of waypoints, as well as route costs from the
     * player ship to each individual waypoint.
     */
    fun preprocessCosts() {
        // Get every waypoint to eventually visit, including those preceded by others
        val allTargets =
            paths.values
                .fold(paths.keys) { acc, targets -> acc.union(targets).toMutableSet() }
                .toList()

        costs.clear()
        if (!source.hasPosition) return

        allTargets.forEachIndexed { i, src ->
            val srcObj = src.obj
            if (!srcObj.hasPosition) return@forEachIndexed
            allTargets.subList(i + 1, allTargets.size).forEach { dst ->
                // By this point, we have fetched two distinct objects and are ready to calculate
                // the cost of the path between them. We are fetching these objects from an ordering
                // of some kind and avoiding paths we have already calculated because path costs go
                // both ways, so there's no need to double-count.
                val dstObj = dst.obj
                if (!dstObj.hasPosition) return@forEach

                val routeCost = calculateRouteCost(srcObj, dstObj)

                costs[generateRouteKey(srcObj, dstObj)] = routeCost
                costs[generateRouteKey(dstObj, srcObj)] = routeCost
            }

            // Now we calculate the path cost from the player ship to each waypoint
            costs[generateRouteKey(source, srcObj)] = calculateRouteCost(source, srcObj)
        }
    }

    /** Releases a bunch of "ants" to search in parallel to attempt to find the optimal route. */
    suspend fun searchForRoute(): List<RouteEntry>? {
        // Find optimal route among a random selection
        val (bestPathList, bestPathCost) =
            List(TOTAL_ANTS) {
                    // Let each ant search in parallel coroutines
                    viewModel.cpu.async { generateRouteCandidate() }
                }
                .awaitAll()
                .minByOrNull { it.second }
                ?.takeUnless {
                    // If we already found a better one previously, ignore this one
                    it.second > minimumCost
                } ?: return null

        val bestPath = bestPathList.map(::RouteEntry)
        if (bestPath.isNotEmpty()) {
            var currentNode = source

            // If this is our first time generating an optimal route, derive our initial pheromone
            // value for decay on subsequent calculations
            var firstP = firstPheromone
            if (firstP == null) {
                firstP = GRID_SECTOR_SIZE / bestPathCost / bestPath.size
                firstPheromone = firstP
            }

            // Each entry in the route needs both its pheromones and its key updated
            for (entry in bestPath) {
                val nextNode = entry.objEntry.obj
                val nextKey = generateRouteKey(currentNode, nextNode)
                entry.pathKey = nextKey
                val oldPheromone = pheromones[nextKey] ?: firstP
                val nextPheromone = PHI / bestPathCost + MINUS_PHI * oldPheromone
                pheromones[nextKey] = nextPheromone
                pheromones[swapKey(nextKey)] = nextPheromone
                currentNode = nextNode
            }
        }

        // Update the current minimum cost values
        minimumCost = bestPathCost

        return bestPath
    }

    private fun generateRouteCandidate(): Pair<List<ObjectEntry<*>>, Float> {
        val currentNodes = paths.keys.toMutableSet()
        var totalCost = 0f
        val currentPath = mutableListOf<ObjectEntry<*>>()

        while (currentNodes.isNotEmpty()) {
            // Get current location in the path we're building, starting at the player ship
            val currentNode = currentPath.lastOrNull()?.obj ?: source

            // Map each possible next waypoint to visit to the route key of the path to it
            val nodesAndKeys =
                currentNodes.map { node -> node to generateRouteKey(currentNode, node.obj) }

            // Choose one at random - waypoints with a higher heuristic value are more likely to be
            // chosen
            val (nextNode, nextKey) =
                nodesAndKeys.randomByWeight { (_, key) ->
                    // Heuristic: divide the current pheromone value of the path by its cost
                    (pheromones[key] ?: DEFAULT_PHEROMONE) *
                        (costs[key]?.takeUnless(Float::isNaN)?.let { GRID_SECTOR_SIZE / it } ?: 1.0)
                }

            // If we previously chose an optimal route, then sub-optimal routes shall have their
            // pheromone values decayed so they are less likely to be considered again later
            firstPheromone?.also {
                val nextPheromone =
                    PHI * it + MINUS_PHI * (pheromones[nextKey] ?: DEFAULT_PHEROMONE)
                pheromones[nextKey] = nextPheromone
                pheromones[swapKey(nextKey)] = nextPheromone
            }

            // Add waypoint to current path, remove from list of waypoints to consider
            val pathCost = costs[nextKey] ?: 0f
            totalCost += pathCost
            currentPath.add(nextNode)
            currentNodes.remove(nextNode)

            // Now open consideration for waypoints that follow this waypoint but aren't also
            // currently preceded by some other waypoint - this may result in a waypoint being
            // visited twice if cycles exist anywhere
            paths[nextNode]
                ?.filter { target ->
                    paths[target]?.isNotEmpty() == true ||
                        currentNodes.none { otherNode ->
                            paths[otherNode]?.contains(target) == true
                        }
                }
                ?.also(currentNodes::addAll)
        }

        // Return the generated route and its total cost
        return currentPath to totalCost
    }

    /**
     * Calculates the total cost of a route from the given source to the given destination while
     * avoiding obstacles.
     */
    private fun calculateAvoidanceRouteCost(
        sourceX: Float,
        sourceZ: Float,
        destX: Float,
        destZ: Float,
        maxCost: Float = Float.POSITIVE_INFINITY,
    ): Float {
        // Calculate simple vector from source to destination
        val dx = destX - sourceX
        val dz = destZ - sourceZ
        val simpleDistanceSquared = dx * dx + dz * dz

        // Determine if there is an obstacle close to this vector
        val firstObstacle =
            objectsToAvoid.keys
                .mapNotNull { obj ->
                    // Object is only an obstacle if it needs to be avoided
                    val clearance = viewModel.getClearanceFor(obj)
                    if (clearance == 0f) return@mapNotNull null

                    // If obstacle is too close to destination, bail
                    val distX = destX - obj.x.value
                    val distZ = destZ - obj.z.value
                    val distToDestSqr = distX * distX + distZ * distZ
                    if (distToDestSqr <= clearance * clearance) {
                        return Float.POSITIVE_INFINITY
                    }

                    obj.distanceToIntersectSquared(sourceX, sourceZ, destX, destZ, clearance)?.let {
                        obj to it
                    }
                }
                .minByOrNull { it.second }
                ?.first

        // Obstacle should belong to a cluster (even if it's only a cluster of one), but
        // it might not if, for example, it was destroyed
        val allObjectsToConsider =
            firstObstacle?.let { objectsToAvoid[it] } ?: return sqrt(simpleDistanceSquared)

        // We will be looking at both clockwise and counterclockwise routes around obstacles and
        // choosing the one with the lower cost
        var minDistance = maxCost
        floatArrayOf(CLOCKWISE, COUNTER_CLOCKWISE).forEach { direction ->
            // As we find these routes around obstacles, these positions will be updated
            var currentSourceX = sourceX
            var currentSourceZ = sourceZ

            var costAllowance = minDistance
            var distance = 0f
            var diffX = dx
            var diffZ = dz

            var lastObstacle: ArtemisObject<*>? = null
            var objectsToConsider = allObjectsToConsider.toList()

            while (true) {
                // Calculate heading to destination from current position
                val currentHeading = atan2(diffX, diffZ)

                // Filter out objects from current cluster that are no longer relevant
                val remainingObjects =
                    objectsToConsider.mapNotNull { obstacle ->
                        // Ignore the last obstacle we've already adjusted to (if any)
                        if (obstacle == lastObstacle) return@mapNotNull null

                        // Calculate vector to obstacle
                        val objX = obstacle.x.value - currentSourceX
                        val objZ = obstacle.z.value - currentSourceZ

                        // Calculate heading to obstacle and normalize against vector to destination
                        var heading = atan2(objX, objZ) - currentHeading
                        while (heading > PI) heading -= TWO_PI
                        while (heading < -PI) heading += TWO_PI
                        heading *= direction

                        // Ignore obstacle located in the wrong direction
                        if (heading < 0f) return@mapNotNull null

                        obstacle to heading
                    }

                // If there are no more obstacles to get around, exit loop
                val nextObstacle = remainingObjects.maxByOrNull { it.second }?.first ?: break

                // Check to see if there's another obstacle closer to our current position in
                // another cluster that we need to avoid
                val closerObstacle =
                    objectsToAvoid.keys
                        .mapNotNull { obj ->
                            // Ignore this obstacle if it's in the same cluster as the current
                            // obstacle
                            if (obj == nextObstacle) return@mapNotNull null
                            if (objectsToAvoid[nextObstacle]?.contains(obj) != false)
                                return@mapNotNull null

                            // Also ignore if it doesn't need avoiding
                            val clearance = viewModel.getClearanceFor(obj)
                            if (clearance == 0f) return@mapNotNull null

                            // If obstacle is too close to destination, bail
                            val distX = destX - obj.x.value
                            val distZ = destZ - obj.z.value
                            val distToDestSqr = distX * distX + distZ * distZ
                            if (distToDestSqr <= clearance * clearance) {
                                return Float.POSITIVE_INFINITY
                            }

                            obj.distanceToIntersectSquared(
                                    currentSourceX,
                                    currentSourceZ,
                                    nextObstacle.x.value,
                                    nextObstacle.z.value,
                                    clearance,
                                )
                                ?.let { obj to it }
                        }
                        .minByOrNull { it.second }
                        ?.first

                // If there is a closer obstacle cluster, restart calculation with it
                if (closerObstacle != null) {
                    objectsToAvoid[closerObstacle]?.also { closerCluster ->
                        objectsToConsider = closerCluster.toList()
                    }
                    continue
                }

                objectsToConsider = remainingObjects.map { it.first }

                // Calculate vector to next obstacle to avoid
                val nextX = nextObstacle.x.value - currentSourceX
                val nextZ = nextObstacle.z.value - currentSourceZ
                val nextDist = sqrt(nextX * nextX + nextZ * nextZ)

                // If there was a previous obstacle, take a wide berth around it
                lastObstacle?.also {
                    // If it's not an obstacle, forget it
                    val clearance = viewModel.getClearanceFor(it)
                    if (clearance == 0f) return@also
                    val scale = clearance / nextDist

                    // Calculate vectors to arc point to reach to move around obstacle
                    val armX = currentSourceX - it.x.value
                    val armZ = currentSourceZ - it.z.value
                    val legX = nextZ * scale * direction
                    val legZ = -nextX * scale * direction

                    // Calculate arc length to move around obstacle
                    val armAngle = atan2(armX, armZ)
                    val legAngle = atan2(legX, legZ)
                    var angleDiff = abs(legAngle - armAngle)
                    if (angleDiff > PI) angleDiff = TWO_PI - angleDiff

                    // Move along arc
                    currentSourceX += legX - armX
                    currentSourceZ += legZ - armZ
                    val addedCost = clearance * angleDiff
                    costAllowance -= addedCost
                    distance += addedCost
                }

                // If we've moved too far, route is impractical
                if (costAllowance < 0f) {
                    distance = Float.POSITIVE_INFINITY
                    break
                }

                lastObstacle = nextObstacle
                val scale = viewModel.getClearanceFor(nextObstacle) / nextDist

                // Calculate vector to tangent point at necessary clearance
                val armX = nextZ * scale * direction
                val armZ = -nextX * scale * direction
                val deltaX = nextX + armX
                val deltaZ = nextZ + armZ

                // Move to tangent point
                val addedCost = sqrt(deltaX * deltaX + deltaZ * deltaZ)
                costAllowance -= addedCost

                // If we've moved too far, route is impractical
                if (costAllowance < 0f) {
                    distance = Float.POSITIVE_INFINITY
                    break
                }

                // Update distance traveled
                distance += addedCost

                // Update vector to destination
                currentSourceX += deltaX
                currentSourceZ += deltaZ
                diffX = destX - currentSourceX
                diffZ = destZ - currentSourceZ
            }

            if (distance == 0f) {
                return sqrt(simpleDistanceSquared)
            } else if (distance.isFinite()) {
                // If we had to get around an obstacle, we'll need to make an arc around it
                lastObstacle?.also {
                    // If it's not an obstacle, forget it
                    val clearance = viewModel.getClearanceFor(it)
                    if (clearance == 0f) return@also

                    // Calculate vectors to arc point to reach to move around obstacle
                    val nextDist = sqrt(diffX * diffX + diffZ * diffZ)
                    val scale = clearance / nextDist
                    val armX = currentSourceX - it.x.value
                    val armZ = currentSourceZ - it.z.value
                    val legX = diffZ * scale * direction
                    val legZ = -diffX * scale * direction

                    // Calculate arc length to move around obstacle
                    val armAngle = atan2(armX, armZ)
                    val legAngle = atan2(legX, legZ)
                    var angleDiff = abs(legAngle - armAngle)
                    if (angleDiff > PI) angleDiff = TWO_PI - angleDiff

                    // Move along arc
                    val addedCost = clearance * angleDiff
                    costAllowance -= addedCost

                    // If we've moved too far, route is impractical
                    if (costAllowance < 0f) {
                        distance = Float.POSITIVE_INFINITY
                    } else {
                        currentSourceX += legX - armX
                        currentSourceZ += legZ - armZ
                        distance += addedCost
                    }
                }

                // Continue search from current position
                if (distance.isFinite()) {
                    distance +=
                        calculateAvoidanceRouteCost(
                            currentSourceX,
                            currentSourceZ,
                            destX,
                            destZ,
                            costAllowance,
                        )
                }

                minDistance = min(minDistance, distance)
            }
        }

        // Return the lesser cost of the two paths
        return minDistance.apply {
            if (isInfinite()) {
                Log.w("RoutingGraph", "Infinite path cost!")
            }
        }
    }

    companion object {
        // Constants
        private const val TWO_PI = PI.toFloat() * 2
        private const val TOTAL_ANTS = 12
        private const val PHI = 0.1
        private const val MINUS_PHI = 0.9
        private const val DEFAULT_PHEROMONE = 1.0
        private const val GRID_SECTOR_SIZE = Artemis.MAP_SIZE / 5.0

        private const val CLOCKWISE = 1.0f
        private const val COUNTER_CLOCKWISE = -1.0f

        /**
         * Calculates the cost of a path from one point to another, with object avoidances taken
         * into consideration. This function is also compatible with the possibility that the graph
         * has not yet been initialized, meaning that no avoidable objects have been registered yet.
         * Also note that avoidances are skipped if the player ship uses a jump drive.
         */
        fun RoutingGraph?.calculateRouteCost(
            source: ArtemisObject<*>,
            dest: ArtemisObject<*>,
        ): Float {
            val sourceX = source.x.value
            val sourceZ = source.z.value
            val destX = dest.x.value
            val destZ = dest.z.value

            return if (allDefined(sourceX, sourceX, destX, destZ)) {
                val dx = destX - sourceX
                val dz = destZ - sourceZ
                if (this == null || viewModel.playerShip?.driveType?.value != DriveType.WARP) {
                    sqrt(dx * dx + dz * dz)
                } else {
                    calculateAvoidanceRouteCost(sourceX, sourceZ, destX, destZ)
                }
            } else {
                Float.NaN
            }
        }

        private fun allDefined(vararg coordinates: Float): Boolean = coordinates.none(Float::isNaN)

        /**
         * Generates a "route key" representing a path from one object to another, encapsulating
         * both objects' IDs and the path direction. This also serves as a unique identifier for
         * each entry in the eventual route since some points may be visited multiple times, but
         * there's no chance of them being visited twice immediately following visits to the same
         * other waypoint.
         */
        private fun generateRouteKey(
            source: ArtemisShielded<*>,
            destination: ArtemisShielded<*>,
        ): Int = source.id or (destination.id shl Short.SIZE_BITS)

        /**
         * Inverts a route key to generate the key that would be generated from the inversion of the
         * path it represents.
         */
        private fun swapKey(key: Int) = (key shl Short.SIZE_BITS) or (key ushr Short.SIZE_BITS)

        private fun AgentViewModel.findNeighbors():
            MutableMap<ArtemisObject<*>, MutableSet<ArtemisObject<*>>> {
            // Start by fetching all of the objects themselves
            val nearObjects = mutableMapOf<ArtemisObject<*>, MutableSet<ArtemisObject<*>>>()

            if (avoidBlackHoles) {
                nearObjects.putAll(blackHoles.values.map { it to mutableSetOf() })
            }
            if (avoidMines) {
                nearObjects.putAll(mines.values.map { it to mutableSetOf() })
            }
            if (avoidTyphons) {
                nearObjects.putAll(typhons.values.map { it to mutableSetOf() })
            }

            // Then map each object to the set of objects within range
            nearObjects.forEach { (obj, nearSet) ->
                val oneClearance = getClearanceFor(obj)
                nearSet.addAll(
                    nearObjects.keys.filter { otherObj ->
                        val totalClearance = getClearanceFor(otherObj) + oneClearance

                        // It is possible for this to fail
                        try {
                            obj horizontalDistanceSquaredTo otherObj <=
                                totalClearance * totalClearance
                        } catch (_: IllegalStateException) {
                            false
                        }
                    }
                )
            }

            return nearObjects
        }

        /** Returns the minimum clearance required to avoid an obstacle, if necessary. */
        private fun AgentViewModel.getClearanceFor(obj: ArtemisObject<*>?) =
            when (obj) {
                is ArtemisMine -> mineClearance
                is ArtemisBlackHole -> blackHoleClearance
                is ArtemisCreature -> typhonClearance
                else -> 0f
            }

        /** Helper function to heuristically select a random entry from a collection. */
        private fun <T> Collection<T>.randomByWeight(weightFn: (T) -> Double): T {
            // Map entries to weights
            val weighted = map { it to weightFn(it) }

            // Select a random entry from the sum total of all weights
            var selector = Random.nextDouble() * weighted.sumOf { it.second }

            // Find and return the selected entry
            for ((entry, weight) in weighted) {
                selector -= weight
                if (selector < 0.0) {
                    return entry
                }
            }

            // If selection didn't work, choose randomly with equal weight
            return random()
        }

        /**
         * Calculates the square of the distance between the source point of the given vector and
         * the intersection point with the avoidance bubble of this obstacle. If there is no
         * intersection point, returns null. The algorithm comes from WolframAlpha.
         */
        private fun ArtemisObject<*>.distanceToIntersectSquared(
            sourceX: Float,
            sourceZ: Float,
            destX: Float,
            destZ: Float,
            clearance: Float,
        ): Float? {
            // Square of the length of the vector
            val diffX = destX - sourceX
            val diffZ = destZ - sourceZ
            val distSqr = diffX * diffX + diffZ * diffZ
            val clearanceSquared = clearance * clearance

            // Adjust to pretend this object is positioned at the origin
            val adjSourceX = sourceX - x.value
            val adjSourceZ = sourceZ - z.value
            val adjDestX = destX - x.value
            val adjDestZ = destZ - z.value

            // Determinant and discriminant for quadratic formula
            val det = adjSourceX * adjDestZ - adjSourceZ * adjDestX
            val discriminant = clearanceSquared * distSqr - det * det

            // Discriminant determines if intersection point exists - if not, return null
            if (discriminant <= 0f) return null
            val root = sqrt(discriminant)

            // Intersection point coordinates
            val intersectZ = (-det * diffX - diffZ * root) / distSqr
            val intersectX =
                sqrt(max(0f, clearanceSquared - intersectZ * intersectZ)) * adjSourceX.sign

            val fromSourceX = intersectX - adjSourceX
            val fromSourceZ = intersectZ - adjSourceZ
            val fromDestX = intersectX - adjDestX
            val fromDestZ = intersectZ - adjDestZ

            // Intersection point must lie between the two points
            return if (fromSourceX * fromDestX + fromSourceZ * fromDestZ > 0f) {
                null
            } else {
                // Return the all-important distance from the source point
                // But it must be outside the radius of the obstacle
                val distanceSquared = fromSourceX * fromSourceX + fromSourceZ * fromSourceZ
                distanceSquared.takeIf { it > clearanceSquared }
            }
        }
    }
}
