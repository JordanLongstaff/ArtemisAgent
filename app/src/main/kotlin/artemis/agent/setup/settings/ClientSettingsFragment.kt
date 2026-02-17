package artemis.agent.setup.settings

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.SeekBar
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import artemis.agent.AgentViewModel
import artemis.agent.AgentViewModel.Companion.formatString
import artemis.agent.R
import artemis.agent.UserSettingsSerializer
import artemis.agent.UserSettingsSerializer.userSettings
import artemis.agent.copy
import artemis.agent.databinding.SettingsClientBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.util.HapticEffect
import artemis.agent.util.SoundEffect
import artemis.agent.util.collectLatestWhileStarted
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

class ClientSettingsFragment : Fragment(R.layout.settings_client) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsClientBinding by fragmentViewBinding()

    private var playSoundsOnTextChange: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val vesselDataOptionButtons =
            arrayOf(
                binding.vesselDataDefault,
                binding.vesselDataInternalStorage,
                binding.vesselDataExternalStorage,
            )
        prepareVesselDataSettingButtons(vesselDataOptionButtons)

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.settingsReset) { clearFocus() }

        viewLifecycleOwner.collectLatestWhileStarted(view.context.userSettings.data) {
            vesselDataOptionButtons[it.vesselDataLocationValue].isChecked = true

            binding.showNetworkInfoButton.isChecked = it.showNetworkInfo

            val addressLimitEnabled = it.recentAddressLimitEnabled
            binding.addressLimitEnableButton.isChecked = addressLimitEnabled
            if (addressLimitEnabled) {
                binding.addressLimitField.visibility = View.VISIBLE
                binding.addressLimitInfinity.visibility = View.GONE
            } else {
                binding.addressLimitField.visibility = View.GONE
                binding.addressLimitInfinity.visibility = View.VISIBLE
            }

            binding.updateIntervalBar.progress = getProgress(value = it.updateInterval)

            playSoundsOnTextChange = false
            binding.serverPortField.setText(it.serverPort.formatString())
            binding.addressLimitField.setText(it.recentAddressLimit.formatString())
            playSoundsOnTextChange = true
        }

        if (viewModel.isIdle) {
            binding.vesselDataDisclaimer.visibility = View.GONE
        }

        prepareServerPortSettingField()
        prepareShowNetworkInfoSettingToggle()
        prepareAddressLimitSettingField()
        prepareUpdateIntervalSettingComponents()
    }

    override fun onPause() {
        clearFocus()
        super.onPause()
    }

    private fun prepareVesselDataSettingButtons(vesselDataOptionButtons: Array<RadioButton>) {
        val numAvailableOptions = viewModel.vesselDataManager.count
        vesselDataOptionButtons.forEachIndexed { index, button ->
            button.visibility =
                if (index < numAvailableOptions) {
                    button.setOnClickListener {
                        viewModel.activateHaptic()
                        viewModel.playSound(SoundEffect.BEEP_2)
                        clearFocus()
                    }
                    button.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) {
                            viewModel.viewModelScope.launch {
                                binding.root.context.userSettings.updateData {
                                    it.copy { vesselDataLocationValue = index }
                                }
                            }
                        }
                    }
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
    }

    private fun prepareServerPortSettingField() {
        binding.serverPortField.addTextChangedListener {
            if (playSoundsOnTextChange) {
                viewModel.playSound(SoundEffect.BEEP_2)
            }
        }

        binding.serverPortField.setOnClickListener {
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.serverPortField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.activateHaptic()
                viewModel.playSound(SoundEffect.BEEP_2)
                return@setOnFocusChangeListener
            }

            val text = binding.serverPortField.text?.toString()
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    if (text.isNullOrBlank()) {
                        binding.serverPortField.setText(it.serverPort.formatString())
                        it
                    } else {
                        it.copy { serverPort = text.toInt() }
                    }
                }
            }
        }

        binding.serverPortResetButton.setOnClickListener {
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy { serverPort = UserSettingsSerializer.DEFAULT_SERVER_PORT }
                }
            }
        }
    }

    private fun prepareShowNetworkInfoSettingToggle() {
        binding.showNetworkInfoButton.setOnClickListener {
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.showNetworkInfoButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy { showNetworkInfo = isChecked }
                }
            }
        }
    }

    private fun prepareAddressLimitSettingField() {
        binding.addressLimitField.addTextChangedListener {
            if (playSoundsOnTextChange) {
                viewModel.playSound(SoundEffect.BEEP_2)
            }
        }

        binding.addressLimitField.setOnClickListener {
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.addressLimitField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.activateHaptic()
                viewModel.playSound(SoundEffect.BEEP_2)
                return@setOnFocusChangeListener
            }

            val text = binding.addressLimitField.text?.toString()
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy { recentAddressLimit = if (text.isNullOrBlank()) 0 else text.toInt() }
                }
            }
        }

        binding.addressLimitEnableButton.setOnClickListener {
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
            clearFocus()
        }

        binding.addressLimitEnableButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy { recentAddressLimitEnabled = isChecked }
                }
            }
        }
    }

    private fun prepareUpdateIntervalSettingComponents() {
        val context = binding.root.context

        binding.updateIntervalLabel.text = viewModel.updateObjectsInterval.formatString()

        binding.updateIntervalBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    if (fromUser) viewModel.activateHaptic(HapticEffect.TICK)
                    val updateInterval = getProgressBarValue(progress)
                    viewModel.updateObjectsInterval = updateInterval
                    binding.updateIntervalLabel.text = updateInterval.formatString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    viewModel.activateHaptic(HapticEffect.TICK)
                    viewModel.playSound(SoundEffect.BEEP_2)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    viewModel.playSound(SoundEffect.BEEP_2)
                    viewModel.viewModelScope.launch {
                        context.userSettings.updateData {
                            it.copy { updateInterval = viewModel.updateObjectsInterval }
                        }
                    }
                }
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.updateIntervalBar.max = MAX_UPDATE_INTERVAL
        }
    }

    private fun clearFocus() {
        viewModel.hideKeyboard(binding.root)
        binding.serverPortField.clearFocus()
        binding.addressLimitField.clearFocus()
    }

    private fun getProgress(value: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            value
        } else {
            (value * MAX_PROGRESS / MAX_UPDATE_INTERVAL).roundToInt()
        }

    private fun getProgressBarValue(progress: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            progress.coerceIn(0, MAX_UPDATE_INTERVAL)
        } else {
            (progress * MAX_UPDATE_INTERVAL / MAX_PROGRESS).roundToInt()
        }

    private companion object {
        const val MAX_PROGRESS = 100f
        const val MAX_UPDATE_INTERVAL = 500
    }
}
