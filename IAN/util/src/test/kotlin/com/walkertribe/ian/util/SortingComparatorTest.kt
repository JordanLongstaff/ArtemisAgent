package com.walkertribe.ian.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual

class SortingComparatorTest : DescribeSpec({
    val unsortedTriples = listOf(
        Triple("B", 1, true),
        Triple("A", 1, false),
        Triple("A", 2, true),
        Triple("A", 1, true),
        Triple("B", 2, false),
        Triple("B", 1, false),
        Triple("B", 2, true),
        Triple("A", 2, false),
    )

    val firstComparator = compareBy<Triple<String, Int, Boolean>> { it.first }
    val secondComparator = compareBy<Triple<String, Int, Boolean>> { it.second }
    val thirdComparator = compareBy<Triple<String, Int, Boolean>> { it.third }

    describe("buildSortingComparator") {
        withData(
            nameFn = { it.first },
            listOf("Sort first", "Don't sort first").zip(
                SortingTestCase.entries.partition { it.sortByFirst }.toList()
            ),
        ) { (_, firstTestCaseSet) ->
            withData(
                nameFn = { it.first },
                listOf("Sort second", "Don't sort second").zip(
                    firstTestCaseSet.partition { it.sortBySecond }.toList()
                ),
            ) { (_, secondTestCaseSet) ->
                withData(
                    nameFn = { it.first },
                    listOf("Sort third", "Don't sort third").zip(
                        secondTestCaseSet.partition { it.sortByThird }.toList()
                    ),
                ) { (_, thirdTestCaseSet) ->
                    thirdTestCaseSet.forEach { sortingTestCase ->
                        val sorter = buildSortingComparator(
                            firstComparator to sortingTestCase.sortByFirst,
                            secondComparator to sortingTestCase.sortBySecond,
                            thirdComparator to sortingTestCase.sortByThird,
                        )

                        val expectedSort = sortingTestCase.expectedSortIndices.map {
                            unsortedTriples[it]
                        }
                        val actualSort = unsortedTriples.sortedWith(sorter)
                        actualSort shouldBeEqual expectedSort
                    }
                }
            }
        }
    }
})

enum class SortingTestCase(
    val sortByFirst: Boolean,
    val sortBySecond: Boolean,
    val sortByThird: Boolean,
    vararg val expectedSortIndices: Int,
) {
    FIRST_SECOND_THIRD(true, true, true, 1, 3, 7, 2, 5, 0, 4, 6),
    FIRST_SECOND(true, true, false, 1, 3, 2, 7, 0, 5, 4, 6),
    FIRST_THIRD(true, false, true, 1, 7, 2, 3, 4, 5, 0, 6),
    FIRST(true, false, false, 1, 2, 3, 7, 0, 4, 5, 6),
    SECOND_THIRD(false, true, true, 1, 5, 0, 3, 4, 7, 2, 6),
    SECOND(false, true, false, 0, 1, 3, 5, 2, 4, 6, 7),
    THIRD(false, false, true, 1, 4, 5, 7, 0, 2, 3, 6),
    NO_SORT(false, false, false, 0, 1, 2, 3, 4, 5, 6, 7),
}
