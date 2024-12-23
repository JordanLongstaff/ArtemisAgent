package artemis.agent.game.stations

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.widget.PopupWindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.SoundEffect
import artemis.agent.collectLatestWhileStarted
import artemis.agent.databinding.SelectorEntryBinding
import artemis.agent.databinding.SelectorPopupBinding
import artemis.agent.databinding.StationEntryBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.game.ObjectEntry.Station
import artemis.agent.game.route.RouteObjective
import artemis.agent.generic.GenericDataViewHolder
import com.walkertribe.ian.enums.BaseMessage
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.protocol.core.comm.CommsOutgoingPacket
import kotlin.math.pow

class StationEntryFragment : Fragment(R.layout.station_entry) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: StationEntryBinding by fragmentViewBinding()

    private val stationPopupBinding: SelectorPopupBinding by lazy {
        SelectorPopupBinding.inflate(layoutInflater)
    }

    private val stationSelectorPopup: PopupWindow by lazy {
        stationPopupBinding.run {
            PopupWindow(root).also { popup ->
                PopupWindowCompat.setOverlapAnchor(popup, true)
                popup.animationStyle = R.style.WindowAnimation

                val selectorButton = binding.stationSelectorButton
                selectorButton.setOnClickListener {
                    viewModel.playSound(SoundEffect.BEEP_2)
                    root.measure(
                        View.MeasureSpec.makeMeasureSpec(
                            it.measuredWidth,
                            View.MeasureSpec.EXACTLY
                        ),
                        View.MeasureSpec.makeMeasureSpec(
                            binding.root.measuredHeight,
                            View.MeasureSpec.AT_MOST
                        )
                    )
                    popup.showAsDropDown(selectorButton)
                    popup.update(
                        it.left,
                        it.top,
                        it.measuredWidth,
                        root.measuredHeight
                    )
                }

                selectorList.itemAnimator = null
                selectorList.adapter = stationAdapter
            }
        }
    }

    private val stationAdapter: StationAdapter by lazy { StationAdapter() }

    private val ordnancePopupBinding: SelectorPopupBinding by lazy {
        SelectorPopupBinding.inflate(layoutInflater)
    }

    private val ordnanceSelectorPopup: PopupWindow by lazy {
        ordnancePopupBinding.run {
            PopupWindow(root).also { popup ->
                PopupWindowCompat.setOverlapAnchor(popup, true)
                popup.animationStyle = R.style.WindowAnimation

                val selectorButton = binding.stationBuildSelector
                selectorButton.setOnClickListener {
                    viewModel.playSound(SoundEffect.BEEP_2)
                    root.measure(
                        View.MeasureSpec.makeMeasureSpec(
                            selectorButton.measuredWidth,
                            View.MeasureSpec.EXACTLY
                        ),
                        View.MeasureSpec.makeMeasureSpec(
                            binding.root.measuredHeight,
                            View.MeasureSpec.AT_MOST
                        )
                    )
                    popup.showAsDropDown(selectorButton)
                    popup.update(
                        it.left,
                        it.bottom,
                        it.measuredWidth,
                        root.measuredHeight
                    )
                }

                selectorList.itemAnimator = null
                selectorList.adapter = ordnanceAdapter
            }
        }
    }

    private val ordnanceAdapter: OrdnanceAdapter by lazy { OrdnanceAdapter() }

    private val ordnanceTypes: Array<OrdnanceType> by lazy {
        OrdnanceType.getAllForVersion(viewModel.version)
    }

    private var currentStation: Station? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindClosestStationButton()

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.stationsRemain) {
            val noStationsVisibility: Int
            val elseVisibility: Int
            if (it) {
                noStationsVisibility = View.GONE
                elseVisibility = View.VISIBLE
            } else {
                noStationsVisibility = View.VISIBLE
                elseVisibility = View.GONE
                stationSelectorPopup.dismiss()
            }

            binding.root.children.forEach { v -> v.visibility = elseVisibility }
            binding.noStationsLabel.visibility = noStationsVisibility
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.currentStation) {
            onStationEntryUpdate(it)
        }

        preparePopupBindings()

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.stationSelectorFlashPercent) {
            binding.stationSelectorFlash.alpha = if (it < 1f) {
                val flashColorSlot = SELECTOR_COLORS.find { (percent) -> it >= percent }
                val alpha = flashColorSlot?.first?.let { percent ->
                    1f - percent.pow(ALPHA_EXPONENT)
                } ?: 0f
                val color = flashColorSlot?.second.let { colorRes ->
                    ContextCompat.getColor(
                        view.context,
                        colorRes ?: R.color.stationSelectorCritical
                    )
                }

                binding.stationSelectorFlash.setBackgroundColor(color)
                alpha
            } else {
                0f
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stationSelectorPopup.dismiss()
        ordnanceSelectorPopup.dismiss()
    }

    private fun bindClosestStationButton() {
        viewLifecycleOwner.collectLatestWhileStarted(viewModel.closestStationName) {
            binding.closestStationButton.apply {
                text = getString(R.string.closest_station, it)
                isEnabled = it != viewModel.stationName.value

                setOnClickListener { _ ->
                    viewModel.playSound(SoundEffect.BEEP_1)
                    viewModel.stationName.value = it
                }
            }
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.stationName) {
            binding.closestStationButton.isEnabled = it != viewModel.closestStationName.value
        }
    }

    private fun preparePopupBindings() {
        stationSelectorPopup.isFocusable = true
        ordnanceSelectorPopup.isFocusable = true

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.rootOpacity) {
            stationPopupBinding.selectorList.alpha = it
            ordnancePopupBinding.selectorList.alpha = it
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.jumping) {
            val visibility = if (it) View.VISIBLE else View.GONE
            stationPopupBinding.jumpInputDisabler.visibility = visibility
            ordnancePopupBinding.jumpInputDisabler.visibility = visibility
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.flashingStations) {
            stationAdapter.update(it)
        }
    }

    private fun onStationEntryUpdate(entry: Station) {
        currentStation = entry
        ordnanceAdapter.onStationChanged()

        with(binding) {
            val context = root.context
            root.setBackgroundColor(entry.getBackgroundColor(context))

            entry.isDocking = (viewModel.playerShip?.dockingBase?.value == entry.obj.id).also {
                entry.isDocked = it && viewModel.playerShip?.docked?.booleanValue == true
            }

            bindStationSelector(entry)
            updateInfoLabels(entry)
            bindStatusAndStandbyButtons(entry)
            updateOrdnanceLabels(entry)

            stationBuildSelector.text = entry.builtOrdnanceType.getLabelFor(viewModel.version)
            stationBuildTimeLabel.text = entry.getTimerText(context)
        }
    }

    private fun StationEntryBinding.bindStationSelector(entry: Station) {
        val context = root.context

        val station = entry.obj
        val shields = station.shieldsFront.value
        val shieldsMax = station.shieldsFrontMax.value
        val percent = shields / shieldsMax

        val selectorBackground = ResourcesCompat.getDrawable(
            context.resources,
            R.drawable.station_selector,
            context.theme
        ) as? GradientDrawable
        selectorBackground?.setStroke(
            context.resources.getDimensionPixelSize(R.dimen.paddingSmall),
            SELECTOR_COLORS.find { percent >= it.first }?.let {
                ContextCompat.getColor(context, it.second)
            } ?: Color.TRANSPARENT
        )
        stationSelectorButton.background = selectorBackground?.let { drawable ->
            InsetDrawable(
                drawable,
                context.resources.getDimensionPixelSize(
                    androidx.appcompat.R.dimen.abc_button_inset_horizontal_material
                ),
                context.resources.getDimensionPixelSize(
                    androidx.appcompat.R.dimen.abc_button_inset_vertical_material
                ),
                context.resources.getDimensionPixelSize(
                    androidx.appcompat.R.dimen.abc_button_inset_horizontal_material
                ),
                context.resources.getDimensionPixelSize(
                    androidx.appcompat.R.dimen.abc_button_inset_vertical_material
                )
            )
        }
        stationSelectorButton.text = viewModel.getFullNameForShip(station)
    }

    private fun StationEntryBinding.updateInfoLabels(entry: Station) {
        val context = root.context

        val shields = entry.obj.shieldsFront.value
        val shieldsMax = entry.obj.shieldsFrontMax.value

        if (viewModel.version < RouteObjective.ReplacementFighters.REPORT_VERSION) {
            stationFightersLabel.visibility = View.GONE
        } else {
            stationFightersLabel.visibility = View.VISIBLE
            stationFightersLabel.text = entry.getFightersText(context)
        }

        stationMissionsLabel.text = entry.getMissionsText(context)
        stationShieldLabel.text = context.getString(
            R.string.station_shield,
            shields.coerceAtLeast(0f),
            shieldsMax
        )
        stationStatusLabel.text = entry.statusString?.let(context::getString).orEmpty()
        stationSpeedLabel.text = entry.getSpeedText(context)

        stationHeadingLabel.text = context.getString(R.string.direction, entry.heading)
        stationRangeLabel.text = context.getString(R.string.range, entry.range)
    }

    private fun StationEntryBinding.bindStatusAndStandbyButtons(entry: Station) {
        requestStandbyButton.isEnabled = !entry.isStandingBy

        requestStatusButton.setOnClickListener { _ ->
            viewModel.playSound(SoundEffect.BEEP_2)
            viewModel.sendToServer(
                CommsOutgoingPacket(
                    entry.obj,
                    BaseMessage.PleaseReportStatus,
                    viewModel.vesselData
                )
            )
        }

        requestStandbyButton.setOnClickListener { _ ->
            viewModel.playSound(SoundEffect.BEEP_2)
            viewModel.sendToServer(
                CommsOutgoingPacket(
                    entry.obj,
                    BaseMessage.StandByForDockingOrCeaseOperation,
                    viewModel.vesselData
                )
            )
        }
    }

    private fun StationEntryBinding.updateOrdnanceLabels(entry: Station) {
        val allOrdnances = OrdnanceType.getAllForVersion(viewModel.version)
        val numOrdnanceTypes = allOrdnances.size

        val visibleOnRight = numOrdnanceTypes / 2
        val visibleOnLeft = numOrdnanceTypes - visibleOnRight

        val ordnanceLabels = arrayOf(
            stationOrdnanceLabel1,
            stationOrdnanceLabel2,
            stationOrdnanceLabel3,
            stationOrdnanceLabel4,
            stationOrdnanceLabel5,
            stationOrdnanceLabel6,
            stationOrdnanceLabel7,
            stationOrdnanceLabel8,
        )
        val halfway = ordnanceLabels.size / 2

        for (i in 0 until visibleOnLeft) {
            val ordnanceType = allOrdnances[i]
            val label = ordnanceLabels[i]
            label.visibility = View.VISIBLE
            label.text = entry.getOrdnanceText(viewModel, root.context, ordnanceType)
        }

        for (i in visibleOnLeft until halfway) {
            ordnanceLabels[i].visibility = View.GONE
        }

        for (i in 0 until visibleOnRight) {
            val ordnanceType = allOrdnances[i + visibleOnLeft]
            val label = ordnanceLabels[halfway + i]
            label.visibility = View.VISIBLE
            label.text = entry.getOrdnanceText(viewModel, root.context, ordnanceType)
        }

        for (i in (halfway + visibleOnRight) until ordnanceLabels.size) {
            ordnanceLabels[i].visibility = View.GONE
        }
    }

    private class StationDiffUtilCallback(
        private val oldList: List<Pair<Station, Boolean>>,
        private val newList: List<Pair<Station, Boolean>>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].first == newList[newItemPosition].first

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].second == newList[newItemPosition].second
    }

    private class StationEntryViewHolder(
        private val entryBinding: SelectorEntryBinding
    ) : RecyclerView.ViewHolder(entryBinding.root) {
        fun bind(station: Station, flashing: Boolean, viewModel: AgentViewModel) {
            val context = itemView.context
            val orientation = context.resources.configuration.orientation

            entryBinding.entryLabel.apply {
                if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                    textSize = PORTRAIT_SELECTOR_TEXT_SIZE
                }

                text = viewModel.getFullNameForShip(station.obj)
                isAllCaps = true
            }

            entryBinding.flashBackground.visibility = if (flashing) {
                val percent = station.obj.shieldsFront.value / station.obj.shieldsFrontMax.value
                val flashColor = SELECTOR_COLORS.find { percent >= it.first }.let {
                    ContextCompat.getColor(
                        context,
                        it?.second ?: R.color.stationSelectorCritical
                    )
                }
                entryBinding.flashBackground.setBackgroundColor(flashColor)

                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private inner class StationAdapter : RecyclerView.Adapter<StationEntryViewHolder>() {
        private var stations = listOf<Pair<Station, Boolean>>()

        override fun getItemCount(): Int = stations.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationEntryViewHolder =
            StationEntryViewHolder(
                SelectorEntryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: StationEntryViewHolder, position: Int) {
            stations[position].also { (station, flashing) ->
                holder.bind(station, flashing, viewModel)
                holder.itemView.setOnClickListener {
                    viewModel.playSound(SoundEffect.BEEP_2)
                    station.obj.name.value?.also { name ->
                        viewModel.stationName.value = name
                    }
                    stationSelectorPopup.dismiss()
                }
            }
        }

        fun update(newList: List<Pair<Station, Boolean>>) {
            DiffUtil.calculateDiff(
                StationDiffUtilCallback(stations, newList)
            ).dispatchUpdatesTo(this)
            stations = newList
        }
    }

    private inner class OrdnanceAdapter : RecyclerView.Adapter<GenericDataViewHolder>() {
        override fun getItemCount(): Int = ordnanceTypes.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericDataViewHolder =
            GenericDataViewHolder(parent)

        override fun onBindViewHolder(holder: GenericDataViewHolder, position: Int) {
            val station = currentStation ?: return
            val ordnance = ordnanceTypes[position]

            holder.name = ordnance.getLabelFor(viewModel.version)
            holder.itemView.setOnClickListener {
                viewModel.playSound(SoundEffect.BEEP_2)
                viewModel.sendToServer(
                    CommsOutgoingPacket(
                        station.obj,
                        BaseMessage.Build(ordnance),
                        viewModel.vesselData
                    )
                )
                ordnanceSelectorPopup.dismiss()
            }
        }

        fun onStationChanged() {
            notifyItemRangeChanged(0, ordnanceTypes.size)
        }
    }

    private companion object {
        const val PORTRAIT_SELECTOR_TEXT_SIZE = 22f
        const val ALPHA_EXPONENT = 1.25f

        val SELECTOR_COLORS = arrayOf(
            Pair(1.0f, R.color.stationSelectorFull),
            Pair(0.7f, R.color.stationSelectorDamaged),
            Pair(0.4f, R.color.stationSelectorModerate),
            Pair(0.2f, R.color.stationSelectorSevere),
            Pair(0.0f, R.color.stationSelectorCritical)
        )
    }
}
