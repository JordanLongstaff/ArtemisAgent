package artemis.agent.setup.settings

import android.os.Bundle
import android.view.View
import android.widget.ToggleButton
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
import artemis.agent.databinding.SettingsBiomechsBinding
import artemis.agent.databinding.fragmentViewBinding
import kotlinx.coroutines.launch
import kotlin.reflect.KMutableProperty1

class BiomechSettingsFragment : Fragment(R.layout.settings_biomechs) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsBiomechsBinding by fragmentViewBinding()

    private val freezeDurationBinder: TimeInputBinder by lazy {
        object : TimeInputBinder(binding.freezeDurationTimeInput, true) {
            override fun onSecondsChange(seconds: Int) {
                viewModel.playSound(SoundEffect.BEEP_2)
                viewModel.viewModelScope.launch {
                    binding.root.context.userSettings.updateData {
                        it.copy { freezeDurationSeconds = seconds }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val biomechSortMethodButtons = mapOf(
            binding.biomechSortingClassButton1 to UserSettingsKt.Dsl::biomechSortClassFirst,
            binding.biomechSortingStatusButton to UserSettingsKt.Dsl::biomechSortStatus,
            binding.biomechSortingClassButton2 to UserSettingsKt.Dsl::biomechSortClassSecond,
            binding.biomechSortingNameButton to UserSettingsKt.Dsl::biomechSortName,
        )

        viewLifecycleOwner.collectLatestWhileStarted(view.context.userSettings.data) {
            it.copy {
                biomechSortMethodButtons.entries.forEach { (button, setting) ->
                    button.isChecked = setting.get(this)
                }
            }

            binding.biomechSortingDefaultButton.isChecked =
                biomechSortMethodButtons.keys.none(ToggleButton::isChecked)

            freezeDurationBinder.timeInSeconds = it.freezeDurationSeconds
        }

        prepareSortMethodButtons(biomechSortMethodButtons)
        prepareDefaultSortMethodButton(biomechSortMethodButtons)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        freezeDurationBinder.destroy()
    }

    private fun prepareSortMethodButtons(biomechSortMethodButtons: ToggleButtonMap) {
        val context = binding.root.context

        biomechSortMethodButtons.keys.forEach { button ->
            button.setOnClickListener { viewModel.playSound(SoundEffect.BEEP_2) }
        }

        binding.biomechSortingClassButton1.setOnCheckedChangeListener { _, isChecked ->
            binding.biomechSortingDefaultOffButton.isChecked = isChecked
            viewModel.viewModelScope.launch {
                context.userSettings.updateData {
                    it.copy {
                        biomechSortClassFirst = isChecked
                        if (isChecked) biomechSortClassSecond = false
                    }
                }
            }
        }

        binding.biomechSortingStatusButton.setOnCheckedChangeListener { _, isChecked ->
            binding.biomechSortingDefaultOffButton.isChecked = isChecked
            viewModel.viewModelScope.launch {
                context.userSettings.updateData {
                    it.copy { biomechSortStatus = isChecked }
                }
            }
        }

        binding.biomechSortingClassButton2.setOnCheckedChangeListener { _, isChecked ->
            binding.biomechSortingDefaultOffButton.isChecked = isChecked
            viewModel.viewModelScope.launch {
                context.userSettings.updateData {
                    it.copy {
                        biomechSortClassSecond = isChecked
                        if (isChecked) biomechSortClassFirst = false
                    }
                }
            }
        }

        binding.biomechSortingNameButton.setOnCheckedChangeListener { _, isChecked ->
            binding.biomechSortingDefaultOffButton.isChecked = isChecked
            viewModel.viewModelScope.launch {
                context.userSettings.updateData {
                    it.copy { biomechSortName = isChecked }
                }
            }
        }
    }

    private fun prepareDefaultSortMethodButton(biomechSortMethodButtons: ToggleButtonMap) {
        binding.biomechSortingDefaultButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.biomechSortingDefaultButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.viewModelScope.launch {
                    binding.root.context.userSettings.updateData {
                        it.copy {
                            biomechSortMethodButtons.values.forEach { setting ->
                                setting.set(this, false)
                            }
                        }
                    }
                }
            }
        }
    }
}
