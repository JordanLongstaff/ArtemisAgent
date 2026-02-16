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
import artemis.agent.databinding.SettingsBiomechsBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.util.SoundEffect
import artemis.agent.util.collectLatestWhileStarted
import kotlinx.coroutines.launch

class BiomechSettingsFragment : Fragment(R.layout.settings_biomechs) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsBiomechsBinding by fragmentViewBinding()

    private val freezeDurationBinder: TimeInputBinder by lazy {
        TimeInputBinder(binding.freezeDurationTimeInput, true) { seconds ->
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy { freezeDurationSeconds = seconds }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.collectLatestWhileStarted(view.context.userSettings.data) { settings ->
            BiomechSortToggle.entries.forEach { toggle ->
                toggle.getButton(binding).isChecked = toggle.isChecked(settings)
            }

            binding.biomechSortingDefaultButton.isChecked =
                BiomechSortToggle.entries.none { it.isChecked(settings) }

            freezeDurationBinder.timeInSeconds = settings.freezeDurationSeconds
        }

        prepareSortMethodButtons()
        prepareDefaultSortMethodButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        freezeDurationBinder.destroy()
    }

    private fun prepareSortMethodButtons() {
        val context = binding.root.context

        BiomechSortToggle.entries.forEach { toggle ->
            val button = toggle.getButton(binding)
            button.setOnClickListener {
                viewModel.activateHaptic()
                viewModel.playSound(SoundEffect.BEEP_2)
            }

            button.setOnCheckedChangeListener { _, isChecked ->
                binding.biomechSortingDefaultOffButton.isChecked = isChecked
                viewModel.viewModelScope.launch {
                    context.userSettings.updateData { settings ->
                        settings.copy { toggle.onCheckedChanged(this, isChecked) }
                    }
                }
            }
        }
    }

    private fun prepareDefaultSortMethodButton() {
        binding.biomechSortingDefaultButton.setOnClickListener {
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.biomechSortingDefaultButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.viewModelScope.launch {
                    binding.root.context.userSettings.updateData { settings ->
                        settings.copy {
                            BiomechSortToggle.entries.forEach { sort ->
                                sort.onCheckedChanged(this, false)
                            }
                        }
                    }
                }
            }
        }
    }
}
