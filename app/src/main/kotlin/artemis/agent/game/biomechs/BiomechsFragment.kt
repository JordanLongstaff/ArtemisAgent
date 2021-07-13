package artemis.agent.game.biomechs

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
import androidx.recyclerview.widget.RecyclerView
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.collectLatestWhileStarted
import artemis.agent.databinding.BiomechEntryBinding
import artemis.agent.databinding.BiomechsFragmentBinding
import artemis.agent.databinding.fragmentViewBinding

class BiomechsFragment : Fragment(R.layout.biomechs_fragment) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: BiomechsFragmentBinding by fragmentViewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val biomechsListView = binding.biomechsListView
        val context = view.context

        val adapter = BiomechListAdapter(viewModel)

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.biomechs) {
            adapter.update(it)
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.biomechRage) { rage ->
            val bgColor = ContextCompat.getColor(context, rage.color)
            biomechsListView.setBackgroundColor(bgColor)
            binding.biomechRageBackground.setBackgroundColor(bgColor)

            binding.biomechRageLabel.text = getString(
                R.string.biomech_rage,
                rage.name
            )
        }

        biomechsListView.itemAnimator = null
        biomechsListView.adapter = adapter
        biomechsListView.layoutManager = GridLayoutManager(
            context,
            COLUMNS[view.resources.configuration.orientation - Configuration.ORIENTATION_PORTRAIT]
        )
    }

    private class BiomechDiffUtilCallback(
        private val oldList: List<BiomechEntry>,
        private val newList: List<BiomechEntry>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldEntry = oldList[oldItemPosition]
            val newEntry = newList[newItemPosition]
            return newEntry.biomech == oldEntry.biomech
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldEntry = oldList[oldItemPosition]
            val newEntry = newList[newItemPosition]
            return !oldEntry.isFrozen && !newEntry.isFrozen
        }
    }

    private class BiomechViewHolder(
        val entryBinding: BiomechEntryBinding
    ) : RecyclerView.ViewHolder(entryBinding.root) {
        fun bind(entry: BiomechEntry, viewModel: AgentViewModel) {
            entryBinding.root.setOnClickListener { entry.freeze(viewModel) }
            entryBinding.biomechNameLabel.text = viewModel.getFullNameForShip(entry.biomech)
            entryBinding.biomechStatusLabel.text = entry.getFrozenStatusText(
                viewModel,
                entryBinding.root.context
            )
        }
    }

    private class BiomechListAdapter(
        private val viewModel: AgentViewModel
    ) : RecyclerView.Adapter<BiomechViewHolder>() {
        var entries = listOf<BiomechEntry>()
            private set

        override fun getItemCount(): Int = entries.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BiomechViewHolder =
            BiomechViewHolder(
                BiomechEntryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: BiomechViewHolder, position: Int) {
            holder.bind(entries[position], viewModel)
        }

        fun update(value: List<BiomechEntry>) {
            DiffUtil.calculateDiff(
                BiomechDiffUtilCallback(entries, value)
            ).dispatchUpdatesTo(this)
            entries = value
        }
    }

    private companion object {
        val COLUMNS = arrayOf(
            1, // portrait
            3, // landscape
        )
    }
}
