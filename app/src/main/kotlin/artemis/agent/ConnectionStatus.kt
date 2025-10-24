package artemis.agent

import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StringRes

sealed class ConnectionStatus(
    @all:StringRes val stringId: Int,
    @all:ColorRes val color: Int,
    val spinnerVisibility: Int = View.GONE,
) {
    data object NotConnected : ConnectionStatus(R.string.not_connected, R.color.notConnected)

    data object Connecting : ConnectionStatus(R.string.connecting, R.color.connecting, View.VISIBLE)

    data object Connected : ConnectionStatus(R.string.connected, R.color.connected)

    data object Failed : ConnectionStatus(R.string.failed_to_connect, R.color.failedToConnect)

    data object HeartbeatLost : ConnectionStatus(R.string.connection_lost, R.color.heartbeatLost)
}
