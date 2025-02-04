package artemis.agent.game.enemies

import artemis.agent.R
import com.walkertribe.ian.util.buildSortingComparator

data class EnemySorter(
    val sortBySurrendered: Boolean = false,
    val sortByFaction: Boolean = false,
    val sortByFactionReversed: Boolean = false,
    val sortByName: Boolean = false,
    val sortByDistance: Boolean = false,
) :
    Comparator<EnemyEntry> by buildSortingComparator(
        SURRENDERED_COMPARATOR to sortBySurrendered,
        FACTION_COMPARATOR.reversedIf(sortByFactionReversed) to sortByFaction,
        NAME_COMPARATOR to sortByName,
        DISTANCE_COMPARATOR to sortByDistance,
    ) {
    fun buildCategoryMap(enemies: List<EnemyEntry>): List<EnemySortCategory> =
        when {
            sortByFaction -> buildCategoryMap(enemies, sortBySurrendered) { it.faction.name }
            sortByName ->
                buildCategoryMap(enemies, sortBySurrendered) {
                    it.enemy.name.value?.substring(0, 1)
                }
            sortBySurrendered -> buildCategoryMap(enemies, true) { null }
            else -> listOf()
        }

    private companion object {
        val FACTION_COMPARATOR: Comparator<EnemyEntry> = compareBy { it.faction.id }

        val NAME_COMPARATOR: Comparator<EnemyEntry> = compareBy { it.enemy.name.value }

        val DISTANCE_COMPARATOR: Comparator<EnemyEntry> = compareBy { it.range }

        val SURRENDERED_COMPARATOR: Comparator<EnemyEntry> = compareBy {
            when {
                !it.enemy.isSurrendered.value.booleanValue -> -1
                it.captainStatus == EnemyCaptainStatus.DUPLICITOUS -> 0
                else -> 1
            }
        }

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

        fun <T> Comparator<T>.reversedIf(condition: Boolean): Comparator<T> =
            if (condition) reversed() else this
    }
}
