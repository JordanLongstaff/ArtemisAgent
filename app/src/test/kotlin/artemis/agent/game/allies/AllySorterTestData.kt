package artemis.agent.game.allies

import artemis.agent.game.ObjectEntry
import com.walkertribe.ian.vesseldata.Empty
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.world.ArtemisNpc
import io.kotest.engine.names.WithDataTestName
import kotlinx.serialization.Serializable

@Serializable
data class AllySorterTestData(
    val entries: List<TestAllyEntry>,
    val sortingTests: List<AllySorterTestCase>,
)

@Serializable
data class TestAllyEntry(
    val name: String,
    val hullId: Int,
    val status: AllyStatus,
    val energy: Boolean,
) {
    fun toAlly() =
        ObjectEntry.Ally(
                npc = ArtemisNpc(0, 0L),
                vesselData = VesselData.Empty,
                isDeepStrikeShip = false,
            )
            .also { entry ->
                entry.status = status
                entry.hasEnergy = energy
                entry.obj.name.value = name
                entry.obj.hullId.value = hullId
            }
}

@Serializable
data class AllySorterTestCase(
    val sortByClassFirst: Boolean,
    val sortByEnergy: Boolean,
    val sortByStatus: Boolean,
    val sortByClassSecond: Boolean,
    val sortByName: Boolean,
    val sortedIndices: List<Int>,
) : WithDataTestName {
    fun toSorter() =
        AllySorter(
            sortByClassFirst = sortByClassFirst,
            sortByEnergy = sortByEnergy,
            sortByStatus = sortByStatus,
            sortByClassSecond = sortByClassSecond,
            sortByName = sortByName,
        )

    override fun dataTestName(): String =
        buildList {
                if (sortByClassFirst) add("Class")
                if (sortByEnergy) add("Energy")
                if (sortByStatus) add("Status")
                if (sortByClassSecond) add("Class")
                if (sortByName) add("Name")
            }
            .mapIndexed { index, s -> if (index == 0) s else s.lowercase() }
            .joinToString()
            .ifEmpty { "Unsorted" }
}
