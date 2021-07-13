package artemis.agent

import com.walkertribe.ian.iface.KtorArtemisNetworkInterface

data class ServerConnection(
    val url: String,
    val networkInterface: KtorArtemisNetworkInterface
) {
    init {
        networkInterface.start()
    }
}
