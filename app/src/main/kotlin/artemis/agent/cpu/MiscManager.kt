package artemis.agent.cpu

import artemis.agent.game.misc.AudioEntry
import artemis.agent.game.misc.CommsActionEntry
import com.walkertribe.ian.enums.AudioMode
import com.walkertribe.ian.iface.Listener
import com.walkertribe.ian.protocol.core.comm.CommsButtonPacket
import com.walkertribe.ian.protocol.core.comm.IncomingAudioPacket
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.flow.MutableStateFlow

class MiscManager {
    val actionsExist: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }
    val audioExists: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }

    val actionSet = CopyOnWriteArraySet<CommsActionEntry>()
    val audioSet = CopyOnWriteArraySet<AudioEntry>()

    val actions: MutableStateFlow<List<CommsActionEntry>> by lazy { MutableStateFlow(emptyList()) }
    val audio: MutableStateFlow<List<AudioEntry>> by lazy { MutableStateFlow(emptyList()) }

    val showingAudio: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }

    var hasUpdate = false

    val shouldFlash: Boolean?
        get() = hasUpdate.takeIf { hasData }

    val hasData: Boolean
        get() = actionsExist.value || audioExists.value

    fun reset() {
        hasUpdate = false
        actionsExist.value = false
        audioExists.value = false
        actionSet.clear()
        audioSet.clear()
        actions.value = emptyList()
        audio.value = emptyList()
    }

    @Listener
    fun onPacket(packet: CommsButtonPacket) {
        when (val action = packet.action) {
            is CommsButtonPacket.Action.RemoveAll -> {
                actionSet.clear()
            }
            is CommsButtonPacket.Action.Create -> {
                actionSet.add(CommsActionEntry(action.label))
                actionsExist.value = true
                hasUpdate = true
            }
            is CommsButtonPacket.Action.Remove -> {
                if (!actionSet.removeIf { it.label == action.label }) {
                    return
                }
            }
        }
        actions.value = actionSet.toList()
    }

    @Listener
    fun onPacket(packet: IncomingAudioPacket) {
        val audioMode = packet.audioMode
        if (audioMode is AudioMode.Incoming) {
            audioSet.add(AudioEntry(packet.audioId, audioMode.title))
            audio.value = audioSet.toList()
            audioExists.value = true
            hasUpdate = true
        }
    }

    fun dismissAudio(entry: AudioEntry) {
        if (audioSet.remove(entry)) {
            audio.value = audioSet.toList()
        }
    }
}
