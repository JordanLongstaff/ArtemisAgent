package artemis.agent.game.enemies

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.SoundEffect
import artemis.agent.collectLatestWhileStarted
import artemis.agent.databinding.EnemiesEntryBinding
import artemis.agent.databinding.EnemiesFragmentBinding
import artemis.agent.databinding.TauntEntryBinding
import artemis.agent.databinding.fragmentViewBinding
import com.walkertribe.ian.enums.EnemyMessage
import com.walkertribe.ian.protocol.core.comm.CommsOutgoingPacket
import com.walkertribe.ian.vesseldata.Taunt

class EnemiesFragment : Fragment(R.layout.enemies_fragment) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: EnemiesFragmentBinding by fragmentViewBinding()

    private val enemyAdapter: EnemyListAdapter by lazy { EnemyListAdapter() }
    private val categoryAdapter: CategoryAdapter by lazy { CategoryAdapter() }
    private val tauntsAdapter: TauntListAdapter by lazy { TauntListAdapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = view.context

        val isLandscape =
            context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val enemyListView = binding.enemyList
        enemyListView.itemAnimator = null
        enemyListView.adapter = enemyAdapter

        val categorySelector = binding.enemyNavigator
        categorySelector.itemAnimator = null
        categorySelector.adapter = categoryAdapter

        val tauntListView = binding.tauntList
        tauntListView.itemAnimator = null
        tauntListView.adapter = tauntsAdapter

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.selectedEnemy) {
            var backgroundColor: Int = Color.TRANSPARENT
            val visibility = if (it != null) {
                binding.selectedEnemyLabel.text = viewModel.getFullNameForShip(it.enemy)
                backgroundColor = it.getBackgroundColor(context)

                View.VISIBLE
            } else if (isLandscape) {
                View.INVISIBLE
            } else {
                View.GONE
            }

            binding.selectedEnemyLabel.visibility = visibility
            binding.selectedEnemyDivider.visibility = visibility
            binding.enemyIntelDivider.visibility = visibility
            binding.tauntList.visibility = visibility

            binding.enemyIntelLabel.visibility =
                if (viewModel.showEnemyIntel) visibility else View.GONE

            binding.selectedEnemyLabel.setBackgroundColor(backgroundColor)
            binding.enemyIntelLabel.setBackgroundColor(backgroundColor)
            binding.tauntList.setBackgroundColor(backgroundColor)
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.enemyIntel) {
            binding.enemyIntelLabel.text = it ?: context.getText(R.string.enemy_status_no_intel)
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.selectedEnemyIndex) { index ->
            binding.selectedEnemyLabel.setOnClickListener {
                if (index >= 0) binding.enemyList.scrollToPosition(index)
            }
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.displayedEnemies) {
            enemyAdapter.onEnemiesUpdate(it)

            binding.noEnemiesLabel.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.enemyCategories) {
            categoryAdapter.onCategoriesUpdate(it)
        }

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.enemyTaunts) {
            tauntsAdapter.onTauntsUpdate(it)
        }
    }

    private class EnemiesDiffUtilCallback(
        private val oldList: List<EnemyEntry>,
        private val newList: List<EnemyEntry>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldEntry = oldList[oldItemPosition]
            val newEntry = newList[newItemPosition]
            return newEntry.enemy == oldEntry.enemy
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = false
    }

    private class EnemyViewHolder(val enemyBinding: EnemiesEntryBinding) :
        RecyclerView.ViewHolder(enemyBinding.root)

    private inner class EnemyListAdapter : RecyclerView.Adapter<EnemyViewHolder>() {
        private var enemies = listOf<EnemyEntry>()

        override fun getItemCount(): Int = enemies.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnemyViewHolder =
            EnemyViewHolder(
                EnemiesEntryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: EnemyViewHolder, position: Int) {
            with(holder.enemyBinding) {
                val context = root.context
                val entry = enemies[position]
                val enemy = entry.enemy

                root.setBackgroundColor(entry.getBackgroundColor(context))

                enemyNameLabel.text = viewModel.getFullNameForShip(enemy)

                enemyDirectionLabel.text =
                    context.getString(R.string.direction, entry.heading)
                enemyRangeLabel.text =
                    context.getString(R.string.range, entry.range)

                enemyStatusLabel.text = context.getString(
                    if (!enemy.isSurrendered.value.booleanValue) {
                        entry.captainStatus.description
                    } else if (entry.captainStatus == EnemyCaptainStatus.DUPLICITOUS) {
                        R.string.surrendered_duplicitous
                    } else {
                        R.string.surrendered
                    }
                )

                enemyTauntsLabel.text = entry.getTauntCountText(context)

                val buttonVisibility: Int = if (enemy.isSurrendered.value.booleanValue) {
                    View.GONE
                } else {
                    enemySurrenderButton.isEnabled = viewModel.maxSurrenderDistance?.let {
                        entry.range < it
                    } != false
                    enemySurrenderButton.setOnClickListener {
                        viewModel.playSound(SoundEffect.BEEP_2)
                        viewModel.sendToServer(
                            CommsOutgoingPacket(
                                enemy,
                                EnemyMessage.WILL_YOU_SURRENDER,
                                viewModel.vesselData
                            )
                        )
                    }

                    val enemyToTaunt = if (viewModel.selectedEnemy.value?.enemy == enemy) {
                        enemyTauntButton.setText(R.string.cancel)
                        null
                    } else {
                        enemyTauntButton.setText(R.string.taunt)
                        entry
                    }

                    enemyTauntButton.setOnClickListener {
                        viewModel.playSound(SoundEffect.BEEP_1)
                        viewModel.selectedEnemy.value = enemyToTaunt
                        viewModel.refreshEnemyTaunts()
                    }

                    View.VISIBLE
                }

                enemySurrenderButton.visibility = buttonVisibility
                enemyTauntButton.visibility = buttonVisibility
            }
        }

        fun onEnemiesUpdate(newList: List<EnemyEntry>) {
            DiffUtil.calculateDiff(
                EnemiesDiffUtilCallback(enemies, newList)
            ).dispatchUpdatesTo(this)
            enemies = newList
        }
    }

    private class CategoryDiffUtilCallback(
        private val oldList: List<EnemySortCategory>,
        private val newList: List<EnemySortCategory>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldItemPosition == newItemPosition

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    private class CategoryViewHolder(val button: Button) : RecyclerView.ViewHolder(button)

    private inner class CategoryAdapter : RecyclerView.Adapter<CategoryViewHolder>() {
        private var categories = listOf<EnemySortCategory>()

        override fun getItemCount(): Int = categories.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder =
            CategoryViewHolder(Button(parent.context))

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            val category = categories[position]
            holder.button.text = category.getString(holder.itemView.context)
            holder.button.setOnClickListener {
                viewModel.playSound(SoundEffect.BEEP_2)
                binding.enemyList.scrollToPosition(category.scrollIndex)
            }
        }

        fun onCategoriesUpdate(newList: List<EnemySortCategory>) {
            DiffUtil.calculateDiff(
                CategoryDiffUtilCallback(categories, newList)
            ).dispatchUpdatesTo(this)
            categories = newList
        }
    }

    private class TauntDiffUtilCallback(
        private val oldList: List<Pair<Taunt, TauntStatus>>,
        private val newList: List<Pair<Taunt, TauntStatus>>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldItemPosition == newItemPosition

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    private class TauntViewHolder(val tauntBinding: TauntEntryBinding) :
        RecyclerView.ViewHolder(tauntBinding.root)

    private inner class TauntListAdapter : RecyclerView.Adapter<TauntViewHolder>() {
        private var taunts = listOf<Pair<Taunt, TauntStatus>>()

        override fun getItemCount(): Int = Taunt.COUNT

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TauntViewHolder =
            TauntViewHolder(
                TauntEntryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: TauntViewHolder, position: Int) {
            if (position >= taunts.size) return

            val (taunt, status) = taunts[position]

            with(holder.tauntBinding) {
                tauntLabel.text = taunt.text

                statusLabel.visibility = if (viewModel.showTauntStatuses) {
                    statusLabel.text = status.name
                    View.VISIBLE
                } else {
                    View.GONE
                }

                sendButton.isEnabled =
                    !viewModel.disableIneffectiveTaunts || status != TauntStatus.INEFFECTIVE
                sendButton.setOnClickListener {
                    viewModel.playSound(SoundEffect.BEEP_2)
                    viewModel.selectedEnemy.value?.also { enemy ->
                        val tauntMessage = EnemyMessage.entries[position + 1]
                        viewModel.sendToServer(
                            CommsOutgoingPacket(
                                enemy.enemy,
                                tauntMessage,
                                viewModel.vesselData
                            )
                        )
                        enemy.lastTaunt = tauntMessage
                    }
                    viewModel.selectedEnemy.value = null
                }
            }
        }

        fun onTauntsUpdate(newList: List<Pair<Taunt, TauntStatus>>) {
            DiffUtil.calculateDiff(
                TauntDiffUtilCallback(taunts, newList)
            ).dispatchUpdatesTo(this)
            taunts = newList
        }
    }
}
