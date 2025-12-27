package artemis.agent.game.status

import android.content.Context
import androidx.annotation.StringRes
import artemis.agent.R
import artemis.agent.util.getDamageReportText
import artemis.agent.util.getShieldText
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.util.version
import com.walkertribe.ian.world.Shields
import io.kotest.engine.names.WithDataTestName
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.triple
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.ints
import io.kotest.property.exhaustive.of
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import kotlin.math.roundToInt

sealed interface StatusInfoTestCase<T : StatusInfo, TC : StatusInfoTestCase<T, TC>> :
    WithDataTestName {
    data object Empty : StatusInfoTestCase<StatusInfo.Empty, Empty> {
        override val context: Context by lazy { mockk<Context>() }

        override val statusInfoGen: Gen<StatusInfo.Empty> = Exhaustive.of(StatusInfo.Empty)

        override val statusInfoEqualsGen: Gen<Pair<StatusInfo.Empty, StatusInfo.Empty>> =
            Exhaustive.of(StatusInfo.Empty to StatusInfo.Empty)

        override fun statusInfoNotEqualsGen(
            other: Empty
        ): Gen<Pair<StatusInfo.Empty, StatusInfo.Empty>>? = null

        override suspend fun testText() {
            StatusInfo.Empty.getString(context).shouldBeEmpty()
        }

        override fun dataTestName(): String = toString()
    }

    sealed class Header(@all:StringRes val stringId: Int, val expectedText: String) :
        StatusInfoTestCase<StatusInfo.Header, Header> {
        data object ShieldsActive : Header(R.string.shields_active, "Shields: active")

        data object ShieldsInactive : Header(R.string.shields_inactive, "Shields: inactive")

        data object Damages : Header(R.string.node_damage, "Damages")

        override val context: Context by lazy {
            mockk<Context> { every { getString(stringId) } returns expectedText }
        }

        override val statusInfoGen: Gen<StatusInfo.Header> =
            Exhaustive.of(StatusInfo.Header(stringId))

        override val statusInfoEqualsGen: Gen<Pair<StatusInfo.Header, StatusInfo.Header>> =
            Exhaustive.of(StatusInfo.Header(stringId) to StatusInfo.Header(stringId))

        override fun statusInfoNotEqualsGen(
            other: Header
        ): Gen<Pair<StatusInfo.Header, StatusInfo.Header>> =
            Exhaustive.of(StatusInfo.Header(stringId) to StatusInfo.Header(other.stringId))

        override suspend fun testText() {
            StatusInfo.Header(stringId).getString(context) shouldBeEqual expectedText
        }

        override fun dataTestName(): String = expectedText
    }

    data object Energy : StatusInfoTestCase<StatusInfo.Energy, Energy> {
        private const val ENERGY = "Energy: "

        override val context: Context by lazy {
            mockk<Context> {
                every { getString(R.string.energy_reserves, *varargAny { nArgs == 1 }) } answers
                    {
                        ENERGY +
                            lastArg<Array<Any?>>().first().toString().toFloatOrNull()?.roundToInt()
                    }
            }
        }

        override val statusInfoGen: Gen<StatusInfo.Energy> = Arb.bind()

        override val statusInfoEqualsGen: Gen<Pair<StatusInfo.Energy, StatusInfo.Energy>> =
            Arb.bind()

        override fun statusInfoNotEqualsGen(
            other: Energy
        ): Gen<Pair<StatusInfo.Energy, StatusInfo.Energy>>? = null

        override suspend fun testText() {
            Exhaustive.ints(0..10000).checkAll { energy ->
                StatusInfo.Energy(energy.toFloat()).getString(context) shouldBeEqual ENERGY + energy
            }
        }

        override fun dataTestName(): String = toString()
    }

    data class Shield(val shieldPosition: ShieldPosition) :
        StatusInfoTestCase<StatusInfo.Shield, Shield> {
        override val context: Context by lazy {
            mockkStatic(::getShieldText)
            every { getShieldText(any(), shieldPosition.stringId, any()) } returns
                shieldPosition.name

            mockk<Context>()
        }

        override val statusInfoGen: Gen<StatusInfo.Shield> =
            Exhaustive.of(StatusInfo.Shield(shieldPosition, mockShields))

        override val statusInfoEqualsGen: Gen<Pair<StatusInfo.Shield, StatusInfo.Shield>> =
            Exhaustive.of(
                StatusInfo.Shield(shieldPosition, mockShields) to
                    StatusInfo.Shield(shieldPosition, mockShields)
            )

        override fun statusInfoNotEqualsGen(
            other: Shield
        ): Gen<Pair<StatusInfo.Shield, StatusInfo.Shield>> =
            Exhaustive.of(
                StatusInfo.Shield(shieldPosition, mockShields) to
                    StatusInfo.Shield(other.shieldPosition, mockShields)
            )

        override suspend fun testText() {
            StatusInfo.Shield(shieldPosition, mockShields).getString(context) shouldBeEqual
                shieldPosition.name
        }

        override fun dataTestName(): String = shieldPosition.name

        private companion object {
            private val mockShields: Shields by lazy { Shields(0L) }
        }
    }

    data class OrdnanceCount(val ordnanceType: OrdnanceType) :
        StatusInfoTestCase<StatusInfo.OrdnanceCount, OrdnanceCount> {
        private val arbData = Arb.triple(Arb.version(), Arb.int(), Arb.int())

        override val context: Context by lazy {
            mockkObject(ordnanceType)
            every { ordnanceType.getLabelFor(any()) } returns ordnanceType.name

            mockk<Context> {
                every { getString(R.string.ordnance_stock, *varargAny { nArgs == 3 }) } answers
                    {
                        val args = lastArg<Array<Any?>>()
                        "${args.first()}: ${args.drop(1).joinToString("/")}"
                    }
            }
        }

        override val statusInfoGen: Gen<StatusInfo.OrdnanceCount> =
            arbData.map { (version, count, max) ->
                StatusInfo.OrdnanceCount(ordnanceType, version, count, max)
            }

        override val statusInfoEqualsGen:
            Gen<Pair<StatusInfo.OrdnanceCount, StatusInfo.OrdnanceCount>> =
            Arb.bind(arbData, arbData) { (v1, count1, max1), (v2, count2, max2) ->
                StatusInfo.OrdnanceCount(ordnanceType, v1, count1, max1) to
                    StatusInfo.OrdnanceCount(ordnanceType, v2, count2, max2)
            }

        override fun statusInfoNotEqualsGen(
            other: OrdnanceCount
        ): Gen<Pair<StatusInfo.OrdnanceCount, StatusInfo.OrdnanceCount>> =
            arbData.map { (version, count, max) ->
                StatusInfo.OrdnanceCount(ordnanceType, version, count, max) to
                    StatusInfo.OrdnanceCount(other.ordnanceType, version, count, max)
            }

        override suspend fun testText() {
            checkAll(Arb.version(), Arb.int(), Arb.int()) { version, count, max ->
                StatusInfo.OrdnanceCount(ordnanceType, version, count, max)
                    .getString(context) shouldBeEqual "${ordnanceType.name}: $count/$max"
            }
        }

        override fun dataTestName(): String = ordnanceType.name
    }

    sealed class Singleseat(@all:StringRes val fighterLabel: Int, val expectedText: String) :
        StatusInfoTestCase<StatusInfo.Singleseat, Singleseat> {
        data object Docked : Singleseat(R.string.single_seat_craft_docked, "docked")

        data object Launched : Singleseat(R.string.single_seat_craft_launched, "launched")

        data object Lost : Singleseat(R.string.single_seat_craft_lost, "lost")

        override val context: Context by lazy {
            mockk<Context> {
                every { getString(fighterLabel, *varargAny { nArgs == 1 }) } answers
                    {
                        "${lastArg<Array<Any?>>().first()} $expectedText"
                    }
            }
        }

        override val statusInfoGen: Gen<StatusInfo.Singleseat> =
            Arb.int().map { count -> StatusInfo.Singleseat(fighterLabel, count) }

        override val statusInfoEqualsGen: Gen<Pair<StatusInfo.Singleseat, StatusInfo.Singleseat>> =
            Arb.bind(Arb.int(), Arb.int()) { count1, count2 ->
                StatusInfo.Singleseat(fighterLabel, count1) to
                    StatusInfo.Singleseat(fighterLabel, count2)
            }

        override fun statusInfoNotEqualsGen(
            other: Singleseat
        ): Gen<Pair<StatusInfo.Singleseat, StatusInfo.Singleseat>> =
            Arb.int().map { count ->
                StatusInfo.Singleseat(fighterLabel, count) to
                    StatusInfo.Singleseat(other.fighterLabel, count)
            }

        override suspend fun testText() {
            Arb.int().checkAll { count ->
                StatusInfo.Singleseat(fighterLabel, count).getString(context) shouldBeEqual
                    "$count $expectedText"
            }
        }

        override fun dataTestName(): String = toString()
    }

    data object DamageReport : StatusInfoTestCase<StatusInfo.DamageReport, DamageReport> {
        private val arbData = Arb.triple(Arb.int(), Arb.int(), Arb.double())

        override val context: Context by lazy {
            mockkStatic(::getDamageReportText)
            every { getDamageReportText(any(), any(), any(), any(), any()) } answers
                {
                    "${arg<String>(1)}: ${arg<Int>(3)}/${arg<Int>(2)} (${arg<Double>(4).toInt()}%)"
                }

            mockk<Context>()
        }

        override val statusInfoGen: Gen<StatusInfo.DamageReport> = Arb.bind()

        override val statusInfoEqualsGen:
            Gen<Pair<StatusInfo.DamageReport, StatusInfo.DamageReport>> =
            Arb.bind(Arb.string(), arbData, arbData) {
                systemLabel,
                (nodes1, damages1, percent1),
                (nodes2, damages2, percent2) ->
                StatusInfo.DamageReport(systemLabel, nodes1, damages1, percent1) to
                    StatusInfo.DamageReport(systemLabel, nodes2, damages2, percent2)
            }

        override fun statusInfoNotEqualsGen(
            other: DamageReport
        ): Gen<Pair<StatusInfo.DamageReport, StatusInfo.DamageReport>> =
            Arb.bind(Arb.pair(Arb.string(), Arb.string()).filter { (a, b) -> a != b }, arbData) {
                (label1, label2),
                (nodeCount, damageCount, percent) ->
                StatusInfo.DamageReport(label1, nodeCount, damageCount, percent) to
                    StatusInfo.DamageReport(label2, nodeCount, damageCount, percent)
            }

        override suspend fun testText() {
            checkAll(Arb.string(), Arb.int(), Arb.int(), Arb.int()) {
                systemLabel,
                nodeCount,
                damageCount,
                percentage ->
                StatusInfo.DamageReport(systemLabel, nodeCount, damageCount, percentage.toDouble())
                    .getString(context) shouldBeEqual
                    "$systemLabel: $damageCount/$nodeCount ($percentage%)"
            }
        }

        override fun dataTestName(): String = toString()
    }

    val context: Context

    val statusInfoGen: Gen<T>

    val statusInfoEqualsGen: Gen<Pair<T, T>>

    fun statusInfoNotEqualsGen(other: TC): Gen<Pair<T, T>>?

    suspend fun testText()
}
