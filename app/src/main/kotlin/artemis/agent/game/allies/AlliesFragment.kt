package artemis.agent.game.allies

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.SoundEffect
import artemis.agent.collectLatestWhileStarted
import artemis.agent.databinding.AlliesEntryBinding
import artemis.agent.databinding.AlliesFragmentBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.game.ObjectEntry.Ally
import artemis.agent.generic.GenericDataAdapter
import artemis.agent.generic.GenericDataEntry
import com.walkertribe.ian.enums.GoDefend
import com.walkertribe.ian.enums.OtherMessage
import com.walkertribe.ian.protocol.core.comm.CommsOutgoingPacket
import com.walkertribe.ian.util.Util.joinSpaceDelimited
import com.walkertribe.ian.world.ArtemisNpc
import com.walkertribe.ian.world.ArtemisObject
import com.walkertribe.ian.world.ArtemisShielded
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class AlliesFragment : Fragment(R.layout.allies_fragment) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: AlliesFragmentBinding by fragmentViewBinding()

    private val commandInfoBinder: AllyInfoBinder by lazy {
        AllyInfoBinder(binding.allyInfoLayout)
    }

    private val aliveAdapter: LivingAlliesListAdapter by lazy {
        LivingAlliesListAdapter()
    }
    private val destroyedAdapter: GenericDataAdapter by lazy {
        GenericDataAdapter()
    }

    private var destinationAdapter: DestinationAdapter? = null
        set(adapter) {
            field = adapter
            binding.allyDefendList.adapter = adapter
        }

    private val listeningForDestroyedAllies: Flow<Boolean> by lazy {
        viewModel.jumping.combine(viewModel.showingDestroyedAllies) { jump, show ->
            !jump && show
        }
    }

    private val destroyedAllies: Flow<List<String>?> by lazy {
        viewModel.destroyedAllies.combine(listeningForDestroyedAllies) { list, listen ->
            if (listen) list else null
        }
    }

    private val allyAndDefendableTargets: Flow<Pair<Ally?, List<ArtemisShielded<*>>>> by lazy {
        viewModel.focusedAlly.combine(viewModel.defendableTargets, ::Pair)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            prepareAlliesListViewAndSelector()
            prepareDefendableTargetsListView()

            arrayOf(
                allyTurnTo0,
                allyTurnTo90,
                allyTurnTo180,
                allyTurnTo270,
            ).forEachIndexed { i, button ->
                button.text = viewModel.formattedHeading(i * NINETY)
            }
        }
    }

    private fun AlliesFragmentBinding.prepareAlliesListViewAndSelector() {
        if (!viewModel.showAllySelector) {
            alliesListSelector.visibility = View.GONE
        }

        alliesListView.itemAnimator = null
        alliesListView.layoutManager = LinearLayoutManager(
            root.context,
            Configuration.ORIENTATION_LANDSCAPE - root.resources.configuration.orientation,
            false
        )

        aliveAlliesButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                alliesListView.adapter = aliveAdapter
                viewModel.showingDestroyedAllies.value = false
            }
        }

        destroyedAlliesButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                alliesListView.adapter = destroyedAdapter
                viewModel.showingDestroyedAllies.value = true
            }
        }

        if (viewModel.showingDestroyedAllies.value) {
            destroyedAlliesButton
        } else {
            aliveAlliesButton
        }.isChecked = true

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.livingAllies) {
            if (!viewModel.showingDestroyedAllies.value) {
                aliveAdapter.onAlliesUpdate(it)

                viewModel.scrollToAlly?.also { ally ->
                    alliesListView.scrollToPosition(
                        it.indexOf(ally).coerceAtLeast(0)
                    )
                    viewModel.scrollToAlly = null
                }
            }
        }

        viewLifecycleOwner.collectLatestWhileStarted(destroyedAllies) {
            if (it != null) {
                destroyedAdapter.onListUpdate(it.map { name -> GenericDataEntry(name) })
            }
        }
    }

    private fun AlliesFragmentBinding.prepareDefendableTargetsListView() {
        allyDefendList.itemAnimator = null
        allyDefendList.layoutManager =
            DestinationGridLayoutManager(allyDefendList.context)

        viewLifecycleOwner.collectLatestWhileStarted(
            allyAndDefendableTargets
        ) { (ally, targets) ->
            if (ally == null) {
                showAlliesListView()
            } else {
                showSelectedAllyCommands(ally, targets)
            }
        }
    }

    private fun AlliesFragmentBinding.showAlliesListView() {
        root.setBackgroundColor(Color.TRANSPARENT)
        root.children.forEach { child ->
            child.visibility = View.GONE
        }
        alliesListSelector.visibility =
            if (viewModel.showAllySelector) View.VISIBLE else View.GONE
        alliesListView.visibility = View.VISIBLE
        destinationAdapter = null
    }

    private fun AlliesFragmentBinding.showSelectedAllyCommands(
        ally: Ally,
        targets: List<ArtemisShielded<*>>,
    ) {
        commandInfoBinder.bind(ally, viewModel)
        root.setBackgroundColor(ally.getBackgroundColor(root.context))

        var targetsAdapter = destinationAdapter
        if (targetsAdapter == null) {
            arrayOf(
                allyTurnTo0 to OtherMessage.TurnToHeading0,
                allyTurnTo90 to OtherMessage.TurnToHeading90,
                allyTurnTo180 to OtherMessage.TurnToHeading180,
                allyTurnTo270 to OtherMessage.TurnToHeading270,
                allyTurnLeft10 to OtherMessage.TurnLeft10Degrees,
                allyTurnRight10 to OtherMessage.TurnRight10Degrees,
                allyTurnLeft25 to OtherMessage.TurnLeft25Degrees,
                allyTurnRight25 to OtherMessage.TurnRight25Degrees,
                allyAttackButton to OtherMessage.AttackNearestEnemy,
                allyProceedButton to OtherMessage.ProceedToYourDestination,
            ).forEach { (button, message) ->
                button.setOnClickListener {
                    viewModel.playSound(SoundEffect.CONFIRMATION)
                    viewModel.sendToServer(
                        CommsOutgoingPacket(
                            ally.obj,
                            message,
                            viewModel.vesselData
                        )
                    )
                    if (
                        !viewModel.manuallyReturnFromCommands &&
                        !viewModel.isSingleAlly
                    ) {
                        viewModel.focusedAlly.value = null
                    }
                }
            }

            targetsAdapter = DestinationAdapter(
                ally.obj,
                viewModel
            )
            destinationAdapter = targetsAdapter
        }

        targetsAdapter.onTargetsUpdate(targets)

        root.children.forEach { child ->
            child.visibility = View.VISIBLE
        }

        if (ally.obj.getVessel(viewModel.vesselData)?.get(WARSHIP) != true) {
            allyAttackButton.visibility = View.GONE
            allyGoDefendLabel.setText(R.string.rendezvous_with)
        } else {
            allyGoDefendLabel.setText(R.string.go_defend)
        }

        alliesListSelector.visibility = View.GONE
        alliesListView.visibility = View.GONE
    }

    private class AllyInfoBinder(val entryBinding: AlliesEntryBinding) {
        constructor(parent: ViewGroup) : this(
            AlliesEntryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        fun bind(entry: Ally, viewModel: AgentViewModel) {
            entryBinding.allyNameLabel.text = viewModel.getFullNameForShip(entry.obj)
            entryBinding.allyHailButton.setOnClickListener {
                viewModel.playSound(SoundEffect.BEEP_2)
                viewModel.sendToServer(
                    CommsOutgoingPacket(
                        entry.obj,
                        OtherMessage.Hail,
                        viewModel.vesselData
                    )
                )
            }

            bindAllyCommandButton(entry, viewModel)
            bindDescriptionLabel(entry, viewModel)
            bindInfoLabels(entry)
        }

        private fun bindAllyCommandButton(entry: Ally, viewModel: AgentViewModel) {
            val root = entryBinding.root
            val allyCommandButton = entryBinding.allyCommandButton

            if (viewModel.focusedAlly.value != null || viewModel.isSingleAlly) {
                root.setBackgroundColor(Color.TRANSPARENT)
                allyCommandButton.setText(R.string.cancel)
                allyCommandButton.setOnClickListener {
                    viewModel.playSound(SoundEffect.BEEP_1)
                    if (!viewModel.isSingleAlly) {
                        viewModel.focusedAlly.value = null
                    }
                }
            } else {
                root.setBackgroundColor(entry.getBackgroundColor(root.context))
                allyCommandButton.setOnClickListener {
                    viewModel.playSound(SoundEffect.BEEP_1)
                    viewModel.focusedAlly.value = entry
                }
            }

            allyCommandButton.visibility =
                if (viewModel.isSingleAlly || entry.isTrap) View.GONE else View.VISIBLE
            allyCommandButton.isEnabled = entry.isInstructable
        }

        private fun bindDescriptionLabel(entry: Ally, viewModel: AgentViewModel) {
            val context = entryBinding.root.context

            val description = mutableListOf(context.getString(entry.status.description))
            if (!entry.isTrap && entry.status != AllyStatus.PIRATE_DATA) {
                if (entry.hasEnergy) {
                    description.add(context.getString(R.string.has_energy))
                }
                if (viewModel.isDeepStrike) {
                    description[0] = context.getString(R.string.ally_status_deep_strike)
                    description.add(
                        if (viewModel.torpedoesReady) {
                            context.getString(R.string.has_ordnance)
                        } else {
                            viewModel.getManufacturingTimer(context)
                        }
                    )
                }
                if (entry.missions > 0) {
                    description.add(entry.getMissionsText(context))
                }

                val destination = entry.destination
                if (destination == null) {
                    entry.direction?.also {
                        description.add(
                            context.getString(
                                R.string.currently_moving_to_heading,
                                viewModel.formattedHeading(it)
                            )
                        )
                    }
                } else {
                    val destinationText = context.getString(
                        if (entry.isAttacking) {
                            R.string.currently_moving_to_attack
                        } else {
                            R.string.currently_moving_toward
                        },
                        destination
                    )
                    if (
                        entry.status == AllyStatus.FLYING_BLIND &&
                        entry.isMovingToStation
                    ) {
                        description[0] = destinationText
                    } else {
                        description.add(destinationText)
                    }
                }
            }

            entryBinding.allyDescriptionLabel.text = description.joinSpaceDelimited()
        }

        private fun bindInfoLabels(entry: Ally) {
            val context = entryBinding.root.context

            entryBinding.allyDirectionLabel.text =
                context.getString(R.string.direction, entry.heading)
            entryBinding.allyRangeLabel.text = context.getString(R.string.range, entry.range)
            entryBinding.allyFrontShieldLabel.text = context.getString(
                R.string.front_shield,
                entry.obj.shieldsFront.value.coerceAtLeast(0f),
                entry.obj.shieldsFrontMax.value
            )
            entryBinding.allyRearShieldLabel.text = context.getString(
                R.string.rear_shield,
                entry.obj.shieldsRear.value.coerceAtLeast(0f),
                entry.obj.shieldsRearMax.value
            )
        }
    }

    private class DestinationGridLayoutManager(context: Context) : GridLayoutManager(
        context,
        1,
        context.resources.configuration.orientation - 1,
        false
    ) {
        private val spanDimension: Int =
            (
                context.resources.displayMetrics.density *
                    if (orientation == HORIZONTAL) NORMAL_BUTTON_HEIGHT else NORMAL_BUTTON_WIDTH
                ).toInt()

        override fun onLayoutChildren(
            recycler: RecyclerView.Recycler?,
            state: RecyclerView.State?
        ) {
            spanCount = 1.coerceAtLeast(
                (if (orientation == HORIZONTAL) height else width) / spanDimension
            )
            super.onLayoutChildren(recycler, state)
        }
    }

    private class DestinationDiffUtilCallback(
        private val oldList: List<ArtemisObject<*>>,
        private val newList: List<ArtemisObject<*>>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldObject = oldList[oldItemPosition]
            val newObject = newList[newItemPosition]
            return newObject == oldObject
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = true
    }

    private class DestinationViewHolder(
        private val button: Button
    ) : RecyclerView.ViewHolder(button) {
        fun bind(ally: ArtemisNpc, obj: ArtemisShielded<*>, viewModel: AgentViewModel) {
            button.text = obj.name.value
            button.setOnClickListener {
                viewModel.playSound(SoundEffect.CONFIRMATION)
                viewModel.sendToServer(
                    CommsOutgoingPacket(
                        ally,
                        OtherMessage.GoDefend(obj),
                        viewModel.vesselData
                    )
                )
                if (!viewModel.manuallyReturnFromCommands && !viewModel.isSingleAlly) {
                    viewModel.focusedAlly.value = null
                }
            }
        }
    }

    private class DestinationAdapter(
        private val ally: ArtemisNpc,
        private val viewModel: AgentViewModel
    ) : RecyclerView.Adapter<DestinationViewHolder>() {
        var objects = listOf<ArtemisShielded<*>>()
            private set

        override fun getItemCount(): Int = objects.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DestinationViewHolder =
            DestinationViewHolder(Button(parent.context))

        override fun onBindViewHolder(holder: DestinationViewHolder, position: Int) {
            holder.bind(ally, objects[position], viewModel)
        }

        fun onTargetsUpdate(newList: List<ArtemisShielded<*>>) {
            if (objects == newList) return

            DiffUtil.calculateDiff(
                DestinationDiffUtilCallback(objects, newList)
            ).dispatchUpdatesTo(this)
            objects = newList
        }
    }

    private class LivingAlliesDiffUtilCallback(
        private val oldList: List<Ally>,
        private val newList: List<Ally>
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

    private class AllyViewHolder(val binder: AllyInfoBinder) :
        RecyclerView.ViewHolder(binder.entryBinding.root)

    private inner class LivingAlliesListAdapter : RecyclerView.Adapter<AllyViewHolder>() {
        var allies = listOf<Ally>()
            private set

        override fun getItemCount(): Int = allies.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllyViewHolder =
            AllyViewHolder(AllyInfoBinder(parent))

        override fun onBindViewHolder(holder: AllyViewHolder, position: Int) {
            holder.binder.bind(allies[position], viewModel)
        }

        fun onAlliesUpdate(newList: List<Ally>) {
            DiffUtil.calculateDiff(
                LivingAlliesDiffUtilCallback(allies, newList)
            ).dispatchUpdatesTo(this)
            allies = newList
        }
    }

    private companion object {
        const val NORMAL_BUTTON_WIDTH = 88
        const val NORMAL_BUTTON_HEIGHT = 48
        const val NINETY = 90

        const val WARSHIP = "warship"
    }
}
