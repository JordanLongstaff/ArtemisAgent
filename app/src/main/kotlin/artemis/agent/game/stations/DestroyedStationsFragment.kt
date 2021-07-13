package artemis.agent.game.stations

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.collectLatestWhileStarted
import artemis.agent.databinding.SimpleListViewBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.generic.GenericDataAdapter
import artemis.agent.generic.GenericDataEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class DestroyedStationsFragment : Fragment(R.layout.simple_list_view) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: SimpleListViewBinding by fragmentViewBinding()

    private val stations: Flow<List<String>?> by lazy {
        viewModel.destroyedStations.combine(viewModel.jumping) { list, jump ->
            if (jump) null else list
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = GenericDataAdapter()
        binding.recyclerListView.adapter = adapter

        viewLifecycleOwner.collectLatestWhileStarted(stations) {
            if (it != null) {
                adapter.onListUpdate(it.map { name -> GenericDataEntry(name) })
            }
        }
    }
}
