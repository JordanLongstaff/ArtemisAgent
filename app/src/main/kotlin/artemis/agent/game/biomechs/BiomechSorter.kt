package artemis.agent.game.biomechs

data class BiomechSorter(
    val sortByClassFirst: Boolean = false,
    val sortByStatus: Boolean = false,
    val sortByClassSecond: Boolean = false,
    val sortByName: Boolean = false,
) : Comparator<BiomechEntry> {
    private val comparators: List<Comparator<BiomechEntry>> =
        mutableListOf<Comparator<BiomechEntry>>().apply {
            if (sortByClassFirst) add(CLASS_COMPARATOR)
            if (sortByStatus) add(STATUS_COMPARATOR)
            if (sortByClassSecond) add(CLASS_COMPARATOR)
            if (sortByName) add(NAME_COMPARATOR)
        }

    override fun compare(ally1: BiomechEntry?, ally2: BiomechEntry?): Int {
        for (comparator in comparators) {
            val result = comparator.compare(ally1, ally2)
            if (result != 0) return result
        }
        return 0
    }

    private companion object {
        val CLASS_COMPARATOR: Comparator<BiomechEntry> = Comparator { b1, b2 ->
            (b2?.biomech?.hullId?.value ?: 0).compareTo(b1?.biomech?.hullId?.value ?: 0)
        }

        val STATUS_COMPARATOR: Comparator<BiomechEntry> = Comparator { b1, b2 ->
            b2?.let { b1?.compareTo(it) } ?: 0
        }

        val NAME_COMPARATOR: Comparator<BiomechEntry> = Comparator { b1, b2 ->
            val firstName = b1?.biomech?.nameString ?: ""
            val secondName = b2?.biomech?.nameString ?: ""
            firstName.compareTo(secondName)
        }
    }
}
