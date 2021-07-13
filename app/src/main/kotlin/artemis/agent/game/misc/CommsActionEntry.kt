package artemis.agent.game.misc

import com.walkertribe.ian.protocol.core.ButtonClickPacket

data class CommsActionEntry(val label: CharSequence) {
    val clickPacket by lazy { ButtonClickPacket(label) }

    override fun equals(other: Any?): Boolean {
        return other is CommsActionEntry && other.clickPacket.hash == clickPacket.hash
    }

    override fun hashCode(): Int = clickPacket.hash
}
