package artemis.agent.game.enemies

import android.content.Context
import androidx.core.content.ContextCompat
import artemis.agent.R
import com.walkertribe.ian.enums.EnemyMessage
import com.walkertribe.ian.vesseldata.Faction
import com.walkertribe.ian.vesseldata.Taunt
import com.walkertribe.ian.vesseldata.Vessel
import com.walkertribe.ian.world.ArtemisNpc

data class EnemyEntry(val enemy: ArtemisNpc, val vessel: Vessel, val faction: Faction) {
    var heading: String = ""
    var range: Float = 0f

    var tauntCount: Int = 0
    var lastTaunt: EnemyMessage? = null

    var intel: String? = null

    var captainStatus: EnemyCaptainStatus = EnemyCaptainStatus.NO_INTEL

    val tauntStatuses = Array(Taunt.COUNT) { TauntStatus.UNUSED }

    fun getTauntCountText(context: Context): String = when {
        tauntStatuses.all { it == TauntStatus.INEFFECTIVE } ->
            context.getString(R.string.cannot_taunt)
        tauntCount < TAUNT_COUNT_STRINGS.size -> context.getString(TAUNT_COUNT_STRINGS[tauntCount])
        else -> context.getString(R.string.taunts_many, tauntCount)
    }

    fun getBackgroundColor(context: Context): Int = ContextCompat.getColor(
        context,
        when {
            captainStatus == EnemyCaptainStatus.DUPLICITOUS -> R.color.duplicitousOrange
            enemy.isSurrendered.value.booleanValue -> R.color.surrenderedYellow
            else -> R.color.enemyRed
        }
    )

    private companion object {
        val TAUNT_COUNT_STRINGS = arrayOf(
            R.string.taunts_zero,
            R.string.taunts_one,
            R.string.taunts_two,
        )
    }
}
