package artemis.agent.game.enemies

import artemis.agent.R
import com.walkertribe.ian.util.BoolState
import io.kotest.datatest.WithDataTestName
import kotlinx.serialization.Serializable

@Serializable
data class EnemySorterTestData(
    val entries: List<TestEnemyEntry>,
    val sortingTests: List<EnemySorterTestCase>,
)

@Suppress("unused")
enum class EnemyStatus {
    HOSTILE {
        override fun applyTo(entry: EnemyEntry) {
            // Do nothing
        }
    },
    DUPLICITOUS {
        override fun applyTo(entry: EnemyEntry) {
            entry.captainStatus = EnemyCaptainStatus.DUPLICITOUS
            entry.enemy.isSurrendered.value = BoolState.True
        }
    },
    SURRENDERED {
        override fun applyTo(entry: EnemyEntry) {
            entry.enemy.isSurrendered.value = BoolState.True
        }
    };

    abstract fun applyTo(entry: EnemyEntry)
}

@Serializable
data class TestEnemyEntry(
    val name: String,
    val hullId: Int,
    val status: EnemyStatus,
    val distance: Float,
)

enum class FactionSort {
    NONE,
    FORWARD,
    REVERSE,
}

@Serializable
data class EnemySorterTestCase(
    val sortBySurrendered: Boolean,
    val sortByFaction: FactionSort,
    val sortByName: Boolean,
    val sortByDistance: Boolean,
    val sortedIndices: List<Int>,
    val categories: List<TestSortCategory>,
) : WithDataTestName {
    fun toSorter() =
        EnemySorter(
            sortBySurrendered = sortBySurrendered,
            sortByFaction = sortByFaction != FactionSort.NONE,
            sortByFactionReversed = sortByFaction == FactionSort.REVERSE,
            sortByName = sortByName,
            sortByDistance = sortByDistance,
        )

    override fun dataTestName(): String =
        buildList {
                if (sortBySurrendered) add("Surrender status")

                when (sortByFaction) {
                    FactionSort.NONE -> {}
                    FactionSort.FORWARD -> add("Faction")
                    FactionSort.REVERSE -> add("Faction reversed")
                }

                if (sortByName) add("Name")
                if (sortByDistance) add("Distance")
            }
            .mapIndexed { index, s -> if (index == 0) s else s.lowercase() }
            .joinToString()
            .ifEmpty { "Unsorted" }
}

@Serializable
sealed interface TestSortCategory {
    @Serializable
    data class Res(val res: String, val scrollIndex: Int) : TestSortCategory {
        private val resId = RES_MAP[res]!!

        override fun toSortCategory(): EnemySortCategory = EnemySortCategory.Res(resId, scrollIndex)

        private companion object {
            val RES_MAP = mapOf("Active" to R.string.active, "Surrendered" to R.string.surrendered)
        }
    }

    @Serializable
    data class Text(val text: String, val scrollIndex: Int) : TestSortCategory {
        override fun toSortCategory(): EnemySortCategory = EnemySortCategory.Text(text, scrollIndex)
    }

    fun toSortCategory(): EnemySortCategory
}
