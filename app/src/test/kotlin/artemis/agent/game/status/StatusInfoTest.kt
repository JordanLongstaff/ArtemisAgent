package artemis.agent.game.status

import android.content.Context
import artemis.agent.R
import artemis.agent.util.getDamageReportText
import artemis.agent.util.getShieldText
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.util.version
import com.walkertribe.ian.world.Shields
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.triple
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.enum
import io.kotest.property.exhaustive.ints
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlin.math.roundToInt

class StatusInfoTest :
    DescribeSpec({
        describe("StatusInfo") {
            val shieldsActive = "Shields: active"
            val shieldsInactive = "Shields: inactive"
            val damageHeader = "Damages"
            val energyReserves = "Energy: "
            val frontShield = "Front shield"
            val rearShield = "Rear shield"
            val singleSeatDocked = "docked"
            val singleSeatLaunched = "launched"
            val singleSeatLost = "lost"

            val headerStrings =
                listOf(
                    R.string.shields_active to shieldsActive,
                    R.string.shields_inactive to shieldsInactive,
                    R.string.node_damage to damageHeader,
                )

            val singleSeatStrings =
                listOf(
                    R.string.single_seat_craft_docked to singleSeatDocked,
                    R.string.single_seat_craft_launched to singleSeatLaunched,
                    R.string.single_seat_craft_lost to singleSeatLost,
                )

            OrdnanceType.entries.forEach { ordnanceType ->
                mockkObject(ordnanceType)
                every { ordnanceType.getLabelFor(any()) } answers { ordnanceType.name }
            }

            val mockContext =
                mockk<Context> {
                    headerStrings.forEach { (stringRes, text) ->
                        every { getString(stringRes) } returns text
                    }

                    singleSeatStrings.forEach { (stringRes, text) ->
                        every { getString(stringRes, *varargAny { nArgs == 1 }) } answers
                            {
                                "${lastArg<Array<Any?>>().first()} $text"
                            }
                    }

                    every { getString(R.string.energy_reserves, *varargAny { nArgs == 1 }) } answers
                        {
                            energyReserves +
                                lastArg<Array<Any?>>()
                                    .first()
                                    .toString()
                                    .toFloatOrNull()
                                    ?.roundToInt()
                        }

                    every { getString(R.string.ordnance_stock, *varargAny { nArgs == 3 }) } answers
                        {
                            val args = lastArg<Array<Any?>>()
                            "${args.first()}: ${args.drop(1).joinToString("/")}"
                        }
                }

            mockkStatic(::getShieldText, ::getDamageReportText)
            every { getShieldText(any(), R.string.front_shield, any()) } returns frontShield
            every { getShieldText(any(), R.string.rear_shield, any()) } returns rearShield
            val mockShields = Shields(0L)

            every { getDamageReportText(mockContext, any(), any(), any(), any()) } answers
                {
                    "${arg<String>(1)}: ${arg<Int>(3)}/${arg<Int>(2)} (${arg<Double>(4).toInt()}%)"
                }

            afterSpec {
                clearAllMocks()
                unmockkAll()
            }

            describe("Text") {
                it("Empty") { StatusInfo.Empty.getString(mockContext).shouldBeEmpty() }

                describe("Header") {
                    withData(nameFn = { it.second }, headerStrings) { (stringId, expected) ->
                        StatusInfo.Header(stringId).getString(mockContext) shouldBeEqual expected
                    }
                }

                it("Energy") {
                    Exhaustive.ints(0..10000).checkAll { energy ->
                        StatusInfo.Energy(energy.toFloat()).getString(mockContext) shouldBeEqual
                            energyReserves + energy
                    }
                }

                describe("Shield") {
                    val shieldStrings = listOf(frontShield, rearShield)

                    withData(
                        nameFn = { it.first.name },
                        ShieldPosition.entries.zip(shieldStrings),
                    ) { (shield, string) ->
                        StatusInfo.Shield(shield, mockShields).getString(mockContext) shouldBeEqual
                            string
                    }
                }

                describe("OrdnanceCount") {
                    withData(nameFn = { it.name }, OrdnanceType.entries) { ordnanceType ->
                        checkAll(Arb.version(), Arb.int(), Arb.int()) { version, count, max ->
                            StatusInfo.OrdnanceCount(ordnanceType, version, count, max)
                                .getString(mockContext) shouldBeEqual
                                "${ordnanceType.name}: $count/$max"
                        }
                    }
                }

                describe("Singleseat") {
                    withData(nameFn = { it.second }, singleSeatStrings) { (stringId, expected) ->
                        Arb.int().checkAll { count ->
                            StatusInfo.Singleseat(stringId, count)
                                .getString(mockContext) shouldBeEqual "$count $expected"
                        }
                    }
                }

                it("DamageReport") {
                    checkAll(Arb.string(), Arb.int(), Arb.int(), Arb.int()) {
                        systemLabel,
                        nodeCount,
                        damageCount,
                        percentage ->
                        StatusInfo.DamageReport(
                                systemLabel,
                                nodeCount,
                                damageCount,
                                percentage.toDouble(),
                            )
                            .getString(mockContext) shouldBeEqual
                            "$systemLabel: $damageCount/$nodeCount ($percentage%)"
                    }
                }
            }

            describe("Item equals") {
                describe("Empty") {
                    it("Equals Empty") {
                        StatusInfo.Empty.itemEquals(StatusInfo.Empty).shouldBeTrue()
                    }

                    it("Does not equal Header") {
                        headerStrings.forEach { (stringId, _) ->
                            StatusInfo.Empty.itemEquals(StatusInfo.Header(stringId)).shouldBeFalse()
                        }
                    }

                    it("Does not equal Energy") {
                        Arb.bind<StatusInfo.Energy>().checkAll { energy ->
                            StatusInfo.Empty.itemEquals(energy).shouldBeFalse()
                        }
                    }

                    it("Does not equal Shield") {
                        ShieldPosition.entries.forEach { shield ->
                            StatusInfo.Empty.itemEquals(StatusInfo.Shield(shield, mockShields))
                                .shouldBeFalse()
                        }
                    }

                    it("Does not equal OrdnanceCount") {
                        checkAll(
                            Exhaustive.enum<OrdnanceType>(),
                            Arb.version(),
                            Arb.int(),
                            Arb.int(),
                        ) { ordnance, version, count, max ->
                            StatusInfo.Empty.itemEquals(
                                    StatusInfo.OrdnanceCount(ordnance, version, count, max)
                                )
                                .shouldBeFalse()
                        }
                    }

                    it("Does not equal Singleseat") {
                        singleSeatStrings.forEach { (stringId, _) ->
                            Arb.int().checkAll { count ->
                                StatusInfo.Empty.itemEquals(StatusInfo.Singleseat(stringId, count))
                                    .shouldBeFalse()
                            }
                        }
                    }

                    it("Does not equal DamageReport") {
                        Arb.bind<StatusInfo.DamageReport>().checkAll { damages ->
                            StatusInfo.Empty.itemEquals(damages).shouldBeFalse()
                        }
                    }
                }

                describe("Header") {
                    headerStrings.forEachIndexed { i, (id1, name) ->
                        describe(name) {
                            val header1 = StatusInfo.Header(id1)

                            it("Does not equal Empty") {
                                header1.itemEquals(StatusInfo.Empty).shouldBeFalse()
                            }

                            it("Equals Header: $name") {
                                header1.itemEquals(StatusInfo.Header(id1)).shouldBeTrue()
                            }

                            it("Does not equal different Header") {
                                headerStrings.forEachIndexed { j, (id2, _) ->
                                    if (i == j) return@forEachIndexed
                                    header1.itemEquals(StatusInfo.Header(id2)).shouldBeFalse()
                                }
                            }

                            it("Does not equal Energy") {
                                Arb.bind<StatusInfo.Energy>().checkAll { energy ->
                                    header1.itemEquals(energy).shouldBeFalse()
                                }
                            }

                            it("Does not equal Shield") {
                                ShieldPosition.entries.forEach { shield ->
                                    header1
                                        .itemEquals(StatusInfo.Shield(shield, mockShields))
                                        .shouldBeFalse()
                                }
                            }

                            it("Does not equal OrdnanceCount") {
                                checkAll(
                                    Exhaustive.enum<OrdnanceType>(),
                                    Arb.version(),
                                    Arb.int(),
                                    Arb.int(),
                                ) { ordnance, version, count, max ->
                                    header1
                                        .itemEquals(
                                            StatusInfo.OrdnanceCount(ordnance, version, count, max)
                                        )
                                        .shouldBeFalse()
                                }
                            }

                            it("Does not equal Singleseat") {
                                singleSeatStrings.forEach { (stringId, _) ->
                                    Arb.int().checkAll { count ->
                                        header1
                                            .itemEquals(StatusInfo.Singleseat(stringId, count))
                                            .shouldBeFalse()
                                    }
                                }
                            }

                            it("Does not equal DamageReport") {
                                Arb.bind<StatusInfo.DamageReport>().checkAll { damages ->
                                    header1.itemEquals(damages).shouldBeFalse()
                                }
                            }
                        }
                    }
                }

                describe("Energy") {
                    it("Does not equal Empty") {
                        Arb.bind<StatusInfo.Energy>().checkAll { energy ->
                            energy.itemEquals(StatusInfo.Empty).shouldBeFalse()
                        }
                    }

                    it("Does not equal Header") {
                        checkAll(Arb.bind<StatusInfo.Energy>(), Arb.of(headerStrings)) {
                            energy,
                            (header, _) ->
                            energy.itemEquals(StatusInfo.Header(header)).shouldBeFalse()
                        }
                    }

                    it("Equals Energy regardless of value") {
                        checkAll(Arb.bind<StatusInfo.Energy>(), Arb.bind<StatusInfo.Energy>()) {
                            energy1,
                            energy2 ->
                            energy1.itemEquals(energy2).shouldBeTrue()
                        }
                    }

                    it("Does not equal Shield") {
                        ShieldPosition.entries.forEach { shield ->
                            Arb.bind<StatusInfo.Energy>().checkAll { energy ->
                                energy
                                    .itemEquals(StatusInfo.Shield(shield, mockShields))
                                    .shouldBeFalse()
                            }
                        }
                    }

                    it("Does not equal OrdnanceCount") {
                        checkAll(
                            Arb.bind<StatusInfo.Energy>(),
                            Exhaustive.enum<OrdnanceType>(),
                            Arb.version(),
                            Arb.int(),
                            Arb.int(),
                        ) { energy, ordnanceType, version, count, max ->
                            energy
                                .itemEquals(
                                    StatusInfo.OrdnanceCount(ordnanceType, version, count, max)
                                )
                                .shouldBeFalse()
                        }
                    }

                    it("Does not equal Singleseat") {
                        checkAll(
                            Arb.bind<StatusInfo.Energy>(),
                            Arb.of(singleSeatStrings),
                            Arb.int(),
                        ) { energy, (fighterLabel, _), count ->
                            energy
                                .itemEquals(StatusInfo.Singleseat(fighterLabel, count))
                                .shouldBeFalse()
                        }
                    }

                    it("Does not equal DamageReport") {
                        checkAll(
                            Arb.bind<StatusInfo.Energy>(),
                            Arb.bind<StatusInfo.DamageReport>(),
                        ) { energy, damages ->
                            energy.itemEquals(damages).shouldBeFalse()
                        }
                    }
                }

                describe("Shield") {
                    withData(nameFn = { it.name }, ShieldPosition.entries) { shieldPos ->
                        val shield = StatusInfo.Shield(shieldPos, mockShields)

                        it("Does not equal Empty") {
                            shield.itemEquals(StatusInfo.Empty).shouldBeFalse()
                        }

                        it("Does not equal Header") {
                            headerStrings.forEach { (header, _) ->
                                shield.itemEquals(StatusInfo.Header(header)).shouldBeFalse()
                            }
                        }

                        it("Does not equal Energy") {
                            Arb.bind<StatusInfo.Energy>().checkAll { energy ->
                                shield.itemEquals(energy).shouldBeFalse()
                            }
                        }

                        it("Equals Shield: $shieldPos") {
                            shield
                                .itemEquals(StatusInfo.Shield(shieldPos, mockShields))
                                .shouldBeTrue()
                        }

                        val otherShieldPos = ShieldPosition.entries[1 xor shieldPos.ordinal]
                        it("Does not equal Shield: $otherShieldPos") {
                            shield
                                .itemEquals(StatusInfo.Shield(otherShieldPos, mockShields))
                                .shouldBeFalse()
                        }

                        it("Does not equal OrdnanceCount") {
                            checkAll(
                                Exhaustive.enum<OrdnanceType>(),
                                Arb.version(),
                                Arb.int(),
                                Arb.int(),
                            ) { ordnance, version, count, max ->
                                shield
                                    .itemEquals(
                                        StatusInfo.OrdnanceCount(ordnance, version, count, max)
                                    )
                                    .shouldBeFalse()
                            }
                        }

                        it("Does not equal Singleseat") {
                            singleSeatStrings.forEach { (fighterLabel, _) ->
                                Arb.int().checkAll { count ->
                                    shield
                                        .itemEquals(StatusInfo.Singleseat(fighterLabel, count))
                                        .shouldBeFalse()
                                }
                            }
                        }

                        it("Does not equal DamageReport") {
                            Arb.bind<StatusInfo.DamageReport>().checkAll { damages ->
                                shield.itemEquals(damages).shouldBeFalse()
                            }
                        }
                    }
                }

                describe("OrdnanceCount") {
                    withData(nameFn = { it.name }, OrdnanceType.entries) { ordnance ->
                        val arbData = Arb.triple(Arb.version(), Arb.int(), Arb.int())

                        it("Does not equal Empty") {
                            arbData.checkAll { (version, count, max) ->
                                StatusInfo.OrdnanceCount(ordnance, version, count, max)
                                    .itemEquals(StatusInfo.Empty)
                                    .shouldBeFalse()
                            }
                        }

                        it("Does not equal Header") {
                            headerStrings.forEach { (header, _) ->
                                arbData.checkAll { (version, count, max) ->
                                    StatusInfo.OrdnanceCount(ordnance, version, count, max)
                                        .itemEquals(StatusInfo.Header(header))
                                        .shouldBeFalse()
                                }
                            }
                        }

                        it("Does not equal Energy") {
                            checkAll(Arb.bind<StatusInfo.Energy>(), arbData) {
                                energy,
                                (version, count, max) ->
                                StatusInfo.OrdnanceCount(ordnance, version, count, max)
                                    .itemEquals(energy)
                                    .shouldBeFalse()
                            }
                        }

                        it("Does not equal Shield") {
                            checkAll(arbData, Exhaustive.enum<ShieldPosition>()) {
                                (version, count, max),
                                shield ->
                                StatusInfo.OrdnanceCount(ordnance, version, count, max)
                                    .itemEquals(StatusInfo.Shield(shield, mockShields))
                                    .shouldBeFalse()
                            }
                        }

                        it("Equals OrdnanceCount: $ordnance") {
                            checkAll(arbData, arbData) { (v1, count1, max1), (v2, count2, max2) ->
                                StatusInfo.OrdnanceCount(ordnance, v1, count1, max1)
                                    .itemEquals(
                                        StatusInfo.OrdnanceCount(ordnance, v2, count2, max2)
                                    )
                                    .shouldBeTrue()
                            }
                        }

                        describe("Does not equal different OrdnanceCount") {
                            OrdnanceType.entries.forEach { otherOrdnance ->
                                if (otherOrdnance == ordnance) return@forEach

                                it(otherOrdnance.name) {
                                    arbData.checkAll { (version, count, max) ->
                                        StatusInfo.OrdnanceCount(ordnance, version, count, max)
                                            .itemEquals(
                                                StatusInfo.OrdnanceCount(
                                                    otherOrdnance,
                                                    version,
                                                    count,
                                                    max,
                                                )
                                            )
                                            .shouldBeFalse()
                                    }
                                }
                            }
                        }

                        it("Does not equal Singleseat") {
                            singleSeatStrings.forEach { (fighterLabel, _) ->
                                checkAll(arbData, Arb.int()) { (version, count, max), fighterCount
                                    ->
                                    StatusInfo.OrdnanceCount(ordnance, version, count, max)
                                        .itemEquals(
                                            StatusInfo.Singleseat(fighterLabel, fighterCount)
                                        )
                                        .shouldBeFalse()
                                }
                            }
                        }

                        it("Does not equal DamageReport") {
                            checkAll(arbData, Arb.bind<StatusInfo.DamageReport>()) {
                                (version, count, max),
                                damages ->
                                StatusInfo.OrdnanceCount(ordnance, version, count, max)
                                    .itemEquals(damages)
                                    .shouldBeFalse()
                            }
                        }
                    }
                }

                describe("Singleseat") {
                    singleSeatStrings.forEachIndexed { i, (id1, name) ->
                        describe(name) {
                            it("Does not equal Empty") {
                                Arb.int().checkAll { count ->
                                    StatusInfo.Singleseat(id1, count)
                                        .itemEquals(StatusInfo.Empty)
                                        .shouldBeFalse()
                                }
                            }

                            it("Does not equal Header") {
                                headerStrings.forEach { (header, _) ->
                                    Arb.int().checkAll { count ->
                                        StatusInfo.Singleseat(id1, count)
                                            .itemEquals(StatusInfo.Header(header))
                                            .shouldBeFalse()
                                    }
                                }
                            }

                            it("Does not equal Energy") {
                                checkAll(Arb.bind<StatusInfo.Energy>(), Arb.int()) { energy, count
                                    ->
                                    StatusInfo.Singleseat(id1, count)
                                        .itemEquals(energy)
                                        .shouldBeFalse()
                                }
                            }

                            it("Does not equal Shield") {
                                ShieldPosition.entries.forEach { shield ->
                                    Arb.int().checkAll { count ->
                                        StatusInfo.Singleseat(id1, count)
                                            .itemEquals(StatusInfo.Shield(shield, mockShields))
                                            .shouldBeFalse()
                                    }
                                }
                            }

                            it("Does not equal OrdnanceCount") {
                                checkAll(
                                    Arb.int(),
                                    Exhaustive.enum<OrdnanceType>(),
                                    Arb.version(),
                                    Arb.int(),
                                    Arb.int(),
                                ) { fighterCount, ordnance, version, ordCount, max ->
                                    StatusInfo.Singleseat(id1, fighterCount)
                                        .itemEquals(
                                            StatusInfo.OrdnanceCount(
                                                ordnance,
                                                version,
                                                ordCount,
                                                max,
                                            )
                                        )
                                        .shouldBeFalse()
                                }
                            }

                            it("Equals Singleseat: $name") {
                                checkAll(Arb.int(), Arb.int()) { count1, count2 ->
                                    StatusInfo.Singleseat(id1, count1)
                                        .itemEquals(StatusInfo.Singleseat(id1, count2))
                                        .shouldBeTrue()
                                }
                            }

                            it("Does not equal different Singleseat") {
                                singleSeatStrings.forEachIndexed { j, (id2, _) ->
                                    if (i == j) return@forEachIndexed
                                    Arb.int().checkAll { count ->
                                        StatusInfo.Singleseat(id1, count)
                                            .itemEquals(StatusInfo.Singleseat(id2, count))
                                            .shouldBeFalse()
                                    }
                                }
                            }

                            it("Does not equal DamageReport") {
                                checkAll(Arb.bind<StatusInfo.DamageReport>(), Arb.int()) {
                                    damages,
                                    count ->
                                    StatusInfo.Singleseat(id1, count)
                                        .itemEquals(damages)
                                        .shouldBeFalse()
                                }
                            }
                        }
                    }
                }

                describe("DamageReport") {
                    it("Does not equal Empty") {
                        Arb.bind<StatusInfo.DamageReport>().checkAll { damages ->
                            damages.itemEquals(StatusInfo.Empty).shouldBeFalse()
                        }
                    }

                    it("Does not equal Header") {
                        checkAll(Arb.bind<StatusInfo.DamageReport>(), Arb.of(headerStrings)) {
                            damages,
                            (header, _) ->
                            damages.itemEquals(StatusInfo.Header(header)).shouldBeFalse()
                        }
                    }

                    it("Does not equal Energy") {
                        checkAll(
                            Arb.bind<StatusInfo.DamageReport>(),
                            Arb.bind<StatusInfo.Energy>(),
                        ) { damages, energy ->
                            damages.itemEquals(energy).shouldBeFalse()
                        }
                    }

                    it("Does not equal Shield") {
                        ShieldPosition.entries.forEach { shield ->
                            Arb.bind<StatusInfo.DamageReport>().checkAll { damages ->
                                damages
                                    .itemEquals(StatusInfo.Shield(shield, mockShields))
                                    .shouldBeFalse()
                            }
                        }
                    }

                    it("Does not equal OrdnanceCount") {
                        checkAll(
                            Arb.bind<StatusInfo.DamageReport>(),
                            Exhaustive.enum<OrdnanceType>(),
                            Arb.version(),
                            Arb.int(),
                            Arb.int(),
                        ) { damages, ordnanceType, version, count, max ->
                            damages
                                .itemEquals(
                                    StatusInfo.OrdnanceCount(ordnanceType, version, count, max)
                                )
                                .shouldBeFalse()
                        }
                    }

                    it("Does not equal Singleseat") {
                        checkAll(
                            Arb.bind<StatusInfo.DamageReport>(),
                            Arb.of(singleSeatStrings),
                            Arb.int(),
                        ) { damages, (fighterLabel, _), count ->
                            damages
                                .itemEquals(StatusInfo.Singleseat(fighterLabel, count))
                                .shouldBeFalse()
                        }
                    }

                    val arbData = Arb.triple(Arb.int(), Arb.int(), Arb.double())

                    it("Equals DamageReport with same system label") {
                        checkAll(Arb.string(), arbData, arbData) {
                            systemLabel,
                            (nodes1, damages1, percent1),
                            (nodes2, damages2, percent2) ->
                            StatusInfo.DamageReport(systemLabel, nodes1, damages1, percent1)
                                .itemEquals(
                                    StatusInfo.DamageReport(systemLabel, nodes2, damages2, percent2)
                                )
                                .shouldBeTrue()
                        }
                    }

                    it("Does not equal DamageReport with different label") {
                        checkAll(
                            Arb.pair(Arb.string(), Arb.string()).filter { it.first != it.second },
                            arbData,
                        ) { (label1, label2), (nodeCount, damageCount, percent) ->
                            StatusInfo.DamageReport(label1, nodeCount, damageCount, percent)
                                .itemEquals(
                                    StatusInfo.DamageReport(label2, nodeCount, damageCount, percent)
                                )
                                .shouldBeFalse()
                        }
                    }
                }
            }
        }
    })
