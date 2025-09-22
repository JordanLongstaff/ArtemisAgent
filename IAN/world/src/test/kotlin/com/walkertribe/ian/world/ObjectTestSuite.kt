package com.walkertribe.ian.world

import com.walkertribe.ian.enums.AlertStatus
import com.walkertribe.ian.enums.DriveType
import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.enums.TubeState
import com.walkertribe.ian.util.BoolState
import com.walkertribe.ian.util.boolState
import com.walkertribe.ian.util.shouldBeFalse
import com.walkertribe.ian.vesseldata.Empty
import com.walkertribe.ian.vesseldata.TestVessel
import com.walkertribe.ian.vesseldata.Vessel
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.vesseldata.vesselData
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrowUnit
import io.kotest.core.factory.TestFactory
import io.kotest.core.spec.style.describeSpec
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.engine.names.WithDataTestName
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.triple
import io.kotest.property.checkAll
import io.mockk.called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KMutableProperty0
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal sealed class ObjectTestSuite<T : BaseArtemisObject<T>>(
    protected val objectType: ObjectType
) {
    abstract val arbObject: Arb<T>
    abstract val arbObjectPair: Arb<Pair<T, T>>
    protected abstract val partialUpdateTestSuites:
        List<PartialUpdateTestSuite<T, out BaseArtemisObject.Dsl<T>, *, *>>

    abstract suspend fun testCreateUnknown()

    abstract suspend fun testCreateFromDsl()

    abstract suspend fun testCreateAndUpdateManually()

    abstract suspend fun testCreateAndUpdateFromDsl()

    abstract suspend fun testUnknownObjectDoesNotProvideUpdates()

    abstract suspend fun testKnownObjectProvidesUpdates()

    abstract suspend fun testDslCannotUpdateKnownObject()

    fun tests(): TestFactory = describeSpec {
        describe("Artemis${this@ObjectTestSuite.javaClass.simpleName}") {
            it("Can create with no data") { testCreateUnknown() }

            it("Can create using Dsl instance") { testCreateFromDsl() }

            it("Can populate properties manually") { testCreateAndUpdateManually() }

            describe("Can apply partial updates") { describeTestCreateAndUpdatePartially(this) }

            it("Can populate properties using Dsl instance") { testCreateAndUpdateFromDsl() }

            it("Unpopulated properties do not provide updates to another object") {
                testUnknownObjectDoesNotProvideUpdates()
            }

            it("Populated properties provide updates to another object") {
                testKnownObjectProvidesUpdates()
            }

            it("Dsl object cannot populate a non-empty object") { testDslCannotUpdateKnownObject() }

            it("Can offer to listener modules") {
                val iterations = PropertyTesting.defaultIterationCount
                val objects = Arb.list(arbObject, iterations..iterations).next()
                objects.forEach { it.offerTo(ArtemisObjectTestModule) }
                ArtemisObjectTestModule.collected shouldContainExactly objects
            }

            describeTestEquality()
            describeTestHashCode()
            describeMore()

            ArtemisObjectTestModule.collected.clear()
        }
    }

    open suspend fun describeTestCreateAndUpdatePartially(scope: DescribeSpecContainerScope) {
        partialUpdateTestSuites.forEachIndexed { i, testSuite ->
            scope.it(testSuite.name) {
                testSuite.testPartiallyUpdatedObject { base ->
                    partialUpdateTestSuites.forEachIndexed { j, testSuite2 ->
                        testSuite2.getProperty(base).hasValue.shouldBeEqual(i == j)
                    }
                }
            }
        }
    }

    private fun DescribeSpecContainerScope.describeTestEquality() = launch {
        describe("Equality") {
            it("Equals itself") { arbObject.checkAll { it shouldBeEqual it } }

            it("Equal type and ID") {
                arbObjectPair.checkAll { (obj1, obj2) -> obj1 shouldBeEqual obj2 }
            }

            it("Different ID") {
                arbObject.checkAll { obj ->
                    val mockObj =
                        mockk<ArtemisObject<*>> {
                            every { id } returns obj.id.inv()
                            every { type } returns obj.type
                        }
                    obj shouldNotBeEqual mockObj
                    clearMocks(mockObj)
                }
            }

            describe("Different type") {
                withData(
                    nameFn = { "Artemis${it.javaClass.simpleName}" },
                    listOf(Base, BlackHole, Creature, Mine, Npc, Player).filter {
                        it.objectType != objectType
                    },
                ) { other ->
                    arbObject.checkAll { obj ->
                        val mockObj =
                            mockk<ArtemisObject<*>> {
                                every { id } returns obj.id
                                every { type } returns other.objectType
                            }
                        obj shouldNotBeEqual mockObj
                        clearMocks(mockObj)
                    }
                }
            }
        }
    }

    private fun DescribeSpecContainerScope.describeTestHashCode() = launch {
        describe("Hash code") {
            it("Equals ID") { arbObject.checkAll { it.hashCode() shouldBeEqual it.id } }

            it("Equal ID, equal hash code") {
                arbObjectPair.checkAll { (obj1, obj2) ->
                    obj1.hashCode() shouldBeEqual obj2.hashCode()
                }
            }

            it("Different ID, different hash code") {
                arbObject.checkAll { obj ->
                    val mockObj = mockk<BaseArtemisObject<*>> { every { id } returns obj.id.inv() }
                    obj.hashCode() shouldNotBeEqual mockObj.hashCode()
                    clearMocks(mockObj)
                }
            }
        }
    }

    open fun DescribeSpecContainerScope.describeMore(): Job? = null

    data class ShieldStrength(val strength: Float, val maxStrength: Float)

    data class Location(val x: Float, val y: Float, val z: Float)

    companion object {
        val X = Arb.numericFloat()
        val Y = Arb.numericFloat()
        val Z = Arb.numericFloat()
        val SHIELDS_STRENGTH = Arb.numericFloat()
        val SHIELDS_MAX = Arb.numericFloat()
        val SHIELDS = Arb.bind(SHIELDS_STRENGTH, SHIELDS_MAX, ::ShieldStrength)
        val LOCATION = Arb.bind(genA = X, genB = Y, genC = Z, bindFn = ::Location)

        fun DescribeSpecContainerScope.describeVesselDataTests(
            arbObject: Gen<BaseArtemisShielded<*>>,
            arbHullId: Gen<Int>,
        ) = launch {
            describe("Vessel") {
                val mockVesselData =
                    mockk<VesselData> { every { this@mockk[any()] } returns mockk<Vessel>() }

                it("Null if object has no hull ID") {
                    arbObject.checkAll { obj ->
                        obj.getVessel(mockVesselData).shouldBeNull()
                        verify { mockVesselData wasNot called }
                    }
                }

                clearMocks(mockVesselData)

                it("Null if not found in vessel data") {
                    checkAll(arbObject, arbHullId) { obj, hullId ->
                        obj.hullId.value = hullId
                        obj.getVessel(VesselData.Empty).shouldBeNull()
                    }
                }

                it("Retrieved from vessel data if found") {
                    checkAll(
                        TestVessel.arbitrary().flatMap { vessel ->
                            Arb.pair(
                                Arb.vesselData(
                                    factions = emptyList(),
                                    vessels = Arb.of(vessel),
                                    numVessels = 1..1,
                                ),
                                Arb.of(vessel.id),
                            )
                        },
                        arbObject,
                    ) { (vesselData, hullId), obj ->
                        obj.hullId.value = hullId
                        obj.getVessel(vesselData).shouldNotBeNull()
                    }
                }
            }
        }

        fun DescribeSpecContainerScope.describeFullNameTests(
            arbObject: Gen<BaseArtemisShielded<*>>,
            arbName: Gen<String>,
        ) = launch {
            describe("Full name") {
                describe("Unnamed object") {
                    it("No vessel") {
                        arbObject.checkAll { it.getFullName(VesselData.Empty).shouldBeEmpty() }
                    }

                    it("Vessel found") {
                        checkAll(
                            TestVessel.arbitrary().flatMap { vessel ->
                                Arb.pair(
                                    Arb.vesselData(vessels = Arb.of(vessel), numVessels = 1..1),
                                    Arb.of(vessel.id),
                                )
                            },
                            arbObject,
                        ) { (vesselData, hullId), obj ->
                            obj.hullId.value = hullId
                            val vessel = obj.getVessel(vesselData).shouldNotBeNull()
                            val faction = vessel.getFaction(vesselData).shouldNotBeNull()
                            obj.getFullName(vesselData) shouldBe "${faction.name} ${vessel.name}"
                        }
                    }
                }

                describe("Named object") {
                    it("No vessel") {
                        checkAll(arbName, arbObject) { name, obj ->
                            obj.name.value = name
                            obj.getFullName(VesselData.Empty) shouldBe name
                        }
                    }

                    it("Vessel found") {
                        checkAll(
                            arbName,
                            TestVessel.arbitrary().flatMap { vessel ->
                                Arb.pair(
                                    Arb.vesselData(vessels = Arb.of(vessel), numVessels = 1..1),
                                    Arb.of(vessel.id),
                                )
                            },
                            arbObject,
                        ) { name, (vesselData, hullId), obj ->
                            obj.hullId.value = hullId
                            obj.name.value = name
                            val vessel = obj.getVessel(vesselData).shouldNotBeNull()
                            val faction = vessel.getFaction(vesselData).shouldNotBeNull()
                            obj.getFullName(vesselData) shouldBe
                                "$name ${faction.name} ${vessel.name}"
                        }
                    }
                }
            }
        }
    }

    protected interface BaseProperties<T : ArtemisObject<T>> {
        val location: Location

        fun updateDirectly(obj: T) {
            obj.x.value = location.x
            obj.y.value = location.y
            obj.z.value = location.z
        }

        fun createThroughDsl(id: Int, timestamp: Long): T

        fun updateThroughDsl(obj: T)

        fun testKnownObject(obj: T)
    }

    protected data class PartialUpdateTestSuite<
        AO : BaseArtemisObject<AO>,
        DSL : BaseArtemisObject.Dsl<AO>,
        V,
        P : Property<V, P>,
    >(
        val name: String,
        val objectGen: Gen<AO>,
        val propGen: Gen<V>,
        val dslProperty: KMutableProperty0<V>,
        val dsl: DSL,
        val getProperty: (AO) -> P,
    ) {
        suspend fun testPartiallyUpdatedObject(test: (AO) -> Unit) {
            checkAll(objectGen, propGen) { obj, value ->
                dslProperty.set(value)
                dsl updates obj
                obj.hasData.shouldBeTrue()
                test(obj)
            }
        }
    }

    data object Base : ObjectTestSuite<ArtemisBase>(ObjectType.BASE) {
        private val NAME = Arb.string()
        private val HULL_ID = Arb.int().filter { it != -1 }

        private data class Properties(
            private val name: String,
            private val shields: ShieldStrength,
            private val hullId: Int,
            override val location: Location,
        ) : BaseProperties<ArtemisBase> {
            override fun updateDirectly(obj: ArtemisBase) {
                super.updateDirectly(obj)
                obj.name.value = name
                obj.shieldsFront.strength.value = shields.strength
                obj.shieldsFront.maxStrength.value = shields.maxStrength
                obj.hullId.value = hullId
            }

            override fun createThroughDsl(id: Int, timestamp: Long): ArtemisBase =
                ArtemisBase.Dsl.let { dsl ->
                    dsl.name = name
                    dsl.shieldsFront = shields.strength
                    dsl.shieldsFrontMax = shields.maxStrength
                    dsl.hullId = hullId
                    dsl.x = location.x
                    dsl.y = location.y
                    dsl.z = location.z

                    dsl.build(id, timestamp).apply { dsl.shouldBeReset() }
                }

            override fun updateThroughDsl(obj: ArtemisBase) {
                ArtemisBase.Dsl.also { dsl ->
                        dsl.name = name
                        dsl.shieldsFront = shields.strength
                        dsl.shieldsFrontMax = shields.maxStrength
                        dsl.hullId = hullId
                        dsl.x = location.x
                        dsl.y = location.y
                        dsl.z = location.z

                        dsl updates obj
                    }
                    .shouldBeReset()
            }

            override fun testKnownObject(obj: ArtemisBase) {
                obj.shouldBeKnownObject(
                    id = obj.id,
                    type = objectType,
                    name,
                    x = location.x,
                    y = location.y,
                    z = location.z,
                    hullId,
                    shieldsFront = shields.strength,
                    shieldsFrontMax = shields.maxStrength,
                )
            }
        }

        override val arbObject: Arb<ArtemisBase> = Arb.bind()
        override val arbObjectPair: Arb<Pair<ArtemisBase, ArtemisBase>> =
            Arb.bind(Arb.int(), Arb.long(), Arb.long()) { id, timestampA, timestampB ->
                Pair(
                    ArtemisBase(id, min(timestampA, timestampB)),
                    ArtemisBase(id, max(timestampA, timestampB)),
                )
            }

        override val partialUpdateTestSuites =
            listOf(
                partialUpdateTest(
                    name = "Name",
                    propGen = NAME,
                    dslProperty = ArtemisBase.Dsl::name,
                ) {
                    it.name
                },
                partialUpdateTest(
                    name = "Shields",
                    propGen = SHIELDS_STRENGTH,
                    dslProperty = ArtemisBase.Dsl::shieldsFront,
                ) {
                    it.shieldsFront.strength
                },
                partialUpdateTest(
                    name = "Shields max",
                    propGen = SHIELDS_MAX,
                    dslProperty = ArtemisBase.Dsl::shieldsFrontMax,
                ) {
                    it.shieldsFront.maxStrength
                },
                partialUpdateTest(
                    name = "Hull ID",
                    propGen = HULL_ID,
                    dslProperty = ArtemisBase.Dsl::hullId,
                ) {
                    it.hullId
                },
                partialUpdateTest(name = "X", propGen = X, dslProperty = ArtemisBase.Dsl::x) {
                    it.x
                },
                partialUpdateTest(name = "Y", propGen = Y, dslProperty = ArtemisBase.Dsl::y) {
                    it.y
                },
                partialUpdateTest(name = "Z", propGen = Z, dslProperty = ArtemisBase.Dsl::z) {
                    it.z
                },
            )

        override suspend fun testCreateUnknown() {
            arbObject.checkAll { it.shouldBeUnknownObject(it.id, objectType) }
        }

        override suspend fun testCreateFromDsl() {
            checkAll(
                Arb.int(),
                Arb.long(),
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS,
                    genC = HULL_ID,
                    genD = LOCATION,
                    bindFn = ::Properties,
                ),
            ) { id, timestamp, test ->
                shouldNotThrow<IllegalStateException> {
                    test.testKnownObject(test.createThroughDsl(id, timestamp))
                }
            }
        }

        override suspend fun testCreateAndUpdateManually() {
            checkAll(
                arbObject,
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS,
                    genC = HULL_ID,
                    genD = LOCATION,
                    bindFn = ::Properties,
                ),
            ) { base, test ->
                test.updateDirectly(base)
                test.testKnownObject(base)
            }
        }

        override suspend fun testCreateAndUpdateFromDsl() {
            checkAll(
                arbObject,
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS,
                    genC = HULL_ID,
                    genD = LOCATION,
                    bindFn = ::Properties,
                ),
            ) { base, test ->
                test.updateThroughDsl(base)
                test.testKnownObject(base)
            }
        }

        override suspend fun testUnknownObjectDoesNotProvideUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS,
                    genC = HULL_ID,
                    genD = LOCATION,
                    bindFn = ::Properties,
                ),
            ) { (oldBase, newBase), test ->
                test.updateDirectly(oldBase)
                newBase updates oldBase
                test.testKnownObject(oldBase)
            }
        }

        override suspend fun testKnownObjectProvidesUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS,
                    genC = HULL_ID,
                    genD = LOCATION,
                    bindFn = ::Properties,
                ),
            ) { (oldBase, newBase), test ->
                test.updateDirectly(newBase)
                newBase updates oldBase
                test.testKnownObject(oldBase)
            }
        }

        override suspend fun testDslCannotUpdateKnownObject() {
            checkAll(
                arbObject,
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS,
                    genC = HULL_ID,
                    genD = LOCATION,
                    bindFn = ::Properties,
                ),
            ) { base, test ->
                test.updateDirectly(base)
                shouldThrowUnit<IllegalArgumentException> { test.updateThroughDsl(base) }
            }
        }

        override fun DescribeSpecContainerScope.describeMore() = launch {
            describeVesselDataTests(arbObject, HULL_ID)
            describeFullNameTests(arbObject, NAME)
        }

        private fun <V, P : Property<V, P>> partialUpdateTest(
            name: String,
            propGen: Gen<V>,
            dslProperty: KMutableProperty0<V>,
            getProperty: (ArtemisBase) -> P,
        ): PartialUpdateTestSuite<ArtemisBase, ArtemisBase.Dsl, V, P> =
            PartialUpdateTestSuite(
                name = name,
                objectGen = arbObject,
                propGen = propGen,
                dslProperty = dslProperty,
                dsl = ArtemisBase.Dsl,
                getProperty = getProperty,
            )
    }

    data object BlackHole : ObjectTestSuite<ArtemisBlackHole>(ObjectType.BLACK_HOLE) {
        private data class Properties(override val location: Location) :
            BaseProperties<ArtemisBlackHole> {
            override fun createThroughDsl(id: Int, timestamp: Long): ArtemisBlackHole =
                ArtemisBlackHole.Dsl.let { dsl ->
                    dsl.x = location.x
                    dsl.y = location.y
                    dsl.z = location.z

                    dsl.build(id, timestamp).apply { dsl.shouldBeReset() }
                }

            override fun updateThroughDsl(obj: ArtemisBlackHole) {
                ArtemisBlackHole.Dsl.also { dsl ->
                        dsl.x = location.x
                        dsl.y = location.y
                        dsl.z = location.z

                        dsl updates obj
                    }
                    .shouldBeReset()
            }

            override fun testKnownObject(obj: ArtemisBlackHole) {
                obj.shouldBeKnownObject(
                    id = obj.id,
                    type = objectType,
                    x = location.x,
                    y = location.y,
                    z = location.z,
                )
            }
        }

        override val arbObject: Arb<ArtemisBlackHole> = Arb.bind()
        override val arbObjectPair: Arb<Pair<ArtemisBlackHole, ArtemisBlackHole>> =
            Arb.bind(Arb.int(), Arb.long(), Arb.long()) { id, timestampA, timestampB ->
                Pair(
                    ArtemisBlackHole(id, min(timestampA, timestampB)),
                    ArtemisBlackHole(id, max(timestampA, timestampB)),
                )
            }

        override val partialUpdateTestSuites =
            listOf(
                partialUpdateTest(name = "X", propGen = X, dslProperty = ArtemisBlackHole.Dsl::x) {
                    it.x
                },
                partialUpdateTest(name = "Y", propGen = Y, dslProperty = ArtemisBlackHole.Dsl::y) {
                    it.y
                },
                partialUpdateTest(name = "Z", propGen = Z, dslProperty = ArtemisBlackHole.Dsl::z) {
                    it.z
                },
            )

        override suspend fun testCreateUnknown() {
            arbObject.checkAll { it.shouldBeUnknownObject(it.id, objectType) }
        }

        override suspend fun testCreateFromDsl() {
            checkAll(Arb.int(), Arb.long(), LOCATION.map(::Properties)) { id, timestamp, test ->
                shouldNotThrow<IllegalStateException> {
                    test.testKnownObject(test.createThroughDsl(id, timestamp))
                }
            }
        }

        override suspend fun testCreateAndUpdateManually() {
            checkAll(arbObject, LOCATION.map(::Properties)) { blackHole, test ->
                test.updateDirectly(blackHole)
                test.testKnownObject(blackHole)
            }
        }

        override suspend fun testCreateAndUpdateFromDsl() {
            checkAll(arbObject, LOCATION.map(::Properties)) { blackHole, test ->
                test.updateThroughDsl(blackHole)
                test.testKnownObject(blackHole)
            }
        }

        override suspend fun testUnknownObjectDoesNotProvideUpdates() {
            checkAll(arbObjectPair, LOCATION.map(::Properties)) { (oldBlackHole, newBlackHole), test
                ->
                test.updateDirectly(oldBlackHole)
                newBlackHole updates oldBlackHole
                test.testKnownObject(oldBlackHole)
            }
        }

        override suspend fun testKnownObjectProvidesUpdates() {
            checkAll(arbObjectPair, LOCATION.map(::Properties)) { (oldBlackHole, newBlackHole), test
                ->
                test.updateDirectly(newBlackHole)
                newBlackHole updates oldBlackHole
                test.testKnownObject(oldBlackHole)
            }
        }

        override suspend fun testDslCannotUpdateKnownObject() {
            checkAll(arbObject, LOCATION.map(::Properties)) { blackHole, test ->
                test.updateDirectly(blackHole)
                shouldThrowUnit<IllegalArgumentException> { test.updateThroughDsl(blackHole) }
            }
        }

        private fun <V, P : Property<V, P>> partialUpdateTest(
            name: String,
            propGen: Gen<V>,
            dslProperty: KMutableProperty0<V>,
            getProperty: (ArtemisBlackHole) -> P,
        ): PartialUpdateTestSuite<ArtemisBlackHole, ArtemisBlackHole.Dsl, V, P> =
            PartialUpdateTestSuite(
                name = name,
                objectGen = arbObject,
                propGen = propGen,
                dslProperty = dslProperty,
                dsl = ArtemisBlackHole.Dsl,
                getProperty = getProperty,
            )
    }

    data object Creature : ObjectTestSuite<ArtemisCreature>(ObjectType.CREATURE) {
        private val IS_NOT_TYPHON = Arb.boolState()

        private data class Properties(
            private val isNotTyphon: BoolState,
            override val location: Location,
        ) : BaseProperties<ArtemisCreature> {
            override fun updateDirectly(obj: ArtemisCreature) {
                super.updateDirectly(obj)
                obj.isNotTyphon.value = isNotTyphon
            }

            override fun createThroughDsl(id: Int, timestamp: Long): ArtemisCreature =
                ArtemisCreature.Dsl.let { dsl ->
                    ArtemisCreature.Dsl.isNotTyphon = isNotTyphon
                    dsl.x = location.x
                    dsl.y = location.y
                    dsl.z = location.z

                    dsl.build(id, timestamp).apply { dsl.shouldBeReset() }
                }

            override fun updateThroughDsl(obj: ArtemisCreature) {
                ArtemisCreature.Dsl.also { dsl ->
                        ArtemisCreature.Dsl.isNotTyphon = isNotTyphon
                        dsl.x = location.x
                        dsl.y = location.y
                        dsl.z = location.z

                        dsl updates obj
                    }
                    .shouldBeReset()
            }

            override fun testKnownObject(obj: ArtemisCreature) {
                obj.shouldBeKnownObject(
                    id = obj.id,
                    type = objectType,
                    x = location.x,
                    y = location.y,
                    z = location.z,
                )

                obj.isNotTyphon shouldContainValue isNotTyphon
            }
        }

        override val arbObject: Arb<ArtemisCreature> = Arb.bind()
        override val arbObjectPair: Arb<Pair<ArtemisCreature, ArtemisCreature>> =
            Arb.bind(Arb.int(), Arb.long(), Arb.long()) { id, timestampA, timestampB ->
                Pair(
                    ArtemisCreature(id, min(timestampA, timestampB)),
                    ArtemisCreature(id, max(timestampA, timestampB)),
                )
            }

        override val partialUpdateTestSuites =
            listOf(
                partialUpdateTest(
                    name = "Is not typhon",
                    propGen = IS_NOT_TYPHON,
                    dslProperty = ArtemisCreature.Dsl::isNotTyphon,
                ) {
                    it.isNotTyphon
                },
                partialUpdateTest(name = "X", propGen = X, dslProperty = ArtemisCreature.Dsl::x) {
                    it.x
                },
                partialUpdateTest(name = "Y", propGen = Y, dslProperty = ArtemisCreature.Dsl::y) {
                    it.y
                },
                partialUpdateTest(name = "Z", propGen = Z, dslProperty = ArtemisCreature.Dsl::z) {
                    it.z
                },
            )

        override suspend fun testCreateUnknown() {
            arbObject.checkAll { creature ->
                creature.shouldBeUnknownObject(creature.id, objectType)
                creature.isNotTyphon.shouldBeUnspecified()
            }
        }

        override suspend fun testCreateFromDsl() {
            checkAll(Arb.int(), Arb.long(), Arb.bind(IS_NOT_TYPHON, LOCATION, ::Properties)) {
                id,
                timestamp,
                test ->
                shouldNotThrow<IllegalStateException> {
                    test.testKnownObject(test.createThroughDsl(id, timestamp))
                }
            }
        }

        override suspend fun testCreateAndUpdateManually() {
            checkAll(arbObject, Arb.bind(IS_NOT_TYPHON, LOCATION, ::Properties)) { creature, test ->
                test.updateDirectly(creature)
                test.testKnownObject(creature)
            }
        }

        override suspend fun testCreateAndUpdateFromDsl() {
            checkAll(arbObject, Arb.bind(IS_NOT_TYPHON, LOCATION, ::Properties)) { creature, test ->
                test.updateThroughDsl(creature)
                test.testKnownObject(creature)
            }
        }

        override suspend fun testUnknownObjectDoesNotProvideUpdates() {
            checkAll(arbObjectPair, Arb.bind(IS_NOT_TYPHON, LOCATION, ::Properties)) {
                (oldCreature, newCreature),
                test ->
                test.updateDirectly(oldCreature)
                newCreature updates oldCreature
                test.testKnownObject(oldCreature)
            }
        }

        override suspend fun testKnownObjectProvidesUpdates() {
            checkAll(arbObjectPair, Arb.bind(IS_NOT_TYPHON, LOCATION, ::Properties)) {
                (oldCreature, newCreature),
                test ->
                test.updateDirectly(newCreature)
                newCreature updates oldCreature
                test.testKnownObject(oldCreature)
            }
        }

        override suspend fun testDslCannotUpdateKnownObject() {
            checkAll(arbObject, Arb.bind(IS_NOT_TYPHON, LOCATION, ::Properties)) { creature, test ->
                test.updateDirectly(creature)
                shouldThrowUnit<IllegalArgumentException> { test.updateThroughDsl(creature) }
            }
        }

        private fun <V, P : Property<V, P>> partialUpdateTest(
            name: String,
            propGen: Gen<V>,
            dslProperty: KMutableProperty0<V>,
            getProperty: (ArtemisCreature) -> P,
        ): PartialUpdateTestSuite<ArtemisCreature, ArtemisCreature.Dsl, V, P> =
            PartialUpdateTestSuite(
                name = name,
                objectGen = arbObject,
                propGen = propGen,
                dslProperty = dslProperty,
                dsl = ArtemisCreature.Dsl,
                getProperty = getProperty,
            )
    }

    data object Mine : ObjectTestSuite<ArtemisMine>(ObjectType.MINE) {
        private data class Properties(override val location: Location) :
            BaseProperties<ArtemisMine> {
            override fun createThroughDsl(id: Int, timestamp: Long): ArtemisMine =
                ArtemisMine.Dsl.let { dsl ->
                    dsl.x = location.x
                    dsl.y = location.y
                    dsl.z = location.z

                    dsl.build(id, timestamp).apply { dsl.shouldBeReset() }
                }

            override fun updateThroughDsl(obj: ArtemisMine) {
                ArtemisMine.Dsl.also { dsl ->
                        dsl.x = location.x
                        dsl.y = location.y
                        dsl.z = location.z

                        dsl updates obj
                    }
                    .shouldBeReset()
            }

            override fun testKnownObject(obj: ArtemisMine) {
                obj.shouldBeKnownObject(
                    id = obj.id,
                    type = objectType,
                    x = location.x,
                    y = location.y,
                    z = location.z,
                )
            }
        }

        override val arbObject: Arb<ArtemisMine> = Arb.bind()
        override val arbObjectPair: Arb<Pair<ArtemisMine, ArtemisMine>> =
            Arb.bind(Arb.int(), Arb.long(), Arb.long()) { id, timestampA, timestampB ->
                Pair(
                    ArtemisMine(id, min(timestampA, timestampB)),
                    ArtemisMine(id, max(timestampA, timestampB)),
                )
            }

        override val partialUpdateTestSuites =
            listOf(
                partialUpdateTest(name = "X", propGen = X, dslProperty = ArtemisMine.Dsl::x) {
                    it.x
                },
                partialUpdateTest(name = "Y", propGen = Y, dslProperty = ArtemisMine.Dsl::y) {
                    it.y
                },
                partialUpdateTest(name = "Z", propGen = Z, dslProperty = ArtemisMine.Dsl::z) {
                    it.z
                },
            )

        override suspend fun testCreateUnknown() {
            arbObject.checkAll { it.shouldBeUnknownObject(it.id, objectType) }
        }

        override suspend fun testCreateFromDsl() {
            checkAll(Arb.int(), Arb.long(), LOCATION.map(::Properties)) { id, timestamp, test ->
                shouldNotThrow<IllegalStateException> {
                    test.testKnownObject(test.createThroughDsl(id, timestamp))
                }
            }
        }

        override suspend fun testCreateAndUpdateManually() {
            checkAll(arbObject, LOCATION.map(::Properties)) { mine, test ->
                test.updateDirectly(mine)
                test.testKnownObject(mine)
            }
        }

        override suspend fun testCreateAndUpdateFromDsl() {
            checkAll(arbObject, LOCATION.map(::Properties)) { mine, test ->
                test.updateThroughDsl(mine)
                test.testKnownObject(mine)
            }
        }

        override suspend fun testUnknownObjectDoesNotProvideUpdates() {
            checkAll(arbObjectPair, LOCATION.map(::Properties)) { (oldMine, newMine), test ->
                test.updateDirectly(oldMine)
                newMine updates oldMine
                test.testKnownObject(oldMine)
            }
        }

        override suspend fun testKnownObjectProvidesUpdates() {
            Arb.pair(arbObject, LOCATION.map(::Properties))
                .flatMap { (mine, test) ->
                    Arb.long()
                        .filter { it != mine.timestamp }
                        .map { timestamp ->
                            val otherMine = ArtemisMine(mine.id, timestamp)
                            if (timestamp < mine.timestamp) Triple(otherMine, mine, test)
                            else Triple(mine, otherMine, test)
                        }
                }
                .checkAll { (oldMine, newMine, test) ->
                    test.updateDirectly(newMine)
                    newMine updates oldMine
                    test.testKnownObject(oldMine)
                }
        }

        override suspend fun testDslCannotUpdateKnownObject() {
            checkAll(arbObject, LOCATION.map(::Properties)) { mine, test ->
                test.updateDirectly(mine)
                shouldThrowUnit<IllegalArgumentException> { test.updateThroughDsl(mine) }
            }
        }

        private fun <V, P : Property<V, P>> partialUpdateTest(
            name: String,
            propGen: Gen<V>,
            dslProperty: KMutableProperty0<V>,
            getProperty: (ArtemisMine) -> P,
        ): PartialUpdateTestSuite<ArtemisMine, ArtemisMine.Dsl, V, P> =
            PartialUpdateTestSuite(
                name = name,
                objectGen = arbObject,
                propGen = propGen,
                dslProperty = dslProperty,
                dsl = ArtemisMine.Dsl,
                getProperty = getProperty,
            )
    }

    data object Npc : ObjectTestSuite<ArtemisNpc>(ObjectType.NPC_SHIP) {
        private val NAME = Arb.string()
        private val SHIELDS_PAIR = Arb.pair(SHIELDS, SHIELDS)
        private val HULL_ID = Arb.int().filter { it != -1 }
        private val IMPULSE = Arb.numericFloat()
        private val IS_ENEMY = Arb.boolState()
        private val IS_SURRENDERED = Arb.boolState()
        private val IN_NEBULA = Arb.boolState()
        private val SCAN_BITS = Arb.int()
        private val SIDE = Arb.byte().filter { it.toInt() != -1 }

        private data class Properties(
            private val name: String,
            private val shields: Pair<ShieldStrength, ShieldStrength>,
            private val hullId: Int,
            private val impulse: Float,
            private val isEnemy: BoolState,
            private val isSurrendered: BoolState,
            private val inNebula: BoolState,
            private val scanBits: Int,
            private val side: Byte,
            override val location: Location,
        ) : BaseProperties<ArtemisNpc> {
            override fun updateDirectly(obj: ArtemisNpc) {
                super.updateDirectly(obj)
                obj.name.value = name
                obj.shieldsFront.strength.value = shields.first.strength
                obj.shieldsFront.maxStrength.value = shields.first.maxStrength
                obj.shieldsRear.strength.value = shields.second.strength
                obj.shieldsRear.maxStrength.value = shields.second.maxStrength
                obj.hullId.value = hullId
                obj.impulse.value = impulse
                obj.isEnemy.value = isEnemy
                obj.isSurrendered.value = isSurrendered
                obj.isInNebula.value = inNebula
                obj.scanBits.value = scanBits
                obj.side.value = side
            }

            override fun createThroughDsl(id: Int, timestamp: Long): ArtemisNpc =
                ArtemisNpc.Dsl.let { dsl ->
                    dsl.name = name
                    dsl.shieldsFront = shields.first.strength
                    dsl.shieldsFrontMax = shields.first.maxStrength
                    dsl.shieldsRear = shields.second.strength
                    dsl.shieldsRearMax = shields.second.maxStrength
                    dsl.hullId = hullId
                    dsl.impulse = impulse
                    ArtemisNpc.Dsl.isEnemy = isEnemy
                    ArtemisNpc.Dsl.isSurrendered = isSurrendered
                    ArtemisNpc.Dsl.isInNebula = inNebula
                    ArtemisNpc.Dsl.scanBits = scanBits
                    dsl.side = side
                    dsl.x = location.x
                    dsl.y = location.y
                    dsl.z = location.z

                    dsl.build(id, timestamp).apply { dsl.shouldBeReset() }
                }

            override fun updateThroughDsl(obj: ArtemisNpc) {
                ArtemisNpc.Dsl.also { dsl ->
                        dsl.name = name
                        dsl.shieldsFront = shields.first.strength
                        dsl.shieldsFrontMax = shields.first.maxStrength
                        dsl.shieldsRear = shields.second.strength
                        dsl.shieldsRearMax = shields.second.maxStrength
                        dsl.hullId = hullId
                        dsl.impulse = impulse
                        ArtemisNpc.Dsl.isEnemy = isEnemy
                        ArtemisNpc.Dsl.isSurrendered = isSurrendered
                        ArtemisNpc.Dsl.isInNebula = inNebula
                        ArtemisNpc.Dsl.scanBits = scanBits
                        dsl.side = side
                        dsl.x = location.x
                        dsl.y = location.y
                        dsl.z = location.z

                        dsl updates obj
                    }
                    .shouldBeReset()
            }

            override fun testKnownObject(obj: ArtemisNpc) {
                obj.shouldBeKnownObject(
                    id = obj.id,
                    type = objectType,
                    name,
                    x = location.x,
                    y = location.y,
                    z = location.z,
                    hullId,
                    shieldsFront = shields.first.strength,
                    shieldsFrontMax = shields.first.maxStrength,
                    shieldsRear = shields.second.strength,
                    shieldsRearMax = shields.second.maxStrength,
                    impulse,
                    side,
                )

                obj.isEnemy shouldContainValue isEnemy
                obj.isSurrendered shouldContainValue isSurrendered
                obj.isInNebula shouldContainValue inNebula
                obj.scanBits shouldContainValue scanBits
            }
        }

        enum class ScanBitsTestCase {
            KNOWN {
                override suspend fun testSides() {
                    checkAll(arbObject, SCAN_BITS) { npc, scanBits ->
                        npc.scanBits.value = scanBits
                        BooleanArray(Int.SIZE_BITS) { scanBits and 1.shl(it) != 0 }
                            .forEachIndexed { index, expected ->
                                npc.hasBeenScannedBy(index.toByte()) shouldBeEqual
                                    if (expected) BoolState.True else BoolState.False
                            }
                    }
                }

                override suspend fun testShips() {
                    checkAll(arbObjectPair, SCAN_BITS, SIDE) { (npc1, npc2), scanBits, side ->
                        npc1.scanBits.value = scanBits
                        npc2.side.value = side

                        npc1.hasBeenScannedBy(npc2) shouldBeEqual
                            if (scanBits and 1.shl(side.toInt()) != 0) BoolState.True
                            else BoolState.False
                    }
                }
            },
            UNKNOWN {
                override suspend fun testSides() {
                    arbObject.checkAll { npc ->
                        repeat(Int.SIZE_BITS) { side ->
                            npc.hasBeenScannedBy(side.toByte()) shouldBeEqual BoolState.Unknown
                        }
                    }
                }

                override suspend fun testShips() {
                    checkAll(arbObjectPair, SCAN_BITS) { (npc1, npc2), scanBits ->
                        npc1.scanBits.value = scanBits
                        npc1.hasBeenScannedBy(npc2) shouldBeEqual BoolState.Unknown
                    }
                }
            };

            abstract suspend fun testSides()

            abstract suspend fun testShips()
        }

        override val arbObject: Arb<ArtemisNpc> = Arb.bind()
        override val arbObjectPair: Arb<Pair<ArtemisNpc, ArtemisNpc>> =
            Arb.bind(Arb.int(), Arb.long(), Arb.long()) { id, timestampA, timestampB ->
                Pair(
                    ArtemisNpc(id, min(timestampA, timestampB)),
                    ArtemisNpc(id, max(timestampA, timestampB)),
                )
            }

        override val partialUpdateTestSuites =
            listOf(
                partialUpdateTest(
                    name = "Name",
                    propGen = NAME,
                    dslProperty = ArtemisNpc.Dsl::name,
                ) {
                    it.name
                },
                partialUpdateTest(
                    name = "Front shields",
                    propGen = SHIELDS_STRENGTH,
                    dslProperty = ArtemisNpc.Dsl::shieldsFront,
                ) {
                    it.shieldsFront.strength
                },
                partialUpdateTest(
                    name = "Front shields max",
                    propGen = SHIELDS_MAX,
                    dslProperty = ArtemisNpc.Dsl::shieldsFrontMax,
                ) {
                    it.shieldsFront.maxStrength
                },
                partialUpdateTest(
                    name = "Rear shields",
                    propGen = SHIELDS_STRENGTH,
                    dslProperty = ArtemisNpc.Dsl::shieldsRear,
                ) {
                    it.shieldsRear.strength
                },
                partialUpdateTest(
                    name = "Rear shields max",
                    propGen = SHIELDS_MAX,
                    dslProperty = ArtemisNpc.Dsl::shieldsRearMax,
                ) {
                    it.shieldsRear.maxStrength
                },
                partialUpdateTest(
                    name = "Hull ID",
                    propGen = HULL_ID,
                    dslProperty = ArtemisNpc.Dsl::hullId,
                ) {
                    it.hullId
                },
                partialUpdateTest(
                    name = "Impulse",
                    propGen = IMPULSE,
                    dslProperty = ArtemisNpc.Dsl::impulse,
                ) {
                    it.impulse
                },
                partialUpdateTest(
                    name = "Is enemy",
                    propGen = IS_ENEMY,
                    dslProperty = ArtemisNpc.Dsl::isEnemy,
                ) {
                    it.isEnemy
                },
                partialUpdateTest(
                    name = "Is surrendered",
                    propGen = IS_SURRENDERED,
                    dslProperty = ArtemisNpc.Dsl::isSurrendered,
                ) {
                    it.isSurrendered
                },
                partialUpdateTest(
                    name = "Is in nebula",
                    propGen = IN_NEBULA,
                    dslProperty = ArtemisNpc.Dsl::isInNebula,
                ) {
                    it.isInNebula
                },
                partialUpdateTest(
                    name = "Scan bits",
                    propGen = SCAN_BITS,
                    dslProperty = ArtemisNpc.Dsl::scanBits,
                ) {
                    it.scanBits
                },
                partialUpdateTest(
                    name = "Side",
                    propGen = SIDE,
                    dslProperty = ArtemisNpc.Dsl::side,
                ) {
                    it.side
                },
                partialUpdateTest(name = "X", propGen = X, dslProperty = ArtemisNpc.Dsl::x) {
                    it.x
                },
                partialUpdateTest(name = "Y", propGen = Y, dslProperty = ArtemisNpc.Dsl::y) {
                    it.y
                },
                partialUpdateTest(name = "Z", propGen = Z, dslProperty = ArtemisNpc.Dsl::z) { it.z },
            )

        override suspend fun testCreateUnknown() {
            arbObject.checkAll { npc ->
                npc.shouldBeUnknownObject(npc.id, objectType)

                npc.isEnemy.shouldBeUnspecified()
                npc.isInNebula.shouldBeUnspecified()
                npc.scanBits.shouldBeUnspecified()
            }
        }

        override suspend fun testCreateFromDsl() {
            checkAll(
                Arb.int(),
                Arb.long(),
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS_PAIR,
                    genC = HULL_ID,
                    genD = IMPULSE,
                    genE = IS_ENEMY,
                    genF = IS_SURRENDERED,
                    genG = IN_NEBULA,
                    genH = SCAN_BITS,
                    genI = SIDE,
                    genJ = LOCATION,
                    bindFn = ::Properties,
                ),
            ) { id, timestamp, test ->
                shouldNotThrow<IllegalStateException> {
                    test.testKnownObject(test.createThroughDsl(id, timestamp))
                }
            }
        }

        override suspend fun testCreateAndUpdateManually() {
            checkAll(
                arbObject,
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS_PAIR,
                    genC = HULL_ID,
                    genD = IMPULSE,
                    genE = IS_ENEMY,
                    genF = IS_SURRENDERED,
                    genG = IN_NEBULA,
                    genH = SCAN_BITS,
                    genI = SIDE,
                    genJ = LOCATION,
                    bindFn = ::Properties,
                ),
            ) { npc, test ->
                test.updateDirectly(npc)
                test.testKnownObject(npc)
            }
        }

        override suspend fun testCreateAndUpdateFromDsl() {
            checkAll(
                arbObject,
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS_PAIR,
                    genC = HULL_ID,
                    genD = IMPULSE,
                    genE = IS_ENEMY,
                    genF = IS_SURRENDERED,
                    genG = IN_NEBULA,
                    genH = SCAN_BITS,
                    genI = SIDE,
                    genJ = LOCATION,
                    bindFn = ::Properties,
                ),
            ) { npc, test ->
                test.updateThroughDsl(npc)
                test.testKnownObject(npc)
            }
        }

        override suspend fun testUnknownObjectDoesNotProvideUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS_PAIR,
                    genC = HULL_ID,
                    genD = IMPULSE,
                    genE = IS_ENEMY,
                    genF = IS_SURRENDERED,
                    genG = IN_NEBULA,
                    genH = SCAN_BITS,
                    genI = SIDE,
                    genJ = LOCATION,
                    bindFn = ::Properties,
                ),
            ) { (oldNpc, newNpc), test ->
                test.updateDirectly(oldNpc)
                newNpc updates oldNpc
                test.testKnownObject(oldNpc)
            }
        }

        override suspend fun testKnownObjectProvidesUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS_PAIR,
                    genC = HULL_ID,
                    genD = IMPULSE,
                    genE = IS_ENEMY,
                    genF = IS_SURRENDERED,
                    genG = IN_NEBULA,
                    genH = SCAN_BITS,
                    genI = SIDE,
                    genJ = LOCATION,
                    bindFn = ::Properties,
                ),
            ) { (oldNpc, newNpc), test ->
                test.updateDirectly(newNpc)
                newNpc updates oldNpc
                test.testKnownObject(oldNpc)
            }
        }

        override suspend fun testDslCannotUpdateKnownObject() {
            checkAll(
                arbObject,
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS_PAIR,
                    genC = HULL_ID,
                    genD = IMPULSE,
                    genE = IS_ENEMY,
                    genF = IS_SURRENDERED,
                    genG = IN_NEBULA,
                    genH = SCAN_BITS,
                    genI = SIDE,
                    genJ = LOCATION,
                    bindFn = ::Properties,
                ),
            ) { npc, test ->
                test.updateDirectly(npc)
                shouldThrowUnit<IllegalArgumentException> { test.updateThroughDsl(npc) }
            }
        }

        override fun DescribeSpecContainerScope.describeMore() = launch {
            describe("Scanned by") {
                ScanBitsTestCase.entries.forEach { case ->
                    describe(case.name.let { it[0] + it.substring(1).lowercase() }) {
                        it("Sides") { case.testSides() }
                        it("Ships") { case.testShips() }
                    }
                }
            }

            describeVesselDataTests(arbObject, HULL_ID)
            describeFullNameTests(arbObject, NAME)
        }

        private fun <V, P : Property<V, P>> partialUpdateTest(
            name: String,
            propGen: Gen<V>,
            dslProperty: KMutableProperty0<V>,
            getProperty: (ArtemisNpc) -> P,
        ): PartialUpdateTestSuite<ArtemisNpc, ArtemisNpc.Dsl, V, P> =
            PartialUpdateTestSuite(
                name = name,
                objectGen = arbObject,
                propGen = propGen,
                dslProperty = dslProperty,
                dsl = ArtemisNpc.Dsl,
                getProperty = getProperty,
            )
    }

    data object Player : ObjectTestSuite<ArtemisPlayer>(ObjectType.PLAYER_SHIP) {
        private val NAME = Arb.string()
        private val SHIELDS_PAIR = Arb.pair(SHIELDS, SHIELDS)
        private val HULL_ID = Arb.int().filter { it != -1 }
        private val IMPULSE = Arb.numericFloat()
        private val SIDE = Arb.byte().filter { it.toInt() != -1 }
        private val SHIP_INDEX = Arb.byte().filter { it != Byte.MIN_VALUE }
        private val CAPITAL_SHIP_ID = Arb.int().filter { it != -1 }
        private val ALERT_STATUS = Arb.enum<AlertStatus>()
        private val DRIVE_TYPE = Arb.enum<DriveType>()
        private val ENUMS = Arb.pair(ALERT_STATUS, DRIVE_TYPE)
        private val WARP = Arb.byte(min = 0, max = Artemis.MAX_WARP)
        private val DOCKING_BASE = Arb.int().filter { it != -1 }
        private val DOUBLE_AGENT_ACTIVE = Arb.boolState()
        private val DOUBLE_AGENT_COUNT = Arb.byte().filter { it.toInt() != -1 }
        private val DOUBLE_AGENT_SECONDS = Arb.int().filter { it != -1 }
        private val DOUBLE_AGENT =
            Arb.triple(DOUBLE_AGENT_ACTIVE, DOUBLE_AGENT_COUNT, DOUBLE_AGENT_SECONDS)
        private val ORDNANCE_COUNTS =
            Arb.list(Arb.byte(min = 0), OrdnanceType.size..OrdnanceType.size)
        private val TUBES =
            Arb.list(
                Arb.pair(Arb.enum<TubeState>(), Arb.enum<OrdnanceType>()),
                Artemis.MAX_TUBES..Artemis.MAX_TUBES,
            )

        private data class Properties(
            private val name: String,
            private val shields: Pair<ShieldStrength, ShieldStrength>,
            private val hullId: Int,
            private val impulse: Float,
            private val side: Byte,
            private val shipIndex: Byte,
            private val capitalShipID: Int,
            private val enumStates: Pair<AlertStatus, DriveType>,
            private val warp: Byte,
            private val dockingBase: Int,
            private val doubleAgentStatus: Triple<BoolState, Byte, Int>,
            override val location: Location,
            private val ordnanceCounts: List<Byte>,
            private val tubes: List<Pair<TubeState, OrdnanceType>>,
        ) : BaseProperties<ArtemisPlayer> {
            override fun updateDirectly(obj: ArtemisPlayer) {
                super.updateDirectly(obj)
                obj.name.value = name
                obj.shieldsFront.strength.value = shields.first.strength
                obj.shieldsFront.maxStrength.value = shields.first.maxStrength
                obj.shieldsRear.strength.value = shields.second.strength
                obj.shieldsRear.maxStrength.value = shields.second.maxStrength
                obj.hullId.value = hullId
                obj.impulse.value = impulse
                obj.side.value = side
                obj.shipIndex.value = shipIndex
                obj.capitalShipID.value = capitalShipID
                obj.alertStatus.value = enumStates.first
                obj.driveType.value = enumStates.second
                obj.warp.value = warp
                obj.dockingBase.value = dockingBase
                obj.doubleAgentActive.value = doubleAgentStatus.first
                obj.doubleAgentCount.value = doubleAgentStatus.second
                obj.doubleAgentSecondsLeft.value = doubleAgentStatus.third
                ordnanceCounts.forEachIndexed { index, count ->
                    obj.ordnanceCounts[index].value = count
                }
                tubes.forEachIndexed { index, (state, contents) ->
                    obj.tubes[index].also { tube ->
                        tube.state.value = state
                        tube.contents = contents
                    }
                }
            }

            override fun updateThroughDsl(obj: ArtemisPlayer) {
                updateThroughPlayerDsl(obj)
                updateThroughWeaponsDsl(obj)
                updateThroughUpgradesDsl(obj)
            }

            override fun createThroughDsl(id: Int, timestamp: Long): ArtemisPlayer =
                ArtemisPlayer.Dsl.Player.let { dsl ->
                    dsl.name = name
                    dsl.shieldsFront = shields.first.strength
                    dsl.shieldsFrontMax = shields.first.maxStrength
                    dsl.shieldsRear = shields.second.strength
                    dsl.shieldsRearMax = shields.second.maxStrength
                    dsl.hullId = hullId
                    dsl.impulse = impulse
                    dsl.side = side
                    dsl.x = location.x
                    dsl.y = location.y
                    dsl.z = location.z
                    dsl.shipIndex = shipIndex
                    dsl.capitalShipID = capitalShipID
                    dsl.alertStatus = enumStates.first
                    dsl.driveType = enumStates.second
                    dsl.warp = warp
                    dsl.dockingBase = dockingBase

                    dsl.build(id, timestamp).also { player ->
                        updateThroughWeaponsDsl(player)
                        updateThroughUpgradesDsl(player)
                        dsl.shouldBeReset()
                    }
                }

            override fun testKnownObject(obj: ArtemisPlayer) {
                obj.shouldBeKnownObject(
                    id = obj.id,
                    type = objectType,
                    name,
                    x = location.x,
                    y = location.y,
                    z = location.z,
                    hullId,
                    shieldsFront = shields.first.strength,
                    shieldsFrontMax = shields.first.maxStrength,
                    shieldsRear = shields.second.strength,
                    shieldsRearMax = shields.second.maxStrength,
                    impulse,
                    side,
                )

                obj.shipIndex shouldContainValue shipIndex
                obj.capitalShipID shouldContainValue capitalShipID
                obj.doubleAgentActive shouldContainValue doubleAgentStatus.first
                obj.doubleAgentCount shouldContainValue doubleAgentStatus.second
                obj.doubleAgentSecondsLeft shouldContainValue doubleAgentStatus.third
                obj.alertStatus shouldContainValue enumStates.first
                obj.driveType shouldContainValue enumStates.second
                obj.warp shouldContainValue warp
                obj.dockingBase shouldContainValue dockingBase

                val totalCounts = IntArray(OrdnanceType.size)

                obj.ordnanceCounts.zip(ordnanceCounts).forEachIndexed { index, (prop, count) ->
                    prop shouldContainValue count
                    totalCounts[index] = count.toInt()
                }
                obj.tubes.zip(tubes).forEach { (tube, props) ->
                    val (state, contents) = props
                    tube.state shouldContainValue state
                    tube.lastContents shouldContainValue contents

                    if (state == TubeState.LOADING || state == TubeState.LOADED) {
                        tube.contents.shouldNotBeNull() shouldBeEqual contents
                        totalCounts[contents.ordinal]++
                    } else {
                        tube.contents.shouldBeNull()
                    }

                    tube.hasData.shouldBeTrue()
                }

                OrdnanceType.entries.forEach {
                    obj.getTotalOrdnanceCount(it) shouldBeEqual totalCounts[it.ordinal]
                }

                obj.hasData.shouldBeTrue()
            }

            fun updateThroughPlayerDsl(player: ArtemisPlayer) {
                ArtemisPlayer.Dsl.Player.also { dsl ->
                        dsl.name = name
                        dsl.shieldsFront = shields.first.strength
                        dsl.shieldsFrontMax = shields.first.maxStrength
                        dsl.shieldsRear = shields.second.strength
                        dsl.shieldsRearMax = shields.second.maxStrength
                        dsl.hullId = hullId
                        dsl.impulse = impulse
                        dsl.side = side
                        dsl.x = location.x
                        dsl.y = location.y
                        dsl.z = location.z
                        dsl.shipIndex = shipIndex
                        dsl.capitalShipID = capitalShipID
                        dsl.alertStatus = enumStates.first
                        dsl.driveType = enumStates.second
                        dsl.warp = warp
                        dsl.dockingBase = dockingBase

                        dsl updates player
                    }
                    .shouldBeReset()
            }

            fun updateThroughWeaponsDsl(player: ArtemisPlayer) {
                ArtemisPlayer.Dsl.Weapons.also { dsl ->
                        ordnanceCounts.forEachIndexed { index, count ->
                            dsl.ordnanceCounts[OrdnanceType.entries[index]] = count
                        }
                        tubes.forEachIndexed { index, (state, contents) ->
                            dsl.tubeStates[index] = state
                            dsl.tubeContents[index] = contents
                        }

                        dsl updates player
                    }
                    .shouldBeReset()
            }

            fun updateThroughUpgradesDsl(player: ArtemisPlayer) {
                ArtemisPlayer.Dsl.Upgrades.also { dsl ->
                        dsl.doubleAgentActive = doubleAgentStatus.first
                        dsl.doubleAgentCount = doubleAgentStatus.second
                        dsl.doubleAgentSecondsLeft = doubleAgentStatus.third

                        dsl updates player
                    }
                    .shouldBeReset()
            }
        }

        override val arbObject: Arb<ArtemisPlayer> = Arb.bind()
        override val arbObjectPair: Arb<Pair<ArtemisPlayer, ArtemisPlayer>> =
            Arb.bind(Arb.int(), Arb.long(), Arb.long()) { id, timestampA, timestampB ->
                Pair(
                    ArtemisPlayer(id, min(timestampA, timestampB)),
                    ArtemisPlayer(id, max(timestampA, timestampB)),
                )
            }

        override val partialUpdateTestSuites =
            listOf(
                partialPlayerUpdateTest(
                    name = "Name",
                    propGen = NAME,
                    dslProperty = ArtemisPlayer.Dsl.Player::name,
                ) {
                    it.name
                },
                partialPlayerUpdateTest(
                    name = "Front shields",
                    propGen = SHIELDS_STRENGTH,
                    dslProperty = ArtemisPlayer.Dsl.Player::shieldsFront,
                ) {
                    it.shieldsFront.strength
                },
                partialPlayerUpdateTest(
                    name = "Front shields max",
                    propGen = SHIELDS_MAX,
                    dslProperty = ArtemisPlayer.Dsl.Player::shieldsFrontMax,
                ) {
                    it.shieldsFront.maxStrength
                },
                partialPlayerUpdateTest(
                    name = "Rear shields",
                    propGen = SHIELDS_STRENGTH,
                    dslProperty = ArtemisPlayer.Dsl.Player::shieldsRear,
                ) {
                    it.shieldsRear.strength
                },
                partialPlayerUpdateTest(
                    name = "Rear shields max",
                    propGen = SHIELDS_MAX,
                    dslProperty = ArtemisPlayer.Dsl.Player::shieldsRearMax,
                ) {
                    it.shieldsRear.maxStrength
                },
                partialPlayerUpdateTest(
                    name = "Hull ID",
                    propGen = HULL_ID,
                    dslProperty = ArtemisPlayer.Dsl.Player::hullId,
                ) {
                    it.hullId
                },
                partialPlayerUpdateTest(
                    name = "Impulse",
                    propGen = IMPULSE,
                    dslProperty = ArtemisPlayer.Dsl.Player::impulse,
                ) {
                    it.impulse
                },
                partialPlayerUpdateTest(
                    name = "Warp",
                    propGen = WARP,
                    dslProperty = ArtemisPlayer.Dsl.Player::warp,
                ) {
                    it.warp
                },
                partialPlayerUpdateTest(
                    name = "Side",
                    propGen = SIDE,
                    dslProperty = ArtemisPlayer.Dsl.Player::side,
                ) {
                    it.side
                },
                partialPlayerUpdateTest(
                    name = "Ship index",
                    propGen = SHIP_INDEX,
                    dslProperty = ArtemisPlayer.Dsl.Player::shipIndex,
                ) {
                    it.shipIndex
                },
                partialPlayerUpdateTest(
                    name = "Capital ship ID",
                    propGen = CAPITAL_SHIP_ID,
                    dslProperty = ArtemisPlayer.Dsl.Player::capitalShipID,
                ) {
                    it.capitalShipID
                },
                partialPlayerUpdateTest(
                    name = "Docking base",
                    propGen = DOCKING_BASE,
                    dslProperty = ArtemisPlayer.Dsl.Player::dockingBase,
                ) {
                    it.dockingBase
                },
                partialPlayerUpdateTest(
                    name = "Alert status",
                    propGen = ALERT_STATUS,
                    dslProperty = ArtemisPlayer.Dsl.Player::alertStatus,
                ) {
                    it.alertStatus
                },
                partialPlayerUpdateTest(
                    name = "Drive type",
                    propGen = DRIVE_TYPE,
                    dslProperty = ArtemisPlayer.Dsl.Player::driveType,
                ) {
                    it.driveType
                },
                partialPlayerUpdateTest(
                    name = "X",
                    propGen = X,
                    dslProperty = ArtemisPlayer.Dsl.Player::x,
                ) {
                    it.x
                },
                partialPlayerUpdateTest(
                    name = "Y",
                    propGen = Y,
                    dslProperty = ArtemisPlayer.Dsl.Player::y,
                ) {
                    it.y
                },
                partialPlayerUpdateTest(
                    name = "Z",
                    propGen = Z,
                    dslProperty = ArtemisPlayer.Dsl.Player::z,
                ) {
                    it.z
                },
                partialUpgradesUpdateTest(
                    name = "Double agent active",
                    propGen = DOUBLE_AGENT_ACTIVE,
                    dslProperty = ArtemisPlayer.Dsl.Upgrades::doubleAgentActive,
                ) {
                    it.doubleAgentActive
                },
                partialUpgradesUpdateTest(
                    name = "Double agent count",
                    propGen = DOUBLE_AGENT_COUNT,
                    dslProperty = ArtemisPlayer.Dsl.Upgrades::doubleAgentCount,
                ) {
                    it.doubleAgentCount
                },
                partialUpgradesUpdateTest(
                    name = "Double agent seconds left",
                    propGen = DOUBLE_AGENT_SECONDS,
                    dslProperty = ArtemisPlayer.Dsl.Upgrades::doubleAgentSecondsLeft,
                ) {
                    it.doubleAgentSecondsLeft
                },
            )

        override suspend fun testCreateUnknown() {
            arbObject.checkAll { player ->
                player.shouldBeUnknownObject(player.id, objectType)

                player.shipIndex.shouldBeUnspecified(Byte.MIN_VALUE)
                player.capitalShipID.shouldBeUnspecified()
                player.doubleAgentActive.shouldBeUnspecified()
                player.doubleAgentCount.shouldBeUnspecified()
                player.doubleAgentSecondsLeft.shouldBeUnspecified()
                player.alertStatus.shouldBeUnspecified()
                player.driveType.shouldBeUnspecified()
                player.warp.shouldBeUnspecified()
                player.dockingBase.shouldBeUnspecified()
                player.ordnanceCounts.forEach { prop -> prop.shouldBeUnspecified() }
                player.tubes.forEach { tube ->
                    tube.state.shouldBeUnspecified()
                    tube.lastContents.shouldBeUnspecified()
                    tube.contents.shouldBeNull()
                    tube.hasData.shouldBeFalse()
                }
            }
        }

        override suspend fun testCreateFromDsl() {
            checkAll(
                Arb.int(),
                Arb.long(),
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS_PAIR,
                    genC = HULL_ID,
                    genD = IMPULSE,
                    genE = SIDE,
                    genF = SHIP_INDEX,
                    genG = CAPITAL_SHIP_ID,
                    genH = ENUMS,
                    genI = WARP,
                    genJ = DOCKING_BASE,
                    genK = DOUBLE_AGENT,
                    genL = LOCATION,
                    genM = ORDNANCE_COUNTS,
                    genN = TUBES,
                    bindFn = ::Properties,
                ),
            ) { id, timestamp, test ->
                shouldNotThrow<IllegalStateException> {
                    test.testKnownObject(test.createThroughDsl(id, timestamp))
                }
            }
        }

        override suspend fun testCreateAndUpdateManually() {
            checkAll(
                arbObject,
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS_PAIR,
                    genC = HULL_ID,
                    genD = IMPULSE,
                    genE = SIDE,
                    genF = SHIP_INDEX,
                    genG = CAPITAL_SHIP_ID,
                    genH = ENUMS,
                    genI = WARP,
                    genJ = DOCKING_BASE,
                    genK = DOUBLE_AGENT,
                    genL = LOCATION,
                    genM = ORDNANCE_COUNTS,
                    genN = TUBES,
                    bindFn = ::Properties,
                ),
            ) { player, test ->
                test.updateDirectly(player)
                test.testKnownObject(player)
            }
        }

        override suspend fun testCreateAndUpdateFromDsl() {
            checkAll(
                arbObject,
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS_PAIR,
                    genC = HULL_ID,
                    genD = IMPULSE,
                    genE = SIDE,
                    genF = SHIP_INDEX,
                    genG = CAPITAL_SHIP_ID,
                    genH = ENUMS,
                    genI = WARP,
                    genJ = DOCKING_BASE,
                    genK = DOUBLE_AGENT,
                    genL = LOCATION,
                    genM = ORDNANCE_COUNTS,
                    genN = TUBES,
                    bindFn = ::Properties,
                ),
            ) { player, test ->
                test.updateThroughDsl(player)
                test.testKnownObject(player)
            }
        }

        override suspend fun testUnknownObjectDoesNotProvideUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS_PAIR,
                    genC = HULL_ID,
                    genD = IMPULSE,
                    genE = SIDE,
                    genF = SHIP_INDEX,
                    genG = CAPITAL_SHIP_ID,
                    genH = ENUMS,
                    genI = WARP,
                    genJ = DOCKING_BASE,
                    genK = DOUBLE_AGENT,
                    genL = LOCATION,
                    genM = ORDNANCE_COUNTS,
                    genN = TUBES,
                    bindFn = ::Properties,
                ),
            ) { (oldPlayer, newPlayer), test ->
                test.updateDirectly(oldPlayer)
                newPlayer updates oldPlayer
                test.testKnownObject(oldPlayer)
            }
        }

        override suspend fun testKnownObjectProvidesUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS_PAIR,
                    genC = HULL_ID,
                    genD = IMPULSE,
                    genE = SIDE,
                    genF = SHIP_INDEX,
                    genG = CAPITAL_SHIP_ID,
                    genH = ENUMS,
                    genI = WARP,
                    genJ = DOCKING_BASE,
                    genK = DOUBLE_AGENT,
                    genL = LOCATION,
                    genM = ORDNANCE_COUNTS,
                    genN = TUBES,
                    bindFn = ::Properties,
                ),
            ) { (oldPlayer, newPlayer), test ->
                test.updateDirectly(newPlayer)
                newPlayer updates oldPlayer
                test.testKnownObject(oldPlayer)
            }
        }

        override suspend fun testDslCannotUpdateKnownObject() {
            checkAll(
                arbObject,
                Arb.bind(
                    genA = NAME,
                    genB = SHIELDS_PAIR,
                    genC = HULL_ID,
                    genD = IMPULSE,
                    genE = SIDE,
                    genF = SHIP_INDEX,
                    genG = CAPITAL_SHIP_ID,
                    genH = ENUMS,
                    genI = WARP,
                    genJ = DOCKING_BASE,
                    genK = DOUBLE_AGENT,
                    genL = LOCATION,
                    genM = ORDNANCE_COUNTS,
                    genN = TUBES,
                    bindFn = ::Properties,
                ),
            ) { player, test ->
                test.updateDirectly(player)
                shouldThrowUnit<IllegalArgumentException> { test.updateThroughPlayerDsl(player) }
                shouldThrowUnit<IllegalArgumentException> { test.updateThroughWeaponsDsl(player) }
                shouldThrowUnit<IllegalArgumentException> { test.updateThroughUpgradesDsl(player) }
            }
        }

        override suspend fun describeTestCreateAndUpdatePartially(
            scope: DescribeSpecContainerScope
        ) {
            super.describeTestCreateAndUpdatePartially(scope)

            scope.describe("Ordnance count") {
                withData(OrdnanceType.entries) { ordnanceType ->
                    checkAll(arbObject, Arb.byte(min = 0)) { player, count ->
                        ArtemisPlayer.Dsl.Weapons.ordnanceCounts[ordnanceType] = count
                        ArtemisPlayer.Dsl.Weapons updates player
                        player.hasData.shouldBeTrue()
                        player.hasWeaponsData.shouldBeTrue()
                        repeat(OrdnanceType.size) { index ->
                            player.ordnanceCounts[index]
                                .hasValue
                                .shouldBeEqual(ordnanceType.ordinal == index)
                        }
                    }
                }
            }

            scope.describe("Tube state") {
                repeat(Artemis.MAX_TUBES) { i ->
                    it("Index: $i") {
                        checkAll(arbObject, Arb.enum<TubeState>()) { player, state ->
                            ArtemisPlayer.Dsl.Weapons.tubeStates[i] = state
                            ArtemisPlayer.Dsl.Weapons updates player
                            player.hasData.shouldBeTrue()
                            player.hasWeaponsData.shouldBeTrue()
                            repeat(Artemis.MAX_TUBES) { j ->
                                val hasData = i == j
                                player.tubes[j].hasData.shouldBeEqual(hasData)
                                player.tubes[j].state.hasValue.shouldBeEqual(hasData)
                            }
                        }
                    }
                }
            }

            scope.describe("Tube contents") {
                repeat(Artemis.MAX_TUBES) { i ->
                    it("Index: $i") {
                        checkAll(arbObject, Arb.enum<OrdnanceType>()) { player, ordnance ->
                            ArtemisPlayer.Dsl.Weapons.tubeContents[i] = ordnance
                            ArtemisPlayer.Dsl.Weapons updates player
                            player.hasData.shouldBeTrue()
                            player.hasWeaponsData.shouldBeTrue()
                            repeat(Artemis.MAX_TUBES) { j ->
                                val hasData = i == j
                                player.tubes[j].hasData.shouldBeEqual(hasData)
                                player.tubes[j].lastContents.hasValue.shouldBeEqual(hasData)
                            }
                        }
                    }
                }
            }
        }

        enum class DockInvalidatingTestCase(val impulseArb: Arb<Float>, val warpArb: Arb<Byte>) :
            WithDataTestName {
            IMPULSE(Arb.numericFloat(min = Float.MIN_VALUE), Arb.of(0)) {
                override fun update(player: ArtemisPlayer, impulse: Float, warp: Byte) {
                    player.impulse.value = impulse
                }
            },
            WARP(Arb.of(0f), Arb.byte(min = 1, max = 4)) {
                override fun update(player: ArtemisPlayer, impulse: Float, warp: Byte) {
                    player.warp.value = warp
                }
            },
            IMPULSE_AND_WARP(Arb.numericFloat(min = Float.MIN_VALUE), Arb.byte(min = 1, max = 4)) {
                override fun update(player: ArtemisPlayer, impulse: Float, warp: Byte) {
                    player.impulse.value = impulse
                    player.warp.value = warp
                }
            };

            abstract fun update(player: ArtemisPlayer, impulse: Float, warp: Byte)

            override fun dataTestName(): String = "At ${name.lowercase().replace('_', ' ')}"
        }

        override fun DescribeSpecContainerScope.describeMore() = launch {
            describeVesselDataTests(arbObject, HULL_ID)
            describeFullNameTests(arbObject, NAME)

            describe("Invalid warp value throws") {
                withData(
                    nameFn = { it.first },
                    "Negative number" to Arb.byte(max = -2),
                    "Higher than 4" to Arb.byte(min = 5),
                ) { (_, testGen) ->
                    checkAll(arbObject, testGen) { player, warp ->
                        shouldThrowUnit<IllegalArgumentException> { player.warp.value = warp }
                    }
                }
            }

            describe("Undock when moving") {
                withData(DockInvalidatingTestCase.entries) { test ->
                    checkAll(
                        genA = arbObjectPair,
                        genB = Arb.int().filter { it != -1 },
                        genC = test.impulseArb,
                        genD = test.warpArb,
                    ) { (playerA, playerB), dockingBase, impulse, warp ->
                        playerA.dockingBase.value = dockingBase
                        playerA.docked = BoolState.True

                        test.update(playerB, impulse, warp)
                        playerB updates playerA

                        playerA.dockingBase shouldContainValue 0
                        playerA.docked.shouldBeFalse()
                    }
                }
            }
        }

        private fun <V, P : Property<V, P>> partialPlayerUpdateTest(
            name: String,
            propGen: Gen<V>,
            dslProperty: KMutableProperty0<V>,
            getProperty: (ArtemisPlayer) -> P,
        ): PartialUpdateTestSuite<ArtemisPlayer, ArtemisPlayer.Dsl, V, P> =
            partialUpdateTest(
                name = name,
                propGen = propGen,
                dslProperty = dslProperty,
                dsl = ArtemisPlayer.Dsl.Player,
                getProperty = getProperty,
            )

        private fun <V, P : Property<V, P>> partialUpgradesUpdateTest(
            name: String,
            propGen: Gen<V>,
            dslProperty: KMutableProperty0<V>,
            getProperty: (ArtemisPlayer) -> P,
        ): PartialUpdateTestSuite<ArtemisPlayer, ArtemisPlayer.Dsl, V, P> =
            partialUpdateTest(
                name = name,
                propGen = propGen,
                dslProperty = dslProperty,
                dsl = ArtemisPlayer.Dsl.Upgrades,
                getProperty = getProperty,
            )

        private fun <DSL : ArtemisPlayer.Dsl, V, P : Property<V, P>> partialUpdateTest(
            name: String,
            propGen: Gen<V>,
            dslProperty: KMutableProperty0<V>,
            dsl: DSL,
            getProperty: (ArtemisPlayer) -> P,
        ): PartialUpdateTestSuite<ArtemisPlayer, DSL, V, P> =
            PartialUpdateTestSuite(
                name = name,
                objectGen = arbObject,
                propGen = propGen,
                dslProperty = dslProperty,
                dsl = dsl,
                getProperty = getProperty,
            )
    }
}
