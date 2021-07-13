package artemis.agent.setup.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.SoundEffect
import artemis.agent.UserSettingsSerializer.userSettings
import artemis.agent.collectLatestWhileStarted
import artemis.agent.copy
import artemis.agent.databinding.SettingsMenuBinding
import artemis.agent.databinding.SettingsMenuEntryBinding
import artemis.agent.databinding.fragmentViewBinding
import kotlinx.coroutines.launch

class SettingsMenuFragment : Fragment(R.layout.settings_menu) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SettingsMenuBinding by fragmentViewBinding()

    private val pageToggles = BooleanArray(SettingsFragment.Page.entries.size) { true }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SettingsPageAdapter()
        val settingsPageMenu = binding.settingsPageMenu
        settingsPageMenu.itemAnimator = null
        settingsPageMenu.adapter = adapter

        viewLifecycleOwner.collectLatestWhileStarted(view.context.userSettings.data) {
            val wasEnabled = pageToggles.copyOf()

            pageToggles[SettingsFragment.Page.MISSION.ordinal] = it.missionsEnabled
            pageToggles[SettingsFragment.Page.ALLIES.ordinal] = it.alliesEnabled
            pageToggles[SettingsFragment.Page.ENEMIES.ordinal] = it.enemiesEnabled
            pageToggles[SettingsFragment.Page.BIOMECHS.ordinal] = it.biomechsEnabled
            pageToggles[SettingsFragment.Page.ROUTING.ordinal] = it.routingEnabled

            wasEnabled.zip(pageToggles).forEachIndexed { index, (before, after) ->
                if (before != after) {
                    adapter.notifyItemChanged(index)
                }
            }
        }
    }

    private class SettingsPageViewHolder(
        private val entryBinding: SettingsMenuEntryBinding
    ) : RecyclerView.ViewHolder(entryBinding.root) {
        fun bind(page: SettingsFragment.Page, viewModel: AgentViewModel, isOn: Boolean) {
            entryBinding.settingsEntryTitle.setText(page.titleRes)
            entryBinding.settingsEntryToggle.visibility = page.onToggle?.let { onToggle ->
                entryBinding.settingsEntryToggle.setOnClickListener {
                    viewModel.playSound(SoundEffect.BEEP_2)
                }
                entryBinding.settingsEntryToggle.setOnCheckedChangeListener { _, isChecked ->
                    viewModel.viewModelScope.launch {
                        itemView.context.userSettings.updateData {
                            it.copy { onToggle(isChecked) }
                        }
                    }
                    if (!isOn && isChecked) {
                        viewModel.settingsPage.value = page
                    }
                }
                entryBinding.settingsEntryToggle.isChecked = isOn

                View.VISIBLE
            } ?: View.INVISIBLE

            entryBinding.root.setOnClickListener {
                if (entryBinding.settingsEntryToggle.isChecked) {
                    viewModel.playSound(SoundEffect.BEEP_2)
                    viewModel.settingsPage.value = page
                }
            }
        }
    }

    private inner class SettingsPageAdapter : RecyclerView.Adapter<SettingsPageViewHolder>() {
        override fun getItemCount(): Int = SettingsFragment.Page.entries.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsPageViewHolder =
            SettingsPageViewHolder(
                SettingsMenuEntryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: SettingsPageViewHolder, position: Int) {
            holder.bind(SettingsFragment.Page.entries[position], viewModel, pageToggles[position])
        }
    }
}
