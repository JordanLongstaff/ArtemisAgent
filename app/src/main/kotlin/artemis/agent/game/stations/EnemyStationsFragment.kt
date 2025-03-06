package artemis.agent.game.stations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.databinding.EnemyStationEntryBinding
import artemis.agent.databinding.EnemyStationsFragmentBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.game.ObjectEntry.Station
import artemis.agent.util.SoundEffect
import artemis.agent.util.collectLatestWhileStarted
import com.walkertribe.ian.enums.BaseMessage
import com.walkertribe.ian.protocol.core.comm.CommsOutgoingPacket

class EnemyStationsFragment : Fragment(R.layout.enemy_stations_fragment) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: EnemyStationsFragmentBinding by fragmentViewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val enemyStationAdapter = EnemyStationListAdapter(viewModel)
        val enemyListView = binding.enemyListView
        enemyListView.itemAnimator = null
        enemyListView.adapter = enemyStationAdapter
        enemyListView.layoutManager =
            GridLayoutManager(view.context, view.resources.configuration.orientation)

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.enemyStations) {
            enemyStationAdapter.onStationsUpdate(it)
            binding.noEnemyStationsLabel.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private class EnemyStationDiffUtilCallback(
        private val oldList: List<Station>,
        private val newList: List<Station>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldEntry = oldList[oldItemPosition]
            val newEntry = newList[newItemPosition]
            return newEntry.obj == oldEntry.obj
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = false
    }

    private class EnemyStationViewHolder(private val entryBinding: EnemyStationEntryBinding) :
        RecyclerView.ViewHolder(entryBinding.root) {
        fun bind(entry: Station, viewModel: AgentViewModel) {
            val root = entryBinding.root
            val station = entry.obj
            val context = root.context

            entryBinding.enemyNameLabel.text = entry.obj.name.value
            root.setOnClickListener {
                viewModel.apply {
                    playSound(SoundEffect.BEEP_2)
                    sendToServer(
                        CommsOutgoingPacket(
                            entry.obj,
                            BaseMessage.StandByForDockingOrCeaseOperation,
                            this@apply.vesselData,
                        )
                    )
                }
            }

            entryBinding.enemyShieldLabel.text =
                context.getString(
                    R.string.station_shield,
                    station.shieldsFront.strength.value.coerceAtLeast(0f),
                    station.shieldsFront.maxStrength.value,
                )
            entryBinding.enemyHeadingLabel.text =
                context.getString(R.string.direction, entry.heading)
            entryBinding.enemyRangeLabel.text = context.getString(R.string.range, entry.range)
        }
    }

    private class EnemyStationListAdapter(private val viewModel: AgentViewModel) :
        RecyclerView.Adapter<EnemyStationViewHolder>() {
        var entries = listOf<Station>()
            private set

        override fun getItemCount(): Int = entries.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnemyStationViewHolder =
            EnemyStationViewHolder(
                EnemyStationEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )

        override fun onBindViewHolder(holder: EnemyStationViewHolder, position: Int) {
            holder.bind(entries[position], viewModel)
        }

        fun onStationsUpdate(list: List<Station>) {
            DiffUtil.calculateDiff(EnemyStationDiffUtilCallback(entries, list))
                .dispatchUpdatesTo(this)
            entries = list
        }
    }
}
