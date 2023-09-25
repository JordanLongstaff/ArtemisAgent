package artemis.agent.setup.settings

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertChecked
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertDisabled
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertEnabled
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItem
import com.adevinta.android.barista.interaction.BaristaListInteractions.clickListItemChild
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsFragmentTest {
    companion object {
        private val pageTitles = intArrayOf(
            R.string.settings_menu_client,
            R.string.settings_menu_connection,
            R.string.settings_menu_missions,
            R.string.settings_menu_allies,
            R.string.settings_menu_enemies,
            R.string.settings_menu_biomechs,
            R.string.settings_menu_routing,
            R.string.settings_menu_personal,
        )

        fun openSettingsMenu() {
            clickOn(R.id.settingsPageButton)
            assertSettingsMainMenuDisplayed()
        }

        fun openSettingsSubMenu(
            index: Int,
            usingToggle: Boolean = false,
            toggleDisplayed: Boolean = usingToggle,
        ) {
            if (usingToggle) {
                clickListItemChild(R.id.settingsPageMenu, index, R.id.settingsEntryToggle)
            } else {
                clickListItem(R.id.settingsPageMenu, index)
            }

            assertDisplayed(R.id.settingsPageTitle, pageTitles[index])
            assertDisplayed(R.id.settingsBack)
            assertNotExist(R.id.settingsPageMenu)

            if (toggleDisplayed) {
                assertDisplayed(R.id.settingsOnOff)
                assertChecked(R.id.settingsOnOff)
            } else {
                assertNotDisplayed(R.id.settingsOnOff)
            }
        }

        fun closeSettingsSubMenu(usingToggle: Boolean = false) {
            clickOn(if (usingToggle) R.id.settingsOnOff else R.id.settingsBack)
            assertSettingsMainMenuDisplayed()
        }

        private fun assertSettingsMainMenuDisplayed() {
            assertDisplayed(R.id.settingsPageTitle, R.string.settings)
            assertNotDisplayed(R.id.settingsBack)

            assertDisplayed(R.id.settingsPageMenu)
            assertRecyclerViewItemCount(R.id.settingsPageMenu, pageTitles.size)
        }

        fun testAllEnabled(
            @IdRes allButton: Int,
            @IdRes noneButton: Int,
            buttons: IntArray,
            skipToggleTest: Boolean,
        ) {
            assertDisabled(allButton)
            assertEnabled(noneButton)

            if (skipToggleTest) return

            listOf(
                Triple(noneButton, allButton, false),
                Triple(allButton, noneButton, true),
            ).forEach { (clicked, other, checked) ->
                testMultipleOptions(clicked, other, buttons, checked)
            }
        }

        fun testNotAllEnabled(
            @IdRes allButton: Int,
            @IdRes noneButton: Int,
            buttons: IntArray,
            enabled: BooleanArray,
            skipToggleTest: Boolean,
            ifEnabled: ((Int, Boolean) -> Unit)? = null,
        ) {
            val anyEnabled = enabled.any { it }

            assertEnabled(allButton)
            artemis.agent.ArtemisAgentTestHelpers.assertEnabled(noneButton, anyEnabled)

            if (skipToggleTest) return

            listOf(
                Triple(allButton, noneButton, true),
                Triple(noneButton, allButton, false),
            ).forEach { (clicked, other, checked) ->
                testMultipleOptions(clicked, other, buttons, checked, ifEnabled)
            }

            if (anyEnabled) {
                enabled.forEachIndexed { index, on ->
                    if (on) {
                        clickOn(buttons[index])
                    }
                }
                assertEnabled(noneButton)
            }
        }

        private fun testMultipleOptions(
            @IdRes allButton: Int,
            @IdRes otherButton: Int,
            buttons: IntArray,
            checked: Boolean,
            ifEnabled: ((Int, Boolean) -> Unit)? = null,
        ) {
            clickOn(allButton)
            buttons.forEachIndexed { index, button ->
                artemis.agent.ArtemisAgentTestHelpers.assertChecked(button, checked)
                ifEnabled?.invoke(index, checked)
            }
            assertEnabled(otherButton)
            assertDisabled(allButton)

            buttons.forEach { button ->
                booleanArrayOf(true, false).forEach { on ->
                    clickOn(button)
                    artemis.agent.ArtemisAgentTestHelpers.assertChecked(button, checked != on)
                    assertEnabled(otherButton)
                    artemis.agent.ArtemisAgentTestHelpers.assertEnabled(allButton, on)
                }
            }
        }

        fun testSortPair(
            @IdRes sortFirst: Int,
            @IdRes sortSecond: Int,
            @IdRes defaultSort: Int,
        ) {
            listOf(
                sortFirst to sortSecond,
                sortSecond to sortFirst,
            ).forEach { (first, second) ->
                clickOn(first)
                assertChecked(first)
                assertUnchecked(defaultSort)

                clickOn(second)
                assertChecked(second)
                assertUnchecked(first)

                clickOn(second)
                assertUnchecked(second)
                assertChecked(defaultSort)
            }
        }

        fun testSortPermutations(
            @IdRes defaultSort: Int,
            @IdRes vararg orderToClick: Int,
        ) {
            val lastIndex = orderToClick.size - 1
            orderToClick.forEachIndexed { index, id ->
                clickOn(id)
                artemis.agent.ArtemisAgentTestHelpers.assertChecked(defaultSort, index == lastIndex)
            }
        }

        fun testSortSingle(
            @IdRes sortButton: Int,
            @IdRes defaultSortButton: Int,
        ) {
            clickOn(sortButton)
            assertChecked(sortButton)
            assertUnchecked(defaultSortButton)

            clickOn(sortButton)
            assertUnchecked(sortButton)
            assertChecked(defaultSortButton)
        }

        fun testSingleToggleSetting(
            @IdRes dividerId: Int,
            @IdRes titleId: Int,
            @StringRes titleText: Int,
            @IdRes buttonId: Int,
            isChecked: Boolean,
        ) {
            scrollTo(dividerId)
            assertDisplayed(titleId, titleText)
            assertDisplayed(buttonId)
            artemis.agent.ArtemisAgentTestHelpers.assertChecked(buttonId, isChecked)
        }
    }

    @Test
    fun settingsMenuTest() {
        Robolectric.buildActivity(MainActivity::class.java).use {
            it.setup()

            openSettingsMenu()
            assertDisplayed(R.id.settingsReset)
        }
    }
}
