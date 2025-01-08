package artemis.agent.game.enemies

import android.content.Context
import androidx.core.content.ContextCompat
import artemis.agent.R
import com.walkertribe.ian.util.BoolState
import com.walkertribe.ian.vesseldata.Faction
import com.walkertribe.ian.vesseldata.Vessel
import com.walkertribe.ian.world.ArtemisNpc
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

class EnemyEntryTest {
    @Test
    fun cannotTauntTest() {
        testEnemyEntry {
            it.tauntStatuses.fill(TauntStatus.INEFFECTIVE)
            Assert.assertEquals(CANNOT_TAUNT, it.getTauntCountText(mockContext))
        }
    }

    @Test
    fun tauntCountLowTest() {
        testEnemyEntry {
            TAUNT_COUNT_STRINGS.forEachIndexed { count, string ->
                it.tauntCount = count
                Assert.assertEquals(string, it.getTauntCountText(mockContext))
            }
        }
    }

    @Test
    fun tauntCountHighTest() {
        testEnemyEntry {
            for (count in 3..10) {
                it.tauntCount = count
                Assert.assertEquals("Taunted $count times", it.getTauntCountText(mockContext))
            }
        }
    }

    @Test
    fun normalColorTest() {
        testEnemyEntry { Assert.assertEquals(R.color.enemyRed, it.getBackgroundColor(mockContext)) }
    }

    @Test
    fun duplicitousColorTest() {
        testEnemyEntry {
            it.captainStatus = EnemyCaptainStatus.DUPLICITOUS
            Assert.assertEquals(R.color.duplicitousOrange, it.getBackgroundColor(mockContext))
        }
    }

    @Test
    fun surrenderedColorTest() {
        testEnemyEntry {
            it.enemy.isSurrendered.value = BoolState.True
            Assert.assertEquals(R.color.surrenderedYellow, it.getBackgroundColor(mockContext))
        }
    }

    private companion object {
        const val CANNOT_TAUNT = "Cannot taunt"
        const val TAUNTS_ZERO = "Has not been taunted"
        const val TAUNTS_ONE = "Taunted once"
        const val TAUNTS_TWO = "Taunted twice"

        val TAUNT_COUNT_STRINGS = arrayOf(TAUNTS_ZERO, TAUNTS_ONE, TAUNTS_TWO)

        val contextStrings =
            mapOf(
                R.string.cannot_taunt to CANNOT_TAUNT,
                R.string.taunts_zero to TAUNTS_ZERO,
                R.string.taunts_one to TAUNTS_ONE,
                R.string.taunts_two to TAUNTS_TWO,
            )

        val mockContext by lazy {
            mockk<Context> {
                contextStrings.forEach { (key, string) -> every { getString(key) } returns string }
                every { getString(R.string.taunts_many, *varargAny { nArgs == 1 }) } answers
                    {
                        "Taunted ${lastArg<Array<Any?>>().joinToString()} times"
                    }
            }
        }

        @JvmStatic
        @BeforeClass
        fun mockkColorCompat() {
            mockkStatic(ContextCompat::getColor)
            every { ContextCompat.getColor(any(), any()) } answers { lastArg() }
        }

        fun testEnemyEntry(test: (EnemyEntry) -> Unit) {
            val enemyEntry = EnemyEntry(ArtemisNpc(0, 0L), mockk<Vessel>(), mockk<Faction>())

            test(enemyEntry)

            clearMocks(enemyEntry.vessel, enemyEntry.faction)
        }

        @JvmStatic
        @AfterClass
        fun cleanup() {
            clearMocks(mockContext)
        }
    }
}
