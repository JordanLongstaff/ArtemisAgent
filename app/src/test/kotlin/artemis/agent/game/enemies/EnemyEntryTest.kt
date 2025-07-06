package artemis.agent.game.enemies

import android.content.Context
import androidx.core.content.ContextCompat
import artemis.agent.R
import com.walkertribe.ian.util.BoolState
import com.walkertribe.ian.vesseldata.Faction
import com.walkertribe.ian.vesseldata.TestVessel
import com.walkertribe.ian.vesseldata.Vessel
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.vesseldata.vesselData
import com.walkertribe.ian.world.ArtemisNpc
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.floats.shouldBeZero
import io.kotest.matchers.ints.shouldBeZero
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.property.Arb
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic

class EnemyEntryTest :
    DescribeSpec({
        val cannotTaunt = "Cannot taunt"
        val tauntCountStrings =
            arrayOf(
                R.string.taunts_zero to "Has not been taunted",
                R.string.taunts_one to "Taunted once",
                R.string.taunts_two to "Taunted twice",
            )

        val contextStrings = tauntCountStrings.toMap() + (R.string.cannot_taunt to cannotTaunt)

        val mockContext =
            mockk<Context> {
                contextStrings.forEach { (key, string) -> every { getString(key) } returns string }
                every { getString(R.string.taunts_many, *varargAny { nArgs == 1 }) } answers
                    {
                        "Taunted ${lastArg<Array<Any?>>().joinToString()} times"
                    }
            }

        mockkStatic(ContextCompat::getColor)
        every { ContextCompat.getColor(any(), any()) } answers { lastArg() }

        afterSpec {
            clearAllMocks()
            unmockkStatic(ContextCompat::getColor)
        }

        fun create(): EnemyEntry =
            EnemyEntry(ArtemisNpc(0, 0L), mockk<Vessel>(), mockk<Faction>(), mockk<VesselData>())

        describe("EnemyEntry") {
            describe("Constructor") {
                val enemy = create()

                it("Heading") { enemy.heading.shouldBeEmpty() }

                it("Range") { enemy.range.shouldBeZero() }

                it("Taunt count") { enemy.tauntCount.shouldBeZero() }

                it("Last taunt") { enemy.lastTaunt.shouldBeNull() }

                it("Intel") { enemy.intel.shouldBeNull() }

                it("Captain status") {
                    enemy.captainStatus shouldBeEqual EnemyCaptainStatus.NO_INTEL
                }
            }

            describe("Taunt count text") {
                it("Three explicit taunt count strings") {
                    EnemyEntry.TAUNT_COUNT_STRINGS shouldHaveSize 3
                }

                it(cannotTaunt) {
                    val enemy = create()
                    enemy.tauntStatuses.fill(TauntStatus.INEFFECTIVE)
                    enemy.getTauntCountText(mockContext) shouldBeEqual cannotTaunt
                }

                tauntCountStrings.forEachIndexed { count, (_, string) ->
                    it(string) {
                        val enemy = create()
                        enemy.tauntCount = count
                        enemy.getTauntCountText(mockContext) shouldBeEqual string
                    }
                }

                it("Taunted <count> times") {
                    val enemy = create()
                    Arb.int(min = 3).checkAll { count ->
                        enemy.tauntCount = count
                        enemy.getTauntCountText(mockContext) shouldBeEqual "Taunted $count times"
                    }
                }
            }

            describe("Color") {
                val enemy = create()

                it("Normal: red") {
                    enemy.getBackgroundColor(mockContext) shouldBeEqual R.color.enemyRed
                }

                it("Surrendered: yellow") {
                    enemy.enemy.isSurrendered.value = BoolState.True
                    enemy.getBackgroundColor(mockContext) shouldBeEqual R.color.surrenderedYellow
                }

                it("Duplicitous: orange") {
                    enemy.captainStatus = EnemyCaptainStatus.DUPLICITOUS
                    enemy.getBackgroundColor(mockContext) shouldBeEqual R.color.duplicitousOrange
                }
            }

            it("Full name") {
                checkAll(
                    TestVessel.arbitrary().flatMap { vessel ->
                        Arb.pair(
                            Arb.vesselData(vessels = Arb.of(vessel), numVessels = 1..1),
                            Arb.of(vessel.id),
                        )
                    },
                    Arb.string(),
                ) { (vesselData, hullId), name ->
                    val npc = ArtemisNpc(0, 0L)
                    npc.name.value = name
                    npc.hullId.value = hullId
                    val vessel = npc.getVessel(vesselData).shouldNotBeNull()
                    val faction = vessel.getFaction(vesselData).shouldNotBeNull()
                    val enemy = EnemyEntry(npc, vessel, faction, vesselData)
                    enemy.fullName shouldBeEqual "$name ${faction.name} ${vessel.name}"
                }
            }
        }
    })
