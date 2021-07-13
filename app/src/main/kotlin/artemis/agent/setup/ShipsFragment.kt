package artemis.agent.setup

import android.graphics.Color
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
import artemis.agent.collectLatestWhileStarted
import artemis.agent.databinding.ShipEntryBinding
import artemis.agent.databinding.ShipsFragmentBinding
import artemis.agent.databinding.fragmentViewBinding
import com.walkertribe.ian.protocol.core.setup.Ship

class ShipsFragment : Fragment(R.layout.ships_fragment) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: ShipsFragmentBinding by fragmentViewBinding()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.settingsPage.value = null

        val shipAdapter = ShipAdapter(viewModel)
        val shipsList = binding.shipsList
        shipsList.itemAnimator = null
        shipsList.adapter = shipAdapter
        shipsList.layoutManager = GridLayoutManager(
            view.context,
            view.resources.configuration.orientation
        )

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.selectableShips) {
            shipAdapter.update(it)
            binding.noShipsLabel.visibility =
                if (it.isEmpty()) View.VISIBLE else View.GONE
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.shipIndex) {
            shipAdapter.update(it)
        }
    }

    private class ShipDiffUtilCallback(
        private val oldList: List<Ship>,
        private val newList: List<Ship>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItemPosition == newItemPosition
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldShip = oldList[oldItemPosition]
            val newShip = newList[newItemPosition]
            return oldShip == newShip
        }
    }

    private class ShipViewHolder(val entryBinding: ShipEntryBinding) :
        RecyclerView.ViewHolder(entryBinding.root)

    private class ShipAdapter(
        private val viewModel: AgentViewModel
    ) : RecyclerView.Adapter<ShipViewHolder>() {
        private var shipsList: List<Ship> = listOf()
        private var selectedShipIndex: Int = -1

        override fun getItemCount(): Int = shipsList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShipViewHolder =
            ShipViewHolder(
                ShipEntryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: ShipViewHolder, position: Int) {
            val ship = shipsList[position]
            val entryBinding = holder.entryBinding

            entryBinding.driveTypeLabel.text = ship.drive.name.substring(0, 1)
            entryBinding.nameLabel.text = ship.name
                ?: holder.itemView.context.getString(R.string.ship_number, position + 1)

            val hue = ship.hue
            val root = entryBinding.root
            root.setBackgroundColor(
                if (hue.isNaN()) {
                    Color.TRANSPARENT
                } else {
                    Color.HSVToColor(floatArrayOf(hue, 1f, VALUE))
                }
            )

            entryBinding.vesselLabel.text = viewModel.getFullNameForVessel(ship)
            entryBinding.descriptionLabel.text =
                ship.getVessel(viewModel.vesselData)?.description ?: ""

            root.setOnClickListener { viewModel.selectShip(position) }

            val selectedShipLabel = entryBinding.selectedShipLabel
            if (selectedShipIndex == position) {
                selectedShipLabel.setText(R.string.selected)
            } else {
                selectedShipLabel.text = ""
            }
        }

        fun update(value: Int) {
            val oldValue = selectedShipIndex
            selectedShipIndex = value
            if (oldValue >= 0) notifyItemChanged(oldValue)
            if (value >= 0) notifyItemChanged(value)
        }

        fun update(newList: List<Ship>) {
            DiffUtil.calculateDiff(
                ShipDiffUtilCallback(shipsList, newList)
            ).dispatchUpdatesTo(this)
            shipsList = newList
        }
    }

    private companion object {
        const val VALUE = 0.5f
    }
}
