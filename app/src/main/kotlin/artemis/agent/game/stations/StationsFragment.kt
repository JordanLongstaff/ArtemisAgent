package artemis.agent.game.stations

import android.os.Bundle
import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.SoundEffect
import artemis.agent.collectLatestWhileStarted
import artemis.agent.databinding.StationsFragmentBinding
import artemis.agent.databinding.fragmentViewBinding

class StationsFragment : Fragment(R.layout.stations_fragment) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: StationsFragmentBinding by fragmentViewBinding()

    enum class Page(
        val pageClass: Class<out Fragment>,
        @IdRes val buttonId: Int
    ) {
        FRIENDLY(
            StationEntryFragment::class.java,
            R.id.friendlyStationsButton
        ),
        ENEMY(
            EnemyStationsFragment::class.java,
            R.id.enemyStationsButton
        ),
        DESTROYED(
            DestroyedStationsFragment::class.java,
            R.id.destroyedStationsButton
        )
    }

    private var currentPage: Page? = null
        set(page) {
            if (page != null && field != page) {
                field = page
                childFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(R.id.stationsFragmentContainer, page.pageClass, null)
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val friendlyStationsButton = binding.friendlyStationsButton
        val enemyStationsButton = binding.enemyStationsButton
        val destroyedStationsButton = binding.destroyedStationsButton
        val stationsListSelector = binding.stationsListSelector

        friendlyStationsButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        enemyStationsButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        destroyedStationsButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        stationsListSelector.setOnCheckedChangeListener { _, checkedId ->
            currentPage = Page.entries.find { it.buttonId == checkedId }
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.stationPage) {
            stationsListSelector.check(it.buttonId)
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.stationsExist) {
            friendlyStationsButton.visibility = if (it) {
                if (currentPage != Page.DESTROYED) {
                    friendlyStationsButton.isChecked = true
                }
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.enemyStationsExist) {
            enemyStationsButton.visibility = if (it) {
                if (
                    !viewModel.isBorderWarPossible &&
                    viewModel.isSingleAlly &&
                    currentPage != Page.DESTROYED
                ) {
                    enemyStationsButton.isChecked = true
                }
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    override fun onStop() {
        currentPage?.also { viewModel.stationPage.value = it }
        super.onStop()
    }
}
