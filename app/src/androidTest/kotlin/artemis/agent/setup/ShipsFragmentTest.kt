package artemis.agent.setup

import android.Manifest
import androidx.activity.viewModels
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.ActivityScenarioManager
import artemis.agent.AgentViewModel
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertDisplayedAtPosition
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaSleepInteractions.sleep
import com.adevinta.android.barista.interaction.PermissionGranter
import com.walkertribe.ian.world.Artemis
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ShipsFragmentTest {
    @get:Rule val activityScenarioManager = ActivityScenarioManager.forActivity<MainActivity>()

    @Test
    fun noShipsTest() {
        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

        clickOn(R.id.shipsPageButton)

        assertDisplayed(R.id.noShipsLabel, R.string.no_ships)
        assertDisplayed(R.id.shipsList)
        assertRecyclerViewItemCount(R.id.shipsList, 0)
    }

    @Test
    fun connectedTest() {
        val connectTimeout = AtomicInteger()
        activityScenarioManager.onActivity { activity ->
            connectTimeout.lazySet(activity.viewModels<AgentViewModel>().value.connectTimeout)
        }

        ConnectFragmentTest.connectToServer(ConnectFragmentTest.FAKE_SERVER_IP)

        sleep(connectTimeout.toLong(), TimeUnit.SECONDS)

        assertNotDisplayed(R.id.noShipsLabel)
        assertDisplayed(R.id.shipsList)
        assertRecyclerViewItemCount(R.id.shipsList, Artemis.SHIP_COUNT)

        repeat(Artemis.SHIP_COUNT) { index ->
            displayedIds.forEach { view -> assertDisplayedAtPosition(R.id.shipsList, index, view) }
        }

        clickOn(R.id.gamePageButton)
        assertDisplayed(R.id.shipNumberLabel, R.string.no_ship_selected)

        clickOn(R.id.setupPageButton)
        assertChecked(R.id.shipsPageButton)
        assertDisplayed(R.id.shipsList)
        clickListItem(R.id.shipsList, 0)

        assertUnchecked(R.id.setupPageButton)
        assertNotExist(R.id.setupPageSelector)
        assertChecked(R.id.gamePageButton)
        assertDisplayed(R.id.shipNumberLabel, "Ship 1")

        clickOn(R.id.setupPageButton)
        assertChecked(R.id.shipsPageButton)
        assertDisplayed(R.id.shipsList)
        assertDisplayedAtPosition(R.id.shipsList, 0, R.id.selectedShipLabel, R.string.selected)
    }

    private companion object {
        val displayedIds =
            intArrayOf(R.id.nameLabel, R.id.vesselLabel, R.id.driveTypeLabel, R.id.descriptionLabel)
    }
}
