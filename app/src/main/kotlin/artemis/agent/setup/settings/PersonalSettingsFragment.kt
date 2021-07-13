package artemis.agent.setup.settings

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.SoundEffect
import artemis.agent.UserSettingsSerializer.userSettings
import artemis.agent.collectLatestWhileStarted
import artemis.agent.copy
import artemis.agent.databinding.SettingsPersonalBinding
import artemis.agent.databinding.fragmentViewBinding
import kotlinx.coroutines.launch

class PersonalSettingsFragment : Fragment(R.layout.settings_personal) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsPersonalBinding by fragmentViewBinding()

    private var volume: Int
        get() = (viewModel.volume * AgentViewModel.VOLUME_SCALE).toInt()
        set(value) { viewModel.volume = value.toFloat() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val themeOptionButtons = arrayOf(
            binding.themeDefaultButton,
            binding.themeRedButton,
            binding.themeGreenButton,
            binding.themeYellowButton,
            binding.themeBlueButton,
            binding.themePurpleButton,
        )

        viewLifecycleOwner.collectLatestWhileStarted(view.context.userSettings.data) {
            themeOptionButtons[it.themeValue].isChecked = true

            binding.threeDigitDirectionsButton.isChecked = it.threeDigitDirections
            binding.threeDigitDirectionsLabel.text = getString(
                R.string.direction,
                if (it.threeDigitDirections) "000" else "0"
            )

            if (volume != it.soundVolume) {
                volume = it.soundVolume
                binding.soundVolumeBar.progress = it.soundVolume
            }
        }

        themeOptionButtons.forEachIndexed { index, button ->
            button.setOnClickListener { viewModel.playSound(SoundEffect.BEEP_2) }
            button.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    viewModel.viewModelScope.launch {
                        button.context.userSettings.updateData {
                            it.copy { themeValue = index }
                        }
                    }
                }
            }
        }

        binding.threeDigitDirectionsButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        binding.threeDigitDirectionsButton.setOnCheckedChangeListener { _, isChecked ->
            viewModel.viewModelScope.launch {
                view.context.userSettings.updateData {
                    it.copy { threeDigitDirections = isChecked }
                }
            }
        }

        binding.soundVolumeBar.progress = volume
        binding.soundVolumeLabel.text = volume.toString()

        binding.soundVolumeBar.setOnSeekBarChangeListener(
            object : OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    volume = progress
                    binding.soundVolumeLabel.text = progress.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    viewModel.playSound(SoundEffect.BEEP_2)
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    viewModel.playSound(SoundEffect.BEEP_2)
                    viewModel.viewModelScope.launch {
                        view.context.userSettings.updateData {
                            it.copy {
                                soundVolume = volume
                            }
                        }
                    }
                }
            }
        )
    }
}
