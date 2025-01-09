package artemis.agent.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import artemis.agent.ActivityScenarioManager
import artemis.agent.MainActivity
import kotlin.io.path.createTempDirectory
import okio.Path.Companion.toPath
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class AssetsResolverTest {
    @get:Rule val activityScenarioManager = ActivityScenarioManager.forActivity<MainActivity>()

    @Test
    fun readTest() {
        resolver("dat".toPath() / "test.txt") { Assert.assertEquals("TEST", readUtf8Line()) }
    }

    @Test
    fun copyTest() {
        val tempFile = createTempDirectory("dat")
        Assert.assertTrue(resolver.copyVesselDataTo(tempFile.toFile()))
    }

    private companion object {
        val context by lazy {
            checkNotNull(InstrumentationRegistry.getInstrumentation().context) {
                "Failed to get context"
            }
        }

        val resolver by lazy { AssetsResolver(context.assets) }
    }
}
