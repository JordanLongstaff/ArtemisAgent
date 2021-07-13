package artemis.agent.setup

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.ServerConnection
import artemis.agent.SoundEffect
import artemis.agent.UserSettingsSerializer.userSettings
import artemis.agent.collectLatestWhileStarted
import artemis.agent.databinding.ConnectFragmentBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.generic.GenericDataAdapter
import artemis.agent.generic.GenericDataEntry
import com.walkertribe.ian.iface.ConnectionEvent
import com.walkertribe.ian.iface.DisconnectCause
import com.walkertribe.ian.iface.Listener

class ConnectFragment : Fragment(R.layout.connect_fragment) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: ConnectFragmentBinding by fragmentViewBinding()

    sealed class ConnectionStatus(
        @StringRes val stringId: Int,
        @ColorRes val color: Int,
        val spinnerVisibility: Int = View.GONE
    ) {
        data object NotConnected : ConnectionStatus(R.string.not_connected, R.color.notConnected)
        data object Connecting : ConnectionStatus(R.string.connecting, R.color.connecting, View.VISIBLE)
        data object Failed : ConnectionStatus(R.string.failed_to_connect, R.color.failedToConnect)

        data class Connected(
            val server: ServerConnection,
            private val setConnectionStatus: (ConnectionStatus) -> Unit
        ) : ConnectionStatus(R.string.connected, R.color.connected) {
            private val heartbeatLost: HeartbeatLost by lazy {
                HeartbeatLost(this)
            }

            @Listener
            fun onDisconnect(event: ConnectionEvent.Disconnect) {
                server.networkInterface.stop()
                if (event.cause !is DisconnectCause.LocalDisconnect) {
                    setConnectionStatus(NotConnected)
                }
            }

            @Listener
            fun onHeartbeatLost(event: ConnectionEvent.HeartbeatLost) {
                setConnectionStatus(heartbeatLost)
            }

            @Listener
            fun onHeartbeatRegained(event: ConnectionEvent.HeartbeatRegained) {
                setConnectionStatus(this)
            }

            init {
                server.networkInterface.addListener(this)
            }
        }

        data class HeartbeatLost(val prevConnect: Connected) :
            ConnectionStatus(R.string.connection_lost, R.color.heartbeatLost)
    }

    private val recentAdapter: RecentServersAdapter by lazy {
        RecentServersAdapter(binding.root.context)
    }

    private var playSoundsOnTextChange: Boolean = false
    private var playSoundOnScanFinished: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.settingsPage.value = null

        val serverListAdapter = GenericDataAdapter {
            playSoundsOnTextChange = false
            binding.addressBar.setText(it.data)
            viewModel.connectToServer()
        }
        binding.serverList.apply {
            itemAnimator = null
            adapter = serverListAdapter
        }

        binding.root.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                hideKeyboard()
            }
        }

        val scanButton = binding.scanButton
        val scanSpinner = binding.scanSpinner
        val noServersLabel = binding.noServersLabel

        scanButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
            hideKeyboard()
            viewModel.scanForServers()
        }

        binding.connectButton.setOnClickListener {
            hideKeyboard()
            viewModel.connectToServer()
        }

        val addressBar = binding.addressBar
        addressBar.setAdapter(recentAdapter)

        addressBar.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.playSound(SoundEffect.BEEP_2)
            }
        }

        addressBar.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.connectionStatus) {
            binding.connectLabel.text = getString(it.stringId)
            binding.connectBar.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, it.color)
            )
            binding.connectSpinner.visibility = it.spinnerVisibility

            if (!viewModel.attemptingConnection && it is ConnectionStatus.Connecting) {
                val prevUrl = viewModel.connectedUrl.value

                addressBar.clearFocus()

                val url = addressBar.text.toString()

                viewModel.tryConnect(url) { connection ->
                    val newUrl = connection.url
                    if (newUrl != prevUrl) {
                        viewModel.connectedUrl.emit(newUrl)
                    }
                }
            }
        }

        viewLifecycleOwner.collectLatestWhileStarted(view.context.userSettings.data) {
            recentAdapter.servers = it.recentServersList.apply {
                var addressText = firstOrNull() ?: ""
                if (viewModel.connectedUrl.value.isBlank()) {
                    viewModel.addressBarText.also { text ->
                        if (text.isNotBlank()) {
                            addressText = text
                        }
                    }
                }
                playSoundsOnTextChange = false
                addressBar.setText(addressText)
            }
        }

        addressBar.addTextChangedListener {
            if (playSoundsOnTextChange) {
                viewModel.playSound(SoundEffect.BEEP_2)
            } else {
                playSoundsOnTextChange = true
            }

            binding.connectButton.isEnabled = !it.isNullOrBlank()
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.discoveredServers) {
            serverListAdapter.onListUpdate(
                it.map { (ip, hostName) -> GenericDataEntry(hostName, ip) }
            )
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.isScanningUDP) {
            scanButton.isEnabled = !it
            if (it) {
                addressBar.clearFocus()
                scanSpinner.visibility = View.VISIBLE
                noServersLabel.visibility = View.GONE
            } else {
                if (playSoundOnScanFinished) {
                    viewModel.playSound(SoundEffect.BEEP_1)
                }
                scanSpinner.visibility = View.GONE
                if (serverListAdapter.itemCount == 0) {
                    noServersLabel.setText(R.string.no_servers_found)
                    noServersLabel.visibility = View.VISIBLE
                } else {
                    noServersLabel.visibility = View.GONE
                }
            }
            playSoundOnScanFinished = true
        }
    }

    override fun onPause() {
        val addressBar = binding.addressBar
        viewModel.addressBarText = addressBar.text.toString()
        addressBar.clearFocus()
        hideKeyboard()

        super.onPause()
    }

    private fun hideKeyboard() {
        with(
            binding.root.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        ) {
            hideSoftInputFromWindow(binding.root.windowToken, 0)
        }
    }

    private class RecentServersAdapter(context: Context) : ArrayAdapter<String>(
        context,
        R.layout.generic_data_entry,
        R.id.entryNameLabel
    ) {
        var servers: List<String> = listOf()
        private val suggestions: MutableList<String> = mutableListOf()
        private val filter = object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults =
                FilterResults().apply {
                    if (constraint.isNullOrBlank()) {
                        values = servers.joinToString("\n")
                        count = servers.size
                    } else {
                        val regex = Regex(
                            constraint
                                .split('.')
                                .filter(String::isNotBlank)
                                .joinToString(".*\\..*")
                        )
                        val newValues = servers.filter(regex::containsMatchIn)
                        values = newValues.joinToString("\n")
                        count = newValues.size
                    }
                }

            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                suggestions.clear()
                if (results.count > 0) {
                    suggestions.addAll(results.values.toString().split('\n'))
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }

        override fun getCount(): Int = suggestions.size

        override fun getItem(position: Int): String = suggestions[position]

        override fun getFilter(): Filter = filter
    }
}
