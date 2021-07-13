package artemis.agent.help

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.SoundEffect
import artemis.agent.collectLatestWhileStarted
import artemis.agent.databinding.HelpFragmentBinding
import artemis.agent.databinding.fragmentViewBinding

class HelpFragment : Fragment(R.layout.help_fragment) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: HelpFragmentBinding by fragmentViewBinding()

    private val currentHelpTopicIndex: Int get() = viewModel.helpTopicIndex.value

    private val helpTopics: List<HelpTopic> by lazy {
        binding.root.resources.run {
            listOf(
                HelpTopic(
                    getString(R.string.help_topics_getting_started),
                    getStringArray(R.array.help_contents_getting_started).map {
                        HelpTopicContent.Text(it)
                    }.toMutableList<HelpTopicContent>().apply {
                        add(3, HelpTopicContent.Image(R.drawable.connect_preview))
                        add(5, HelpTopicContent.Image(R.drawable.ship_entry_preview))
                    },
                ),
                HelpTopic(
                    getString(R.string.help_topics_basics),
                    getStringArray(R.array.help_contents_basics).map {
                        HelpTopicContent.Text(it)
                    }.toMutableList<HelpTopicContent>().apply {
                        add(1, HelpTopicContent.Image(R.drawable.game_header_preview))
                    },
                ),
                HelpTopic(
                    getString(R.string.help_topics_stations),
                    getStringArray(R.array.help_contents_stations).map {
                        HelpTopicContent.Text(it)
                    }.toMutableList<HelpTopicContent>().apply {
                        add(1, HelpTopicContent.Image(R.drawable.station_entry_preview))
                    },
                ),
                HelpTopic(
                    getString(R.string.help_topics_allies),
                    getStringArray(R.array.help_contents_allies).map {
                        HelpTopicContent.Text(it)
                    }.toMutableList<HelpTopicContent>().apply {
                        add(1, HelpTopicContent.Image(R.drawable.ally_entry_preview))
                    },
                ),
                HelpTopic(
                    getString(R.string.help_topics_missions),
                    getStringArray(R.array.help_contents_missions).map {
                        HelpTopicContent.Text(it)
                    }.toMutableList<HelpTopicContent>().apply {
                        add(1, HelpTopicContent.Image(R.drawable.comms_message))
                        add(7, HelpTopicContent.Image(R.drawable.mission_entry_preview))
                    },
                ),
                HelpTopic(
                    getString(R.string.help_topics_routing),
                    getStringArray(R.array.help_contents_routing).map {
                        HelpTopicContent.Text(it)
                    }.toMutableList<HelpTopicContent>().apply {
                        add(1, HelpTopicContent.Image(R.drawable.route_tasks_preview))
                        add(3, HelpTopicContent.Image(R.drawable.route_supplies_preview))
                    }
                ),
                HelpTopic(
                    getString(R.string.help_topics_enemies),
                    getStringArray(R.array.help_contents_enemies).map {
                        HelpTopicContent.Text(it)
                    }.toMutableList<HelpTopicContent>().apply {
                        add(1, HelpTopicContent.Image(R.drawable.enemy_entry_preview))
                        add(3, HelpTopicContent.Image(R.drawable.enemy_intel_preview))
                    },
                ),
                HelpTopic(
                    getString(R.string.help_topics_biomechs),
                    getStringArray(R.array.help_contents_biomechs).map {
                        HelpTopicContent.Text(it)
                    }.toMutableList<HelpTopicContent>().apply {
                        add(1, HelpTopicContent.Image(R.drawable.biomech_entry_preview))
                    },
                ),
                HelpTopic(
                    getString(R.string.help_topics_about),
                    getStringArray(R.array.help_contents_about).map {
                        HelpTopicContent.Text(it)
                    }.toMutableList<HelpTopicContent>().apply {
                        add(0, HelpTopicContent.Text(getString(R.string.app_version)))
                        add(HelpTopicContent.Image(R.drawable.ic_launcher_foreground))
                    },
                ),
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.settingsPage.value = null

        val onBackPressedCallback = requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner
        ) {
            viewModel.playSound(SoundEffect.BEEP_2)
            viewModel.helpTopicIndex.value = MENU
        }

        val helpTopicContent = binding.helpTopicContent

        viewModel.focusedAlly.value = null

        val layoutManager = GridLayoutManager(
            binding.root.context,
            2
        )
        val adapter = HelpTopicsAdapter()

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.helpTopicIndex) { index ->
            val headerVisibility = if (index == MENU) {
                layoutManager.spanCount = 2
                onBackPressedCallback.isEnabled = false
                View.GONE
            } else {
                layoutManager.spanCount = 1
                binding.helpTopicTitle.text = helpTopics[index].buttonLabel
                onBackPressedCallback.isEnabled = true
                View.VISIBLE
            }
            arrayOf(
                binding.backButton,
                binding.helpTopicTitle,
                binding.helpTopicHeaderDivider
            ).forEach { it.visibility = headerVisibility }
            @Suppress("NotifyDataSetChanged")
            adapter.notifyDataSetChanged()
        }

        binding.backButton.setOnClickListener {
            viewModel.playSound(SoundEffect.BEEP_1)
            viewModel.helpTopicIndex.value = MENU
        }

        helpTopicContent.itemAnimator = null
        helpTopicContent.adapter = adapter
        helpTopicContent.layoutManager = layoutManager
    }

    private interface ViewProvider {
        val viewType: Int
    }

    private class HelpTopic(
        val buttonLabel: String,
        val contents: List<HelpTopicContent>
    ) : ViewProvider {
        override val viewType: Int = MENU
    }

    private sealed interface HelpTopicContent : ViewProvider {
        data class Image(@DrawableRes val imageSrcId: Int) : HelpTopicContent {
            override val viewType: Int = IMAGE
        }
        data class Text(val text: String) : HelpTopicContent {
            override val viewType: Int = TEXT
        }
    }

    private sealed class HelpTopicsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        class MenuButton(context: Context) : HelpTopicsViewHolder(Button(context))
        class Image(context: Context) : HelpTopicsViewHolder(ImageView(context))
        class Text(context: Context) : HelpTopicsViewHolder(TextView(context))
    }

    private inner class HelpTopicsAdapter : RecyclerView.Adapter<HelpTopicsViewHolder>() {
        private val contents: List<ViewProvider> get() = currentHelpTopicIndex.let {
            if (it == MENU) {
                helpTopics
            } else {
                helpTopics[it].contents
            }
        }

        override fun getItemCount(): Int = contents.size

        override fun getItemViewType(position: Int): Int = contents[position].viewType

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpTopicsViewHolder {
            return checkNotNull(
                when (viewType) {
                    MENU -> HelpTopicsViewHolder.MenuButton(parent.context)
                    IMAGE -> HelpTopicsViewHolder.Image(parent.context)
                    TEXT -> HelpTopicsViewHolder.Text(parent.context)
                    else -> null
                }
            ) { "Unrecognized view type: $viewType" }
        }

        override fun onBindViewHolder(holder: HelpTopicsViewHolder, position: Int) {
            when (holder) {
                is HelpTopicsViewHolder.MenuButton -> {
                    with(holder.itemView as Button) {
                        text = helpTopics[position].buttonLabel
                        setOnClickListener {
                            viewModel.playSound(SoundEffect.BEEP_2)
                            viewModel.helpTopicIndex.value = position
                        }
                    }
                }
                is HelpTopicsViewHolder.Image -> {
                    with(holder.itemView as ImageView) {
                        val imageSrc = helpTopics[currentHelpTopicIndex].contents[position] as
                            HelpTopicContent.Image
                        setImageResource(imageSrc.imageSrcId)
                        adjustViewBounds = true
                    }
                }
                is HelpTopicsViewHolder.Text -> {
                    with(holder.itemView as TextView) {
                        val textContent = helpTopics[currentHelpTopicIndex].contents[position] as
                            HelpTopicContent.Text
                        text = textContent.text
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.baseTextSize))
                    }
                }
            }
        }
    }

    companion object {
        const val MENU = -1
        const val IMAGE = 0
        const val TEXT = 1
    }
}
