package com.walkertribe.ian.vesseldata

import com.walkertribe.ian.enums.OrdnanceType
import io.kotest.datatest.WithDataTestName
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.floats.shouldBeWithinPercentageOf
import io.kotest.matchers.floats.shouldBeZero
import io.kotest.matchers.ints.shouldBeZero
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.Codepoint
import io.kotest.property.arbitrary.az
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import korlibs.io.serialization.xml.Xml

@Suppress("LongParameterList")
class TestVessel(
    val id: Int,
    private val side: Int,
    private val faction: TestFaction,
    private val vesselName: String,
    private val broadType: String,
    private val productionCoefficient: Float?,
    private val ordnanceCounts: List<Int?>,
    private val bayCount: Int?,
    private val expectedAttributes: Set<String>,
    private val description: String? = null,
) : WithDataTestName {
    fun build(): Vessel =
        Vessel(
            id = id,
            side = side,
            name = vesselName,
            broadType = broadType,
            ordnanceStorage =
                ordnanceCounts
                    .mapIndexedNotNull { index, count ->
                        count?.let { OrdnanceType.entries[index] to it }
                    }
                    .toMap(),
            productionCoefficient = productionCoefficient ?: 0f,
            bayCount = bayCount ?: 0,
            description = description,
        )

    suspend fun test(vessel: Vessel?, vesselData: VesselData?) {
        vessel.shouldNotBeNull()
        vessel.name shouldBeEqual vesselName
        vessel.side shouldBeEqual side

        val coefficient = productionCoefficient
        if (coefficient == null) vessel.productionCoefficient.shouldBeZero()
        else vessel.productionCoefficient.shouldBeWithinPercentageOf(coefficient, EPSILON)

        val bays = bayCount
        if (bays == null) {
            vessel.bayCount.shouldBeZero()
        } else {
            vessel.bayCount shouldBeEqual bays
        }

        vessel.ordnanceStorage.toSortedMap().values.toList() shouldContainExactly
            ordnanceCounts.filterNotNull()
        vessel.attributes shouldContainExactly expectedAttributes

        val desc = description
        if (desc == null) vessel.description.shouldBeNull()
        else vessel.description.shouldNotBeNull() shouldBeEqual desc.replace('^', '\n')

        expectedAttributes.forEach { vessel[it].shouldBeTrue() }

        Arb.string().filterNot(expectedAttributes::contains).checkAll { vessel[it].shouldBeFalse() }

        if (vesselData != null) {
            vessel.getFaction(vesselData).shouldNotBeNull().id shouldBeEqual faction.ordinal
        }
    }

    fun serialize(): Xml =
        Xml(
            "vessel",
            "uniqueID" to id,
            "side" to side,
            "classname" to vesselName,
            "broadType" to broadType,
        ) {
            productionCoefficient?.let { node("production", "coeff" to it) }

            OrdnanceType.entries.forEachIndexed { index, ordnanceType ->
                val count = ordnanceCounts[index] ?: return@forEachIndexed
                node("torpedo_storage", "type" to ordnanceType.code, "amount" to count)
            }

            description?.let { text ->
                if (text.isBlank()) node("long_desc") else node("long_desc", "text" to text)
            }

            bayCount?.let { node("carrierload", "baycount" to it) }
        }

    override fun dataTestName(): String = "$vesselName #$id"

    override fun hashCode(): Int = id

    override fun equals(other: Any?): Boolean =
        this === other || (other is TestVessel && id == other.id)

    companion object {
        private const val EPSILON = 0.00000001

        fun arbitrary(arbFaction: Arb<TestFaction> = Arb.enum<TestFaction>()): Arb<TestVessel> {
            val arbXmlString = Arb.string().map { it.replace(Regex("[\"&]"), "") }

            val arbOrdnanceCounts =
                Arb.bind(
                    genA = Arb.int().orNull(),
                    genB = Arb.int().orNull(),
                    genC = Arb.int().orNull(),
                    genD = Arb.int().orNull(),
                    genE = Arb.int().orNull(),
                    genF = Arb.int().orNull(),
                    genG = Arb.int().orNull(),
                    genH = Arb.int().orNull(),
                ) { trp, nuk, mine, emp, shk, bea, pro, tag ->
                    listOf(trp, nuk, mine, emp, shk, bea, pro, tag)
                }

            return Arb.bind(
                genA = Arb.int().filter { it != -1 },
                genB = arbFaction,
                genC = arbXmlString,
                genD = Arb.set(Arb.string(minSize = 1, codepoints = Codepoint.az())),
                genE = Arb.numericFloat().orNull(),
                genF = arbOrdnanceCounts,
                genG = Arb.int().orNull(),
                genH = arbXmlString.orNull(),
            ) { id, faction, name, broadType, prod, ordCounts, bays, desc ->
                TestVessel(
                    id,
                    side = faction.ordinal,
                    faction,
                    vesselName = name,
                    broadType = broadType.joinToString(" "),
                    productionCoefficient = prod,
                    ordnanceCounts = ordCounts,
                    bayCount = bays,
                    expectedAttributes = broadType.map { it.lowercase() }.toSet(),
                    description = desc?.trim(),
                )
            }
        }
    }
}
