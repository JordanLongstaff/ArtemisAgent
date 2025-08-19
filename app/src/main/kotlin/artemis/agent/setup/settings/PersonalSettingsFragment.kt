package artemis.agent.setup.settings

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import artemis.agent.AgentViewModel
import artemis.agent.AgentViewModel.Companion.formatString
import artemis.agent.R
import artemis.agent.UserSettingsSerializer.userSettings
import artemis.agent.copy
import artemis.agent.databinding.SettingsPersonalBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.util.HapticEffect
import artemis.agent.util.SoundEffect
import artemis.agent.util.collectLatestWhileStarted
import kotlinx.coroutines.launch

class PersonalSettingsFragment : Fragment(R.layout.settings_personal) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsPersonalBinding by fragmentViewBinding()

    private var initialized = false

    private var volume: Int
        get() = (viewModel.volume * AgentViewModel.VOLUME_SCALE).toInt()
        set(value) {
            viewModel.volume = value.toFloat()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val themeOptionButtons =
            arrayOf(
                binding.themeDefaultButton,
                binding.themeRedButton,
                binding.themeGreenButton,
                binding.themeYellowButton,
                binding.themeBlueButton,
                binding.themePurpleButton,
                binding.themeOrangeButton,
            )

        viewLifecycleOwner.collectLatestWhileStarted(view.context.userSettings.data) {
            themeOptionButtons[it.themeValue].isChecked = true

            binding.threeDigitDirectionsButton.isChecked = it.threeDigitDirections
            binding.threeDigitDirectionsLabel.text =
                getString(R.string.direction, if (it.threeDigitDirections) "000" else "0")

            binding.soundVolumeBar.progress = it.soundVolume

            binding.soundMuteButton.isChecked = it.soundMuted
            updateMuteButtonEnabled(it.soundVolume)

            binding.enableHapticsButton.isChecked = it.hapticsEnabled
        }

        themeOptionButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                viewModel.activateHaptic()
                viewModel.playSound(SoundEffect.BEEP_2)
            }
            button.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.viewModelScope.launch {
                        button.context.userSettings.updateData { it.copy { themeValue = index } }
                    }
                }
            }
        }

        binding.threeDigitDirectionsButton.setOnClickListener {
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.threeDigitDirectionsButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.viewModelScope.launch {
                view.context.userSettings.updateData {
                    it.copy { threeDigitDirections = isChecked }
                }
            }
        }

        binding.enableHapticsButton.setOnClickListener { viewModel.playSound(SoundEffect.BEEP_2) }

        binding.enableHapticsButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.viewModelScope.launch {
                view.context.userSettings.updateData { it.copy { hapticsEnabled = isChecked } }
                if (initialized) viewModel.activateHaptic() else initialized = true
            }
        }

        prepareSoundVolumeComponents()
    }

    private fun prepareSoundVolumeComponents() {
        val context = binding.root.context

        binding.soundMuteButton.setOnClickListener { viewModel.activateHaptic() }

        binding.soundMuteButton.setOnCheckedChangeListener { view, isChecked ->
            viewModel.viewModelScope.launch {
                context.userSettings.updateData { it.copy { soundMuted = isChecked } }
                updateSoundVolumeLabel(volume)
                viewModel.playSound(SoundEffect.BEEP_2)
            }
        }

        updateSoundVolumeLabel(volume)

        binding.soundVolumeBar.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    if (fromUser) viewModel.activateHaptic(HapticEffect.TICK)
                    volume = progress
                    updateSoundVolumeLabel(progress)
                    updateMuteButtonEnabled(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    viewModel.activateHaptic(HapticEffect.TICK)
                    viewModel.playSound(SoundEffect.BEEP_2)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    viewModel.playSound(SoundEffect.BEEP_2)
                    viewModel.viewModelScope.launch {
                        context.userSettings.updateData { it.copy { soundVolume = volume } }
                    }
                }
            }
        )
    }

    private fun updateSoundVolumeLabel(progress: Int) {
        binding.soundVolumeLabel.text =
            if (binding.soundMuteButton.isChecked) "0" else progress.formatString()
    }

    private fun updateMuteButtonEnabled(volume: Int) {
        binding.soundMuteButton.isEnabled = volume > 0
    }
}
