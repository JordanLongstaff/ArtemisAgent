package artemis.agent.setup

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.databinding.SetupFragmentBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.help.HelpFragment
import artemis.agent.setup.settings.SettingsFragment
import artemis.agent.util.SoundEffect
import artemis.agent.util.collectLatestWhileStarted

class SetupFragment : Fragment(R.layout.setup_fragment) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SetupFragmentBinding by fragmentViewBinding()

    enum class Page(val pageClass: Class<out Fragment>, @IdRes val buttonId: Int) {
        CONNECT(ConnectFragment::class.java, R.id.connectPageButton),
        SHIPS(ShipsFragment::class.java, R.id.shipsPageButton),
        SETTINGS(SettingsFragment::class.java, R.id.settingsPageButton),
    }

    private var currentPage: Page? = null
        set(newPage) {
            if (newPage != null && field != newPage) {
                field = newPage
                childFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(R.id.setupFragmentContainer, newPage.pageClass, null)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.setupPageSelector.children.forEach { button ->
            button.setOnClickListener { viewModel.playSound(SoundEffect.BEEP_2) }
        }

        binding.setupPageSelector.setOnCheckedChangeListener { _, checkedId ->
            currentPage = Page.entries.find { it.buttonId == checkedId }
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.connectedUrl) {
            if (it.isNotEmpty()) {
                binding.shipsPageButton.isChecked = true
            }
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.setupFragmentPage) {
            binding.setupPageSelector.check(it.buttonId)
        }

        viewModel.focusedAlly.value = null
        viewModel.helpTopicIndex.value = HelpFragment.MENU
    }

    override fun onStop() {
        currentPage?.also { viewModel.setupFragmentPage.value = it }
        super.onStop()
    }
}
