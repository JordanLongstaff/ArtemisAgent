package artemis.agent.game.biomechs

import com.walkertribe.ian.util.buildSortingComparator

data class BiomechSorter(
    val sortByClassFirst: Boolean = false,
    val sortByStatus: Boolean = false,
    val sortByClassSecond: Boolean = false,
    val sortByName: Boolean = false,
) :
    Comparator<BiomechEntry> by buildSortingComparator(
        CLASS_COMPARATOR to sortByClassFirst,
        STATUS_COMPARATOR to sortByStatus,
        CLASS_COMPARATOR to sortByClassSecond,
        NAME_COMPARATOR to sortByName,
    ) {
    private companion object {
        val CLASS_COMPARATOR: Comparator<BiomechEntry> = compareByDescending { it.biomech.hullId }

        val STATUS_COMPARATOR: Comparator<BiomechEntry> = reverseOrder()

        val NAME_COMPARATOR: Comparator<BiomechEntry> = compareBy { it.biomech.name.value }
    }
}
