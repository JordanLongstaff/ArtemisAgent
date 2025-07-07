package artemis.agent.game.enemies

import com.walkertribe.ian.util.FilePathResolver
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.world.ArtemisNpc
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldContainExactly
import java.io.File
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okio.Path.Companion.toOkioPath

@OptIn(ExperimentalSerializationApi::class)
class EnemySorterTest :
    DescribeSpec({
        describe("EnemySorter") {
            val (data, sortingTests) =
                EnemySorter::class.java.getResourceAsStream("entries.json")!!.use {
                    Json.decodeFromStream<EnemySorterTestData>(it)
                }

            val tmpDir = tempdir()
            val datDir = File(tmpDir, "dat")
            datDir.mkdir()
            val datFile = File(datDir, "vesselData.xml")
            datFile.writeText(EnemySorter::class.java.getResource("vesselData.xml")!!.readText())

            val vesselData = VesselData.load(FilePathResolver(tmpDir.toOkioPath()))

            val entries =
                data.map { entry ->
                    val faction = vesselData.getFaction(entry.hullId)!!
                    val vessel = vesselData[entry.hullId]!!
                    val enemy = ArtemisNpc(0, 0L).also { it.name.value = entry.name }
                    EnemyEntry(enemy, vessel, faction, vesselData).also {
                        it.range = entry.distance
                        entry.status.applyTo(it)
                    }
                }

            sortingTests.forEach { sortingTest ->
                describe(sortingTest.dataTestName()) {
                    val sorter = sortingTest.toSorter()
                    lateinit var actualSort: List<EnemyEntry>

                    it("Sorted correctly") {
                        val expectedSort = sortingTest.sortedIndices.map { entries[it] }
                        actualSort = entries.sortedWith(sorter)
                        actualSort shouldContainExactly expectedSort
                    }

                    it("Categories: ${sortingTest.categories.joinToString(" ")}") {
                        sorter.buildCategoryMap(actualSort) shouldContainExactly
                            sortingTest.categories.map { it.toSortCategory() }
                    }
                }
            }
        }
    })
