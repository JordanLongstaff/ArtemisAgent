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
import artemis.agent.databinding.SettingsAlliesBinding
import artemis.agent.databinding.fragmentViewBinding
import kotlinx.coroutines.launch
import kotlin.reflect.KMutableProperty1

class AllySettingsFragment : Fragment(R.layout.settings_allies) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsAlliesBinding by fragmentViewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val allySortMethodButtons = mapOf(
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

    private fun prepareAllySortMethodButtons(
        allySortMethodButtons: Map<ToggleButton, KMutableProperty1<UserSettingsKt.Dsl, Boolean>>,
    ) {
        val context = binding.root.context

        allySortMethodButtons.keys.forEach { button ->
            button.setOnClickListener { viewModel.playSound(SoundEffect.BEEP_2) }
        }

        binding.allySortingClassButton1.setOnCheckedChangeListener { _, isChecked ->
            binding.allySortingDefaultOffButton.isChecked = isChecked
            viewModel.viewModelScope.launch {
                context.userSettings.updateData {
                    it.copy {
                        allySortClassFirst = isChecked
                        if (isChecked && allySortClassSecond) allySortClassSecond = false
                    }
                }
            }
        }

        binding.allySortingEnergyButton.setOnCheckedChangeListener { _, isChecked ->
            binding.allySortingDefaultOffButton.isChecked = isChecked
            viewModel.viewModelScope.launch {
                context.userSettings.updateData {
                    it.copy {
                        allySortEnergyFirst = isChecked
                        if (isChecked && !allySortStatus) allySortStatus = true
                    }
                }
            }
        }

        binding.allySortingStatusButton.setOnCheckedChangeListener { _, isChecked ->
            binding.allySortingDefaultOffButton.isChecked = isChecked
            viewModel.viewModelScope.launch {
                context.userSettings.updateData {
                    it.copy {
                        allySortStatus = isChecked
                        if (!isChecked && allySortEnergyFirst) allySortEnergyFirst = false
                    }
                }
            }
        }

        binding.allySortingClassButton2.setOnCheckedChangeListener { _, isChecked ->
            binding.allySortingDefaultOffButton.isChecked = isChecked
            viewModel.viewModelScope.launch {
                context.userSettings.updateData {
                    it.copy {
                        allySortClassSecond = isChecked
                        if (isChecked && allySortClassFirst) allySortClassFirst = false
                    }
                }
            }
        }

        binding.allySortingNameButton.setOnCheckedChangeListener { _, isChecked ->
            binding.allySortingDefaultOffButton.isChecked = isChecked
            viewModel.viewModelScope.launch {
                context.userSettings.updateData {
                    it.copy { allySortName = isChecked }
                }
            }
        }
    }

    private fun prepareDefaultSortMethodButton(
        allySortMethodButtons: Map<ToggleButton, KMutableProperty1<UserSettingsKt.Dsl, Boolean>>,
    ) {
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

        binding.manuallyReturnButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

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
