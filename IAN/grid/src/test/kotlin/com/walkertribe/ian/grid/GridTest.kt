package com.walkertribe.ian.grid

import com.walkertribe.ian.enums.ShipSystem
import com.walkertribe.ian.util.FilePathResolver
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeWithinPercentageOf
import io.kotest.matchers.doubles.shouldBeZero
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.floats.shouldBeWithinPercentageOf
import io.kotest.matchers.floats.shouldBeZero
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import java.io.File
import okio.Path.Companion.toOkioPath

class GridTest :
    DescribeSpec({
        describe("Grid") {
            describe("Constructors") {
                it("From list of nodes") {
                    val nodes = arbitraryNodesList()
                    val grid = Grid(nodes)
                    grid.nodeMap shouldHaveSize nodes.size
                }

                it("From list of systems") {
                    val systems = arbitraryShipSystemsList()
                    val grid =
                        Grid(
                            Coordinate.ALL.zip(systems).mapNotNull { (coord, system) ->
                                system?.let { Node(coord, it) }
                            }
                        )

                    val nodeSystems = systems.filterNotNull()
                    grid.nodeMap shouldHaveSize nodeSystems.size

                    val countSystems = nodeSystems.distinct().size
                    grid.nodeSystemMap shouldHaveSize countSystems
                }

                describe("From path") {
                    val systems = arbitraryShipSystemsList()
                    val tmpDir = tempdir()
                    val tmpDirPath = tmpDir.toOkioPath()

                    it("Can read nodes") {
                        val sntFileName = "test.snt"
                        val sntFile = File(tmpDir, sntFileName)

                        sntFile.outputStream().use { output ->
                            systems.forEach { system ->
                                repeat(12) { output.write(0) }
                                val value = system?.value ?: -2
                                repeat(Int.SIZE_BYTES) { i -> output.write(value.shr(i * 8)) }
                                repeat(16) { output.write(0) }
                            }
                        }

                        val grid = Grid(FilePathResolver(tmpDirPath), sntFileName)
                        grid.nodeMap shouldHaveSize systems.filterNotNull().size
                    }

                    it("Empty if file not found") {
                        val grid = Grid(FilePathResolver(tmpDirPath), "empty.snt")
                        grid.nodeMap.shouldBeEmpty()
                    }
                }
            }

            describe("Functions") {
                val systems = ShipSystem.entries.take(3)
                val nodes =
                    systems.flatMap { system ->
                        List(5) { x ->
                            Node(Coordinate(x.toByte(), 0, system.ordinal.toByte()), system)
                        }
                    }
                val grid = Grid(nodes)

                describe("Get by coordinate") {
                    it("Exists") {
                        systems.forEach { system ->
                            repeat(5) { x ->
                                grid[x, 0, system.ordinal].shouldNotBeNull().system shouldBeEqual
                                    system
                            }
                        }
                    }

                    it("Does not exist") {
                        repeat(5) { x ->
                            for (z in 3 until 10) {
                                grid[x, 0, z].shouldBeNull()
                            }

                            for (y in 1 until 5) {
                                repeat(10) { z -> grid[x, y, z].shouldBeNull() }
                            }
                        }
                    }
                }

                it("Get nodes by system") {
                    ShipSystem.entries.forEach { system ->
                        val expectedSize = if (system.ordinal >= 3) 0 else 5
                        grid.getNodesBySystem(system) shouldHaveSize expectedSize
                    }
                }

                it("Apply damage") {
                    val damageScale = 0.046875f
                    repeat(5) { x ->
                        val amount = x * (x + 1) * damageScale
                        grid.applyDamage(Damage(Coordinate(x.toByte(), 0, 0), amount))

                        val nodeDamage = grid[x, 0, 0]!!.damage
                        if (x == 0) {
                            nodeDamage.shouldBeZero()
                        } else {
                            nodeDamage.shouldBeWithinPercentageOf(amount, damageScale / 10000.0)
                        }
                    }
                }

                it("Get damage by system") {
                    grid
                        .getDamageBySystem(ShipSystem.HALLWAY)
                        .shouldBeWithinPercentageOf(0.375, 0.0000001)

                    grid.getDamageBySystem(ShipSystem.BEAMS).shouldBeZero()
                    grid.getDamageBySystem(ShipSystem.WARP_JUMP_DRIVE).shouldBeZero()
                }

                it("Clear damage") {
                    grid.clearDamage()

                    repeat(5) { x -> grid[x, 0, 0]!!.damage.shouldBeZero() }

                    grid.getDamageBySystem(ShipSystem.HALLWAY).shouldBeZero()
                }
            }
        }
    })
