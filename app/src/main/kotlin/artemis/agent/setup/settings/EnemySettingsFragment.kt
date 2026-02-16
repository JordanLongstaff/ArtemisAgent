package artemis.agent.setup.settings

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import artemis.agent.AgentViewModel
import artemis.agent.AgentViewModel.Companion.formatString
import artemis.agent.R
import artemis.agent.UserSettingsSerializer.userSettings
import artemis.agent.copy
import artemis.agent.databinding.SettingsEnemiesBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.util.HapticEffect
import artemis.agent.util.SoundEffect
import artemis.agent.util.collectLatestWhileStarted
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

class EnemySettingsFragment : Fragment(R.layout.settings_enemies) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsEnemiesBinding by fragmentViewBinding()

    private var playSoundsOnTextChange: Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.collectLatestWhileStarted(view.context.userSettings.data) { settings ->
            EnemySettingsToggle.entries.forEach { toggle ->
                toggle.getButton(binding).isChecked = toggle.isChecked(settings)
            }

            binding.enemySortingDefaultButton.isChecked =
                EnemySettingsToggle.sortEntries.none { it.isChecked(settings) }

            if (settings.surrenderRangeEnabled) {
                binding.surrenderRangeKm.visibility = View.VISIBLE
                binding.surrenderRangeField.visibility = View.VISIBLE
                binding.surrenderRangeInfinity.visibility = View.GONE
            } else {
                binding.surrenderRangeKm.visibility = View.GONE
                binding.surrenderRangeField.visibility = View.GONE
                binding.surrenderRangeInfinity.visibility = View.VISIBLE
            }

            binding.surrenderBurstCountBar.progress =
                getProgress(
                    value = settings.surrenderBurstCount,
                    min = MIN_BURST_COUNT,
                    max = MAX_BURST_COUNT,
                )
            binding.surrenderBurstIntervalBar.progress =
                getProgress(
                    value = settings.surrenderBurstInterval,
                    min = MIN_BURST_INTERVAL,
                    max = MAX_BURST_INTERVAL,
                )

            val reverseRaceSortVisibility =
                if (settings.enemySortFaction) View.VISIBLE else View.GONE
            binding.reverseRaceSortButton.visibility = reverseRaceSortVisibility
            binding.reverseRaceSortTitle.visibility = reverseRaceSortVisibility

            playSoundsOnTextChange = false
            binding.surrenderRangeField.setText(settings.surrenderRange.formatString())
            playSoundsOnTextChange = true
        }

        prepareDefaultSortMethodButton()
        bindToggleSettingButtons()
        bindSurrenderRangeField()
        bindProgressBars()
    }

    override fun onPause() {
        clearFocus()
        super.onPause()
    }

    private fun prepareDefaultSortMethodButton() {
        binding.enemySortingDefaultButton.setOnClickListener {
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.enemySortingDefaultButton.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) return@setOnCheckedChangeListener
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData { settings ->
                    settings.copy {
                        EnemySettingsToggle.sortEntries.forEach { toggle ->
                            toggle.onCheckedChanged(this, false)
                        }
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
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.surrenderRangeField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.activateHaptic()
                viewModel.playSound(SoundEffect.BEEP_2)
                return@setOnFocusChangeListener
            }

            val text = binding.surrenderRangeField.text?.toString()
            viewModel.viewModelScope.launch {
                binding.root.context.userSettings.updateData {
                    it.copy { surrenderRange = if (text.isNullOrBlank()) 0f else text.toFloat() }
                }
            }
        }

        binding.surrenderRangeEnableButton.setOnClickListener {
            viewModel.activateHaptic()
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

    private fun bindToggleSettingButtons() {
        EnemySettingsToggle.entries.forEach { toggle ->
            val button = toggle.getButton(binding)

            button.setOnClickListener {
                viewModel.activateHaptic()
                viewModel.playSound(SoundEffect.BEEP_2)
            }

            button.setOnCheckedChangeListener { _, isChecked ->
                if (toggle.isSort) {
                    binding.enemySortingDefaultOffButton.isChecked = isChecked
                }
                viewModel.viewModelScope.launch {
                    binding.root.context.userSettings.updateData {
                        it.copy { toggle.onCheckedChanged(this, isChecked) }
                    }
                }
            }
        }
    }

    private fun bindProgressBars() {
        val context = binding.root.context

        binding.surrenderBurstCountLabel.text =
            viewModel.enemiesManager.surrenderBurstCount.formatString()
        binding.surrenderBurstIntervalLabel.text =
            viewModel.enemiesManager.surrenderBurstInterval.formatString()

        binding.surrenderBurstCountBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    if (fromUser) viewModel.activateHaptic(HapticEffect.TICK)
                    val surrenderBurstCount =
                        getProgressBarValue(
                            progress = progress,
                            min = MIN_BURST_COUNT,
                            max = MAX_BURST_COUNT,
                        )
                    viewModel.enemiesManager.surrenderBurstCount = surrenderBurstCount
                    binding.surrenderBurstCountLabel.text = surrenderBurstCount.formatString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    viewModel.activateHaptic(HapticEffect.TICK)
                    viewModel.playSound(SoundEffect.BEEP_2)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    viewModel.playSound(SoundEffect.BEEP_2)
                    viewModel.viewModelScope.launch {
                        context.userSettings.updateData {
                            it.copy {
                                surrenderBurstCount = viewModel.enemiesManager.surrenderBurstCount
                            }
                        }
                    }
                }
            }
        )

        binding.surrenderBurstIntervalBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    if (fromUser) viewModel.activateHaptic(HapticEffect.TICK)
                    val surrenderBurstInterval =
                        getProgressBarValue(
                            progress = progress,
                            min = MIN_BURST_INTERVAL,
                            max = MAX_BURST_INTERVAL,
                        )
                    viewModel.enemiesManager.surrenderBurstInterval = surrenderBurstInterval
                    binding.surrenderBurstIntervalLabel.text = surrenderBurstInterval.formatString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    viewModel.activateHaptic(HapticEffect.TICK)
                    viewModel.playSound(SoundEffect.BEEP_2)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    viewModel.playSound(SoundEffect.BEEP_2)
                    viewModel.viewModelScope.launch {
                        context.userSettings.updateData {
                            it.copy {
                                surrenderBurstInterval =
                                    viewModel.enemiesManager.surrenderBurstInterval
                            }
                        }
                    }
                }
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            with(binding) {
                surrenderBurstCountBar.max = MAX_BURST_COUNT
                surrenderBurstCountBar.min = MIN_BURST_COUNT
                surrenderBurstIntervalBar.max = MAX_BURST_INTERVAL
                surrenderBurstIntervalBar.min = MIN_BURST_INTERVAL
            }
        }
    }

    private fun clearFocus() {
        viewModel.hideKeyboard(binding.root)
        binding.surrenderRangeField.clearFocus()
    }

    private fun getProgress(value: Int, min: Int, max: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            value
        } else {
            ((value - min) * MAX_PROGRESS / (max - min)).roundToInt()
        }

    private fun getProgressBarValue(progress: Int, min: Int, max: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            progress.coerceIn(min, max)
        } else {
            min + (progress * (max - min) / MAX_PROGRESS).roundToInt()
        }

    private companion object {
        const val MAX_PROGRESS = 100f

        const val MIN_BURST_COUNT = 1
        const val MAX_BURST_COUNT = 20

        const val MIN_BURST_INTERVAL = 200
        const val MAX_BURST_INTERVAL = 1000
    }
}
