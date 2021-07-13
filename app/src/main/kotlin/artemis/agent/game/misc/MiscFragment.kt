package artemis.agent.game.misc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.view.ContextThemeWrapper
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
import artemis.agent.databinding.AudioEntryBinding
import artemis.agent.databinding.MiscFragmentBinding
import artemis.agent.databinding.fragmentViewBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class MiscFragment : Fragment(R.layout.misc_fragment) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: MiscFragmentBinding by fragmentViewBinding()

    private val listeningForAudio: Flow<Boolean?> by lazy {
        viewModel.jumping.combine(viewModel.showingAudio) { jump, show ->
            if (jump) null else show
        }
    }

    private val miscOptions: Flow<Pair<Boolean, Boolean>> by lazy {
        viewModel.miscActionsExist.combine(viewModel.miscAudioExists, ::Pair)
    }

    private val updatedMiscActions: Flow<List<CommsActionEntry>?> by lazy {
        viewModel.miscActions.combine(listeningForAudio) { list, listen ->
            if (listen == false) list else null
        }
    }

    private val updatedMiscAudio: Flow<List<AudioEntry>?> by lazy {
        viewModel.miscAudio.combine(listeningForAudio) { list, listen ->
            if (listen == true) list else null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.miscListView.itemAnimator = null

        val actionsAdapter = CommsActionAdapter(viewModel)
        binding.actionsButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.miscListView.apply {
                    layoutManager = LinearLayoutManager(binding.root.context)
                    adapter = actionsAdapter
                }
                viewModel.showingAudio.value = false
            }
        }

        viewLifecycleOwner.collectLatestWhileStarted(updatedMiscActions) {
            actionsAdapter.update(it)
        }

        val audioAdapter = CommsAudioAdapter(viewModel)
        binding.audioButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val context = view.context
                binding.miscListView.apply {
                    layoutManager = GridLayoutManager(
                        context,
                        context.resources.configuration.orientation
                    )
                    adapter = audioAdapter
                }
                viewModel.showingAudio.value = true
            }
        }

        viewLifecycleOwner.collectLatestWhileStarted(updatedMiscAudio) {
            audioAdapter.update(it)
        }

        viewLifecycleOwner.collectLatestWhileStarted(miscOptions) { (actions, audio) ->
            binding.miscListSelector.visibility = if (actions && audio) {
                View.VISIBLE
            } else {
                if (actions || audio) {
                    viewModel.showingAudio.value = audio
                }
                View.GONE
            }
        }

        if (viewModel.showingAudio.value) {
            binding.audioButton
        } else {
            binding.actionsButton
        }.isChecked = true
    }

    private class CommsAudioDiffUtilCallback(
        private val oldList: List<AudioEntry>,
        private val newList: List<AudioEntry>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldEntry = oldList[oldItemPosition]
            val newEntry = newList[newItemPosition]
            return oldEntry.audioId == newEntry.audioId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = true
    }

    private class CommsAudioViewHolder(
        val entryBinding: AudioEntryBinding
    ) : RecyclerView.ViewHolder(entryBinding.root) {
        fun bind(entry: AudioEntry, viewModel: AgentViewModel) {
            entryBinding.messageLabel.text = entry.title
            entryBinding.playButton.setOnClickListener {
                viewModel.playSound(SoundEffect.BEEP_2)
                viewModel.sendToServer(entry.playPacket)
            }
            entryBinding.deleteButton.setOnClickListener {
                viewModel.playSound(SoundEffect.BEEP_2)
                viewModel.sendToServer(entry.dismissPacket)
                viewModel.dismissAudio(entry)
            }
        }
    }

    private class CommsAudioAdapter(
        private val viewModel: AgentViewModel
    ) : RecyclerView.Adapter<CommsAudioViewHolder>() {
        var audios = listOf<AudioEntry>()
            private set

        override fun getItemCount(): Int = audios.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommsAudioViewHolder =
            CommsAudioViewHolder(
                AudioEntryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: CommsAudioViewHolder, position: Int) {
            holder.bind(audios[position], viewModel)
        }

        fun update(value: List<AudioEntry>?) {
            if (value == null) return
            DiffUtil.calculateDiff(
                CommsAudioDiffUtilCallback(audios, value)
            ).dispatchUpdatesTo(this)
            audios = value
        }
    }

    private class CommsActionDiffUtilCallback(
        private val oldList: List<CommsActionEntry>,
        private val newList: List<CommsActionEntry>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldEntry = oldList[oldItemPosition]
            val newEntry = newList[newItemPosition]
            return oldEntry.clickPacket.hash == newEntry.clickPacket.hash
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean = true
    }

    private class CommsActionViewHolder(
        private val button: Button
    ) : RecyclerView.ViewHolder(button) {
        fun bind(entry: CommsActionEntry, viewModel: AgentViewModel) {
            button.text = entry.label
            button.setOnClickListener {
                viewModel.playSound(SoundEffect.BEEP_2)
                viewModel.sendToServer(entry.clickPacket)
            }
        }
    }

    private class CommsActionAdapter(
        private val viewModel: AgentViewModel
    ) : RecyclerView.Adapter<CommsActionViewHolder>() {
        var actions = listOf<CommsActionEntry>()
            private set

        override fun getItemCount(): Int = actions.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommsActionViewHolder =
            CommsActionViewHolder(
                Button(
                    ContextThemeWrapper(
                        parent.context,
                        R.style.Widget_ArtemisAgent_Button_Yellow
                    )
                ).apply {
                    layoutParams = RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.MATCH_PARENT,
                        RecyclerView.LayoutParams.WRAP_CONTENT
                    )
                }
            )

        override fun onBindViewHolder(holder: CommsActionViewHolder, position: Int) {
            holder.bind(actions[position], viewModel)
        }

        fun update(value: List<CommsActionEntry>?) {
            if (value == null) return
            DiffUtil.calculateDiff(
                CommsActionDiffUtilCallback(actions, value)
            ).dispatchUpdatesTo(this)
            actions = value
        }
    }
}
