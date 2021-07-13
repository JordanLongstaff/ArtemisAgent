package artemis.agent.game.allies

import artemis.agent.game.ObjectEntry

data class AllySorter(
    val sortByClassFirst: Boolean = false,
    val sortByEnergy: Boolean = false,
    val sortByStatus: Boolean = false,
    val sortByClassSecond: Boolean = false,
    val sortByName: Boolean = false,
) : Comparator<ObjectEntry.Ally> {
    private val comparators: List<Comparator<ObjectEntry.Ally>> =
        mutableListOf<Comparator<ObjectEntry.Ally>>().apply {
            if (sortByClassFirst) add(CLASS_COMPARATOR)
            if (sortByEnergy) add(ENERGY_COMPARATOR)
            if (sortByStatus) add(STATUS_COMPARATOR)
            if (sortByClassSecond) add(CLASS_COMPARATOR)
            if (sortByName) add(NAME_COMPARATOR)
        }

    override fun compare(ally1: ObjectEntry.Ally?, ally2: ObjectEntry.Ally?): Int {
        for (comparator in comparators) {
            val result = comparator.compare(ally1, ally2)
            if (result != 0) return result
        }
        return 0
    }

    private companion object {
        val CLASS_COMPARATOR: Comparator<ObjectEntry.Ally> = buildComparator {
            obj.hullId.value
        }

        val ENERGY_COMPARATOR: Comparator<ObjectEntry.Ally> = buildComparator {
            if (hasEnergy) 1 else 0
        }

        val STATUS_COMPARATOR: Comparator<ObjectEntry.Ally> = buildComparator {
            status.sortIndex.ordinal
        }

        val NAME_COMPARATOR: Comparator<ObjectEntry.Ally> = Comparator { ally1, ally2 ->
            val firstName = ally1?.obj?.nameString ?: ""
            val secondName = ally2?.obj?.nameString ?: ""
            firstName.compareTo(secondName)
        }

        fun buildComparator(map: ObjectEntry.Ally.() -> Int): Comparator<ObjectEntry.Ally> =
            Comparator { ally1, ally2 ->
                (ally2?.map() ?: 0).compareTo(ally1?.map() ?: 0)
            }
    }
}
