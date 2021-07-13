package artemis.agent.game.enemies

import artemis.agent.R

data class EnemySorter(
    val sortBySurrendered: Boolean = false,
    val sortByFaction: Boolean = false,
    val sortByFactionReversed: Boolean = false,
    val sortByName: Boolean = false,
    val sortByDistance: Boolean = false,
) : Comparator<EnemyEntry> {
    private val comparators: List<Comparator<EnemyEntry>> =
        mutableListOf<Comparator<EnemyEntry>>().apply {
            if (sortBySurrendered) add(SURRENDERED_COMPARATOR)
            if (sortByFaction) add(factionComparator(sortByFactionReversed))
            if (sortByName) add(NAME_COMPARATOR)
            if (sortByDistance) add(DISTANCE_COMPARATOR)
        }

    override fun compare(enemy1: EnemyEntry?, enemy2: EnemyEntry?): Int {
        for (comparator in comparators) {
            val result = comparator.compare(enemy1, enemy2)
            if (result != 0) return result
        }
        return 0
    }

    fun buildCategoryMap(enemies: List<EnemyEntry>): List<EnemySortCategory> = when {
        sortByFaction -> buildCategoryMap(enemies, sortBySurrendered) { it.faction.name }
        sortByName -> buildCategoryMap(enemies, sortBySurrendered) {
            it.enemy.nameString?.substring(0, 1)
        }
        sortBySurrendered -> buildCategoryMap(enemies, true) { null }
        else -> listOf()
    }

    private companion object {
        fun factionComparator(reversed: Boolean): Comparator<EnemyEntry> = buildComparator {
            val compare = it?.run { faction.id } ?: -1
            compare * if (reversed) -1 else 1
        }

        val NAME_COMPARATOR: Comparator<EnemyEntry> = buildComparator {
            it?.run { enemy.nameString } ?: ""
        }

        val DISTANCE_COMPARATOR: Comparator<EnemyEntry> = buildComparator {
            it?.range ?: Float.MAX_VALUE
        }

        val SURRENDERED_COMPARATOR: Comparator<EnemyEntry> = buildComparator {
            when {
                it == null || !it.enemy.isSurrendered.value.booleanValue -> -1
                it.captainStatus == EnemyCaptainStatus.DUPLICITOUS -> 0
                else -> 1
            }
        }

        fun <C : Comparable<C>> buildComparator(map: (EnemyEntry?) -> C): Comparator<EnemyEntry> =
            Comparator { e1, e2 -> map(e1).compareTo(map(e2)) }

        fun buildCategoryMap(
            enemies: List<EnemyEntry>,
            sortBySurrendered: Boolean,
            map: (EnemyEntry) -> String?,
        ): List<EnemySortCategory> {
            val categoryMap = mutableListOf<EnemySortCategory>()
            var prevCategory = ""

            for (i in enemies.indices) {
                val enemy = enemies[i]
                val category = map(enemy)
                if (category != null) {
                    if (category != prevCategory) {
                        categoryMap.add(EnemySortCategory.Text(category, i))
                        prevCategory = category
                    }
                } else if (sortBySurrendered && enemy.enemy.isSurrendered.value.booleanValue) {
                    if (categoryMap.isEmpty()) {
                        categoryMap.add(EnemySortCategory.Res(R.string.active, 0))
                    }

                    categoryMap.add(EnemySortCategory.Res(R.string.surrendered, i))
                    break
                }
            }

            return categoryMap
        }
    }
}
