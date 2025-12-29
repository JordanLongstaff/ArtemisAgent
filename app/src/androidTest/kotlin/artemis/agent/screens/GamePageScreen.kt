package artemis.agent.screens

import artemis.agent.R
import artemis.agent.game.GameFragment
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.text.KTextView

object GamePageScreen : KScreen<GamePageScreen>() {
    override val layoutId: Int = R.layout.game_fragment
    override val viewClass: Class<*> = GameFragment::class.java

    val shipNumberLabel = KTextView { withId(R.id.shipNumberLabel) }
    val gamePageSelectorButton = KTextView { withId(R.id.gamePageSelectorButton) }
}
