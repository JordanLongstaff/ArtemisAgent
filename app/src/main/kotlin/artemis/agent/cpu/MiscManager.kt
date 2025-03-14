package artemis.agent.cpu

import artemis.agent.game.misc.AudioEntry
import artemis.agent.game.misc.CommsActionEntry
import com.walkertribe.ian.enums.AudioMode
import com.walkertribe.ian.iface.Listener
import com.walkertribe.ian.protocol.core.comm.CommsButtonPacket
import com.walkertribe.ian.protocol.core.comm.IncomingAudioPacket
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MiscManager {
    private val mutActionsExist: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }
    private val mutAudioExists: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }

    val actionsExist: StateFlow<Boolean>
        get() = mutActionsExist.asStateFlow()

    val audioExists: StateFlow<Boolean>
        get() = mutAudioExists.asStateFlow()

    private val actionSet = CopyOnWriteArraySet<CommsActionEntry>()
    private val audioSet = CopyOnWriteArraySet<AudioEntry>()

    private val mutActions: MutableStateFlow<List<CommsActionEntry>> by lazy {
        MutableStateFlow(emptyList())
    }
    private val mutAudio: MutableStateFlow<List<AudioEntry>> by lazy {
        MutableStateFlow(emptyList())
    }

    val actions: StateFlow<List<CommsActionEntry>>
        get() = mutActions.asStateFlow()

    val audio: StateFlow<List<AudioEntry>>
        get() = mutAudio.asStateFlow()

    val showingAudio: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }

    var hasUpdate = false
        private set

    val shouldFlash: Boolean?
        get() = hasUpdate.takeIf { hasData }

    private val hasData: Boolean
        get() = mutActionsExist.value || mutAudioExists.value

    fun reset() {
        hasUpdate = false
        mutActionsExist.value = false
        mutAudioExists.value = false
        actionSet.clear()
        audioSet.clear()
        mutActions.value = emptyList()
        mutAudio.value = emptyList()
    }

    fun resetUpdate() {
        hasUpdate = false
    }

    @Listener
    fun onPacket(packet: CommsButtonPacket) {
        when (val action = packet.action) {
            is CommsButtonPacket.Action.RemoveAll -> {
                actionSet.clear()
                hasUpdate = false
            }
            is CommsButtonPacket.Action.Create -> {
                actionSet.add(CommsActionEntry(action.label))
                mutActionsExist.value = true
                hasUpdate = true
            }
            is CommsButtonPacket.Action.Remove -> {
                if (!actionSet.removeIf { it.label == action.label }) {
                    return
                }
            }
        }
        mutActions.value = actionSet.toList()
    }

    @Listener
    fun onPacket(packet: IncomingAudioPacket) {
        val audioMode = packet.audioMode as? AudioMode.Incoming ?: return

        audioSet.add(AudioEntry(packet.audioId, audioMode.title))
        mutAudio.value = audioSet.toList()
        mutAudioExists.value = true
        hasUpdate = true
    }

    fun dismissAudio(entry: AudioEntry) {
        if (audioSet.remove(entry)) {
            mutAudio.value = audioSet.toList()
        }
    }
}
