package artemis.agent.screens

import artemis.agent.R
import artemis.agent.setup.SetupFragment
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.check.KCheckBox

object SetupPageScreen : KScreen<SetupPageScreen>() {
    override val layoutId: Int = R.layout.setup_fragment
    override val viewClass: Class<*> = SetupFragment::class.java

    val connectPageButton = KCheckBox { withId(R.id.connectPageButton) }
    val shipsPageButton = KCheckBox { withId(R.id.shipsPageButton) }
    val settingsPageButton = KCheckBox { withId(R.id.settingsPageButton) }
}
