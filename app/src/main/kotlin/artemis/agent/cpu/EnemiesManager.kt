package artemis.agent.cpu

import androidx.lifecycle.viewModelScope
import artemis.agent.AgentViewModel
import artemis.agent.UserSettingsKt
import artemis.agent.UserSettingsOuterClass.UserSettings
import artemis.agent.game.enemies.EnemyCaptainStatus
import artemis.agent.game.enemies.EnemyEntry
import artemis.agent.game.enemies.EnemySortCategory
import artemis.agent.game.enemies.EnemySorter
import artemis.agent.game.enemies.TauntStatus
import artemis.agent.util.SoundEffect
import com.walkertribe.ian.enums.EnemyMessage
import com.walkertribe.ian.enums.IntelType
import com.walkertribe.ian.iface.Listener
import com.walkertribe.ian.protocol.core.comm.CommsOutgoingPacket
import com.walkertribe.ian.protocol.core.world.IntelPacket
import com.walkertribe.ian.vesseldata.Taunt
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class EnemiesManager {
    var enabled: Boolean = true

    val selection: MutableStateFlow<EnemyEntry?> by lazy { MutableStateFlow(null) }
    val selectionIndex: MutableStateFlow<Int> by lazy { MutableStateFlow(-1) }

    val displayedEnemies: MutableSharedFlow<List<EnemyEntry>> by lazy {
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val categories: MutableStateFlow<List<EnemySortCategory>> by lazy {
        MutableStateFlow(emptyList())
    }

    val taunts: MutableStateFlow<List<Pair<Taunt, TauntStatus>>> by lazy {
        MutableStateFlow(emptyList())
    }
    val intel: MutableStateFlow<String?> by lazy { MutableStateFlow(null) }

    val destroyedEnemyName: MutableSharedFlow<String> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    val perfidy: MutableSharedFlow<EnemyEntry> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    var sorter = EnemySorter()
        private set

    val nameIndex = ConcurrentHashMap<String, Int>()
    val allEnemies = ConcurrentHashMap<Int, EnemyEntry>()

    var surrenderBurstCount: Int = DEFAULT_SURRENDER_BURST_COUNT
    var surrenderBurstInterval: Int = DEFAULT_SURRENDER_BURST_INTERVAL

    var showTauntStatuses = true
    var showIntel = true
    var disableIneffectiveTaunts = true
    var maxSurrenderDistance: Float? = DEFAULT_SURRENDER_DISTANCE
    var hasUpdate = false

    val shouldFlash: Boolean?
        get() = hasUpdate.takeIf { hasData }

    private val hasData: Boolean
        get() = enabled && allEnemies.isNotEmpty()

    fun addEnemy(enemy: EnemyEntry, name: String) {
        val id = enemy.enemy.id
        allEnemies[id] = enemy
        nameIndex[name] = id
    }

    fun getEnemyByName(name: String): EnemyEntry? = nameIndex[name]?.let(allEnemies::get)

    fun isEnemyInRange(enemy: EnemyEntry): Boolean =
        maxSurrenderDistance?.let { enemy.range < it } != false

    fun sendSurrenderBurst(enemy: EnemyEntry, viewModel: AgentViewModel) {
        with(viewModel) {
            activateHaptic()
            playSound(SoundEffect.BEEP_2)
            viewModelScope.launch {
                val surrenderPacket =
                    CommsOutgoingPacket(enemy.enemy, EnemyMessage.WILL_YOU_SURRENDER, vesselData)
                for (i in surrenderBurstCount - 1 downTo 0) {
                    if (enemy.enemy.isSurrendered.value.booleanValue || !isEnemyInRange(enemy)) {
                        enemy.pendingSurrenders = 0
                        break
                    }
                    sendToServer(surrenderPacket)
                    enemy.pendingSurrenders = i
                    if (i > 0) {
                        delay(surrenderBurstInterval.toLong())
                    }
                }
            }
        }
    }

    @Listener
    fun onIntel(packet: IntelPacket) {
        if (packet.intelType != IntelType.LEVEL_2_SCAN) return

        val enemy = allEnemies[packet.id] ?: return
        val taunts = enemy.faction.taunts

        val intel = packet.intel
        enemy.intel = intel

        val description = intel.substring(INTEL_PREFIX_LENGTH)
        val tauntIndex = taunts.indexOfFirst { taunt -> description.startsWith(taunt.immunity) }
        val immunityEnd =
            if (tauntIndex < 0) description.indexOf(',') else taunts[tauntIndex].immunity.length

        val rest = description.substring(immunityEnd)
        val captainStatus =
            if (rest.startsWith(CAPTAIN_STATUS_PREFIX)) {
                val status = rest.substring(CAPTAIN_STATUS_PREFIX.length)
                EnemyCaptainStatus.entries.find {
                    status.startsWith(it.name.lowercase().replace('_', ' '))
                }
            } else {
                null
            }
        enemy.captainStatus = captainStatus ?: EnemyCaptainStatus.NORMAL
        if (tauntIndex >= 0 && captainStatus != EnemyCaptainStatus.EASILY_OFFENDED) {
            enemy.tauntStatuses[tauntIndex] = TauntStatus.INEFFECTIVE
        }
    }

    fun reset() {
        hasUpdate = false
        allEnemies.clear()
        nameIndex.clear()
        selection.value = null
    }

    fun refreshTaunts() {
        val enemy = selection.value
        taunts.value = enemy?.run { faction.taunts.zip(tauntStatuses) }.orEmpty()
    }

    fun updateFromSettings(settings: UserSettings) {
        enabled = settings.enemiesEnabled
        sorter =
            EnemySorter(
                sortBySurrendered = settings.enemySortSurrendered,
                sortByFaction = settings.enemySortFaction,
                sortByFactionReversed = settings.enemySortFactionReversed,
                sortByName = settings.enemySortName,
                sortByDistance = settings.enemySortDistance,
            )
        maxSurrenderDistance = settings.surrenderRange.takeIf { settings.surrenderRangeEnabled }
        showTauntStatuses = settings.showTauntStatuses
        showIntel = settings.showEnemyIntel
        disableIneffectiveTaunts = settings.disableIneffectiveTaunts
        surrenderBurstCount = settings.surrenderBurstCount
        surrenderBurstInterval = settings.surrenderBurstInterval
    }

    fun revertSettings(settings: UserSettingsKt.Dsl) {
        settings.enemiesEnabled = enabled
        settings.enemySortSurrendered = sorter.sortBySurrendered
        settings.enemySortFaction = sorter.sortByFaction
        settings.enemySortFactionReversed = sorter.sortByFactionReversed
        settings.enemySortName = sorter.sortByName
        settings.enemySortDistance = sorter.sortByDistance
        settings.surrenderRangeEnabled =
            maxSurrenderDistance?.also { settings.surrenderRange = it } != null
        settings.showTauntStatuses = showTauntStatuses
        settings.showEnemyIntel = showIntel
        settings.disableIneffectiveTaunts = disableIneffectiveTaunts
        settings.surrenderBurstCount = surrenderBurstCount
        settings.surrenderBurstInterval = surrenderBurstInterval
    }

    private companion object {
        const val DEFAULT_SURRENDER_BURST_COUNT = 1
        const val DEFAULT_SURRENDER_BURST_INTERVAL = 500
        const val DEFAULT_SURRENDER_DISTANCE = 5000f
        const val INTEL_PREFIX_LENGTH = 19
        const val CAPTAIN_STATUS_PREFIX = ", and is "
    }
}
