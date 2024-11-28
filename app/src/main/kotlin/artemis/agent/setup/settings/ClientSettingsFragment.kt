package artemis.agent.setup.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.RadioButton
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import artemis.agent.AgentViewModel
import artemis.agent.AgentViewModel.Companion.formatString
import artemis.agent.R
import artemis.agent.SoundEffect
import artemis.agent.UserSettingsSerializer.userSettings
import artemis.agent.collectLatestWhileStarted
import artemis.agent.copy
import artemis.agent.databinding.SettingsClientBinding
import artemis.agent.databinding.fragmentViewBinding
import kotlinx.coroutines.launch

class ClientSettingsFragment : Fragment(R.layout.settings_client) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsClientBinding by fragmentViewBinding()

    private var playSoundsOnTextChange: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val vesselDataOptionButtons = arrayOf(
            binding.vesselDataDefault,
            binding.vesselDataInternalStorage,
            binding.vesselDataExternalStorage,
        )
        prepareVesselDataSettingButtons(vesselDataOptionButtons)

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.settingsReset) {
            clearFocus()
        }

        viewLifecycleOwner.collectLatestWhileStarted(view.context.userSettings.data) {
            vesselDataOptionButtons[it.vesselDataLocationValue].isChecked = true

            binding.showNetworkInfoButton.isChecked = it.showNetworkInfo
            binding.alwaysScanPublicButton.isChecked = it.alwaysScanPublic

            val addressLimitEnabled = it.recentAddressLimitEnabled
            binding.addressLimitEnableButton.isChecked = addressLimitEnabled
            if (addressLimitEnabled) {
                binding.addressLimitField.visibility = View.VISIBLE
                binding.addressLimitInfinity.visibility = View.GONE
            } else {
                binding.addressLimitField.visibility = View.GONE
                binding.addressLimitInfinity.visibility = View.VISIBLE
            }

            playSoundsOnTextChange = false
            binding.serverPortField.setText(it.serverPort.formatString())
            binding.addressLimitField.setText(it.recentAddressLimit.formatString())
            binding.updateIntervalField.setText(it.updateInterval.formatString())
            playSoundsOnTextChange = true
        }

        prepareServerPortSettingField()
        prepareScanSettingToggles()
        prepareAddressLimitSettingField()

        binding.updateIntervalField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.playSound(SoundEffect.BEEP_2)
                return@setOnFocusChangeListener
            }

            val text = binding.updateIntervalField.text?.toString()
            viewModel.viewModelScope.launch {
                view.context.userSettings.updateData {
                    it.copy {
                        updateInterval = if (text.isNullOrBlank()) {
                            0
                        } else {
                            text.toInt().coerceIn(0, MAX_UPDATE_INTERVAL)
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        clearFocus()
        super.onPause()
    }

    private fun prepareVesselDataSettingButtons(vesselDataOptionButtons: Array<RadioButton>) {
        val numAvailableOptions = viewModel.storageDirectories.size + 1
        vesselDataOptionButtons.forEachIndexed { index, button ->
            button.visibility = if (index < numAvailableOptions) {
                button.setOnClickListener {
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
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.serverPortField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
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
    }

    private fun prepareScanSettingToggles() {
        binding.showNetworkInfoButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.showNetworkInfoButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy { showNetworkInfo = isChecked }
                }
            }
        }

        binding.alwaysScanPublicButton.setOnClickListener {
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

    private fun prepareAddressLimitSettingField() {
        binding.addressLimitField.addTextChangedListener {
            if (playSoundsOnTextChange) {
                viewModel.playSound(SoundEffect.BEEP_2)
            }
        }

        binding.addressLimitField.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.addressLimitField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.playSound(SoundEffect.BEEP_2)
                return@setOnFocusChangeListener
            }

            val text = binding.addressLimitField.text?.toString()
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy {
                        recentAddressLimit = if (text.isNullOrBlank()) {
                            0
                        } else {
                            text.toInt()
                        }
                    }
                }
            }
        }

        binding.addressLimitEnableButton.setOnClickListener {
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

    private fun clearFocus() {
        with(
            binding.root.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        ) {
            hideSoftInputFromWindow(binding.root.windowToken, 0)
        }
        binding.serverPortField.clearFocus()
        binding.addressLimitField.clearFocus()
    }

    private companion object {
        const val MAX_UPDATE_INTERVAL = 500
    }
}
