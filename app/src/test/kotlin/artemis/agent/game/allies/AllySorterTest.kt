package artemis.agent.game.allies

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@OptIn(ExperimentalSerializationApi::class)
class AllySorterTest :
    DescribeSpec({
        val (data, sortingTests) =
            AllySorter::class.java.getResourceAsStream("entries.json")!!.use {
                Json.decodeFromStream<AllySorterTestData>(it)
            }

        val entries = data.map { it.toAlly() }

        describe("AllySorter") {
            withData(sortingTests) { sortingTest ->
                val sorter = sortingTest.toSorter()

                val expectedSort = sortingTest.sortedIndices.map { entries[it] }
                val actualSort = entries.sortedWith(sorter)
                actualSort shouldContainExactly expectedSort
            }
        }
    })
