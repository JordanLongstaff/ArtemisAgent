package artemis.agent.game.status

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.databinding.StatusFragmentBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.game.route.RouteObjective
import artemis.agent.util.collectLatestWhileStarted
import com.walkertribe.ian.enums.DriveType
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.enums.ShipSystem
import com.walkertribe.ian.util.Util.splitSpaceDelimited
import com.walkertribe.ian.world.ArtemisPlayer
import kotlin.math.sign

class StatusFragment : Fragment(R.layout.status_fragment) {
    private val viewModel: AgentViewModel by activityViewModels()

    private val binding: StatusFragmentBinding by fragmentViewBinding()

    private val adapter: ReportAdapter by lazy { ReportAdapter() }

    private val fighterStockStrings: IntArray by lazy {
        intArrayOf(
            R.string.single_seat_craft_docked,
            R.string.single_seat_craft_launched,
            R.string.single_seat_craft_lost,
        )
    }

    private val nodeNames: Array<String> by lazy {
        binding.root.resources.getStringArray(R.array.node_names)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.statusReportView.itemAnimator = null
        binding.statusReportView.layoutManager = ReportGridLayoutManager(view.context)
        binding.statusReportView.adapter = adapter

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.playerUpdate) {
            updateStatusReport()
        }
    }

    private fun updateStatusReport() {
        adapter.onInfoUpdate(getStatusReport())
    }

    private fun getStatusReport(): List<StatusInfo> = buildList {
        val player = viewModel.playerShip ?: return@buildList

        addAll(getEnergyAndShields(player))
        addAll(getOrdnanceAndFighters(player))
        addAll(getDamageReport(player))
    }

    private fun getEnergyAndShields(player: ArtemisPlayer): List<StatusInfo> =
        listOf(
            StatusInfo.Energy(player.energy.value),
            StatusInfo.Header(
                if (player.shieldsActive.value.booleanValue) R.string.shields_active
                else R.string.shields_inactive
            ),
            StatusInfo.Shield(ShieldPosition.FRONT, player.shieldsFront),
            StatusInfo.Shield(ShieldPosition.REAR, player.shieldsRear),
        )

    private fun getOrdnanceAndFighters(player: ArtemisPlayer): List<StatusInfo> = buildList {
        val vessel = player.getVessel(viewModel.vesselData) ?: return@buildList

        add(StatusInfo.Empty)
        OrdnanceType.getAllForVersion(viewModel.version).forEach { ordnance ->
            val max = vessel.ordnanceStorage[ordnance] ?: 0
            val count = player.getTotalOrdnanceCount(ordnance)
            add(StatusInfo.OrdnanceCount(ordnance, viewModel.version, count, max))
        }

        val maxFighters =
            viewModel.version
                .compareTo(RouteObjective.ReplacementFighters.SHUTTLE_VERSION)
                .sign
                .coerceAtMost(0) + 1 + vessel.bayCount
        if (maxFighters == 0) return@buildList
        add(StatusInfo.Empty)

        val launchedFighters = viewModel.fighterIDs.size
        val lostFighters = maxFighters - viewModel.totalFighters.value
        val dockedFighters = maxFighters - lostFighters - launchedFighters

        addAll(
            intArrayOf(dockedFighters, launchedFighters, lostFighters)
                .zip(fighterStockStrings)
                .filter { it.first > 0 }
                .map { (count, fighterLabel) -> StatusInfo.Singleseat(fighterLabel, count) }
        )
    }

    private fun getDamageReport(player: ArtemisPlayer): List<StatusInfo> = buildList {
        val grid = viewModel.vesselData.getGrid(player.hullId.value) ?: return@buildList
        val drive = player.driveType.value ?: DriveType.WARP

        val damageMap =
            ShipSystem.entries
                .mapNotNull { system ->
                    val nodes = grid.getNodesBySystem(system)
                    val damageCount = nodes.count { it.damage > 0f }
                    if (damageCount == 0) return@mapNotNull null

                    val systemLabel =
                        nodeNames[system.ordinal].let { name ->
                            if (system == ShipSystem.WARP_JUMP_DRIVE) {
                                val tokens = name.splitSpaceDelimited()
                                "${tokens[drive.ordinal]} ${tokens.last()}"
                            } else {
                                name
                            }
                        }

                    system to
                        StatusInfo.DamageReport(
                            systemLabel,
                            nodes.size,
                            damageCount,
                            grid.getDamageBySystem(system),
                        )
                }
                .toMap()
        if (damageMap.isEmpty()) return@buildList

        add(StatusInfo.Empty)
        add(StatusInfo.Header(R.string.node_damage))
        ShipSystem.entries.drop(1).forEach { system ->
            damageMap[system]?.also { report -> add(report) }
        }
        damageMap[ShipSystem.HALLWAY]?.also { report -> add(report) }
    }

    private class ReportGridLayoutManager(context: Context) :
        GridLayoutManager(context, 1, 2 - context.resources.configuration.orientation, false) {
        private val heightDimension: Int =
            (context.resources.getDimension(R.dimen.baseTextSize) +
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        TOP_BOTTOM_PADDING * 2f,
                        context.resources.displayMetrics,
                    ))
                .toInt()

        override fun onLayoutChildren(
            recycler: RecyclerView.Recycler?,
            state: RecyclerView.State?,
        ) {
            spanCount =
                if (orientation == VERTICAL) {
                    1
                } else {
                    height / heightDimension
                }
            super.onLayoutChildren(recycler, state)
        }
    }

    private class ReportInfoDiffUtilCallback(
        private val oldList: List<StatusInfo>,
        private val newList: List<StatusInfo>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldLine = oldList[oldItemPosition]
            val newLine = newList[newItemPosition]
            return oldLine.itemEquals(newLine)
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldLine = oldList[oldItemPosition]
            val newLine = newList[newItemPosition]
            return oldLine == newLine
        }
    }

    private class ReportLineViewHolder(context: Context) :
        RecyclerView.ViewHolder(
            TextView(context).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )

                setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    context.resources.getDimension(R.dimen.baseTextSize),
                )

                minLines = 1

                setPaddingRelative(
                    0,
                    TOP_BOTTOM_PADDING,
                    context.resources.getDimensionPixelSize(R.dimen.baseTextSize),
                    TOP_BOTTOM_PADDING,
                )
            }
        ) {
        var info: StatusInfo? = null
            set(value) {
                field = value
                (itemView as TextView).text = value?.getString(itemView.context)
            }
    }

    private class ReportAdapter : RecyclerView.Adapter<ReportLineViewHolder>() {
        var infoList = listOf<StatusInfo>()
            private set

        override fun getItemCount(): Int = infoList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportLineViewHolder =
            ReportLineViewHolder(parent.context)

        override fun onBindViewHolder(holder: ReportLineViewHolder, position: Int) {
            holder.info = infoList[position]
        }

        fun onInfoUpdate(newList: List<StatusInfo>) {
            DiffUtil.calculateDiff(ReportInfoDiffUtilCallback(infoList, newList))
                .dispatchUpdatesTo(this)
            infoList = newList
        }
    }

    private companion object {
        const val TOP_BOTTOM_PADDING = 2
    }
}
