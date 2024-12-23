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
import artemis.agent.SoundEffect
import artemis.agent.collectLatestWhileStarted
import artemis.agent.databinding.CompletedMissionsEntryBinding
import artemis.agent.databinding.MissionsEntryBinding
import artemis.agent.databinding.MissionsFragmentBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.game.ObjectEntry
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
        viewModel.jumping.combine(viewModel.showingPayouts) { jump, show ->
            if (jump) null else show
        }
    }

    private val updatedMissions: Flow<List<SideMissionEntry>?> by lazy {
        viewModel.missions.combine(listeningForPayouts) { list, listen ->
            if (listen == false) list else null
        }
    }

    private val updatedPayouts: Flow<List<Pair<RewardType, Int>>?> by lazy {
        viewModel.displayedPayouts.combine(listeningForPayouts) { list, listen ->
            if (listen == true) list else null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val missionsListView = binding.missionsListView
        val activeMissionsButton = binding.activeMissionsButton
        val completedMissionsButton = binding.completedMissionsButton

        missionsListView.itemAnimator = null

        val orientation = view.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            missionsListView.layoutManager = LinearLayoutManager(view.context)
        }

        activeMissionsButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        completedMissionsButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_2)
        }

        val missionListAdapter = MissionListAdapter()
        activeMissionsButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    missionsListView.layoutManager = LinearLayoutManager(
                        missionsListView.context,
                        RecyclerView.HORIZONTAL,
                        false
                    )
                }

                missionsListView.adapter = missionListAdapter
                viewModel.showingPayouts.value = false
            }
        }

        viewLifecycleOwner.collectLatestWhileStarted(updatedMissions) {
            it?.also(missionListAdapter::onMissionsUpdate)
        }

        completedMissionsButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    missionsListView.layoutManager = GridLayoutManager(
                        missionsListView.context,
                        viewModel.displayedRewards.size
                    )
                }

                missionsListView.adapter = completedAdapter
                viewModel.showingPayouts.value = true
            }
        }

        viewLifecycleOwner.collectLatestWhileStarted(updatedPayouts) {
            it?.also(completedAdapter::onPayoutsUpdate)
        }

        if (viewModel.showingPayouts.value) {
            completedMissionsButton
        } else {
            activeMissionsButton
        }.isChecked = true
    }

    private class MissionsDiffUtilCallback(
        private val oldList: List<SideMissionEntry>,
        private val newList: List<SideMissionEntry>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = false
    }

    private class PayoutsDiffUtilCallback(
        private val oldList: List<Pair<RewardType, Int>>,
        private val newList: List<Pair<RewardType, Int>>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].first == newList[newItemPosition].first

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].second == newList[newItemPosition].second
    }

    private inner class MissionViewHolder(
        private val entryBinding: MissionsEntryBinding
    ) : RecyclerView.ViewHolder(entryBinding.root) {
        fun bind(entry: SideMissionEntry) {
            with(entryBinding) {
                if (entry.isCompleted) {
                    bindCompleted(entry)
                } else {
                    bindInProgress(entry)
                }

                val rewardList = viewModel.displayedRewards.filter {
                    entry.rewards[it.ordinal] > 0
                }.joinToString { reward ->
                    val value = entry.rewards[reward.ordinal] *
                        if (reward == RewardType.NUKE) 2 else 1
                    val name = if (reward == RewardType.PRODUCTION) {
                        entry.destination.obj.name.value
                    } else {
                        null
                    }
                    val prefix = name?.let { n -> "$n " } ?: ""
                    val suffix = if (value > 1) " x$value" else ""
                    "$prefix${labels[reward.ordinal]}$suffix"
                }
                rewardsLabel.text = getString(R.string.rewards, rewardList)
            }
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
                    SideMissionStatus.maxOf(
                        nextTo.missionStatus,
                        thenTo?.missionStatus ?: SideMissionStatus.ALL_CLEAR
                    ).backgroundColor
                )
            )

            val nextVessel = nextTo.obj.getVessel(viewModel.vesselData)
            nextLabel.text = getString(
                R.string.next_to,
                nextTo.obj.name.value,
                nextVessel?.getFaction(viewModel.vesselData)?.name,
                nextVessel?.name
            )
            val thenVessel = thenTo?.obj?.getVessel(viewModel.vesselData)
            thenLabel.text = getString(
                R.string.then_to,
                thenTo?.run { obj.name.value },
                thenVessel?.getFaction(viewModel.vesselData)?.name,
                thenVessel?.name
            )

            missionDirectionLabel.text = getString(
                R.string.direction,
                nextTo.heading
            )
            missionRangeLabel.text = getString(
                R.string.range,
                nextTo.range
            )
            missionTimeLabel.text = entry.durationText
        }

        private fun MissionsEntryBinding.bindCompleted(entry: SideMissionEntry) {
            val seconds = AgentViewModel.getTimeToEnd(entry.completionTimestamp).second

            root.setOnClickListener { viewModel.allMissions.remove(entry) }

            root.setBackgroundColor(
                ContextCompat.getColor(root.context, R.color.completedMissionGreen)
            )
            nextLabel.text = getString(R.string.mission_completed)
            thenLabel.text = if (viewModel.autoDismissCompletedMissions) {
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

    private class CompletedViewHolder(
        private val entryBinding: CompletedMissionsEntryBinding
    ) : RecyclerView.ViewHolder(entryBinding.root) {
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
                MissionsEntryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: MissionViewHolder, position: Int) {
            holder.bind(missions[position])
        }

        fun onMissionsUpdate(list: List<SideMissionEntry>) {
            DiffUtil.calculateDiff(
                MissionsDiffUtilCallback(missions, list)
            ).dispatchUpdatesTo(this)
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
                    false
                )
            )

        override fun onBindViewHolder(holder: CompletedViewHolder, position: Int) {
            val entry = payouts[position]
            holder.bind(labels[entry.first.ordinal], entry.second)
        }

        fun onPayoutsUpdate(list: List<Pair<RewardType, Int>>) {
            if (payouts == list) return

            DiffUtil.calculateDiff(
                PayoutsDiffUtilCallback(payouts, list)
            ).dispatchUpdatesTo(this)
            payouts = list
        }
    }
}
