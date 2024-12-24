package artemis.agent

import android.app.Activity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.adevinta.android.barista.rule.flaky.FlakyTestRule
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class ActivityScenarioManager<A : Activity>(
    activityClass: Class<A>,
) : TestRule {
    private val activityScenarioRule: ActivityScenarioRule<A> = ActivityScenarioRule(activityClass)
    private val ruleChain: RuleChain = RuleChain.outerRule(
        FlakyTestRule().allowFlakyAttemptsByDefault(RETRIES)
    ).around(activityScenarioRule)

    fun onActivity(action: ActivityScenario.ActivityAction<A>): ActivityScenario<A> =
        activityScenarioRule.scenario.onActivity(action)

    override fun apply(base: Statement?, description: Description?): Statement =
        ruleChain.apply(base, description)

    companion object {
        private const val RETRIES = 3

        inline fun <reified A : Activity> forActivity(): ActivityScenarioManager<A> =
            ActivityScenarioManager(A::class.java)
    }
}
