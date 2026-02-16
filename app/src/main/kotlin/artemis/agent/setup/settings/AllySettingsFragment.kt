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
import artemis.agent.databinding.SettingsAlliesBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.util.SoundEffect
import artemis.agent.util.collectLatestWhileStarted
import kotlinx.coroutines.launch

class AllySettingsFragment : Fragment(R.layout.settings_allies) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsAlliesBinding by fragmentViewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.collectLatestWhileStarted(view.context.userSettings.data) { settings ->
            AllySettingsToggle.entries.forEach { toggle ->
                toggle.getButton(binding).isChecked = toggle.isChecked(settings)
            }

            binding.allySortingDefaultButton.isChecked =
                AllySettingsToggle.sortEntries.none { it.isChecked(settings) }
        }

        bindToggleSettingButtons()
        prepareDefaultSortMethodButton()
    }

    private fun bindToggleSettingButtons() {
        AllySettingsToggle.entries.forEach { toggle ->
            val button = toggle.getButton(binding)

            button.setOnClickListener {
                viewModel.activateHaptic()
                viewModel.playSound(SoundEffect.BEEP_2)
            }

            button.setOnCheckedChangeListener { _, isChecked ->
                if (toggle.isSort) {
                    binding.allySortingDefaultOffButton.isChecked = isChecked
                }
                viewModel.viewModelScope.launch {
                    binding.root.context.userSettings.updateData { settings ->
                        settings.copy { toggle.onCheckedChanged(this, isChecked) }
                    }
                }
            }
        }
    }

    private fun prepareDefaultSortMethodButton() {
        binding.allySortingDefaultButton.setOnClickListener {
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.allySortingDefaultButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.viewModelScope.launch {
                    binding.root.context.userSettings.updateData { settings ->
                        settings.copy {
                            AllySettingsToggle.sortEntries.forEach { sort ->
                                sort.onCheckedChanged(this, false)
                            }
                        }
                    }
                }
            }
        }
    }
}
