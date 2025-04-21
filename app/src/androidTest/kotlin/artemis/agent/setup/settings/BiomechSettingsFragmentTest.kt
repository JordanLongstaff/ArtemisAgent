package artemis.agent.setup.settings

import android.Manifest
import androidx.activity.viewModels
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.ActivityScenarioManager
import artemis.agent.AgentViewModel
import artemis.agent.ArtemisAgentTestHelpers
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaAssertions.assertThatBackButtonClosesTheApp
import com.adevinta.android.barista.assertion.BaristaCheckedAssertions.assertUnchecked
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotExist
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import com.adevinta.android.barista.interaction.BaristaScrollInteractions.scrollTo
import com.adevinta.android.barista.interaction.PermissionGranter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BiomechSettingsFragmentTest {
    @get:Rule val activityScenarioManager = ActivityScenarioManager.forActivity<MainActivity>()

    @Test
    fun biomechSettingsMutableTest() {
        testWithSettings { data ->
            booleanArrayOf(true, false).forEach { testSettings ->
                data.testMenu(
                    openWithToggle = data.enabled != testSettings,
                    testSettings = testSettings,
                    closeWithToggle = data.enabled == testSettings,
                    closeWithBack = false,
                )
            }
        }
    }

    @Test
    fun biomechSettingsBackButtonTest() {
        testWithSettings { data ->
            if (data.enabled) testBiomechsSubMenuDisableFromMenu()

            data.testMenu(
                openWithToggle = true,
                testSettings = false,
                closeWithToggle = false,
                closeWithBack = true,
            )

            if (!data.enabled) testBiomechsSubMenuDisableFromMenu()
        }
    }

    private fun testWithSettings(test: (Data) -> Unit) {
        val biomechsEnabled = AtomicBoolean()
        val freezeTime = AtomicLong()

        val sortByClassFirst = AtomicBoolean()
        val sortByStatus = AtomicBoolean()
        val sortByClassSecond = AtomicBoolean()
        val sortByName = AtomicBoolean()

        activityScenarioManager.onActivity { activity ->
            val viewModel = activity.viewModels<AgentViewModel>().value
            val biomechManager = viewModel.biomechManager
            val biomechSorter = biomechManager.sorter

            sortByClassFirst.lazySet(biomechSorter.sortByClassFirst)
            sortByStatus.lazySet(biomechSorter.sortByStatus)
            sortByClassSecond.lazySet(biomechSorter.sortByClassSecond)
            sortByName.lazySet(biomechSorter.sortByName)

            biomechsEnabled.lazySet(biomechManager.enabled)
            freezeTime.lazySet(biomechManager.freezeTime)
        }

        PermissionGranter.allowPermissionsIfNeeded(Manifest.permission.POST_NOTIFICATIONS)

        SettingsFragmentTest.openSettingsMenu()

        val enabled = biomechsEnabled.get()
        val freezeSeconds = freezeTime.get().milliseconds.inWholeSeconds.toInt()
        val sortMethods =
            SortMethods(
                classFirst = sortByClassFirst.get(),
                status = sortByStatus.get(),
                classSecond = sortByClassSecond.get(),
                name = sortByName.get(),
            )

        test(Data(enabled, freezeSeconds, sortMethods))
        assertThatBackButtonClosesTheApp()
    }

    private data class Data(
        val enabled: Boolean,
        val freezeSeconds: Int,
        val sortMethods: SortMethods,
    ) {
        fun testMenu(
            openWithToggle: Boolean,
            testSettings: Boolean,
            closeWithToggle: Boolean,
            closeWithBack: Boolean,
        ) {
            SettingsFragmentTest.openSettingsSubMenu(ENTRY_INDEX, openWithToggle, true)
            testBiomechsSubMenuOpen(sortMethods, freezeSeconds, testSettings)

            val isToggleOn =
                if (closeWithBack) {
                    SettingsFragmentTest.backFromSubMenu()
                    true
                } else {
                    SettingsFragmentTest.closeSettingsSubMenu(closeWithToggle)
                    !closeWithToggle
                }
            testBiomechsSubMenuClosed(isToggleOn)
        }
    }

    private data class SortMethods(
        val classFirst: Boolean,
        val status: Boolean,
        val classSecond: Boolean,
        val name: Boolean,
    ) {
        private val array by lazy { booleanArrayOf(classFirst, status, classSecond, name) }

        fun toArray(): BooleanArray = array
    }

    private companion object {
        const val ENTRY_INDEX = 5

        val biomechSortMethodSettings by lazy {
            arrayOf(
                GroupedToggleButtonSetting(R.id.biomechSortingClassButton1, R.string.sort_by_class),
                GroupedToggleButtonSetting(
                    R.id.biomechSortingStatusButton,
                    R.string.sort_by_status,
                ),
                GroupedToggleButtonSetting(R.id.biomechSortingClassButton2, R.string.sort_by_class),
                GroupedToggleButtonSetting(R.id.biomechSortingNameButton, R.string.sort_by_name),
            )
        }

        fun testBiomechsSubMenuOpen(
            sortMethods: SortMethods,
            freezeSeconds: Int,
            shouldTestSettings: Boolean,
        ) {
            testBiomechSubMenuSortMethods(sortMethods, shouldTestSettings)

            scrollTo(R.id.freezeDurationDivider)
            assertDisplayed(R.id.freezeDurationTitle, R.string.freeze_duration)
            assertDisplayed(R.id.freezeDurationTimeInput)

            if (shouldTestSettings) {
                TimeInputTestHelper(R.id.freezeDurationTimeInput, freezeSeconds, true).testFully()
            }
        }

        fun testBiomechsSubMenuClosed(isToggleOn: Boolean) {
            assertNotExist(R.id.biomechSortingTitle)
            assertNotExist(R.id.biomechSortingDefaultButton)
            biomechSortMethodSettings.forEach { assertNotExist(it.button) }
            assertNotExist(R.id.biomechSortingDivider)
            assertNotExist(R.id.freezeDurationTitle)
            assertNotExist(R.id.freezeDurationTimeInput)
            assertNotExist(R.id.freezeDurationDivider)

            SettingsFragmentTest.assertSettingsMenuEntryToggleState(ENTRY_INDEX, isToggleOn)
        }

        fun testBiomechsSubMenuDisableFromMenu() {
            SettingsFragmentTest.toggleSettingsSubMenu(ENTRY_INDEX)
            testBiomechsSubMenuClosed(false)
        }

        fun testBiomechSubMenuSortMethods(sortMethods: SortMethods, shouldTest: Boolean) {
            scrollTo(R.id.biomechSortingDivider)
            assertDisplayed(R.id.biomechSortingTitle, R.string.sort_methods)
            assertDisplayed(R.id.biomechSortingDefaultButton, R.string.default_setting)

            val sortMethodArray = sortMethods.toArray()

            biomechSortMethodSettings.forEachIndexed { index, setting ->
                assertDisplayed(setting.button, setting.text)
                ArtemisAgentTestHelpers.assertChecked(setting.button, sortMethodArray[index])
            }

            ArtemisAgentTestHelpers.assertChecked(
                R.id.biomechSortingDefaultButton,
                sortMethodArray.none { it },
            )

            if (!shouldTest) return

            clickOn(R.id.biomechSortingDefaultButton)
            biomechSortMethodSettings.forEach { assertUnchecked(it.button) }

            testBiomechsSubMenuSortByClass()
            testBiomechsSubMenuSortByStatus()
            testBiomechsSubMenuSortByName()
            testBiomechsSubMenuSortPermutations()

            biomechSortMethodSettings.forEachIndexed { index, setting ->
                if (sortMethodArray[index]) {
                    clickOn(setting.button)
                }
            }
        }

        fun testBiomechsSubMenuSortByClass() {
            SettingsFragmentTest.testSortPair(
                R.id.biomechSortingClassButton1,
                R.id.biomechSortingClassButton2,
                R.id.biomechSortingDefaultButton,
            )
        }

        fun testBiomechsSubMenuSortByStatus() {
            SettingsFragmentTest.testSortSingle(
                R.id.biomechSortingStatusButton,
                R.id.biomechSortingDefaultButton,
            )
        }

        fun testBiomechsSubMenuSortByName() {
            SettingsFragmentTest.testSortSingle(
                R.id.biomechSortingNameButton,
                R.id.biomechSortingDefaultButton,
            )
        }

        fun testBiomechsSubMenuSortPermutations() {
            SettingsFragmentTest.testSortPermutations(
                R.id.biomechSortingDefaultButton,
                R.id.biomechSortingClassButton1,
                R.id.biomechSortingStatusButton,
                R.id.biomechSortingClassButton2,
                R.id.biomechSortingNameButton,
                R.id.biomechSortingStatusButton,
                R.id.biomechSortingClassButton2,
                R.id.biomechSortingNameButton,
            )
        }
    }
}
