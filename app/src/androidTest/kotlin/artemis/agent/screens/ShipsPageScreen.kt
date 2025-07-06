package artemis.agent.screens

import android.view.View
import artemis.agent.R
import artemis.agent.setup.ShipsFragment
import com.kaspersky.kaspresso.screens.KScreen
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.recycler.KRecyclerView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher

object ShipsPageScreen : KScreen<ShipsPageScreen>() {
    override val layoutId: Int = R.layout.ships_fragment
    override val viewClass: Class<*> = ShipsFragment::class.java

    val noShipsLabel = KTextView { withId(R.id.noShipsLabel) }
    val shipsList = KRecyclerView({ withId(R.id.shipsList) }, { itemType(::ShipItem) })

    class ShipItem(parent: Matcher<View>) : KRecyclerItem<ShipItem>(parent) {
        val selectedShipLabel = KTextView(parent) { withId(R.id.selectedShipLabel) }
        val nameLabel = KTextView(parent) { withId(R.id.nameLabel) }
        val vesselLabel = KTextView(parent) { withId(R.id.vesselLabel) }
        val driveTypeLabel = KTextView(parent) { withId(R.id.driveTypeLabel) }
        val descriptionLabel = KTextView(parent) { withId(R.id.descriptionLabel) }
    }
}
