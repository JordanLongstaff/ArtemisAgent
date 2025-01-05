package artemis.agent.game.misc

import com.walkertribe.ian.enums.AudioCommand
import com.walkertribe.ian.protocol.core.comm.AudioCommandPacket

data class AudioEntry(val audioId: Int, val title: String) {
    val playPacket by lazy { AudioCommandPacket(audioId, AudioCommand.PLAY) }
    val dismissPacket by lazy { AudioCommandPacket(audioId, AudioCommand.DISMISS) }

    override fun equals(other: Any?): Boolean = other is AudioEntry && other.audioId == audioId

    override fun hashCode(): Int = audioId
}
