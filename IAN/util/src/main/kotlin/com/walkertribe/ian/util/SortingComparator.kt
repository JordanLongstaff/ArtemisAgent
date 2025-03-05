package com.walkertribe.ian.util

inline fun <reified T> buildSortingComparator(
    vararg comparators: Pair<Comparator<T>, Boolean>
): Comparator<T> =
    comparators
        .filter { it.second }
        .map { it.first }
        .let { list ->
            Comparator { t1, t2 ->
                list.firstNotNullOfOrNull { comparator ->
                    comparator.compare(t1, t2).takeIf { it != 0 }
                } ?: 0
            }
        }
