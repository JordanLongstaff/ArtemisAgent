package artemis.agent.help

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.databinding.HelpFragmentBinding
import artemis.agent.databinding.fragmentViewBinding
import artemis.agent.util.BackPreview
import artemis.agent.util.SoundEffect
import artemis.agent.util.collectLatestWhileStarted

class HelpFragment : Fragment(R.layout.help_fragment) {
    private val viewModel: AgentViewModel by activityViewModels()
    private val binding: HelpFragmentBinding by fragmentViewBinding()

    private val currentHelpTopicIndex: Int
        get() = viewModel.helpTopicIndex.value

    private val backPreview by lazy {
        object : BackPreview(false) {
            private var currentTopicIndex: Int = MENU

            override fun beforePreview() {
                currentTopicIndex = viewModel.helpTopicIndex.value
            }

            override fun preview() {
                viewModel.helpTopicIndex.value = MENU
                binding.backPressAlpha.visibility = View.VISIBLE
            }

            override fun revert() {
                viewModel.helpTopicIndex.value = currentTopicIndex
                binding.backPressAlpha.visibility = View.GONE
            }

            override fun close() {
                viewModel.activateHaptic()
                viewModel.playSound(SoundEffect.BEEP_1)
                binding.backPressAlpha.visibility = View.GONE
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.settingsPage.value = null
        viewModel.backPreview = backPreview

        val helpTopicContent = binding.helpTopicContent

        viewModel.focusedAlly.value = null

        val layoutManager = GridLayoutManager(binding.root.context, 2)
        val adapter = HelpTopicsAdapter()
        val resources = view.resources

        viewLifecycleOwner.collectLatestWhileStarted(viewModel.helpTopicIndex) { index ->
            val headerVisibility =
                if (index == MENU) {
                    layoutManager.spanCount = 2
                    View.GONE
                } else {
                    helpTopics[index].initContents(resources)
                    layoutManager.spanCount = 1
                    binding.helpTopicTitle.setText(helpTopics[index].buttonLabel)
                    backPreview.isEnabled = true
                    View.VISIBLE
                }
            arrayOf(binding.backButton, binding.helpTopicTitle, binding.helpTopicHeaderDivider)
                .forEach { it.visibility = headerVisibility }
            @Suppress("NotifyDataSetChanged") adapter.notifyDataSetChanged()
        }

        binding.backButton.setOnClickListener { backPreview.handleOnBackPressed() }

        helpTopicContent.itemAnimator = null
        helpTopicContent.adapter = adapter
        helpTopicContent.layoutManager = layoutManager
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPreview)
    }

    private interface ViewProvider {
        val viewType: Int
    }

    private class HelpTopic(
        @all:StringRes val buttonLabel: Int,
        @all:ArrayRes private val paragraphs: Int,
        private val insert: MutableList<HelpTopicContent>.(Resources) -> Unit,
    ) : ViewProvider {
        override val viewType: Int = MENU

        var contents: List<HelpTopicContent> = emptyList()
            private set

        fun initContents(res: Resources) {
            if (contents.isEmpty()) {
                contents =
                    res.getStringArray(paragraphs)
                        .map { HelpTopicContent.Text(it) }
                        .toMutableList<HelpTopicContent>()
                        .apply { insert(res) }
            }
        }
    }

    private sealed interface HelpTopicContent : ViewProvider {
        data class Image(@all:DrawableRes val imageSrcId: Int) : HelpTopicContent {
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
        private val contents: List<ViewProvider>
            get() =
                currentHelpTopicIndex.let {
                    if (it == MENU) helpTopics else helpTopics[it].contents
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
            ) {
                "Unrecognized view type: $viewType"
            }
        }

        override fun onBindViewHolder(holder: HelpTopicsViewHolder, position: Int) {
            when (holder) {
                is HelpTopicsViewHolder.MenuButton -> {
                    with(holder.itemView as Button) {
                        setText(helpTopics[position].buttonLabel)
                        setOnClickListener {
                            viewModel.activateHaptic()
                            viewModel.playSound(SoundEffect.BEEP_2)
                            viewModel.helpTopicIndex.value = position
                        }
                    }
                }
                is HelpTopicsViewHolder.Image -> {
                    with(holder.itemView as ImageView) {
                        val imageSrc =
                            helpTopics[currentHelpTopicIndex].contents[position]
                                as HelpTopicContent.Image
                        setImageResource(imageSrc.imageSrcId)
                        adjustViewBounds = true
                    }
                }
                is HelpTopicsViewHolder.Text -> {
                    with(holder.itemView as TextView) {
                        val textContent =
                            helpTopics[currentHelpTopicIndex].contents[position]
                                as HelpTopicContent.Text
                        text = textContent.text
                        setTextSize(
                            TypedValue.COMPLEX_UNIT_PX,
                            resources.getDimension(R.dimen.baseTextSize),
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val helpTopics: List<HelpTopic> =
            listOf(
                HelpTopic(
                    R.string.help_topics_getting_started,
                    R.array.help_contents_getting_started,
                ) {
                    addImages(
                        INDEX_PREVIEW_CONNECT to R.drawable.connect_preview,
                        INDEX_PREVIEW_SHIP to R.drawable.ship_entry_preview,
                    )
                },
                HelpTopic(R.string.help_topics_basics, R.array.help_contents_basics) {
                    addImages(1 to R.drawable.status_preview)
                },
                HelpTopic(R.string.help_topics_stations, R.array.help_contents_stations) {
                    addImages(1 to R.drawable.station_entry_preview)
                },
                HelpTopic(R.string.help_topics_allies, R.array.help_contents_allies) {
                    addImages(
                        INDEX_PREVIEW_ALLY to R.drawable.ally_entry_preview,
                        INDEX_PREVIEW_RECAP to R.drawable.ally_recap_preview,
                    )
                },
                HelpTopic(R.string.help_topics_missions, R.array.help_contents_missions) {
                    addImages(
                        INDEX_PREVIEW_COMMS_MESSAGE to R.drawable.comms_message,
                        INDEX_PREVIEW_MISSION to R.drawable.mission_entry_preview,
                    )
                },
                HelpTopic(R.string.help_topics_routing, R.array.help_contents_routing) {
                    addImages(
                        INDEX_PREVIEW_ROUTE_TASKS to R.drawable.route_tasks_preview,
                        INDEX_PREVIEW_ROUTE_SUPPLIES to R.drawable.route_supplies_preview,
                    )
                },
                HelpTopic(R.string.help_topics_enemies, R.array.help_contents_enemies) {
                    addImages(
                        INDEX_PREVIEW_ENEMY to R.drawable.enemy_entry_preview,
                        INDEX_PREVIEW_INTEL to R.drawable.enemy_intel_preview,
                    )
                },
                HelpTopic(R.string.help_topics_biomechs, R.array.help_contents_biomechs) {
                    addImages(1 to R.drawable.biomech_entry_preview)
                },
                HelpTopic(
                    R.string.help_topics_notifications,
                    R.array.help_contents_notifications,
                ) {},
                HelpTopic(R.string.help_topics_about, R.array.help_contents_about) { res ->
                    add(0, HelpTopicContent.Text(res.getString(R.string.app_version)))
                    add(HelpTopicContent.Image(R.drawable.ic_launcher_foreground))
                },
            )

        const val MENU = -1
        const val IMAGE = 0
        const val TEXT = 1

        val ABOUT_TOPIC_INDEX = helpTopics.lastIndex

        private const val INDEX_PREVIEW_CONNECT = 4
        private const val INDEX_PREVIEW_SHIP = 6

        private const val INDEX_PREVIEW_ALLY = 1
        private const val INDEX_PREVIEW_RECAP = 3

        private const val INDEX_PREVIEW_COMMS_MESSAGE = 1
        private const val INDEX_PREVIEW_MISSION = 7

        private const val INDEX_PREVIEW_ROUTE_TASKS = 1
        private const val INDEX_PREVIEW_ROUTE_SUPPLIES = 3

        private const val INDEX_PREVIEW_ENEMY = 1
        private const val INDEX_PREVIEW_INTEL = 3

        private fun MutableList<HelpTopicContent>.addImages(vararg entries: Pair<Int, Int>) {
            entries.forEach { (index, entry) -> add(index, HelpTopicContent.Image(entry)) }
        }
    }
}
