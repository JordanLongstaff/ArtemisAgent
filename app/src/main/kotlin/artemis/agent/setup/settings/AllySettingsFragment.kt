package artemis.agent.setup.settings

import android.os.Bundle
import android.view.View
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.UserSettingsKt
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

        val allySortMethodButtons =
            mapOf(
                binding.allySortingClassButton1 to UserSettingsKt.Dsl::allySortClassFirst,
                binding.allySortingStatusButton to UserSettingsKt.Dsl::allySortStatus,
                binding.allySortingClassButton2 to UserSettingsKt.Dsl::allySortClassSecond,
                binding.allySortingNameButton to UserSettingsKt.Dsl::allySortName,
                binding.allySortingEnergyButton to UserSettingsKt.Dsl::allySortEnergyFirst,
            )

        viewLifecycleOwner.collectLatestWhileStarted(view.context.userSettings.data) {
            binding.showDestroyedAlliesButton.isChecked = it.showDestroyedAllies
            binding.manuallyReturnButton.isChecked = it.allyCommandManualReturn

            it.copy {
                allySortMethodButtons.entries.forEach { (button, setting) ->
                    button.isChecked = setting.get(this)
                }
            }

            binding.allySortingDefaultButton.isChecked =
                allySortMethodButtons.keys.none(ToggleButton::isChecked)
        }

        prepareAllySortMethodButtons(allySortMethodButtons)
        prepareDefaultSortMethodButton(allySortMethodButtons)
        prepareOtherSettingButtons()
    }

    private fun prepareAllySortMethodButtons(allySortMethodButtons: ToggleButtonMap) {
        allySortMethodButtons.keys.forEach { button ->
            button.setOnClickListener { viewModel.playSound(SoundEffect.BEEP_2) }
        }

        binding.allySortingClassButton1.bindSortButton { isChecked ->
            allySortClassFirst = isChecked
            if (isChecked && allySortClassSecond) allySortClassSecond = false
        }

        binding.allySortingEnergyButton.bindSortButton { isChecked ->
            allySortEnergyFirst = isChecked
            if (isChecked && !allySortStatus) allySortStatus = true
        }

        binding.allySortingStatusButton.bindSortButton { isChecked ->
            allySortStatus = isChecked
            if (!isChecked && allySortEnergyFirst) allySortEnergyFirst = false
        }

        binding.allySortingClassButton2.bindSortButton { isChecked ->
            allySortClassSecond = isChecked
            if (isChecked && allySortClassFirst) allySortClassFirst = false
        }

        binding.allySortingNameButton.bindSortButton { isChecked -> allySortName = isChecked }
    }

    private fun ToggleButton.bindSortButton(onChange: UserSettingsKt.Dsl.(Boolean) -> Unit = {}) {
        setOnCheckedChangeListener { _, isChecked ->
            binding.allySortingDefaultOffButton.isChecked = isChecked
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData { it.copy { onChange(isChecked) } }
            }
        }
    }

    private fun prepareDefaultSortMethodButton(allySortMethodButtons: ToggleButtonMap) {
        binding.allySortingDefaultButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.allySortingDefaultButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.viewModelScope.launch {
                    binding.root.context.userSettings.updateData {
                        it.copy {
                            allySortMethodButtons.values.forEach { setting ->
                                setting.set(this, false)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun prepareOtherSettingButtons() {
        binding.showDestroyedAlliesButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.manuallyReturnButton.setOnClickListener { viewModel.playSound(SoundEffect.BEEP_2) }

        binding.showDestroyedAlliesButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy { showDestroyedAllies = isChecked }
                }
            }
        }

        binding.manuallyReturnButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy { allyCommandManualReturn = isChecked }
                }
            }
        }
    }
}
