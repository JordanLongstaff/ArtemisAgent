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
import artemis.agent.databinding.SettingsMissionsBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.util.SoundEffect
import artemis.agent.util.collectLatestWhileStarted
import kotlinx.coroutines.launch

class MissionSettingsFragment : Fragment(R.layout.settings_missions) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsMissionsBinding by fragmentViewBinding()

    private val autoDismissalBinder: TimeInputBinder by lazy {
        TimeInputBinder(binding.autoDismissalTimeInput) { seconds ->
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData { settings ->
                    settings.copy { completedMissionDismissalSeconds = seconds }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.collectLatestWhileStarted(view.context.userSettings.data) { settings ->
            DisplayRewardSetting.entries.forEach { display ->
                display.getButton(binding).isChecked = display.isChecked(settings)
            }

            val displayCount = DisplayRewardSetting.entries.count { it.isChecked(settings) }
            binding.rewardsAllButton.isEnabled = displayCount < DisplayRewardSetting.entries.size
            binding.rewardsNoneButton.isEnabled = displayCount > 0

            val enabled = settings.completedMissionDismissalEnabled
            binding.autoDismissalButton.isChecked = enabled

            val timeVisibility =
                if (enabled) {
                    autoDismissalBinder.timeInSeconds = settings.completedMissionDismissalSeconds
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }

            binding.autoDismissalSecondsLabel.visibility = timeVisibility
            binding.autoDismissalTimeInput.root.visibility = timeVisibility
        }

        prepareAutoDismissalToggleButton()
        prepareRewardSettingButtons()
    }

    private fun prepareAutoDismissalToggleButton() {
        val autoDismissalButton = binding.autoDismissalButton

        autoDismissalButton.setOnClickListener {
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        autoDismissalButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.viewModelScope.launch {
                autoDismissalButton.context.userSettings.updateData { settings ->
                    settings.copy { completedMissionDismissalEnabled = isChecked }
                }
            }
        }
    }

    private fun prepareRewardSettingButtons() {
        val context = binding.root.context
        listOf(binding.rewardsAllButton to true, binding.rewardsNoneButton to false).forEach {
            (button, isOn) ->
            button.setOnClickListener {
                viewModel.activateHaptic()
                viewModel.playSound(SoundEffect.BEEP_2)
                viewModel.viewModelScope.launch {
                    context.userSettings.updateData { settings ->
                        settings.copy {
                            DisplayRewardSetting.entries.forEach { setting ->
                                setting.onCheckedChanged(this, isOn)
                            }
                        }
                    }
                }
            }
        }

        DisplayRewardSetting.entries.forEach { display ->
            val button = display.getButton(binding)

            button.setOnClickListener {
                viewModel.activateHaptic()
                viewModel.playSound(SoundEffect.BEEP_2)
            }

            button.setOnCheckedChangeListener { _, isChecked ->
                viewModel.viewModelScope.launch {
                    context.userSettings.updateData { settings ->
                        settings.copy { display.onCheckedChanged(this, isChecked) }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoDismissalBinder.destroy()
    }
}
