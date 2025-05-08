package artemis.agent.game.biomechs

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@OptIn(ExperimentalSerializationApi::class)
class BiomechSorterTest :
    DescribeSpec({
        describe("BiomechSorter") {
            val (data, sortingTests) =
                BiomechSorter::class.java.getResourceAsStream("entries.json")!!.use {
                    Json.decodeFromStream<BiomechSorterTestData>(it)
                }

            val entries = data.map { it.toBiomech() }

            withData(sortingTests) { sortingTest ->
                val sorter = sortingTest.toSorter()

                val expectedSort = sortingTest.sortedIndices.map { entries[it] }
                val actualSort = entries.sortedWith(sorter)
                actualSort shouldContainExactly expectedSort
            }
        }
    })
