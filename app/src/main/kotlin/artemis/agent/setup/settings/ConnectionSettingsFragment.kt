package artemis.agent.setup.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.UserSettingsSerializer.userSettings
import artemis.agent.copy
import artemis.agent.databinding.SettingsConnectionBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.util.SoundEffect
import artemis.agent.util.collectLatestWhileStarted
import kotlinx.coroutines.launch

class ConnectionSettingsFragment : Fragment(R.layout.settings_connection) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsConnectionBinding by fragmentViewBinding()

    private val connectionTimeoutBinder: TimeInputBinder by lazy {
        TimeInputBinder(binding.connectionTimeoutTimeInput, minimumSeconds = 1) { seconds ->
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy { connectionTimeoutSeconds = seconds }
                }
            }
        }
    }

    private val heartbeatTimeoutBinder: TimeInputBinder by lazy {
        TimeInputBinder(binding.heartbeatTimeoutTimeInput, minimumSeconds = 1) { seconds ->
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy { serverTimeoutSeconds = seconds }
                }
            }
        }
    }

    private val scanTimeoutBinder: TimeInputBinder by lazy {
        TimeInputBinder(binding.scanTimeoutTimeInput, minimumSeconds = 1) { seconds ->
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy { scanTimeoutSeconds = seconds }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.collectLatestWhileStarted(view.context.userSettings.data) {
            connectionTimeoutBinder.timeInSeconds = it.connectionTimeoutSeconds
            heartbeatTimeoutBinder.timeInSeconds = it.serverTimeoutSeconds
            scanTimeoutBinder.timeInSeconds = it.scanTimeoutSeconds
            binding.alwaysScanPublicButton.isChecked = it.alwaysScanPublic
        }

        binding.alwaysScanPublicButton.setOnClickListener {
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.alwaysScanPublicButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy { alwaysScanPublic = isChecked }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        connectionTimeoutBinder.destroy()
        heartbeatTimeoutBinder.destroy()
        scanTimeoutBinder.destroy()
    }
}
