package artemis.agent.game.biomechs

import android.content.Context
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.SoundEffect
import com.walkertribe.ian.enums.EnemyMessage
import com.walkertribe.ian.protocol.core.comm.CommsOutgoingPacket
import com.walkertribe.ian.world.ArtemisNpc

data class BiomechEntry(val biomech: ArtemisNpc) : Comparable<BiomechEntry> {
    private var timesFrozen = 0
    var isFrozen = false
        private set
    private var freezeStartTime = 0L
    private var freezeSent = false
    private var isReadyToReanimate = false

    val canFreezeAgain: Boolean get() = timesFrozen < MAX_FREEZES

    fun getFrozenStatusText(viewModel: AgentViewModel, context: Context): String {
        val (minutes, seconds) =
            AgentViewModel.getTimeToEnd(freezeStartTime + viewModel.biomechFreezeTime)

        return when {
            seconds > 0 || minutes > 0 ->
                context.getString(R.string.biomech_frozen, minutes, seconds)
            !canFreezeAgain -> context.getString(R.string.biomech_cannot_freeze)
            isFrozen -> context.getString(R.string.biomech_will_move)
            else -> context.getString(R.string.biomech_moving)
        }
    }

    fun freeze(viewModel: AgentViewModel) {
        if (!canFreezeAgain) {
            return
        }

        if (isFrozen && !isReadyToReanimate) {
            return
        }

        freezeSent = true
        viewModel.playSound(SoundEffect.BEEP_1)
        viewModel.sendToServer(
            CommsOutgoingPacket(
                biomech,
                EnemyMessage.entries[++timesFrozen],
                viewModel.vesselData
            )
        )
    }

    fun onFreezeResponse() {
        if (freezeSent) {
            freezeSent = false
            isFrozen = true
            isReadyToReanimate = false
            freezeStartTime = System.currentTimeMillis()
        }
    }

    fun onFreezeTimeExpired(elapsedTime: Long): Boolean {
        if (
            !isFrozen ||
            isReadyToReanimate ||
            elapsedTime < freezeStartTime
        ) {
            return false
        }
        isReadyToReanimate = true
        return true
    }

    fun onFreezeEnd() {
        if (!isReadyToReanimate) {
            return
        }
        isFrozen = false
        isReadyToReanimate = false
    }

    override fun compareTo(other: BiomechEntry): Int =
        when (isFrozen to other.isFrozen) {
            true to true -> freezeStartTime.compareTo(other.freezeStartTime)
            false to false -> timesFrozen - other.timesFrozen
            true to false -> UNFROZEN_COMPARE - other.timesFrozen * 2
            else -> timesFrozen * 2 - UNFROZEN_COMPARE
        }

    companion object {
        const val MAX_FREEZES = 3
        private const val UNFROZEN_COMPARE = 5
    }
}
