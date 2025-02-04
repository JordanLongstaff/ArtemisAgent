package artemis.agent.cpu

import artemis.agent.game.allies.AllyStatus
import kotlinx.serialization.Serializable

@Serializable
data class HailResponseEffectTestData(
    val data: List<HailResponseEffectTestCase>,
    val ignored: List<HailResponseEffectTestMessage>,
)

@Serializable
data class HailResponseEffectTestCase(
    val response: HailResponseEffect,
    val status: AllyStatus = enumValueOf(response.name),
    val messages: Set<HailResponseEffectTestMessage>,
)

@Serializable
data class HailResponseEffectTestMessage(val long: String, val short: String) {
    override fun toString(): String = long

    override fun hashCode(): Int = long.hashCode()

    override fun equals(other: Any?): Boolean =
        other is HailResponseEffectTestMessage && other.long == long

    fun hasEnergy(): HailResponseEffectTestMessage =
        HailResponseEffectTestMessage(long + HAS_ENERGY, short + HAS_ENERGY_SHORT)

    private companion object {
        const val HAS_ENERGY_SHORT = "..need some."
        const val HAS_ENERGY = "  We also have energy to spare, if you need some."
    }
}
