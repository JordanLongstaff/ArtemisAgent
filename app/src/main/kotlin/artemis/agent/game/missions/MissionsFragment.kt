package artemis.agent.game.missions

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import artemis.agent.AgentViewModel
import artemis.agent.AgentViewModel.Companion.formatString
import artemis.agent.R
import artemis.agent.databinding.CompletedMissionsEntryBinding
import artemis.agent.databinding.MissionsEntryBinding
import artemis.agent.databinding.MissionsFragmentBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.game.ObjectEntry
import artemis.agent.util.SoundEffect
import artemis.agent.util.collectLatestWhileStarted
import kotlin.math.sign
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class MissionsFragment : Fragment(R.layout.missions_fragment) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: MissionsFragmentBinding by fragmentViewBinding()

    private val completedAdapter: PayoutListAdapter by lazy { PayoutListAdapter() }

    private val labels: Array<String> by lazy {
        binding.root.resources.getStringArray(R.array.reward_type_entries)
    }

    private val listeningForPayouts: Flow<Boolean?> by lazy {
        viewModel.jumping.combine(viewModel.missionManager.showingPayouts) { jump, show ->
            if (jump) null else show
        }
    }

    private val updatedMissions: Flow<List<SideMissionEntry>?> by lazy {
        viewModel.missionManager.missions.combine(listeningForPayouts) { list, listen ->
            if (listen == false) list else null
        }
    }

    private val updatedPayouts: Flow<List<Pair<RewardType, Int>>?> by lazy {
        viewModel.missionManager.displayedPayouts.combine(listeningForPayouts) { list, listen ->
            if (listen == true) list else null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val missionsListView = binding.missionsListView
        missionsListView.itemAnimator = null

        if (view.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            missionsListView.layoutManager = LinearLayoutManager(view.context)
        }

        setupActiveMissions()
        setupCompletedMissions()

        if (viewModel.missionManager.showingPayouts.value) {
                binding.completedMissionsButton
            } else {
                binding.activeMissionsButton
            }
            .isChecked = true
    }

    private fun setupActiveMissions() {
        val missionsListView = binding.missionsListView
        val activeMissionsButton = binding.activeMissionsButton

        activeMissionsButton.setOnClickListener {
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        val missionListAdapter = MissionListAdapter()
        activeMissionsButton.setOnCheckedChangeListener { v, isChecked ->
            if (isChecked) {
                if (
                    v.context.resources.configuration.orientation ==
                        Configuration.ORIENTATION_LANDSCAPE
                ) {
                    missionsListView.layoutManager =
                        LinearLayoutManager(
                            missionsListView.context,
                            RecyclerView.HORIZONTAL,
                            false,
                        )
                }

                missionsListView.adapter = missionListAdapter
                viewModel.missionManager.showingPayouts.value = false
            }
        }

        viewLifecycleOwner.collectLatestWhileStarted(updatedMissions) {
            it?.also(missionListAdapter::onMissionsUpdate)
        }
    }

    private fun setupCompletedMissions() {
        val missionsListView = binding.missionsListView
        val completedMissionsButton = binding.completedMissionsButton

        completedMissionsButton.setOnClickListener {
            viewModel.activateHaptic()
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        completedMissionsButton.setOnCheckedChangeListener { v, isChecked ->
            if (isChecked) {
                if (
                    v.context.resources.configuration.orientation ==
                        Configuration.ORIENTATION_LANDSCAPE
                ) {
                    missionsListView.layoutManager =
                        GridLayoutManager(
                            missionsListView.context,
                            viewModel.missionManager.displayedRewards.size,
                        )
                }

                missionsListView.adapter = completedAdapter
                viewModel.missionManager.showingPayouts.value = true
            }
        }

        viewLifecycleOwner.collectLatestWhileStarted(updatedPayouts) {
            it?.also(completedAdapter::onPayoutsUpdate)
        }
    }

    private class MissionsDiffUtilCallback(
        private val oldList: List<SideMissionEntry>,
        private val newList: List<SideMissionEntry>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = false
    }

    private class PayoutsDiffUtilCallback(
        private val oldList: List<Pair<RewardType, Int>>,
        private val newList: List<Pair<RewardType, Int>>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].first == newList[newItemPosition].first

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].second == newList[newItemPosition].second
    }

    private inner class MissionViewHolder(private val entryBinding: MissionsEntryBinding) :
        RecyclerView.ViewHolder(entryBinding.root) {
        fun bind(entry: SideMissionEntry) {
            entryBinding.bindStatus(entry)

            val rewardList =
                viewModel.missionManager.displayedRewards
                    .filter { entry.rewards[it.ordinal] > 0 }
                    .joinToString { reward ->
                        val value =
                            entry.rewards[reward.ordinal] * if (reward == RewardType.NUKE) 2 else 1
                        val name =
                            if (reward == RewardType.PRODUCTION) {
                                entry.destination.obj.name.value
                            } else {
                                null
                            }
                        val prefix = name?.let { n -> "$n " } ?: ""
                        val suffix = if (value > 1) " x$value" else ""
                        "$prefix${labels[reward.ordinal]}$suffix"
                    }
            entryBinding.rewardsLabel.text = getString(R.string.rewards, rewardList)
        }

        private fun MissionsEntryBinding.bindStatus(entry: SideMissionEntry) {
            if (entry.isCompleted) bindCompleted(entry) else bindInProgress(entry)
        }

        private fun MissionsEntryBinding.bindInProgress(entry: SideMissionEntry) {
            val nextTo: ObjectEntry<*>
            val thenTo: ObjectEntry<*>?
            if (entry.isStarted) {
                nextTo = entry.destination
                thenTo = null
                thenLabel.visibility = View.GONE
            } else {
                nextTo = entry.source
                thenTo = entry.destination
                thenLabel.visibility = View.VISIBLE
            }

            root.setBackgroundColor(
                ContextCompat.getColor(
                    root.context,
                    maxOf(
                            nextTo.missionStatus,
                            thenTo?.missionStatus ?: SideMissionStatus.ALL_CLEAR,
                        )
                        .backgroundColor,
                )
            )

            val nextVessel = nextTo.obj.getVessel(viewModel.vesselData)
            nextLabel.text =
                getString(
                    R.string.next_to,
                    nextTo.obj.name.value,
                    nextVessel?.getFaction(viewModel.vesselData)?.name,
                    nextVessel?.name,
                )
            val thenVessel = thenTo?.obj?.getVessel(viewModel.vesselData)
            thenLabel.text =
                getString(
                    R.string.then_to,
                    thenTo?.run { obj.name.value },
                    thenVessel?.getFaction(viewModel.vesselData)?.name,
                    thenVessel?.name,
                )

            missionDirectionLabel.text = getString(R.string.direction, nextTo.heading)
            missionRangeLabel.text = getString(R.string.range, nextTo.range)
            missionTimeLabel.text = entry.durationText
        }

        private fun MissionsEntryBinding.bindCompleted(entry: SideMissionEntry) {
            root.setOnClickListener {
                viewModel.activateHaptic()
                viewModel.missionManager.allMissions.remove(entry)
            }

            root.setBackgroundColor(
                ContextCompat.getColor(root.context, R.color.completedMissionGreen)
            )
            nextLabel.text = getString(R.string.mission_completed)
            thenLabel.text =
                if (viewModel.autoDismissCompletedMissions) {
                    val timeToDismiss = System.currentTimeMillis() - entry.completionTimestamp
                    val seconds =
                        timeToDismiss.milliseconds.toComponents { sec, nano -> sec + nano.sign }
                    getString(R.string.mission_will_be_removed, seconds)
                } else {
                    getString(R.string.tap_to_dismiss)
                }
            thenLabel.visibility = View.VISIBLE
            thenLabel.alpha = 1f
            missionDirectionLabel.text = ""
            missionRangeLabel.text = ""
            missionTimeLabel.text = ""
        }
    }

    private class CompletedViewHolder(private val entryBinding: CompletedMissionsEntryBinding) :
        RecyclerView.ViewHolder(entryBinding.root) {
        fun bind(label: String, count: Int) {
            entryBinding.rewardTypeLabel.text = label
            entryBinding.rewardQuantityLabel.text = count.formatString()
        }
    }

    private inner class MissionListAdapter : RecyclerView.Adapter<MissionViewHolder>() {
        private var missions = listOf<SideMissionEntry>()

        override fun getItemCount(): Int = missions.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MissionViewHolder =
            MissionViewHolder(
                MissionsEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )

        override fun onBindViewHolder(holder: MissionViewHolder, position: Int) {
            holder.bind(missions[position])
        }

        fun onMissionsUpdate(list: List<SideMissionEntry>) {
            DiffUtil.calculateDiff(MissionsDiffUtilCallback(missions, list)).dispatchUpdatesTo(this)
            missions = list
        }
    }

    private inner class PayoutListAdapter : RecyclerView.Adapter<CompletedViewHolder>() {
        var payouts = listOf<Pair<RewardType, Int>>()
            private set

        override fun getItemCount(): Int = payouts.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompletedViewHolder =
            CompletedViewHolder(
                CompletedMissionsEntryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false,
                )
            )

        override fun onBindViewHolder(holder: CompletedViewHolder, position: Int) {
            val entry = payouts[position]
            holder.bind(labels[entry.first.ordinal], entry.second)
        }

        fun onPayoutsUpdate(list: List<Pair<RewardType, Int>>) {
            if (payouts == list) return

            DiffUtil.calculateDiff(PayoutsDiffUtilCallback(payouts, list)).dispatchUpdatesTo(this)
            payouts = list
        }
    }
}
