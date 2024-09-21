package artemis.agent.game.allies

import artemis.agent.game.ObjectEntry
import artemis.agent.game.buildSortingComparator

data class AllySorter(
    val sortByClassFirst: Boolean = false,
    val sortByEnergy: Boolean = false,
    val sortByStatus: Boolean = false,
    val sortByClassSecond: Boolean = false,
    val sortByName: Boolean = false,
) : Comparator<ObjectEntry.Ally> by buildSortingComparator(
    CLASS_COMPARATOR to sortByClassFirst,
    ENERGY_COMPARATOR to sortByEnergy,
    STATUS_COMPARATOR to sortByStatus,
    CLASS_COMPARATOR to sortByClassSecond,
    NAME_COMPARATOR to sortByName,
) {
    private companion object {
        val CLASS_COMPARATOR: Comparator<ObjectEntry.Ally> = compareByDescending {
            it.obj.hullId.value
        }

        val ENERGY_COMPARATOR: Comparator<ObjectEntry.Ally> = compareByDescending {
            if (it.hasEnergy) 1 else 0
        }

        val STATUS_COMPARATOR: Comparator<ObjectEntry.Ally> = compareByDescending {
            it.status.sortIndex.ordinal
        }

        val NAME_COMPARATOR: Comparator<ObjectEntry.Ally> = compareBy { it.obj.name.value ?: "" }
    }
}
