package artemis.agent.game.allies

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AllyStatusTestCase(
    @SerialName("status") val allyStatus: AllyStatus,
    @SerialName("pirate") val ifPirate: AllyStatus = allyStatus,
    @SerialName("notPirate") val ifNotPirate: AllyStatus = allyStatus,
    @SerialName("sortIndex") val expectedSortIndex: AllySortIndex = enumValueOf(ifNotPirate.name),
) {
    fun getStatusForPirate(isPirate: Boolean) = if (isPirate) ifPirate else ifNotPirate
}
