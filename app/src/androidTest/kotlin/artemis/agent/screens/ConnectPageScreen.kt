package artemis.agent.screens

import artemis.agent.R
import artemis.agent.generic.GenericItem
import artemis.agent.setup.ConnectFragment
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.common.views.KView
import io.github.kakaocup.kakao.edit.KEditText
import io.github.kakaocup.kakao.progress.KProgressBar
import io.github.kakaocup.kakao.recycler.KRecyclerView
import io.github.kakaocup.kakao.text.KButton
import io.github.kakaocup.kakao.text.KTextView

object ConnectPageScreen : KScreen<ConnectPageScreen>() {
    override val layoutId: Int = R.layout.connect_fragment
    override val viewClass: Class<*> = ConnectFragment::class.java

    val connectButton = KButton { withId(R.id.connectButton) }
    val addressBar = KEditText { withId(R.id.addressBar) }
    val connectLabel = KTextView { withId(R.id.connectLabel) }
    val connectSpinner = KProgressBar { withId(R.id.connectSpinner) }

    val scanButton = KButton { withId(R.id.scanButton) }
    val scanSpinner = KProgressBar { withId(R.id.scanSpinner) }
    val noServersLabel = KTextView { withId(R.id.noServersLabel) }
    val serverList = KRecyclerView({ withId(R.id.serverList) }, { itemType(::GenericItem) })

    private val addressLabel = KTextView { withId(R.id.addressLabel) }
    private val networkTypeLabel = KTextView { withId(R.id.networkTypeLabel) }
    private val networkInfoDivider = KView { withId(R.id.networkInfoDivider) }

    val infoViews by lazy { listOf(addressLabel, networkTypeLabel, networkInfoDivider) }
}
