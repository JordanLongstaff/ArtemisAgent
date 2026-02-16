package artemis.agent.setup.settings

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import artemis.agent.AgentViewModel
import artemis.agent.AgentViewModel.Companion.formatString
import artemis.agent.R
import artemis.agent.UserSettingsKt
import artemis.agent.UserSettingsSerializer.userSettings
import artemis.agent.copy
import artemis.agent.databinding.SettingsRoutingBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.util.SoundEffect
import artemis.agent.util.collectLatestWhileStarted
import kotlinx.coroutines.launch

class RoutingSettingsFragment : Fragment(R.layout.settings_routing) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsRoutingBinding by fragmentViewBinding()

    private var playSoundsOnTextChange: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.settingsReset) { clearFocus() }

        initializeFromSettings()

        prepareAvoidanceSettingButtons()
        prepareIncentiveSettingButtons()
    }

    private fun initializeFromSettings() {
        viewLifecycleOwner.collectLatestWhileStarted(binding.root.context.userSettings.data) {
            settings ->
            IncentiveSetting.entries.forEach { incentive ->
                incentive.getButton(binding).isChecked = incentive.isChecked(settings)
            }

            setMasterButtonsEnabled(
                binding.incentivesAllButton,
                binding.incentivesNoneButton,
                IncentiveSetting.entries,
            ) { incentive ->
                incentive.isChecked(settings)
            }

            playSoundsOnTextChange = false

            AvoidanceSetting.entries.forEach { avoidance ->
                val toggleButton = avoidance.getToggleButton(binding)
                val kmLabel = avoidance.getKmLabel(binding)
                val clearanceField = avoidance.getClearanceField(binding)
                val enabled = avoidance.isEnabled(settings)

                toggleButton.isChecked = enabled

                val fieldVisibility =
                    if (enabled) {
                        clearanceField.setText(avoidance.getClearance(settings).formatString())
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                kmLabel.visibility = fieldVisibility
                clearanceField.visibility = fieldVisibility
            }

            playSoundsOnTextChange = true

            setMasterButtonsEnabled(
                binding.avoidancesAllButton,
                binding.avoidancesNoneButton,
                AvoidanceSetting.entries,
            ) { avoidance ->
                avoidance.isEnabled(settings)
            }
        }
    }

    private fun <T> setMasterButtonsEnabled(
        allButton: Button,
        noneButton: Button,
        data: Collection<T>,
        predicate: (T) -> Boolean,
    ) {
        val countOn = data.count(predicate)
        val countOff = data.count() - countOn
        allButton.isEnabled = countOff != 0
        noneButton.isEnabled = countOn != 0
    }

    private fun prepareAvoidanceSettingButtons() {
        prepareMasterButtons(
            binding.avoidancesAllButton,
            binding.avoidancesNoneButton,
            maintainFocus = true,
        ) { isOn ->
            avoidBlackHoles = isOn
            avoidMines = isOn
            avoidTyphon = isOn
        }

        AvoidanceSetting.entries.forEach { prepareAvoidanceSettingView(it) }
    }

    private fun prepareAvoidanceSettingView(avoidance: AvoidanceSetting) {
        val toggleButton = avoidance.getToggleButton(binding)
        val clearanceField = avoidance.getClearanceField(binding)

        toggleButton.setOnClickListener {
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked && clearanceField.hasFocus()) {
                hideKeyboard()
                clearanceField.clearFocus()
            }

            viewModel.viewModelScope.launch {
                toggleButton.context.userSettings.updateData { settings ->
                    settings.copy { avoidance.setEnabled(this, isChecked) }
                }
            }
        }

        clearanceField.setOnClickListener {
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        clearanceField.addTextChangedListener {
            if (playSoundsOnTextChange) {
                viewModel.playSound(SoundEffect.BEEP_2)
            }
        }

        clearanceField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.activateHaptic()
                viewModel.playSound(SoundEffect.BEEP_2)
                return@setOnFocusChangeListener
            }

            val text = clearanceField.text?.toString()
            viewModel.viewModelScope.launch {
                clearanceField.context.userSettings.updateData {
                    it.copy {
                        if (!text.isNullOrBlank()) {
                            avoidance.setClearance(this, text.toFloat())
                        }
                    }
                }
            }
        }
    }

    private fun prepareIncentiveSettingButtons() {
        prepareMasterButtons(binding.incentivesAllButton, binding.incentivesNoneButton) { isOn ->
            routeMissions = isOn
            routeMalfunction = isOn
            routeHostage = isOn
            routeAmbassador = isOn
            routeCommandeered = isOn
            routeNeedsEnergy = isOn
            routeHasEnergy = isOn
            routeNeedsDamcon = isOn
        }

        IncentiveSetting.entries.forEach { incentive ->
            val button = incentive.getButton(binding)

            button.setOnClickListener {
                viewModel.activateHaptic()
                viewModel.playSound(SoundEffect.BEEP_2)
                clearFocus()
            }
            button.setOnCheckedChangeListener { _, isChecked ->
                viewModel.viewModelScope.launch {
                    button.context.userSettings.updateData { settings ->
                        settings.copy { incentive.onCheckedChanged(this, isChecked) }
                    }
                }
            }
        }
    }

    private fun prepareMasterButtons(
        allButton: Button,
        noneButton: Button,
        maintainFocus: Boolean = false,
        update: UserSettingsKt.Dsl.(Boolean) -> Unit,
    ) {
        listOf(allButton to true, noneButton to false).forEach { (button, isOn) ->
            button.setOnClickListener {
                if (!maintainFocus || !isOn) {
                    clearFocus()
                }
                viewModel.activateHaptic()
                viewModel.playSound(SoundEffect.BEEP_2)
                viewModel.viewModelScope.launch {
                    button.context.userSettings.updateData { settings ->
                        settings.copy { update(isOn) }
                    }
                }
            }
        }
    }

    override fun onPause() {
        clearFocus()
        super.onPause()
    }

    private fun hideKeyboard() {
        viewModel.hideKeyboard(binding.root)
    }

    private fun clearFocus() {
        hideKeyboard()
        binding.blackHolesClearanceField.clearFocus()
        binding.minesClearanceField.clearFocus()
        binding.typhonsClearanceField.clearFocus()
    }
}
