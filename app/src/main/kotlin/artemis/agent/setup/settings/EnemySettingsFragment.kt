package artemis.agent.setup.settings

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ToggleButton
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import artemis.agent.AgentViewModel
import artemis.agent.AgentViewModel.Companion.formatString
import artemis.agent.R
import artemis.agent.SoundEffect
import artemis.agent.UserSettingsKt
import artemis.agent.UserSettingsSerializer.userSettings
import artemis.agent.collectLatestWhileStarted
import artemis.agent.copy
import artemis.agent.databinding.SettingsEnemiesBinding
import artemis.agent.databinding.fragmentViewBinding
import kotlinx.coroutines.launch

class EnemySettingsFragment : Fragment(R.layout.settings_enemies) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsEnemiesBinding by fragmentViewBinding()

    private var playSoundsOnTextChange: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val enemySortMethodButtons = mapOf(
            binding.enemySortingSurrenderButton to UserSettingsKt.Dsl::enemySortSurrendered,
            binding.enemySortingRaceButton to UserSettingsKt.Dsl::enemySortFaction,
            binding.enemySortingNameButton to UserSettingsKt.Dsl::enemySortName,
            binding.enemySortingRangeButton to UserSettingsKt.Dsl::enemySortDistance,
        )

        val enemyToggleButtons = mapOf(
            binding.showIntelButton to UserSettingsKt.Dsl::showEnemyIntel,
            binding.showTauntStatusButton to UserSettingsKt.Dsl::showTauntStatuses,
            binding.disableIneffectiveButton to UserSettingsKt.Dsl::disableIneffectiveTaunts,
        )

        viewLifecycleOwner.collectLatestWhileStarted(view.context.userSettings.data) {
            it.copy {
                enemySortMethodButtons.entries.forEach { (button, setting) ->
                    button.isChecked = setting.get(this)
                }

                enemyToggleButtons.entries.forEach { (button, setting) ->
                    button.isChecked = setting.get(this)
                }
            }

            binding.enemySortingDefaultButton.isChecked =
                enemySortMethodButtons.keys.none(ToggleButton::isChecked)
            binding.reverseRaceSortButton.isChecked = it.enemySortFactionReversed

            val surrenderRangeEnabled = it.surrenderRangeEnabled
            binding.surrenderRangeEnableButton.isChecked = surrenderRangeEnabled
            if (surrenderRangeEnabled) {
                binding.surrenderRangeKm.visibility = View.VISIBLE
                binding.surrenderRangeField.visibility = View.VISIBLE
                binding.surrenderRangeInfinity.visibility = View.GONE
            } else {
                binding.surrenderRangeKm.visibility = View.GONE
                binding.surrenderRangeField.visibility = View.GONE
                binding.surrenderRangeInfinity.visibility = View.VISIBLE
            }

            val reverseRaceSortVisibility = if (it.enemySortFaction) View.VISIBLE else View.GONE
            binding.reverseRaceSortButton.visibility = reverseRaceSortVisibility
            binding.reverseRaceSortTitle.visibility = reverseRaceSortVisibility

            playSoundsOnTextChange = false
            binding.surrenderRangeField.setText(it.surrenderRange.formatString())
            playSoundsOnTextChange = true
        }

        prepareDefaultSortMethodButton(enemySortMethodButtons)
        prepareEnemySortMethodButtons(enemySortMethodButtons)
        prepareReverseRaceSortButton()
        bindSurrenderRangeField()
        bindToggleSettingButtons(enemyToggleButtons)
    }

    override fun onPause() {
        clearFocus()
        super.onPause()
    }

    private fun prepareDefaultSortMethodButton(enemySortMethodButtons: ToggleButtonMap) {
        binding.enemySortingDefaultButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.enemySortingDefaultButton.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) return@setOnCheckedChangeListener
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy {
                        enemySortMethodButtons.values.forEach { setting ->
                            setting.set(this, false)
                        }
                    }
                }
            }
        }
    }

    private fun prepareEnemySortMethodButtons(enemySortMethodButtons: ToggleButtonMap) {
        val context = binding.root.context

        enemySortMethodButtons.keys.forEach { button ->
            button.setOnClickListener { viewModel.playSound(SoundEffect.BEEP_2) }
        }

        binding.enemySortingSurrenderButton.setOnCheckedChangeListener { _, isChecked ->
            binding.enemySortingDefaultOffButton.isChecked = isChecked
            viewModel.viewModelScope.launch {
                context.userSettings.updateData {
                    it.copy {
                        enemySortSurrendered = isChecked
                    }
                }
            }
        }

        binding.enemySortingRaceButton.setOnCheckedChangeListener { _, isChecked ->
            binding.enemySortingDefaultOffButton.isChecked = isChecked
            viewModel.viewModelScope.launch {
                context.userSettings.updateData {
                    it.copy {
                        enemySortFaction = isChecked
                    }
                }
            }
        }

        binding.enemySortingNameButton.setOnCheckedChangeListener { _, isChecked ->
            binding.enemySortingDefaultOffButton.isChecked = isChecked
            viewModel.viewModelScope.launch {
                context.userSettings.updateData {
                    it.copy {
                        enemySortName = isChecked
                        if (isChecked && enemySortDistance) enemySortDistance = false
                    }
                }
            }
        }

        binding.enemySortingRangeButton.setOnCheckedChangeListener { _, isChecked ->
            binding.enemySortingDefaultOffButton.isChecked = isChecked
            viewModel.viewModelScope.launch {
                context.userSettings.updateData {
                    it.copy {
                        enemySortDistance = isChecked
                        if (isChecked && enemySortName) enemySortName = false
                    }
                }
            }
        }
    }

    private fun prepareReverseRaceSortButton() {
        binding.reverseRaceSortButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.reverseRaceSortButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy {
                        enemySortFactionReversed = isChecked
                    }
                }
            }
        }
    }

    private fun bindSurrenderRangeField() {
        binding.surrenderRangeField.addTextChangedListener {
            if (playSoundsOnTextChange) {
                viewModel.playSound(SoundEffect.BEEP_2)
            }
        }

        binding.surrenderRangeField.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.surrenderRangeField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.playSound(SoundEffect.BEEP_2)
                return@setOnFocusChangeListener
            }

            val text = binding.surrenderRangeField.text?.toString()
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy {
                        surrenderRange = if (text.isNullOrBlank()) {
                            0
                        } else {
                            text.toInt()
                        }
                    }
                }
            }
        }

        binding.surrenderRangeEnableButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
            clearFocus()
        }

        binding.surrenderRangeEnableButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy { surrenderRangeEnabled = isChecked }
                }
            }
        }
    }

    private fun bindToggleSettingButtons(enemyToggleButtons: ToggleButtonMap) {
        enemyToggleButtons.entries.forEach { (button, setting) ->
            button.setOnClickListener { viewModel.playSound(SoundEffect.BEEP_2) }

            button.setOnCheckedChangeListener { _, isChecked ->
                viewModel.viewModelScope.launch {
                    binding.root.context.userSettings.updateData {
                        it.copy { setting.set(this, isChecked) }
                    }
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
        binding.surrenderRangeField.clearFocus()
    }
}
