package artemis.agent.cpu

import android.util.Log
import android.util.Pair
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

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

    /**
     * Clears all data from the current route calculation.
     */
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

    /**
     * Tests a previous route to see if it still traverses all necessary paths.
     */
    fun testRoute(previousRoute: List<RouteEntry>?) {
        val currentNodes = paths.keys.toMutableSet()
        previousRoute?.onEach { (point) ->
            currentNodes.remove(point)
            paths[point]?.filter { target ->
                paths[target]?.isNotEmpty() == true || currentNodes.none { otherNode ->
                    paths[otherNode]?.contains(target) == true
                }
            }?.also(currentNodes::addAll)
        }
        if (currentNodes.isNotEmpty()) {
            minimumCost = Float.POSITIVE_INFINITY
        }
    }

    /**
     * Pre-process the list of objects to avoid based on user settings.
     */
    fun preprocessObjectsToAvoid() {
        // Start by fetching all of the objects themselves
        val nearObjects = mutableMapOf<ArtemisObject<*>, MutableSet<ArtemisObject<*>>>()

        if (viewModel.avoidBlackHoles) {
            nearObjects.putAll(viewModel.blackHoles.values.map { it to mutableSetOf() })
        }
        if (viewModel.avoidMines) {
            nearObjects.putAll(viewModel.mines.values.map { it to mutableSetOf() })
        }
        if (viewModel.avoidTyphons) {
            nearObjects.putAll(viewModel.typhons.values.map { it to mutableSetOf() })
        }

        // Then map each object to the set of objects within range
        nearObjects.forEach { (obj, nearSet) ->
            val oneClearance = getClearanceFor(obj)
            nearSet.addAll(
                nearObjects.keys.filter { otherObj ->
                    val totalClearance = getClearanceFor(otherObj) + oneClearance

                    // It is possible for this to fail
                    try {
                        obj horizontalDistanceSquaredTo otherObj <= totalClearance * totalClearance
                    } catch (_: IllegalStateException) {
                        false
                    }
                }
            )
        }

        // Then spread out search for nearby objects to form "clusters" in which proximity becomes
        // transitive - this is important for minefields etc.
        val clusterSets = mutableListOf<CopyOnWriteArraySet<ArtemisObject<*>>>()
        while (true) {
            // Grab an object from the set - exit loop if none are left
            val firstKey = nearObjects.keys.firstOrNull() ?: break

            // Remove object from the set to avoid processing it repeatedly
            val openSet = nearObjects.remove(firstKey) ?: continue

            // If this object already belongs to a cluster, skip it
            if (clusterSets.any { it.contains(firstKey) }) continue

            // Start a new cluster beginning with this object
            val newSet = CopyOnWriteArraySet<ArtemisObject<*>>()
            newSet.add(firstKey)

            while (true) {
                // Find a nearby object - if none left, finalize cluster
                val nextKey = openSet.firstOrNull() ?: break

                // Add all of its neighbours to the cluster
                nearObjects[nextKey]?.also {
                    openSet.addAll(it.filterNot(newSet::contains))
                }

                // Add it to the cluster as well
                newSet.add(nextKey)
                openSet.remove(nextKey)
            }

            clusterSets.add(newSet)
        }

        // Finally, map every object to the cluster that contains it
        clusterSets.forEach { objSet ->
            objSet.forEach { obj ->
                objectsToAvoid[obj] = objSet
            }
        }
    }

    /**
     * Removes an obstacle from the set of objects to avoid once it is deleted.
     */
    fun removeObstacle(obj: ArtemisObject<*>) {
        objectsToAvoid.remove(obj)
    }

    /**
     * Pre-process all route costs between every pair of waypoints, as well as route costs from the
     * player ship to each individual waypoint.
     */
    fun preprocessCosts() {
        // Get every waypoint to eventually visit, including those preceded by others
        val allTargets = paths.values.fold(paths.keys) { acc, targets ->
            acc.union(targets).toMutableSet()
        }.toList()

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

                val routeCost = calculateRouteCost(
                    srcObj.x.value,
                    srcObj.z.value,
                    dstObj.x.value,
                    dstObj.z.value
                )

                costs[generateRouteKey(srcObj, dstObj)] = routeCost
                costs[generateRouteKey(dstObj, srcObj)] = routeCost
            }

            // Now we calculate the path cost from the player ship to each waypoint
            costs[generateRouteKey(source, srcObj)] = calculateRouteCost(
                source.x.value,
                source.z.value,
                srcObj.x.value,
                srcObj.z.value
            )
        }
    }

    /**
     * Releases a bunch of "ants" to search in parallel to attempt to find the optimal route.
     */
    suspend fun searchForRoute(): List<RouteEntry>? {
        val possibleRoutes = Array(TOTAL_ANTS) {
            // Let each ant search in parallel coroutines
            viewModel.cpu.async {
                val currentNodes = paths.keys.toMutableSet()
                var totalCost = 0f
                val currentPath = mutableListOf<ObjectEntry<*>>()

                while (currentNodes.isNotEmpty()) {
                    // Get current location in the path we're building, starting at the player ship
                    val currentNode = currentPath.lastOrNull()?.obj ?: source

                    // Line up each possible next waypoint to visit by heuristically dividing the
                    // current pheromone value of the path to it by the cost of said path
                    val nodePheromones = currentNodes.map { node ->
                        val key = generateRouteKey(currentNode, node.obj)
                        Triple(
                            node,
                            key,
                            (pheromones[key] ?: DEFAULT_PHEROMONE) * (
                                costs[key]?.takeUnless(Float::isNaN)?.let { GRID_SECTOR_SIZE / it }
                                    ?: 1.0
                            )
                        )
                    }

                    // Choose one at random - waypoints with a higher heuristic value are more
                    // likely to be chosen
                    var pheromoneSelect = Random.nextDouble() * nodePheromones.sumOf { it.third }

                    var nextNode: ObjectEntry<*>? = null
                    var nextKey = -1
                    for (pheromone in nodePheromones) {
                        pheromoneSelect -= pheromone.third
                        // Seek out the next waypoint to visit based on randomly chosen
                        // pheromone value
                        if (pheromoneSelect < 0.0) {
                            nextNode = pheromone.first
                            nextKey = pheromone.second

                            // If we previously chose an optimal route, then sub-optimal routes
                            // shall have their pheromone values decayed so they are less likely to
                            // be considered again later
                            firstPheromone?.also {
                                val nextPheromone = PHI * it +
                                    MINUS_PHI * (pheromones[nextKey] ?: DEFAULT_PHEROMONE)
                                pheromones[nextKey] = nextPheromone
                                pheromones[swapKey(nextKey)] = nextPheromone
                            }
                            break
                        }
                    }

                    if (nextNode == null) {
                        // If previous selection didn't work, choose with equal weight
                        val pheromone = nodePheromones.random()
                        nextNode = pheromone.first
                        nextKey = pheromone.second

                        // If we previously chose an optimal route, then sub-optimal routes
                        // shall have their pheromone values decayed so they are less likely to
                        // be considered again later
                        firstPheromone?.also {
                            val nextPheromone = PHI * it +
                                MINUS_PHI * (pheromones[nextKey] ?: DEFAULT_PHEROMONE)
                            pheromones[nextKey] = nextPheromone
                            pheromones[swapKey(nextKey)] = nextPheromone
                        }
                    }

                    // Add waypoint to current path, remove from list of waypoints to consider
                    val pathCost = costs[nextKey] ?: 0f
                    totalCost += pathCost
                    currentPath.add(nextNode)
                    currentNodes.remove(nextNode)

                    // Now open consideration for waypoints that follow this waypoint but aren't
                    // also currently preceded by some other waypoint - this may result in a
                    // waypoint being visited twice if cycles exist anywhere
                    paths[nextNode]?.filter { target ->
                        paths[target]?.isNotEmpty() == true || currentNodes.none { otherNode ->
                            paths[otherNode]?.contains(target) == true
                        }
                    }?.also(currentNodes::addAll)
                }

                // Return the generated route and its total cost
                Pair(currentPath, totalCost)
            }
        }.toList()

        // Take the best route out of the ones that were considered
        val bestPair = possibleRoutes.awaitAll().minByOrNull { it.second } ?: return null

        // If we already found a better one previously, ignore this one
        val bestPathCost = bestPair.second
        if (bestPathCost > minimumCost) return null

        val bestPath = bestPair.first.map(::RouteEntry)
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

    /**
     * Calculates the total cost of a route from the given source to the given destination while
     * avoiding obstacles.
     */
    private fun calculateAvoidanceRouteCost(
        sourceX: Float,
        sourceZ: Float,
        destX: Float,
        destZ: Float,
        simpleDistance: Float,
        maxCost: Float = Float.POSITIVE_INFINITY
    ): Float = viewModel.run {
        // Calculate simple vector from source to destination
        val dx = destX - sourceX
        val dz = destZ - sourceZ

        // Determine if there is an obstacle close to this vector
        val firstObstacle = objectsToAvoid.keys.mapNotNull { obj ->
            // Object is only an obstacle if it needs to be avoided
            val clearance = getClearanceFor(obj)
            if (clearance == 0f) return@mapNotNull null

            // If obstacle is too close to destination, bail
            val distX = destX - obj.x.value
            val distZ = destZ - obj.z.value
            val distToDestSqr = distX * distX + distZ * distZ
            if (distToDestSqr <= clearance * clearance) {
                return@run Float.POSITIVE_INFINITY
            }

            // Calculate vector from source to obstacle
            val objX = obj.x.value - sourceX
            val objZ = obj.z.value - sourceZ

            val dot = dx * objX + dz * objZ

            // Ignore obstacle if it is located in the wrong direction
            if (dot <= 0f) return@mapNotNull null

            // Ignore obstacle if it is located beyond destination
            if (dot >= simpleDistance * simpleDistance) return@mapNotNull null

            // Ignore obstacle if we are already too close to it
            val objDistSqr = objX * objX + objZ * objZ
            if (objDistSqr <= clearance * clearance) return@mapNotNull null

            // Consider obstacle if and only if it is within range of travel vector
            val cross = abs(dx * objZ - dz * objX)
            if (cross >= simpleDistance * clearance) {
                null
            } else {
                Pair(obj, objDistSqr)
            }
            // Return closest obstacle among those found; terminate if none was found
        }.minByOrNull { it.second }?.first ?: return@run simpleDistance

        // Obstacle should belong to a cluster (even if it's only a cluster of one), but
        // it might not if, for example, it was destroyed
        val allObjectsToConsider = objectsToAvoid[firstObstacle] ?: return@run simpleDistance

        // We will be looking at both clockwise and counterclockwise routes around obstacles and
        // choosing the one with the lower cost
        var clockwiseDistance = 0f
        var counterClockwiseDistance = 0f

        // As we find these routes around obstacles, these positions will be updated
        var currentSourceX = sourceX
        var currentSourceZ = sourceZ

        var costAllowance = maxCost
        var diffX = dx
        var diffZ = dz

        var lastObstacle: ArtemisObject<*>? = null
        var objectsToConsider = allObjectsToConsider.toList()

        while (true) {
            // Calculate heading to destination from current position
            val currentHeading = atan2(diffX, diffZ)

            // Filter out objects from current cluster that are no longer relevant
            val remainingObjects = objectsToConsider.mapNotNull {
                // Ignore the last obstacle we've already adjusted to (if any)
                if (it == lastObstacle) return@mapNotNull null

                // Calculate vector to obstacle
                val objX = it.x.value - currentSourceX
                val objZ = it.z.value - currentSourceZ

                // Calculate heading to obstacle and normalize against vector to destination
                var heading = atan2(objX, objZ) - currentHeading
                while (heading > PI) heading -= TWO_PI
                while (heading < -PI) heading += TWO_PI

                // Ignore obstacle located in the wrong direction
                if (heading < 0f) {
                    null
                } else {
                    Pair(it, heading)
                }
            }

            // If there are no more obstacles to get around, exit loop
            val nextObstacle = remainingObjects.maxByOrNull { it.second }?.first ?: break

            // Calculate vector to next obstacle to avoid as well as clearance
            val nextX = nextObstacle.x.value - currentSourceX
            val nextZ = nextObstacle.z.value - currentSourceZ
            val nextDist = sqrt(nextX * nextX + nextZ * nextZ)
            val neededClearance = getClearanceFor(nextObstacle)

            // Check to see if there's another obstacle closer to our current position in another
            // cluster that we need to avoid
            val closerObstacle = objectsToAvoid.keys.mapNotNull { obj ->
                // Ignore this obstacle if it's in the same cluster as the current obstacle
                if (obj == nextObstacle) return@mapNotNull null
                if (objectsToAvoid[nextObstacle]?.contains(obj) != false) return@mapNotNull null

                // Also ignore if it doesn't need avoiding
                val clearance = getClearanceFor(obj)
                if (clearance == 0f) return@mapNotNull null

                // If obstacle is too close to destination, bail
                val distX = destX - obj.x.value
                val distZ = destZ - obj.z.value
                val distToDestSqr = distX * distX + distZ * distZ
                if (distToDestSqr <= clearance * clearance) {
                    return@run Float.POSITIVE_INFINITY
                }

                // Calculate vector from position to obstacle
                val objX = obj.x.value - currentSourceX
                val objZ = obj.z.value - currentSourceZ

                val dot = nextX * objX + nextZ * objZ

                // Ignore obstacle if it is located in the wrong direction
                if (dot <= 0f) return@mapNotNull null

                // Ignore obstacle if it is located beyond destination
                if (dot >= nextDist * nextDist) return@mapNotNull null
                val objDistSqr = objX * objX + objZ * objZ
                if (objDistSqr <= clearance * clearance) return@mapNotNull null

                // Consider obstacle if and only if it is within range of travel vector
                val cross = abs(nextX * objZ - nextZ * objX)
                if (cross >= nextDist * clearance) {
                    null
                } else {
                    Pair(obj, objDistSqr)
                }
            }.minByOrNull { it.second }?.first

            // If there is a closer obstacle cluster, restart calculation with it
            if (closerObstacle != null) {
                objectsToAvoid[closerObstacle]?.also { closerCluster ->
                    objectsToConsider = closerCluster.toList()
                }
                continue
            }

            objectsToConsider = remainingObjects.map { it.first }

            // If there was a previous obstacle, take a wide berth around it
            lastObstacle?.also {
                // If it's not an obstacle, forget it
                val clearance = getClearanceFor(it)
                if (clearance == 0f) return@also
                val scale = clearance / nextDist

                // Calculate vectors to arc point to reach to move around obstacle
                val armX = currentSourceX - it.x.value
                val armZ = currentSourceZ - it.z.value
                val legX = nextZ * scale
                val legZ = -nextX * scale

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
                clockwiseDistance += addedCost
            }

            // If we've moved too far, clockwise route is impractical
            if (costAllowance < 0f) {
                clockwiseDistance = Float.POSITIVE_INFINITY
                break
            }

            lastObstacle = nextObstacle
            val scale = neededClearance / nextDist

            // Calculate vector to tangent point at necessary clearance
            val armX = nextZ * scale
            val armZ = -nextX * scale
            val deltaX = nextX + armX
            val deltaZ = nextZ + armZ

            // Move to tangent point
            val addedCost = sqrt(deltaX * deltaX + deltaZ * deltaZ)
            costAllowance -= addedCost

            // If we've moved too far, clockwise route is impractical
            if (costAllowance < 0f) {
                clockwiseDistance = Float.POSITIVE_INFINITY
                break
            }

            // Update distance traveled
            clockwiseDistance += addedCost

            // Update vector to destination
            currentSourceX += deltaX
            currentSourceZ += deltaZ
            diffX = destX - currentSourceX
            diffZ = destZ - currentSourceZ
        }

        if (clockwiseDistance == 0f) {
            return@run simpleDistance
        } else if (clockwiseDistance.isFinite()) {
            // If we had to get around an obstacle, we'll need to make an arc around it
            lastObstacle?.also {
                // If it's not an obstacle, forget it
                val clearance = getClearanceFor(it)
                if (clearance == 0f) return@also

                // Calculate vectors to arc point to reach to move around obstacle
                val nextDist = sqrt(diffX * diffX + diffZ * diffZ)
                val scale = clearance / nextDist
                val armX = currentSourceX - it.x.value
                val armZ = currentSourceZ - it.z.value
                val legX = diffZ * scale
                val legZ = -diffX * scale

                // Calculate arc length to move around obstacle
                val armAngle = atan2(armX, armZ)
                val legAngle = atan2(legX, legZ)
                var angleDiff = abs(legAngle - armAngle)
                if (angleDiff > PI) angleDiff = TWO_PI - angleDiff

                // Move along arc
                val addedCost = clearance * angleDiff
                costAllowance -= addedCost

                // If we've moved too far, clockwise route is impractical
                if (costAllowance < 0f) {
                    clockwiseDistance = Float.POSITIVE_INFINITY
                } else {
                    currentSourceX += legX - armX
                    currentSourceZ += legZ - armZ
                    clockwiseDistance += addedCost
                }
            }

            // Continue search from current position
            if (clockwiseDistance.isFinite()) {
                clockwiseDistance += calculateAvoidanceRouteCost(
                    currentSourceX,
                    currentSourceZ,
                    destX,
                    destZ,
                    sqrt(diffX * diffX + diffZ * diffZ),
                    costAllowance
                )
            }
        }

        // Set up to search in counter-clockwise direction
        currentSourceX = sourceX
        currentSourceZ = sourceZ
        diffX = dx
        diffZ = dz
        lastObstacle = null
        costAllowance = min(maxCost, clockwiseDistance)
        objectsToConsider = allObjectsToConsider.toList()

        while (true) {
            // Calculate heading to destination from current position
            val currentHeading = atan2(diffX, diffZ)

            // Filter out objects from current cluster that are no longer relevant
            val remainingObjects = objectsToConsider.mapNotNull {
                // Ignore the last obstacle we've already adjusted to (if any)
                if (it == lastObstacle) return@mapNotNull null

                // Calculate vector to obstacle
                val objX = it.x.value - currentSourceX
                val objZ = it.z.value - currentSourceZ

                // Calculate heading to obstacle and normalize against vector to destination
                var heading = atan2(objX, objZ) - currentHeading
                while (heading > PI) heading -= TWO_PI
                while (heading < -PI) heading += TWO_PI

                // Ignore obstacle located in the wrong direction
                if (heading > 0f) {
                    null
                } else {
                    Pair(it, heading)
                }
            }

            // If there are no more obstacles to get around, exit loop
            val nextObstacle = remainingObjects.minByOrNull { it.second }?.first ?: break

            // Calculate vector to next obstacle to avoid as well as clearance
            val nextX = nextObstacle.x.value - currentSourceX
            val nextZ = nextObstacle.z.value - currentSourceZ
            val nextDist = sqrt(nextX * nextX + nextZ * nextZ)
            val neededClearance = getClearanceFor(nextObstacle)

            // Check to see if there's another obstacle closer to our current position in another
            // cluster that we need to avoid
            val closerObstacle = objectsToAvoid.keys.mapNotNull { obj ->
                // Ignore this obstacle if it's in the same cluster as the current obstacle
                if (obj == nextObstacle) return@mapNotNull null
                if (objectsToAvoid[nextObstacle]?.contains(obj) != false) return@mapNotNull null

                // Also ignore if it doesn't need avoiding
                val clearance = getClearanceFor(obj)
                if (clearance == 0f) return@mapNotNull null

                // If obstacle is too close to destination, bail
                val distX = destX - obj.x.value
                val distZ = destZ - obj.z.value
                val distToDestSqr = distX * distX + distZ * distZ
                if (distToDestSqr <= clearance * clearance) {
                    return@run Float.POSITIVE_INFINITY
                }

                // Calculate vector from position to obstacle
                val objX = obj.x.value - currentSourceX
                val objZ = obj.z.value - currentSourceZ

                val dot = nextX * objX + nextZ * objZ

                // Ignore obstacle if it is located in the wrong direction
                if (dot <= 0f) return@mapNotNull null

                // Ignore obstacle if it is located beyond destination
                if (dot >= nextDist * nextDist) return@mapNotNull null
                val objDistSqr = objX * objX + objZ * objZ
                if (objDistSqr <= clearance * clearance) return@mapNotNull null

                // Consider obstacle if and only if it is within range of travel vector
                val cross = abs(nextX * objZ - nextZ * objX)
                if (cross >= nextDist * clearance) {
                    null
                } else {
                    Pair(obj, objDistSqr)
                }
            }.minByOrNull { it.second }?.first

            // If there is a closer obstacle cluster, restart calculation with it
            if (closerObstacle != null) {
                objectsToAvoid[closerObstacle]?.also { closerCluster ->
                    objectsToConsider = closerCluster.toList()
                }
                continue
            }

            objectsToConsider = remainingObjects.map { it.first }

            // If there was a previous obstacle, take a wide berth around it
            lastObstacle?.also {
                // If it's not an obstacle, forget it
                val clearance = getClearanceFor(it)
                if (clearance == 0f) return@also
                val scale = clearance / nextDist

                // Calculate vectors to arc point to reach to move around obstacle
                val armX = currentSourceX - it.x.value
                val armZ = currentSourceZ - it.z.value
                val legX = -nextZ * scale
                val legZ = nextX * scale

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
                counterClockwiseDistance += addedCost
            }

            // If we've moved too far, counterclockwise route is impractical
            if (costAllowance < 0f) {
                counterClockwiseDistance = Float.POSITIVE_INFINITY
                break
            }

            lastObstacle = nextObstacle
            val scale = neededClearance / nextDist

            // Calculate vector to tangent point at necessary clearance
            val armX = -nextZ * scale
            val armZ = nextX * scale
            val deltaX = nextX + armX
            val deltaZ = nextZ + armZ

            // Move to tangent point
            val addedCost = sqrt(deltaX * deltaX + deltaZ * deltaZ)
            costAllowance -= addedCost

            // If we've moved too far, clockwise route is impractical
            if (costAllowance < 0f) {
                counterClockwiseDistance = Float.POSITIVE_INFINITY
                break
            }

            // Update distance traveled
            counterClockwiseDistance += addedCost

            // Update vector to destination
            currentSourceX += deltaX
            currentSourceZ += deltaZ
            diffX = destX - currentSourceX
            diffZ = destZ - currentSourceZ
        }

        if (counterClockwiseDistance == 0f) {
            return@run simpleDistance
        } else if (counterClockwiseDistance.isFinite()) {
            // If we had to get around an obstacle, we'll need to make an arc around it
            lastObstacle?.also {
                // If it's not an obstacle, forget it
                val clearance = getClearanceFor(it)
                if (clearance == 0f) return@also

                // Calculate vectors to arc point to reach to move around obstacle
                val nextDist = sqrt(diffX * diffX + diffZ * diffZ)
                val scale = clearance / nextDist
                val armX = currentSourceX - it.x.value
                val armZ = currentSourceZ - it.z.value
                val legX = -diffZ * scale
                val legZ = diffX * scale

                // Calculate arc length to move around obstacle
                val armAngle = atan2(armX, armZ)
                val legAngle = atan2(legX, legZ)
                var angleDiff = abs(legAngle - armAngle)
                if (angleDiff > PI) angleDiff = TWO_PI - angleDiff

                // Move along arc
                val addedCost = clearance * angleDiff
                costAllowance -= addedCost

                // If we've moved too far, clockwise route is impractical
                if (costAllowance < 0f) {
                    counterClockwiseDistance = Float.POSITIVE_INFINITY
                } else {
                    currentSourceX += legX - armX
                    currentSourceZ += legZ - armZ
                    counterClockwiseDistance += addedCost
                }
            }

            // Continue search from current position
            if (counterClockwiseDistance.isFinite()) {
                counterClockwiseDistance += calculateAvoidanceRouteCost(
                    currentSourceX,
                    currentSourceZ,
                    destX,
                    destZ,
                    sqrt(diffX * diffX + diffZ * diffZ),
                    min(costAllowance, clockwiseDistance)
                )
            }
        }

        // Return the lesser cost of the two paths
        min(clockwiseDistance, counterClockwiseDistance).apply {
            if (isInfinite()) {
                Log.w("RoutingGraph", "Infinite path cost!")
            }
        }
    }

    /**
     * Returns the minimum clearance required to avoid an obstacle, if necessary.
     */
    private fun getClearanceFor(obj: ArtemisObject<*>?) = viewModel.run {
        when (obj) {
            is ArtemisMine -> mineClearance
            is ArtemisBlackHole -> blackHoleClearance
            is ArtemisCreature -> typhonClearance
            else -> 0f
        }
    }

    companion object {
        /**
         * Calculates the cost of a path from one point to another, with object avoidances taken
         * into consideration. This function is also compatible with the possibility that the graph
         * has not yet been initialized, meaning that no avoidable objects have been registered yet.
         * Also note that avoidances are skipped if the player ship uses a jump drive.
         */
        fun RoutingGraph?.calculateRouteCost(
            sourceX: Float,
            sourceZ: Float,
            destX: Float,
            destZ: Float
        ): Float =
            if (allDefined(sourceX, sourceX, destX, destZ)) {
                val dx = destX - sourceX
                val dz = destZ - sourceZ
                val simpleDistance = sqrt(dx * dx + dz * dz)
                if (this == null || viewModel.playerShip?.driveType?.value != DriveType.WARP) {
                    simpleDistance
                } else {
                    calculateAvoidanceRouteCost(
                        sourceX,
                        sourceZ,
                        destX,
                        destZ,
                        simpleDistance
                    )
                }
            } else {
                Float.NaN
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
        ): Int = source.id or (destination.id shl DEST_SHIFT)

        /**
         * Inverts a route key to generate the key that would be generated from the inversion of
         * the path it represents.
         */
        private fun swapKey(key: Int) = (key shl DEST_SHIFT) or (key ushr DEST_SHIFT)

        // Constants
        private const val TWO_PI = PI.toFloat() * 2
        private const val DEST_SHIFT = 16
        private const val TOTAL_ANTS = 12
        private const val PHI = 0.1
        private const val MINUS_PHI = 0.9
        private const val DEFAULT_PHEROMONE = 1.0
        private const val GRID_SECTOR_SIZE = Artemis.MAP_SIZE / 5.0
    }
}
