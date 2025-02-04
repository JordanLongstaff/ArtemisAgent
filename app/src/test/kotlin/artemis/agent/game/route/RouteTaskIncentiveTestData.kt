package artemis.agent.game.route

import artemis.agent.game.allies.AllyStatus
import kotlinx.serialization.Serializable

@Serializable
data class RouteTaskIncentiveTestData(
    val status: AllyStatus,
    val incentive: RouteTaskIncentive,
    val energy: Boolean = false,
    val text: String,
)
