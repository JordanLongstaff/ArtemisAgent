package artemis.agent

import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.WithDataTestName
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual

class ConnectionStatusTest :
    DescribeSpec({
        describe("ConnectionStatus") { withData(ConnectionStatusTestCase.entries) { it.test() } }
    })

private enum class ConnectionStatusTestCase(
    private val status: ConnectionStatus,
    @StringRes private val expectedStringId: Int,
    @ColorRes private val expectedColor: Int,
    private val expectedSpinnerVisibility: Int,
) : WithDataTestName {
    NOT_CONNECTED(
        ConnectionStatus.NotConnected,
        R.string.not_connected,
        R.color.notConnected,
        View.GONE,
    ),
    CONNECTING(ConnectionStatus.Connecting, R.string.connecting, R.color.connecting, View.VISIBLE),
    CONNECTED(ConnectionStatus.Connected, R.string.connected, R.color.connected, View.GONE),
    FAILED(ConnectionStatus.Failed, R.string.failed_to_connect, R.color.failedToConnect, View.GONE),
    HEARTBEAT_LOST(
        ConnectionStatus.HeartbeatLost,
        R.string.connection_lost,
        R.color.heartbeatLost,
        View.GONE,
    );

    override fun dataTestName(): String = status.toString()

    fun test() {
        status.stringId shouldBeEqual expectedStringId
        status.color shouldBeEqual expectedColor
        status.spinnerVisibility shouldBeEqual expectedSpinnerVisibility
    }
}
