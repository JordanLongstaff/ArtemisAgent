package artemis.agent.cpu

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.walkertribe.ian.vesseldata.VesselData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class VesselDataManagerTest {
    @Test
    fun defaultVesselDataTest() {
        assertTrue(vesselDataManager.vesselData is VesselData.Loaded)
    }

    @Test
    fun countTest() {
        assert(vesselDataManager.externalCount > 0)
        assertEquals(vesselDataManager.externalCount + 1, vesselDataManager.count)
    }

    @Test
    fun reconcileTest() {
        assertEquals(vesselDataManager.reconcileIndex(-1), 0)
        repeat(vesselDataManager.count) { index ->
            assertEquals(vesselDataManager.reconcileIndex(index), index)
        }
        assertEquals(vesselDataManager.reconcileIndex(vesselDataManager.count), 0)
    }

    private companion object {
        val context by lazy {
            checkNotNull(InstrumentationRegistry.getInstrumentation().targetContext) {
                "Failed to get context"
            }
        }

        val vesselDataManager by lazy { VesselDataManager(context) }
    }
}
