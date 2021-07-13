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
import artemis.agent.databinding.SettingsMissionsBinding
import artemis.agent.databinding.fragmentViewBinding
import kotlinx.coroutines.launch

class MissionSettingsFragment : Fragment(R.layout.settings_missions) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsMissionsBinding by fragmentViewBinding()

    private val autoDismissalBinder: TimeInputBinder by lazy {
        object : TimeInputBinder(binding.autoDismissalTimeInput) {
            override fun onSecondsChange(seconds: Int) {
                viewModel.playSound(SoundEffect.BEEP_2)
                viewModel.viewModelScope.launch {
                    binding.root.context.userSettings.updateData {
                        it.copy { completedMissionDismissalSeconds = seconds }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val displayRewardButtons = mapOf(
            binding.rewardsBatteryButton to UserSettingsKt.Dsl::displayRewardBattery,
            binding.rewardsCoolantButton to UserSettingsKt.Dsl::displayRewardCoolant,
            binding.rewardsNukeButton to UserSettingsKt.Dsl::displayRewardNukes,
            binding.rewardsProductionButton to UserSettingsKt.Dsl::displayRewardProduction,
            binding.rewardsShieldButton to UserSettingsKt.Dsl::displayRewardShield,
        )

        viewLifecycleOwner.collectLatestWhileStarted(view.context.userSettings.data) {
            it.copy {
                displayRewardButtons.entries.forEach { (button, setting) ->
                    button.isChecked = setting.get(this)
                }
            }

            binding.rewardsAllButton.isEnabled =
                !displayRewardButtons.keys.all(ToggleButton::isChecked)
            binding.rewardsNoneButton.isEnabled =
                displayRewardButtons.keys.any(ToggleButton::isChecked)

            if (it.completedMissionDismissalEnabled) {
                binding.autoDismissalButton.isChecked = true
                binding.autoDismissalSecondsLabel.visibility = View.VISIBLE
                binding.autoDismissalTimeInput.root.visibility = View.VISIBLE
                autoDismissalBinder.timeInSeconds = it.completedMissionDismissalSeconds
            } else {
                binding.autoDismissalButton.isChecked = false
                binding.autoDismissalSecondsLabel.visibility = View.INVISIBLE
                binding.autoDismissalTimeInput.root.visibility = View.INVISIBLE
            }
        }

        binding.autoDismissalButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.autoDismissalButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.viewModelScope.launch {
                view.context.userSettings.updateData {
                    it.copy { completedMissionDismissalEnabled = isChecked }
                }
            }
        }

        binding.rewardsAllButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
            viewModel.viewModelScope.launch {
                view.context.userSettings.updateData {
                    it.copy {
                        displayRewardButtons.values.forEach { setting ->
                            setting.set(this, true)
                        }
                    }
                }
            }
        }

        binding.rewardsNoneButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
            viewModel.viewModelScope.launch {
                view.context.userSettings.updateData {
                    it.copy {
                        displayRewardButtons.values.forEach { setting ->
                            setting.set(this, false)
                        }
                    }
                }
            }
        }

        displayRewardButtons.entries.forEach { (button, setting) ->
            button.setOnClickListener { viewModel.playSound(SoundEffect.BEEP_2) }

            button.setOnCheckedChangeListener { _, isChecked ->
                viewModel.viewModelScope.launch {
                    view.context.userSettings.updateData {
                        it.copy { setting.set(this, isChecked) }
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
