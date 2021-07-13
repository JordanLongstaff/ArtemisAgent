package artemis.agent.setup.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.SoundEffect
import artemis.agent.UserSettingsKt
import artemis.agent.UserSettingsSerializer.userSettings
import artemis.agent.collectLatestWhileStarted
import artemis.agent.copy
import artemis.agent.databinding.SettingsRoutingBinding
import artemis.agent.databinding.fragmentViewBinding
import kotlinx.coroutines.launch
import kotlin.reflect.KMutableProperty1

class RoutingSettingsFragment : Fragment(R.layout.settings_routing) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsRoutingBinding by fragmentViewBinding()

    private data class Avoidance(
        val toggleButton: ToggleButton,
        val enabledSetting: KMutableProperty1<UserSettingsKt.Dsl, Boolean>,
        val clearanceField: EditText,
        val clearanceSetting: KMutableProperty1<UserSettingsKt.Dsl, Int>,
        val kmLabel: TextView,
    )

    private var playSoundsOnTextChange: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val incentiveButtons = mapOf(
            binding.incentivesMissionsButton to UserSettingsKt.Dsl::routeMissions,
            binding.incentivesNeedsDamConButton to UserSettingsKt.Dsl::routeNeedsDamcon,
            binding.incentivesNeedsEnergyButton to UserSettingsKt.Dsl::routeNeedsEnergy,
            binding.incentivesHasEnergyButton to UserSettingsKt.Dsl::routeHasEnergy,
            binding.incentivesMalfunctionButton to UserSettingsKt.Dsl::routeMalfunction,
            binding.incentivesAmbassadorButton to UserSettingsKt.Dsl::routeAmbassador,
            binding.incentivesHostageButton to UserSettingsKt.Dsl::routeHostage,
            binding.incentivesCommandeeredButton to UserSettingsKt.Dsl::routeCommandeered,
        )

        val avoidances = arrayOf(
            Avoidance(
                toggleButton = binding.blackHolesButton,
                clearanceField = binding.blackHolesClearanceField,
                kmLabel = binding.blackHolesClearanceKm,
                enabledSetting = UserSettingsKt.Dsl::avoidBlackHoles,
                clearanceSetting = UserSettingsKt.Dsl::blackHoleClearance,
            ),
            Avoidance(
                toggleButton = binding.minesButton,
                clearanceField = binding.minesClearanceField,
                kmLabel = binding.minesClearanceKm,
                enabledSetting = UserSettingsKt.Dsl::avoidMines,
                clearanceSetting = UserSettingsKt.Dsl::mineClearance,
            ),
            Avoidance(
                toggleButton = binding.typhonsButton,
                clearanceField = binding.typhonsClearanceField,
                kmLabel = binding.typhonsClearanceKm,
                enabledSetting = UserSettingsKt.Dsl::avoidTyphon,
                clearanceSetting = UserSettingsKt.Dsl::typhonClearance,
            ),
        )

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.settingsReset) {
            clearFocus()
        }

        viewLifecycleOwner.collectLatestWhileStarted(view.context.userSettings.data) {
            it.copy {
                incentiveButtons.entries.forEach { (button, setting) ->
                    button.isChecked = setting.get(this)
                }
            }

            binding.incentivesAllButton.isEnabled =
                !incentiveButtons.keys.all(ToggleButton::isChecked)
            binding.incentivesNoneButton.isEnabled =
                incentiveButtons.keys.any(ToggleButton::isChecked)

            playSoundsOnTextChange = false

            it.copy {
                avoidances.forEach { avoidance ->
                    if (avoidance.enabledSetting.get(this)) {
                        avoidance.toggleButton.isChecked = true
                        avoidance.kmLabel.visibility = View.VISIBLE
                        avoidance.clearanceField.visibility = View.VISIBLE
                        avoidance.clearanceField.setText(
                            avoidance.clearanceSetting.get(this).toString(),
                        )
                    } else {
                        avoidance.toggleButton.isChecked = false
                        avoidance.kmLabel.visibility = View.GONE
                        avoidance.clearanceField.visibility = View.GONE
                    }
                }
            }

            playSoundsOnTextChange = true

            binding.minesButton.isChecked = it.avoidMines
            binding.typhonsButton.isChecked = it.avoidTyphon

            binding.avoidancesAllButton.isEnabled = !avoidances.all { (button) -> button.isChecked }
            binding.avoidancesNoneButton.isEnabled = avoidances.any { (button) -> button.isChecked }
        }

        binding.avoidancesAllButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
            viewModel.viewModelScope.launch {
                view.context.userSettings.updateData {
                    it.copy {
                        avoidances.forEach { (_, enabledSetting) ->
                            enabledSetting.set(this, true)
                        }
                    }
                }
            }
        }

        binding.avoidancesNoneButton.setOnClickListener {
            clearFocus()
            viewModel.playSound(SoundEffect.BEEP_2)
            viewModel.viewModelScope.launch {
                view.context.userSettings.updateData {
                    it.copy {
                        avoidances.forEach { (_, enabledSetting) ->
                            enabledSetting.set(this, false)
                        }
                    }
                }
            }
        }

        avoidances.forEach { avoidance ->
            avoidance.toggleButton.setOnClickListener { viewModel.playSound(SoundEffect.BEEP_2) }

            avoidance.toggleButton.setOnCheckedChangeListener { _, isChecked ->
                if (!isChecked && avoidance.clearanceField.hasFocus()) {
                    hideKeyboard()
                    avoidance.clearanceField.clearFocus()
                }

                viewModel.viewModelScope.launch {
                    view.context.userSettings.updateData {
                        it.copy { avoidance.enabledSetting.set(this, isChecked) }
                    }
                }
            }

            avoidance.clearanceField.setOnClickListener {
                viewModel.playSound(SoundEffect.BEEP_2)
            }

            avoidance.clearanceField.addTextChangedListener {
                if (playSoundsOnTextChange) {
                    viewModel.playSound(SoundEffect.BEEP_2)
                }
            }

            avoidance.clearanceField.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    viewModel.playSound(SoundEffect.BEEP_2)
                    return@setOnFocusChangeListener
                }

                val text = avoidance.clearanceField.text?.toString()
                viewModel.viewModelScope.launch {
                    view.context.userSettings.updateData {
                        it.copy {
                            if (!text.isNullOrBlank()) {
                                avoidance.clearanceSetting.set(this, text.toInt())
                            }
                        }
                    }
                }
            }
        }

        binding.incentivesAllButton.setOnClickListener {
            clearFocus()
            viewModel.playSound(SoundEffect.BEEP_2)
            viewModel.viewModelScope.launch {
                view.context.userSettings.updateData {
                    it.copy {
                        incentiveButtons.values.forEach { setting ->
                            setting.set(this, true)
                        }
                    }
                }
            }
        }

        binding.incentivesNoneButton.setOnClickListener {
            clearFocus()
            viewModel.playSound(SoundEffect.BEEP_2)
            viewModel.viewModelScope.launch {
                view.context.userSettings.updateData {
                    it.copy {
                        incentiveButtons.values.forEach { setting ->
                            setting.set(this, false)
                        }
                    }
                }
            }
        }

        incentiveButtons.entries.forEach { (button, setting) ->
            button.setOnClickListener {
                viewModel.playSound(SoundEffect.BEEP_2)
                clearFocus()
            }
            button.setOnCheckedChangeListener { _, isChecked ->
                viewModel.viewModelScope.launch {
                    view.context.userSettings.updateData {
                        it.copy { setting.set(this, isChecked) }
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
        with(
            binding.root.context.getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager
        ) {
            hideSoftInputFromWindow(binding.root.windowToken, 0)
        }
    }

    private fun clearFocus() {
        hideKeyboard()
        binding.blackHolesClearanceField.clearFocus()
        binding.minesClearanceField.clearFocus()
        binding.typhonsClearanceField.clearFocus()
    }
}
