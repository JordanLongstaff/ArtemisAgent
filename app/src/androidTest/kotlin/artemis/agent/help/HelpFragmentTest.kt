package artemis.agent.help

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.MainActivity
import artemis.agent.R
import artemis.agent.isDisplayedWithText
import artemis.agent.isRemoved
import artemis.agent.screens.HelpPageScreen
import artemis.agent.screens.MainScreen.mainScreenTest
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class HelpFragmentTest : TestCase() {
    @get:Rule val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun menuOptionsTest() {
        testHelpFragment { backButton.click() }
    }

    @Test
    fun backButtonTest() {
        testHelpFragment { pressBack() }
    }

    private fun testHelpFragment(goBack: HelpPageScreen.() -> Unit) {
        run {
            mainScreenTest {
                step("Navigate to Help page") { helpPageButton.click() }

                step("Initial state: menu") { HelpPageScreen.assertMainMenuDisplayed() }

                HelpPageScreen {
                    helpTopics.forEachIndexed { index, (stringRes, _) ->
                        val pageName = device.targetContext.getString(stringRes)
                        step("Open topic: $pageName") {
                            openTopicAtIndex(index)

                            this@mainScreenTest.updateButton {
                                if (index == this@HelpPageScreen.aboutHelpTopicIndex) {
                                    isDisplayedWithText(R.string.check_for_updates)
                                } else {
                                    isRemoved()
                                }
                            }
                        }

                        step("Close topic: $pageName") {
                            goBack()
                            assertMainMenuDisplayed()
                        }
                    }
                }
            }
        }
    }
}
