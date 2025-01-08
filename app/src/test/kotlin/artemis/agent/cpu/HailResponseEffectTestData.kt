package artemis.agent.cpu

import artemis.agent.game.allies.AllyStatus
import kotlinx.serialization.Serializable

@Serializable
data class HailResponseEffectTestData(
    val data: List<HailResponseEffectTestCase>,
    val ignored: List<String>,
)

@Serializable
data class HailResponseEffectTestCase(
    val response: HailResponseEffect,
    val status: AllyStatus = enumValueOf(response.name),
    val messages: List<String>,
)
