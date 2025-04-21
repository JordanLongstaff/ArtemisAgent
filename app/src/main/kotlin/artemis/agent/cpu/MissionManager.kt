package artemis.agent.cpu

import artemis.agent.AgentViewModel
import artemis.agent.UserSettingsKt
import artemis.agent.UserSettingsOuterClass.UserSettings
import artemis.agent.game.ObjectEntry
import artemis.agent.game.missions.RewardType
import artemis.agent.game.missions.SideMissionEntry
import com.walkertribe.ian.iface.Listener
import com.walkertribe.ian.protocol.core.GameStartPacket
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket
import com.walkertribe.ian.world.BaseArtemisShielded
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class MissionManager(private val viewModel: AgentViewModel) {
    var enabled = true
    var confirmed = false

    val allMissions = CopyOnWriteArrayList<SideMissionEntry>()
    val missions: MutableSharedFlow<List<SideMissionEntry>> by lazy {
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    val payouts = IntArray(RewardType.entries.size)
    var displayedRewards: Array<RewardType> = emptyArray()
    val displayedPayouts: MutableStateFlow<List<Pair<RewardType, Int>>> by lazy {
        MutableStateFlow(emptyList())
    }
    val showingPayouts: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }

    var autoDismissCompletedMissions: Boolean = true
    var completedDismissalSeconds: Duration = DEFAULT_COMPLETED_DISMISSAL.seconds

    val newMissionPacket: MutableSharedFlow<CommsIncomingPacket> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val missionProgressPacket: MutableSharedFlow<CommsIncomingPacket> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val missionCompletionPacket: MutableSharedFlow<CommsIncomingPacket> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    var hasUpdate = false

    val shouldFlash: Boolean?
        get() = hasUpdate.takeIf { hasData }

    private val hasData: Boolean
        get() = enabled && confirmed

    fun reset() {
        hasUpdate = false
        allMissions.clear()
        payouts.fill(0)
        confirmed = false
    }

    @Listener
    fun onPacket(@Suppress("UNUSED_PARAMETER") packet: GameStartPacket) {
        updatePayouts()
    }

    fun purgeMissions(obj: BaseArtemisShielded<*>) {
        allMissions.removeAll(
            allMissions
                .filter {
                    !it.isCompleted &&
                        (it.destination.obj == obj || (!it.isStarted && it.source.obj == obj))
                }
                .toSet()
        )
    }

    fun updateFromSettings(settings: UserSettings) {
        enabled = settings.missionsEnabled
        reconcile(
            battery = settings.displayRewardBattery,
            coolant = settings.displayRewardCoolant,
            nukes = settings.displayRewardNukes,
            production = settings.displayRewardProduction,
            shieldBoost = settings.displayRewardShield,
        )

        autoDismissCompletedMissions = settings.completedMissionDismissalEnabled
        completedDismissalSeconds = settings.completedMissionDismissalSeconds.seconds
    }

    fun revertSettings(settings: UserSettingsKt.Dsl) {
        settings.missionsEnabled = enabled
        settings.completedMissionDismissalEnabled = autoDismissCompletedMissions
        settings.completedMissionDismissalSeconds = completedDismissalSeconds.inWholeSeconds.toInt()

        val rewardSettings =
            mapOf(
                RewardType.BATTERY to settings::displayRewardBattery,
                RewardType.COOLANT to settings::displayRewardCoolant,
                RewardType.NUKE to settings::displayRewardNukes,
                RewardType.PRODUCTION to settings::displayRewardProduction,
                RewardType.SHIELD to settings::displayRewardShield,
            )
        rewardSettings.values.forEach { it.set(false) }
        displayedRewards.forEach { rewardSettings[it]?.set(true) }
    }

    private fun reconcile(
        battery: Boolean,
        coolant: Boolean,
        nukes: Boolean,
        production: Boolean,
        shieldBoost: Boolean,
    ) {
        val oldRewards = displayedRewards
        val newRewards =
            listOfNotNull(
                    RewardType.BATTERY.takeIf { battery },
                    RewardType.COOLANT.takeIf { coolant },
                    RewardType.NUKE.takeIf { nukes },
                    RewardType.PRODUCTION.takeIf { production },
                    RewardType.SHIELD.takeIf { shieldBoost },
                )
                .toTypedArray()
        displayedRewards = newRewards

        var oldIndex = 0
        var newIndex = 0
        val allObjects =
            viewModel.livingStations.values.toList() + viewModel.allyShips.values.toList()
        RewardType.entries.forEach { reward ->
            var missionsSignum = 0

            val inOldSet = oldIndex < oldRewards.size && oldRewards[oldIndex] == reward
            if (inOldSet) {
                oldIndex++
                missionsSignum--
            }
            val inNewSet = newIndex < newRewards.size && newRewards[newIndex] == reward
            if (inNewSet) {
                newIndex++
                missionsSignum++
            }

            if (missionsSignum != 0) {
                allObjects.forEach {
                    it.missions += missionsSignum * calculateMissionsFor(it, reward)
                }
            }
        }

        updatePayouts()
    }

    fun updatePayouts() {
        displayedPayouts.value = displayedRewards.map { it to payouts[it.ordinal] }
    }

    private fun calculateMissionsFor(entry: ObjectEntry<*>, reward: RewardType): Int =
        allMissions
            .filter {
                val isDest = it.destination == entry
                if (it.isStarted) isDest && it.associatedShipName == viewModel.playerName
                else isDest || it.source == entry
            }
            .sumOf { it.rewards[reward.ordinal] }

    private companion object {
        const val DEFAULT_COMPLETED_DISMISSAL = 3
    }
}
