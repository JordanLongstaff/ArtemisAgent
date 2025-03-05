package artemis.agent.game

import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.core.widget.PopupWindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.databinding.GameFragmentBinding
import artemis.agent.databinding.SelectorEntryBinding
import artemis.agent.databinding.SelectorPopupBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.game.allies.AlliesFragment
import artemis.agent.game.biomechs.BiomechsFragment
import artemis.agent.game.enemies.EnemiesFragment
import artemis.agent.game.misc.MiscFragment
import artemis.agent.game.missions.MissionsFragment
import artemis.agent.game.route.RouteFragment
import artemis.agent.game.route.RouteObjective
import artemis.agent.game.stations.StationsFragment
import artemis.agent.help.HelpFragment
import artemis.agent.util.SoundEffect
import artemis.agent.util.collectLatestWhileStarted
import com.walkertribe.ian.enums.AlertStatus
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.protocol.core.comm.ToggleRedAlertPacket
import kotlin.math.sign
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GameFragment : Fragment(R.layout.game_fragment) {
    private val viewModel: AgentViewModel by activityViewModels()

    enum class Page(val pageClass: Class<out Fragment>) {
        STATIONS(StationsFragment::class.java),
        ALLIES(AlliesFragment::class.java),
        MISSIONS(MissionsFragment::class.java),
        ROUTE(RouteFragment::class.java),
        ENEMIES(EnemiesFragment::class.java),
        BIOMECHS(BiomechsFragment::class.java),
        MISC(MiscFragment::class.java),
    }

    private var currentPage: Page? = null
        set(page) {
            if (field != page) {
                field = page
                val pageName = page?.name ?: ""
                binding.gamePageSelectorButton.text = pageName
                childFragmentManager.commit {
                    setReorderingAllowed(true)
                    if (page == null) {
                        childFragmentManager.fragments.forEach(this::remove)
                    } else {
                        replace(R.id.gameFragmentContainer, page.pageClass, null)
                    }
                }
            }
        }

    private val binding: GameFragmentBinding by fragmentViewBinding()

    private val popupBinding: SelectorPopupBinding by lazy {
        SelectorPopupBinding.inflate(layoutInflater)
    }

    private val gamePagePopup: PopupWindow by lazy {
        popupBinding.run {
            PopupWindow(root).also { popup ->
                PopupWindowCompat.setOverlapAnchor(popup, true)
                popup.animationStyle = R.style.WindowAnimation

                val gamePageSelectorButton = binding.gamePageSelectorButton
                gamePageSelectorButton.setOnClickListener {
                    viewModel.playSound(SoundEffect.BEEP_2)
                    root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                    popup.showAsDropDown(gamePageSelectorButton)
                    popup.update(it.left, it.top, it.measuredWidth, root.measuredHeight)
                }

                selectorList.itemAnimator = null

                selectorList.adapter = gamePageAdapter
            }
        }
    }

    private val gamePageAdapter: GamePageAdapter by lazy { GamePageAdapter() }

    private val borderWar: Flow<WarStatus?> by lazy {
        viewModel.isBorderWar.combine(viewModel.borderWarStatus) { isWar, status ->
            if (isWar) status else null
        }
    }

    private val fighterStockStrings =
        intArrayOf(
            R.string.single_seat_craft_docked,
            R.string.single_seat_craft_launched,
            R.string.single_seat_craft_lost,
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.helpTopicIndex.value = HelpFragment.MENU
        viewModel.settingsPage.value = null

        setupInventoryButton()
        setupVisibilityControllers()
        setupPageSelector()
        setupAlertButton()
        setupAgentButton()

        viewLifecycleOwner.collectLatestWhileStarted(borderWar) { status ->
            val visibility =
                if (status != null) {
                    binding.borderWarLabel.text = getString(R.string.border_war_status, status.name)
                    binding.borderWarBackground.setBackgroundColor(
                        ContextCompat.getColor(
                            binding.borderWarBackground.context,
                            status.backgroundColor,
                        )
                    )
                    View.VISIBLE
                } else {
                    View.GONE
                }
            binding.borderWarBackground.visibility = visibility
            binding.borderWarLabel.visibility = visibility
        }
    }

    private fun setupVisibilityControllers() {
        viewLifecycleOwner.collectLatestWhileStarted(viewModel.rootOpacity) {
            binding.root.alpha = it
            popupBinding.selectorList.alpha = it
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.jumping) {
            popupBinding.jumpInputDisabler.visibility = if (it) View.VISIBLE else View.GONE
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.selectableShips) { ships ->
            val index = viewModel.shipIndex.value
            val (shipNameVisibility, shipNumberText) =
                when {
                    ships.isEmpty() -> View.GONE to getString(R.string.no_ships)
                    index in ships.indices -> {
                        binding.shipNameLabel.text = ships[index].name
                        View.VISIBLE to getString(R.string.ship_number, index + 1)
                    }
                    else -> View.GONE to getString(R.string.no_ship_selected)
                }

            binding.shipNumberLabel.text = shipNumberText
            binding.shipNameLabel.visibility = shipNameVisibility
            binding.waitingForGameLabel.visibility = shipNameVisibility
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.gameIsRunning) { isRunning ->
            val waitingVisibility: Int
            val visibility =
                if (isRunning) {
                    waitingVisibility = View.GONE
                    View.VISIBLE
                } else {
                    waitingVisibility = binding.shipNameLabel.visibility
                    viewModel.rootOpacity.value = 1f
                    binding.redAlertButton.isChecked = false
                    gamePagePopup.dismiss()
                    View.GONE
                }

            binding.waitingForGameLabel.visibility = waitingVisibility
            binding.gamePageSelectorButton.visibility = visibility
            binding.gameFragmentContainer.visibility = visibility
            binding.inventoryButton.visibility = visibility
            binding.redAlertButton.visibility = visibility
            binding.doubleAgentButton.visibility = visibility

            if (
                binding.root.resources.configuration.orientation ==
                    Configuration.ORIENTATION_PORTRAIT
            ) {
                binding.agentLabel.visibility = visibility
            }
        }
    }

    private fun setupInventoryButton() {
        binding.inventoryButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)

            val player = viewModel.playerShip ?: return@setOnClickListener
            val vessel = player.getVessel(viewModel.vesselData) ?: return@setOnClickListener

            val ordnanceStocks =
                OrdnanceType.getAllForVersion(viewModel.version).map {
                    val max = vessel.ordnanceStorage[it] ?: 0
                    val current = player.getTotalOrdnanceCount(it)
                    Triple(it, current, max)
                }

            val ordnanceStockMessage =
                ordnanceStocks.joinToString("\n") { (ordnanceType, current, max) ->
                    getString(
                        R.string.ordnance_stock,
                        ordnanceType.getLabelFor(viewModel.version),
                        current,
                        max,
                    )
                }

            val neededOrdnanceType =
                ordnanceStocks.find { (_, current, max) -> current < max }?.first

            val maxFighters =
                viewModel.version
                    .compareTo(RouteObjective.ReplacementFighters.SHUTTLE_VERSION)
                    .sign
                    .coerceAtMost(0) + 1 + vessel.bayCount
            val launchedFighters = viewModel.fighterIDs.size
            val lostFighters = maxFighters - viewModel.totalFighters.value
            val dockedFighters = maxFighters - lostFighters - launchedFighters

            val fighterStockMessage =
                intArrayOf(dockedFighters, launchedFighters, lostFighters)
                    .zip(fighterStockStrings)
                    .filter { it.first > 0 }
                    .joinToString("\n") { (count, id) -> getString(id, count) }

            val fullMessage = "$ordnanceStockMessage\n$fighterStockMessage"

            val routeObjective =
                neededOrdnanceType?.let(RouteObjective::Ordnance)
                    ?: RouteObjective.ReplacementFighters.takeIf { lostFighters > 0 }

            AlertDialog.Builder(requireContext())
                .setMessage(fullMessage)
                .setCancelable(true)
                .apply {
                    if (routeObjective != null) {
                        setPositiveButton(R.string.route_for_supplies) { _, _ ->
                            viewModel.playSound(SoundEffect.BEEP_2)
                            viewModel.routeObjective.value = routeObjective
                            viewModel.currentGamePage.value = Page.ROUTE
                        }
                    }
                }
                .show()
        }
    }

    private fun setupPageSelector() {
        gamePagePopup.isFocusable = true

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.currentGamePage) { currentPage = it }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.gamePages) {
            gamePageAdapter.update(it)
        }
    }

    private fun setupAlertButton() {
        binding.redAlertButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_1)
            viewModel.sendToServer(ToggleRedAlertPacket())
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.alertStatus) {
            binding.redAlertButton.isChecked = it == AlertStatus.RED
        }
    }

    private fun setupAgentButton() {
        viewLifecycleOwner.collectLatestWhileStarted(viewModel.doubleAgentEnabled) {
            binding.doubleAgentButton.isEnabled = it
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.doubleAgentActive) {
            binding.doubleAgentButton.isChecked = it
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.doubleAgentText) {
            binding.doubleAgentButton.text = it
        }

        binding.doubleAgentButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_1)
            viewModel.activateDoubleAgent()
        }
    }

    override fun onPause() {
        super.onPause()
        gamePagePopup.dismiss()
    }

    private class GamePagesDiffUtilCallback(
        private val oldList: List<Pair<Page, Boolean>>,
        private val newList: List<Pair<Page, Boolean>>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].first == newList[newItemPosition].first

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].second == newList[newItemPosition].second
    }

    private class GamePageViewHolder(private val entryBinding: SelectorEntryBinding) :
        RecyclerView.ViewHolder(entryBinding.root) {
        fun bind(page: Page, flashing: Boolean) {
            entryBinding.entryLabel.text = page.name
            entryBinding.flashBackground.alpha = if (flashing) PAGE_FLASH_ALPHA else 0f
        }
    }

    private inner class GamePageAdapter : RecyclerView.Adapter<GamePageViewHolder>() {
        private var pages = listOf<Pair<Page, Boolean>>()

        override fun getItemCount(): Int = pages.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GamePageViewHolder =
            GamePageViewHolder(
                SelectorEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )

        override fun onBindViewHolder(holder: GamePageViewHolder, position: Int) {
            pages[position].also { (page, flashing) ->
                holder.bind(page, flashing)
                holder.itemView.setOnClickListener {
                    viewModel.playSound(
                        if (viewModel.currentGamePage.value == page) SoundEffect.BEEP_2
                        else SoundEffect.CONFIRMATION
                    )
                    if (page != Page.ALLIES) {
                        viewModel.focusedAlly.value = null
                    }
                    viewModel.currentGamePage.value = page
                    gamePagePopup.dismiss()
                }
            }
        }

        fun update(value: Map<Page, Boolean>) {
            val newList = value.toList()
            DiffUtil.calculateDiff(GamePagesDiffUtilCallback(pages, newList))
                .dispatchUpdatesTo(this)
            pages = newList

            binding.gamePageSelectorFlash.visibility =
                if (newList.any { it.first != currentPage && it.second }) View.VISIBLE
                else View.GONE

            viewModel.currentGamePage.also {
                val pageStillExists = it.value?.let(value::containsKey)
                if (pageStillExists != true) {
                    it.value = value.keys.firstOrNull()
                }
            }
        }
    }

    private companion object {
        const val PAGE_FLASH_ALPHA = 0.375f
    }
}
