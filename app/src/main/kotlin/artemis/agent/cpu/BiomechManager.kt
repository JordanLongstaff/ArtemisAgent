package artemis.agent.cpu

import artemis.agent.UserSettingsKt
import artemis.agent.UserSettingsOuterClass.UserSettings
import artemis.agent.game.biomechs.BiomechEntry
import artemis.agent.game.biomechs.BiomechRageStatus
import artemis.agent.game.biomechs.BiomechSorter
import com.walkertribe.ian.iface.Listener
import com.walkertribe.ian.protocol.core.world.BiomechRagePacket
import com.walkertribe.ian.world.ArtemisNpc
import com.walkertribe.ian.world.Property
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class BiomechManager {
    var enabled = true
    var confirmed = true

    val allBiomechs: MutableSharedFlow<List<BiomechEntry>> by lazy {
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val scanned = CopyOnWriteArrayList<BiomechEntry>()
    val unscanned = ConcurrentHashMap<Int, ArtemisNpc>()

    val rageProperty = Property.IntProperty(Long.MIN_VALUE)
    val rageStatus: MutableStateFlow<BiomechRageStatus> by lazy {
        MutableStateFlow(BiomechRageStatus.NEUTRAL)
    }

    val nextActiveBiomech: MutableSharedFlow<BiomechEntry> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val destroyedBiomechName: MutableSharedFlow<String> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    var sorter = BiomechSorter()

    var freezeTime: Long = DEFAULT_FREEZE_TIME.seconds.inWholeMilliseconds
        set(value) {
            field = value.seconds.inWholeMilliseconds
        }

    var hasUpdate = false

    val shouldFlash: Boolean?
        get() = hasUpdate.takeIf { hasData }

    private val hasData: Boolean
        get() = enabled && confirmed

    fun reset() {
        confirmed = false
        hasUpdate = false
        rageProperty.value = 0
        rageStatus.value = BiomechRageStatus.NEUTRAL
        allBiomechs.tryEmit(emptyList())
        scanned.clear()
        unscanned.clear()
    }

    @Listener
    fun onPacket(packet: BiomechRagePacket) {
        val newRage = Property.IntProperty(packet.timestamp)
        newRage.value = packet.rage
        newRage updates rageProperty
        rageStatus.value =
            BiomechRageStatus[rageProperty.value].also {
                if (
                    rageStatus.value == BiomechRageStatus.NEUTRAL && it == BiomechRageStatus.HOSTILE
                ) {
                    hasUpdate = true
                }
            }
    }

    fun updateFromSettings(settings: UserSettings) {
        enabled = settings.biomechsEnabled
        sorter =
            BiomechSorter(
                sortByClassFirst = settings.biomechSortClassFirst,
                sortByStatus = settings.biomechSortStatus,
                sortByClassSecond = settings.biomechSortClassSecond,
                sortByName = settings.biomechSortName,
            )
        freezeTime = settings.freezeDurationSeconds.toLong()
    }

    fun revertSettings(settings: UserSettingsKt.Dsl) {
        settings.biomechsEnabled = enabled
        settings.freezeDurationSeconds = freezeTime.milliseconds.inWholeSeconds.toInt()
        settings.biomechSortClassFirst = sorter.sortByClassFirst
        settings.biomechSortStatus = sorter.sortByStatus
        settings.biomechSortClassSecond = sorter.sortByClassSecond
        settings.biomechSortName = sorter.sortByName
    }

    private companion object {
        const val DEFAULT_FREEZE_TIME = 220
    }
}
