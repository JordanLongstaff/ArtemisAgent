package artemis.agent.game.biomechs

import com.walkertribe.ian.world.ArtemisNpc
import io.kotest.datatest.WithDataTestName
import kotlinx.serialization.Serializable

@Serializable
data class BiomechSorterTestData(
    val entries: List<TestBiomechEntry>,
    val sortingTests: List<BiomechSorterTestCase>,
)

@Serializable
data class TestBiomechEntry(val name: String, val hullId: Int) {
    fun toBiomech() =
        BiomechEntry(
            ArtemisNpc(0, 0L).also { npc ->
                npc.name.value = name
                npc.hullId.value = hullId
            }
        )
}

@Serializable
data class BiomechSorterTestCase(
    val sortByClassFirst: Boolean,
    val sortByClassSecond: Boolean,
    val sortByName: Boolean,
    val sortedIndices: List<Int>,
) : WithDataTestName {
    fun toSorter() =
        BiomechSorter(
            sortByClassFirst = sortByClassFirst,
            sortByStatus = false,
            sortByClassSecond = sortByClassSecond,
            sortByName = sortByName,
        )

    override fun dataTestName(): String =
        buildList {
                if (sortByClassFirst) add("Class (first)")
                if (sortByClassSecond) add("Class (second)")
                if (sortByName) add("Name")
            }
            .mapIndexed { index, s -> if (index == 0) s else s.lowercase() }
            .joinToString()
            .ifEmpty { "Unsorted" }
}
