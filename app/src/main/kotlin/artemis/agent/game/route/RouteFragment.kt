package artemis.agent.game.route

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.widget.PopupWindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.databinding.RouteEntryBinding
import artemis.agent.databinding.RouteFragmentBinding
import artemis.agent.databinding.SelectorPopupBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.game.GameFragment
import artemis.agent.game.ObjectEntry
import artemis.agent.game.stations.StationsFragment
import artemis.agent.generic.GenericDataViewHolder
import artemis.agent.util.SoundEffect
import artemis.agent.util.collectLatestWhileStarted
import com.walkertribe.ian.enums.BaseMessage
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.protocol.core.comm.CommsOutgoingPacket

class RouteFragment : Fragment(R.layout.route_fragment) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: RouteFragmentBinding by fragmentViewBinding()

    private val popupBinding: SelectorPopupBinding by lazy {
        SelectorPopupBinding.inflate(layoutInflater)
    }

    private val suppliesSelectorPopup: PopupWindow by lazy {
        popupBinding.run {
            PopupWindow(root).also { popup ->
                PopupWindowCompat.setOverlapAnchor(popup, true)
                popup.animationStyle = R.style.WindowAnimation

                val selectorButton = binding.routeSuppliesSelector
                selectorButton.setOnClickListener {
                    viewModel.activateHaptic()
                    viewModel.playSound(SoundEffect.BEEP_2)
                    root.measure(
                        View.MeasureSpec.makeMeasureSpec(
                            it.measuredWidth,
                            View.MeasureSpec.EXACTLY,
                        ),
                        View.MeasureSpec.makeMeasureSpec(
                            binding.root.measuredHeight,
                            View.MeasureSpec.AT_MOST,
                        ),
                    )
                    popup.showAsDropDown(selectorButton)
                    popup.update(it.left, it.top, it.measuredWidth, root.measuredHeight)
                }

                selectorList.itemAnimator = null
                selectorList.adapter = routeSuppliesAdapter
            }
        }
    }

    private val routeSuppliesAdapter: RouteSuppliesAdapter by lazy { RouteSuppliesAdapter() }

    private val ordnanceTypes: Array<OrdnanceType> by lazy {
        OrdnanceType.getAllForVersion(viewModel.version)
    }

    private val objective: RouteObjective
        get() = viewModel.routeObjective.value

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prepareRouteListView()
        setupRouteSelectionHandlers()
        setupRouteObjectiveCollectors()

        suppliesSelectorPopup.isFocusable = true

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.rootOpacity) {
            popupBinding.selectorList.alpha = it
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.jumping) {
            popupBinding.jumpInputDisabler.visibility = if (it) View.VISIBLE else View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        suppliesSelectorPopup.dismiss()
    }

    private fun prepareRouteListView() {
        val context = binding.root.context

        val routeListView = binding.routeListView
        routeListView.itemAnimator = null

        val routeAdapter = RouteAdapter()
        routeListView.adapter = routeAdapter

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.routeList) {
            routeAdapter.update(it)
        }

        routeListView.layoutManager =
            LinearLayoutManager(
                context,
                Configuration.ORIENTATION_LANDSCAPE - context.resources.configuration.orientation,
                false,
            )
    }

    private fun setupRouteSelectionHandlers() {
        val routeTasksButton = binding.routeTasksButton
        val routeSuppliesButton = binding.routeSuppliesButton
        val routeSuppliesSelector = binding.routeSuppliesSelector
        val fighterSupplyIndex = ordnanceTypes.size

        arrayOf(routeTasksButton, routeSuppliesButton).forEach { button ->
            button.setOnClickListener {
                viewModel.activateHaptic()
                viewModel.playSound(SoundEffect.BEEP_2)
            }
        }

        routeTasksButton.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) return@setOnCheckedChangeListener

            routeSuppliesSelector.visibility =
                View.GONE / routeSuppliesSelector.resources.configuration.orientation
            viewModel.routeObjective.value = RouteObjective.Tasks
        }

        routeSuppliesButton.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) return@setOnCheckedChangeListener

            routeSuppliesSelector.visibility = View.VISIBLE

            val position = viewModel.routeSuppliesIndex
            val newObjective =
                if (position == fighterSupplyIndex) RouteObjective.ReplacementFighters
                else RouteObjective.Ordnance(OrdnanceType.entries[position])
            viewModel.routeObjective.value = newObjective
        }
    }

    private fun setupRouteObjectiveCollectors() {
        viewLifecycleOwner.collectLatestWhileStarted(viewModel.routeObjective) { objective ->
            if (objective is RouteObjective.Tasks) {
                binding.routeTasksButton.isChecked = true
            } else {
                binding.routeSuppliesSelector.text =
                    if (objective is RouteObjective.Ordnance)
                        objective.ordnanceType.getLabelFor(viewModel.version)
                    else binding.routeSuppliesSelector.context.getString(R.string.fighters)
                binding.routeSuppliesButton.isChecked = true
            }
            binding.routeSuppliesData.text = objective.getDataFrom(viewModel)
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.totalFighters) {
            val routeObjective = viewModel.routeObjective.value
            if (routeObjective is RouteObjective.ReplacementFighters) {
                binding.routeSuppliesData.text = routeObjective.getDataFrom(viewModel)
            }
            routeSuppliesAdapter.notifyItemChanged(ordnanceTypes.size)
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.ordnanceUpdated) {
            if (!it) return@collectLatestWhileStarted

            val routeObjective = viewModel.routeObjective.value
            if (routeObjective is RouteObjective.Ordnance) {
                binding.routeSuppliesData.text = routeObjective.getDataFrom(viewModel)
            }
            routeSuppliesAdapter.notifyItemRangeChanged(0, ordnanceTypes.size)
        }
    }

    private class RouteDiffUtilCallback(
        private val oldRoute: List<RouteEntry>,
        private val newRoute: List<RouteEntry>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldRoute.size

        override fun getNewListSize(): Int = newRoute.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldRoute[oldItemPosition].pathKey == newRoute[newItemPosition].pathKey

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = false
    }

    private inner class RouteEntryViewHolder(private val entryBinding: RouteEntryBinding) :
        RecyclerView.ViewHolder(entryBinding.root) {
        fun bind(entry: RouteEntry) {
            val root = entryBinding.root
            val context = root.context
            val objEntry = entry.objEntry

            root.setBackgroundColor(objEntry.getBackgroundColor(context))
            entryBinding.destDirectionLabel.text = getString(R.string.direction, objEntry.heading)
            entryBinding.destRangeLabel.text = getString(R.string.range, objEntry.range)

            entryBinding.destReasonsLabel.text = entry.getReasonText(objective, context, viewModel)
            entryBinding.destNameLabel.text = objEntry.fullName

            if (objEntry is ObjectEntry.Station) bindStation(entry, objEntry)
            else if (objEntry is ObjectEntry.Ally) bindAlly(objEntry)
        }

        private fun bindStation(entry: RouteEntry, objEntry: ObjectEntry.Station) {
            val ordnanceObjective = objective as? RouteObjective.Ordnance

            val root = entryBinding.root
            val destStandbyButton = entryBinding.destStandbyButton
            val destBuildButton = entryBinding.destBuildButton
            val destAllyCommandButton = entryBinding.destAllyCommandButton
            val destBuildTimeLabel = entryBinding.destBuildTimeLabel

            destStandbyButton.visibility = View.VISIBLE
            destStandbyButton.isEnabled = !objEntry.isStandingBy
            destStandbyButton.setOnClickListener {
                sendToStation(objEntry, BaseMessage.StandByForDockingOrCeaseOperation)
            }

            destBuildTimeLabel.visibility =
                if (ordnanceObjective == null) {
                    destBuildButton.visibility = View.GONE
                    View.GONE
                } else if (ordnanceObjective.ordnanceType == objEntry.builtOrdnanceType) {
                    destBuildButton.visibility = View.GONE
                    destBuildTimeLabel.text = entry.getBuildTimeText(objective, root.context)
                    View.VISIBLE
                } else {
                    destBuildButton.visibility = View.VISIBLE
                    destBuildButton.setOnClickListener {
                        sendToStation(objEntry, BaseMessage.Build(ordnanceObjective.ordnanceType))
                    }

                    View.GONE
                }

            destAllyCommandButton.visibility = View.INVISIBLE

            root.setOnClickListener {
                with(viewModel) {
                    activateHaptic()
                    playSound(SoundEffect.BEEP_1)
                    objEntry.obj.name.value?.also {
                        stationName.value = it
                        stationPage.value = StationsFragment.Page.FRIENDLY
                        currentGamePage.value = GameFragment.Page.STATIONS
                    }
                }
            }
        }

        private fun sendToStation(objEntry: ObjectEntry.Station, message: BaseMessage) {
            with(viewModel) {
                activateHaptic()
                playSound(SoundEffect.BEEP_2)
                sendToServer(CommsOutgoingPacket(objEntry.obj, message, vesselData))
            }
        }

        private fun bindAlly(objEntry: ObjectEntry.Ally) {
            val root = entryBinding.root
            val destStandbyButton = entryBinding.destStandbyButton
            val destBuildButton = entryBinding.destBuildButton
            val destAllyCommandButton = entryBinding.destAllyCommandButton
            val destBuildTimeLabel = entryBinding.destBuildTimeLabel

            destBuildButton.visibility = View.GONE
            destStandbyButton.visibility = View.GONE
            destBuildTimeLabel.visibility = View.GONE

            destAllyCommandButton.visibility =
                if (objEntry.isInstructable) {
                    destAllyCommandButton.setOnClickListener {
                        with(viewModel) {
                            activateHaptic()
                            playSound(SoundEffect.BEEP_1)
                            showingDestroyedAllies.value = false
                            scrollToAlly = objEntry
                            focusedAlly.value = objEntry
                            currentGamePage.value = GameFragment.Page.ALLIES
                        }
                    }
                    View.VISIBLE
                } else {
                    View.INVISIBLE
                }

            root.setOnClickListener {
                with(viewModel) {
                    activateHaptic()
                    playSound(SoundEffect.BEEP_1)
                    showingDestroyedAllies.value = false
                    scrollToAlly = objEntry
                    currentGamePage.value = GameFragment.Page.ALLIES
                }
            }
        }
    }

    private inner class RouteAdapter : RecyclerView.Adapter<RouteEntryViewHolder>() {
        var routePoints = listOf<RouteEntry>()
            private set

        override fun getItemCount(): Int = routePoints.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteEntryViewHolder =
            RouteEntryViewHolder(
                RouteEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )

        override fun onBindViewHolder(holder: RouteEntryViewHolder, position: Int) {
            holder.bind(routePoints[position])
        }

        fun update(value: List<RouteEntry>) {
            DiffUtil.calculateDiff(RouteDiffUtilCallback(routePoints, value))
                .dispatchUpdatesTo(this)
            routePoints = value
        }
    }

    private inner class RouteSuppliesAdapter : RecyclerView.Adapter<GenericDataViewHolder>() {
        override fun getItemCount(): Int =
            ordnanceTypes.size +
                if (viewModel.version < RouteObjective.ReplacementFighters.REPORT_VERSION) 0 else 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericDataViewHolder =
            GenericDataViewHolder(parent)

        override fun onBindViewHolder(holder: GenericDataViewHolder, position: Int) {
            val boundObjective: RouteObjective
            val routeObjectiveLabel: String =
                if (position == ordnanceTypes.size) {
                    boundObjective = RouteObjective.ReplacementFighters
                    holder.itemView.context.getString(R.string.fighters)
                } else {
                    val ordnance = ordnanceTypes[position]
                    boundObjective = RouteObjective.Ordnance(ordnance)
                    ordnance.getLabelFor(viewModel.version)
                }

            holder.name = routeObjectiveLabel
            holder.data = boundObjective.getDataFrom(viewModel)
            holder.itemView.setOnClickListener {
                with(viewModel) {
                    activateHaptic()
                    playSound(SoundEffect.BEEP_2)
                    routeObjective.value = boundObjective
                    routeSuppliesIndex = position
                }
                suppliesSelectorPopup.dismiss()
            }
        }
    }
}
