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
import io.kotest.datatest.WithDataTestName
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
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
import kotlin.reflect.KProperty1

internal sealed class ObjectTestSuite<T : BaseArtemisObject<T>>(
    protected val objectType: ObjectType,
) {
    class Location(val x: Float, val y: Float, val z: Float)

    companion object {
        val X = Arb.numericFloat()
        val Y = Arb.numericFloat()
        val Z = Arb.numericFloat()
        val LOCATION = Arb.bind(X, Y, Z, ::Location)

        suspend fun DescribeSpecContainerScope.describeVesselDataTests(
            arbObject: Gen<BaseArtemisShielded<*>>,
            arbHullId: Gen<Int>,
        ) {
            describe("Vessel") {
                val mockVesselData = mockk<VesselData> {
                    every { this@mockk[any()] } returns mockk<Vessel>()
                }

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
                        TestVessel.arbitrary().flatMap {
                            Arb.pair(
                                Arb.vesselData(
                                    factions = listOf(),
                                    vessels = Arb.of(it),
                                    numVessels = 1..1,
                                ),
                                Arb.of(it.id),
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
    }

    protected abstract class BaseProperties<T : ArtemisObject<T>>(val location: Location) {
        open fun updateDirectly(obj: T) {
            obj.x.value = location.x
            obj.y.value = location.y
            obj.z.value = location.z
        }
        abstract fun createThroughDsl(id: Int, timestamp: Long): T
        abstract fun updateThroughDsl(obj: T)
        abstract fun testKnownObject(obj: T)
    }

    protected data class PartialUpdateTestSuite<
        AO : BaseArtemisObject<AO>,
        DSL : BaseArtemisObject.Dsl<AO>,
        V,
        P : Property<V, P>
    >(
        val name: String,
        val objectGen: Gen<AO>,
        val propGen: Gen<V>,
        val property: KProperty1<AO, P>,
        val dslProperty: KMutableProperty0<V>,
        val dsl: DSL,
    ) {
        suspend fun testPartiallyUpdatedObject(test: (AO) -> Unit) {
            checkAll(objectGen, propGen) { obj, value ->
                dslProperty.setter.call(value)
                dsl updates obj
                obj.hasData.shouldBeTrue()
                test(obj)
            }
        }
    }

    data object Base : ObjectTestSuite<ArtemisBase>(ObjectType.BASE) {
        private val NAME = Arb.string()
        private val SHIELDS = Arb.numericFloat()
        private val SHIELDS_MAX = Arb.numericFloat()
        private val HULL_ID = Arb.int().filter { it != -1 }

        private class Properties(
            private val name: String,
            private val shields: Float,
            private val shieldsMax: Float,
            private val hullId: Int,
            location: Location,
        ) : BaseProperties<ArtemisBase>(location) {
            override fun updateDirectly(obj: ArtemisBase) {
                super.updateDirectly(obj)
                obj.name.value = name
                obj.shieldsFront.value = shields
                obj.shieldsFrontMax.value = shieldsMax
                obj.hullId.value = hullId
            }

            override fun createThroughDsl(id: Int, timestamp: Long): ArtemisBase =
                ArtemisBase.Dsl.let {
                    it.name = name
                    it.shieldsFront = shields
                    it.shieldsFrontMax = shieldsMax
                    it.hullId = hullId
                    it.x = location.x
                    it.y = location.y
                    it.z = location.z

                    it.create(id, timestamp).apply { it.shouldBeReset() }
                }

            override fun updateThroughDsl(obj: ArtemisBase) {
                ArtemisBase.Dsl.also {
                    it.name = name
                    it.shieldsFront = shields
                    it.shieldsFrontMax = shieldsMax
                    it.hullId = hullId
                    it.x = location.x
                    it.y = location.y
                    it.z = location.z

                    it updates obj
                }.shouldBeReset()
            }

            override fun testKnownObject(obj: ArtemisBase) {
                obj.shouldBeKnownObject(
                    obj.id,
                    objectType,
                    name,
                    location.x,
                    location.y,
                    location.z,
                    hullId,
                    shields,
                    shieldsMax,
                )
            }
        }

        override val arbObject: Arb<ArtemisBase> = Arb.bind()
        override val arbObjectPair: Arb<Pair<ArtemisBase, ArtemisBase>> = Arb.bind(
            Arb.int(),
            Arb.long(),
            Arb.long(),
        ) { id, timestampA, timestampB ->
            Pair(
                ArtemisBase(id, min(timestampA, timestampB)),
                ArtemisBase(id, max(timestampA, timestampB)),
            )
        }

        override val partialUpdateTestSuites = listOf(
            PartialUpdateTestSuite(
                "Name",
                arbObject,
                NAME,
                ArtemisBase::name,
                ArtemisBase.Dsl::name,
                ArtemisBase.Dsl,
            ),
            PartialUpdateTestSuite(
                "Shields",
                arbObject,
                SHIELDS,
                ArtemisBase::shieldsFront,
                ArtemisBase.Dsl::shieldsFront,
                ArtemisBase.Dsl,
            ),
            PartialUpdateTestSuite(
                "Shields max",
                arbObject,
                SHIELDS_MAX,
                ArtemisBase::shieldsFrontMax,
                ArtemisBase.Dsl::shieldsFrontMax,
                ArtemisBase.Dsl,
            ),
            PartialUpdateTestSuite(
                "Hull ID",
                arbObject,
                HULL_ID,
                ArtemisBase::hullId,
                ArtemisBase.Dsl::hullId,
                ArtemisBase.Dsl,
            ),
            PartialUpdateTestSuite(
                "X",
                arbObject,
                X,
                ArtemisBase::x,
                ArtemisBase.Dsl::x,
                ArtemisBase.Dsl,
            ),
            PartialUpdateTestSuite(
                "Y",
                arbObject,
                Y,
                ArtemisBase::y,
                ArtemisBase.Dsl::y,
                ArtemisBase.Dsl,
            ),
            PartialUpdateTestSuite(
                "Z",
                arbObject,
                Z,
                ArtemisBase::z,
                ArtemisBase.Dsl::z,
                ArtemisBase.Dsl,
            ),
        )

        override suspend fun testCreateUnknown() {
            arbObject.checkAll { it.shouldBeUnknownObject(it.id, objectType) }
        }

        override suspend fun testCreateFromDsl() {
            checkAll(
                Arb.int(),
                Arb.long(),
                Arb.bind(NAME, SHIELDS, SHIELDS_MAX, HULL_ID, LOCATION, ::Properties),
            ) { id, timestamp, test ->
                shouldNotThrow<IllegalStateException> {
                    test.testKnownObject(test.createThroughDsl(id, timestamp))
                }
            }
        }

        override suspend fun testCreateAndUpdateManually() {
            checkAll(
                arbObject,
                Arb.bind(NAME, SHIELDS, SHIELDS_MAX, HULL_ID, LOCATION, ::Properties),
            ) { base, test ->
                test.updateDirectly(base)
                test.testKnownObject(base)
            }
        }

        override suspend fun testCreateAndUpdateFromDsl() {
            checkAll(
                arbObject,
                Arb.bind(NAME, SHIELDS, SHIELDS_MAX, HULL_ID, LOCATION, ::Properties),
            ) { base, test ->
                test.updateThroughDsl(base)
                test.testKnownObject(base)
            }
        }

        override suspend fun testUnknownObjectDoesNotProvideUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(NAME, SHIELDS, SHIELDS_MAX, HULL_ID, LOCATION, ::Properties),
            ) { (oldBase, newBase), test ->
                test.updateDirectly(oldBase)
                newBase updates oldBase
                test.testKnownObject(oldBase)
            }
        }

        override suspend fun testKnownObjectProvidesUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(NAME, SHIELDS, SHIELDS_MAX, HULL_ID, LOCATION, ::Properties),
            ) { (oldBase, newBase), test ->
                test.updateDirectly(newBase)
                newBase updates oldBase
                test.testKnownObject(oldBase)
            }
        }

        override suspend fun testDslCannotUpdateKnownObject() {
            checkAll(
                arbObject,
                Arb.bind(NAME, SHIELDS, SHIELDS_MAX, HULL_ID, LOCATION, ::Properties),
            ) { base, test ->
                test.updateDirectly(base)
                shouldThrowUnit<IllegalArgumentException> { test.updateThroughDsl(base) }
            }
        }

        override suspend fun DescribeSpecContainerScope.describeMore() {
            describeVesselDataTests(arbObject, HULL_ID)
        }
    }

    data object BlackHole : ObjectTestSuite<ArtemisBlackHole>(ObjectType.BLACK_HOLE) {
        private class Properties(location: Location) : BaseProperties<ArtemisBlackHole>(location) {
            override fun createThroughDsl(id: Int, timestamp: Long): ArtemisBlackHole =
                ArtemisBlackHole.Dsl.let {
                    it.x = location.x
                    it.y = location.y
                    it.z = location.z

                    it.create(id, timestamp).apply { it.shouldBeReset() }
                }

            override fun updateThroughDsl(obj: ArtemisBlackHole) {
                ArtemisBlackHole.Dsl.also {
                    it.x = location.x
                    it.y = location.y
                    it.z = location.z

                    it updates obj
                }.shouldBeReset()
            }

            override fun testKnownObject(obj: ArtemisBlackHole) {
                obj.shouldBeKnownObject(
                    obj.id,
                    objectType,
                    location.x,
                    location.y,
                    location.z,
                )
            }
        }

        override val arbObject: Arb<ArtemisBlackHole> = Arb.bind()
        override val arbObjectPair: Arb<Pair<ArtemisBlackHole, ArtemisBlackHole>> = Arb.bind(
            Arb.int(),
            Arb.long(),
            Arb.long(),
        ) { id, timestampA, timestampB ->
            Pair(
                ArtemisBlackHole(id, min(timestampA, timestampB)),
                ArtemisBlackHole(id, max(timestampA, timestampB)),
            )
        }

        override val partialUpdateTestSuites = listOf(
            PartialUpdateTestSuite(
                "X",
                arbObject,
                X,
                ArtemisBlackHole::x,
                ArtemisBlackHole.Dsl::x,
                ArtemisBlackHole.Dsl,
            ),
            PartialUpdateTestSuite(
                "Y",
                arbObject,
                Y,
                ArtemisBlackHole::y,
                ArtemisBlackHole.Dsl::y,
                ArtemisBlackHole.Dsl,
            ),
            PartialUpdateTestSuite(
                "Z",
                arbObject,
                Z,
                ArtemisBlackHole::z,
                ArtemisBlackHole.Dsl::z,
                ArtemisBlackHole.Dsl,
            ),
        )

        override suspend fun testCreateUnknown() {
            arbObject.checkAll { it.shouldBeUnknownObject(it.id, objectType) }
        }

        override suspend fun testCreateFromDsl() {
            checkAll(
                Arb.int(),
                Arb.long(),
                LOCATION.map(::Properties),
            ) { id, timestamp, test ->
                shouldNotThrow<IllegalStateException> {
                    test.testKnownObject(test.createThroughDsl(id, timestamp))
                }
            }
        }

        override suspend fun testCreateAndUpdateManually() {
            checkAll(
                arbObject,
                LOCATION.map(::Properties),
            ) { blackHole, test ->
                test.updateDirectly(blackHole)
                test.testKnownObject(blackHole)
            }
        }

        override suspend fun testCreateAndUpdateFromDsl() {
            checkAll(
                arbObject,
                LOCATION.map(::Properties),
            ) { blackHole, test ->
                test.updateThroughDsl(blackHole)
                test.testKnownObject(blackHole)
            }
        }

        override suspend fun testUnknownObjectDoesNotProvideUpdates() {
            checkAll(
                arbObjectPair,
                LOCATION.map(::Properties),
            ) { (oldBlackHole, newBlackHole), test ->
                test.updateDirectly(oldBlackHole)
                newBlackHole updates oldBlackHole
                test.testKnownObject(oldBlackHole)
            }
        }

        override suspend fun testKnownObjectProvidesUpdates() {
            checkAll(
                arbObjectPair,
                LOCATION.map(::Properties),
            ) { (oldBlackHole, newBlackHole), test ->
                test.updateDirectly(newBlackHole)
                newBlackHole updates oldBlackHole
                test.testKnownObject(oldBlackHole)
            }
        }

        override suspend fun testDslCannotUpdateKnownObject() {
            checkAll(
                arbObject,
                LOCATION.map(::Properties),
            ) { blackHole, test ->
                test.updateDirectly(blackHole)
                shouldThrowUnit<IllegalArgumentException> { test.updateThroughDsl(blackHole) }
            }
        }
    }

    data object Creature : ObjectTestSuite<ArtemisCreature>(ObjectType.CREATURE) {
        private val IS_NOT_TYPHON = Arb.boolState()

        private class Properties(
            private val isNotTyphon: BoolState,
            location: Location,
        ) : BaseProperties<ArtemisCreature>(location) {
            override fun updateDirectly(obj: ArtemisCreature) {
                super.updateDirectly(obj)
                obj.isNotTyphon.value = isNotTyphon
            }

            override fun createThroughDsl(id: Int, timestamp: Long): ArtemisCreature =
                ArtemisCreature.Dsl.let {
                    ArtemisCreature.Dsl.isNotTyphon = isNotTyphon
                    it.x = location.x
                    it.y = location.y
                    it.z = location.z

                    it.create(id, timestamp).apply { it.shouldBeReset() }
                }

            override fun updateThroughDsl(obj: ArtemisCreature) {
                ArtemisCreature.Dsl.also {
                    ArtemisCreature.Dsl.isNotTyphon = isNotTyphon
                    it.x = location.x
                    it.y = location.y
                    it.z = location.z

                    it updates obj
                }.shouldBeReset()
            }

            override fun testKnownObject(obj: ArtemisCreature) {
                obj.shouldBeKnownObject(
                    obj.id,
                    objectType,
                    location.x,
                    location.y,
                    location.z,
                )
                obj.isNotTyphon shouldContainValue isNotTyphon
            }
        }

        override val arbObject: Arb<ArtemisCreature> = Arb.bind()
        override val arbObjectPair: Arb<Pair<ArtemisCreature, ArtemisCreature>> = Arb.bind(
            Arb.int(),
            Arb.long(),
            Arb.long(),
        ) { id, timestampA, timestampB ->
            Pair(
                ArtemisCreature(id, min(timestampA, timestampB)),
                ArtemisCreature(id, max(timestampA, timestampB)),
            )
        }

        override val partialUpdateTestSuites = listOf(
            PartialUpdateTestSuite(
                "Is not typhon",
                arbObject,
                IS_NOT_TYPHON,
                ArtemisCreature::isNotTyphon,
                ArtemisCreature.Dsl::isNotTyphon,
                ArtemisCreature.Dsl,
            ),
            PartialUpdateTestSuite(
                "X",
                arbObject,
                X,
                ArtemisCreature::x,
                ArtemisCreature.Dsl::x,
                ArtemisCreature.Dsl,
            ),
            PartialUpdateTestSuite(
                "Y",
                arbObject,
                Y,
                ArtemisCreature::y,
                ArtemisCreature.Dsl::y,
                ArtemisCreature.Dsl,
            ),
            PartialUpdateTestSuite(
                "Z",
                arbObject,
                Z,
                ArtemisCreature::z,
                ArtemisCreature.Dsl::z,
                ArtemisCreature.Dsl,
            ),
        )

        override suspend fun testCreateUnknown() {
            arbObject.checkAll {
                it.shouldBeUnknownObject(it.id, objectType)
                it.isNotTyphon.shouldBeUnspecified()
            }
        }

        override suspend fun testCreateFromDsl() {
            checkAll(
                Arb.int(),
                Arb.long(),
                Arb.bind(IS_NOT_TYPHON, LOCATION, ::Properties),
            ) { id, timestamp, test ->
                shouldNotThrow<IllegalStateException> {
                    test.testKnownObject(test.createThroughDsl(id, timestamp))
                }
            }
        }

        override suspend fun testCreateAndUpdateManually() {
            checkAll(
                arbObject,
                Arb.bind(IS_NOT_TYPHON, LOCATION, ::Properties),
            ) { creature, test ->
                test.updateDirectly(creature)
                test.testKnownObject(creature)
            }
        }

        override suspend fun testCreateAndUpdateFromDsl() {
            checkAll(
                arbObject,
                Arb.bind(IS_NOT_TYPHON, LOCATION, ::Properties),
            ) { creature, test ->
                test.updateThroughDsl(creature)
                test.testKnownObject(creature)
            }
        }

        override suspend fun testUnknownObjectDoesNotProvideUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(IS_NOT_TYPHON, LOCATION, ::Properties),
            ) { (oldCreature, newCreature), test ->
                test.updateDirectly(oldCreature)
                newCreature updates oldCreature
                test.testKnownObject(oldCreature)
            }
        }

        override suspend fun testKnownObjectProvidesUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(IS_NOT_TYPHON, LOCATION, ::Properties),
            ) { (oldCreature, newCreature), test ->
                test.updateDirectly(newCreature)
                newCreature updates oldCreature
                test.testKnownObject(oldCreature)
            }
        }

        override suspend fun testDslCannotUpdateKnownObject() {
            checkAll(
                arbObject,
                Arb.bind(IS_NOT_TYPHON, LOCATION, ::Properties),
            ) { creature, test ->
                test.updateDirectly(creature)
                shouldThrowUnit<IllegalArgumentException> { test.updateThroughDsl(creature) }
            }
        }
    }

    data object Mine : ObjectTestSuite<ArtemisMine>(ObjectType.MINE) {
        private class Properties(location: Location) : BaseProperties<ArtemisMine>(location) {
            override fun createThroughDsl(id: Int, timestamp: Long): ArtemisMine =
                ArtemisMine.Dsl.let {
                    it.x = location.x
                    it.y = location.y
                    it.z = location.z

                    it.create(id, timestamp).apply { it.shouldBeReset() }
                }

            override fun updateThroughDsl(obj: ArtemisMine) {
                ArtemisMine.Dsl.also {
                    it.x = location.x
                    it.y = location.y
                    it.z = location.z

                    it updates obj
                }.shouldBeReset()
            }

            override fun testKnownObject(obj: ArtemisMine) {
                obj.shouldBeKnownObject(
                    obj.id,
                    objectType,
                    location.x,
                    location.y,
                    location.z,
                )
            }
        }

        override val arbObject: Arb<ArtemisMine> = Arb.bind()
        override val arbObjectPair: Arb<Pair<ArtemisMine, ArtemisMine>> = Arb.bind(
            Arb.int(),
            Arb.long(),
            Arb.long(),
        ) { id, timestampA, timestampB ->
            Pair(
                ArtemisMine(id, min(timestampA, timestampB)),
                ArtemisMine(id, max(timestampA, timestampB)),
            )
        }

        override val partialUpdateTestSuites = listOf(
            PartialUpdateTestSuite(
                "X",
                arbObject,
                X,
                ArtemisMine::x,
                ArtemisMine.Dsl::x,
                ArtemisMine.Dsl,
            ),
            PartialUpdateTestSuite(
                "Y",
                arbObject,
                Y,
                ArtemisMine::y,
                ArtemisMine.Dsl::y,
                ArtemisMine.Dsl,
            ),
            PartialUpdateTestSuite(
                "Z",
                arbObject,
                Z,
                ArtemisMine::z,
                ArtemisMine.Dsl::z,
                ArtemisMine.Dsl,
            ),
        )

        override suspend fun testCreateUnknown() {
            arbObject.checkAll { it.shouldBeUnknownObject(it.id, objectType) }
        }

        override suspend fun testCreateFromDsl() {
            checkAll(
                Arb.int(),
                Arb.long(),
                LOCATION.map(::Properties),
            ) { id, timestamp, test ->
                shouldNotThrow<IllegalStateException> {
                    test.testKnownObject(test.createThroughDsl(id, timestamp))
                }
            }
        }

        override suspend fun testCreateAndUpdateManually() {
            checkAll(
                arbObject,
                LOCATION.map(::Properties),
            ) { mine, test ->
                test.updateDirectly(mine)
                test.testKnownObject(mine)
            }
        }

        override suspend fun testCreateAndUpdateFromDsl() {
            checkAll(
                arbObject,
                LOCATION.map(::Properties),
            ) { mine, test ->
                test.updateThroughDsl(mine)
                test.testKnownObject(mine)
            }
        }

        override suspend fun testUnknownObjectDoesNotProvideUpdates() {
            checkAll(
                arbObjectPair,
                LOCATION.map(::Properties),
            ) { (oldMine, newMine), test ->
                test.updateDirectly(oldMine)
                newMine updates oldMine
                test.testKnownObject(oldMine)
            }
        }

        override suspend fun testKnownObjectProvidesUpdates() {
            Arb.pair(
                arbObject,
                LOCATION.map(::Properties),
            ).flatMap { (mine, test) ->
                Arb.long().filter { it != mine.timestamp }.map { timestamp ->
                    val otherMine = ArtemisMine(mine.id, timestamp)
                    if (timestamp < mine.timestamp) {
                        Triple(otherMine, mine, test)
                    } else {
                        Triple(mine, otherMine, test)
                    }
                }
            }.checkAll { (oldMine, newMine, test) ->
                test.updateDirectly(newMine)
                newMine updates oldMine
                test.testKnownObject(oldMine)
            }
        }

        override suspend fun testDslCannotUpdateKnownObject() {
            checkAll(
                arbObject,
                LOCATION.map(::Properties),
            ) { mine, test ->
                test.updateDirectly(mine)
                shouldThrowUnit<IllegalArgumentException> { test.updateThroughDsl(mine) }
            }
        }
    }

    data object Npc : ObjectTestSuite<ArtemisNpc>(ObjectType.NPC_SHIP) {
        private val NAME = Arb.string()
        private val SHIELDS_FRONT = Arb.numericFloat()
        private val SHIELDS_FRONT_MAX = Arb.numericFloat()
        private val SHIELDS_REAR = Arb.numericFloat()
        private val SHIELDS_REAR_MAX = Arb.numericFloat()
        private val SHIELDS = Arb.pair(
            Arb.pair(SHIELDS_FRONT, SHIELDS_FRONT_MAX),
            Arb.pair(SHIELDS_REAR, SHIELDS_REAR_MAX),
        )
        private val HULL_ID = Arb.int().filter { it != -1 }
        private val IMPULSE = Arb.numericFloat()
        private val IS_ENEMY = Arb.boolState()
        private val IS_SURRENDERED = Arb.boolState()
        private val IN_NEBULA = Arb.boolState()
        private val SCAN_BITS = Arb.int()
        private val SIDE = Arb.byte().filter { it.toInt() != -1 }

        private class Properties(
            private val name: String,
            private val shields: Pair<Pair<Float, Float>, Pair<Float, Float>>,
            private val hullId: Int,
            private val impulse: Float,
            private val isEnemy: BoolState,
            private val isSurrendered: BoolState,
            private val inNebula: BoolState,
            private val scanBits: Int,
            private val side: Byte,
            location: Location,
        ) : BaseProperties<ArtemisNpc>(location) {
            override fun updateDirectly(obj: ArtemisNpc) {
                super.updateDirectly(obj)
                obj.name.value = name
                obj.shieldsFront.value = shields.first.first
                obj.shieldsFrontMax.value = shields.first.second
                obj.shieldsRear.value = shields.second.first
                obj.shieldsRearMax.value = shields.second.second
                obj.hullId.value = hullId
                obj.impulse.value = impulse
                obj.isEnemy.value = isEnemy
                obj.isSurrendered.value = isSurrendered
                obj.isInNebula.value = inNebula
                obj.scanBits.value = scanBits
                obj.side.value = side
            }

            override fun createThroughDsl(id: Int, timestamp: Long): ArtemisNpc =
                ArtemisNpc.Dsl.let {
                    it.name = name
                    it.shieldsFront = shields.first.first
                    it.shieldsFrontMax = shields.first.second
                    it.shieldsRear = shields.second.first
                    it.shieldsRearMax = shields.second.second
                    it.hullId = hullId
                    it.impulse = impulse
                    ArtemisNpc.Dsl.isEnemy = isEnemy
                    ArtemisNpc.Dsl.isSurrendered = isSurrendered
                    ArtemisNpc.Dsl.isInNebula = inNebula
                    ArtemisNpc.Dsl.scanBits = scanBits
                    it.side = side
                    it.x = location.x
                    it.y = location.y
                    it.z = location.z

                    it.create(id, timestamp).apply { it.shouldBeReset() }
                }

            override fun updateThroughDsl(obj: ArtemisNpc) {
                ArtemisNpc.Dsl.also {
                    it.name = name
                    it.shieldsFront = shields.first.first
                    it.shieldsFrontMax = shields.first.second
                    it.shieldsRear = shields.second.first
                    it.shieldsRearMax = shields.second.second
                    it.hullId = hullId
                    it.impulse = impulse
                    ArtemisNpc.Dsl.isEnemy = isEnemy
                    ArtemisNpc.Dsl.isSurrendered = isSurrendered
                    ArtemisNpc.Dsl.isInNebula = inNebula
                    ArtemisNpc.Dsl.scanBits = scanBits
                    it.side = side
                    it.x = location.x
                    it.y = location.y
                    it.z = location.z

                    it updates obj
                }.shouldBeReset()
            }

            override fun testKnownObject(obj: ArtemisNpc) {
                obj.shouldBeKnownObject(
                    obj.id,
                    objectType,
                    name,
                    location.x,
                    location.y,
                    location.z,
                    hullId,
                    shields.first.first,
                    shields.first.second,
                    shields.second.first,
                    shields.second.second,
                    impulse,
                    side,
                )

                obj.isEnemy shouldContainValue isEnemy
                obj.isSurrendered shouldContainValue isSurrendered
                obj.isInNebula shouldContainValue inNebula
                obj.scanBits shouldContainValue scanBits
            }
        }

        override val arbObject: Arb<ArtemisNpc> = Arb.bind()
        override val arbObjectPair: Arb<Pair<ArtemisNpc, ArtemisNpc>> = Arb.bind(
            Arb.int(),
            Arb.long(),
            Arb.long(),
        ) { id, timestampA, timestampB ->
            Pair(
                ArtemisNpc(id, min(timestampA, timestampB)),
                ArtemisNpc(id, max(timestampA, timestampB)),
            )
        }

        override val partialUpdateTestSuites = listOf(
            PartialUpdateTestSuite(
                "Name",
                arbObject,
                NAME,
                ArtemisNpc::name,
                ArtemisNpc.Dsl::name,
                ArtemisNpc.Dsl,
            ),
            PartialUpdateTestSuite(
                "Front shields",
                arbObject,
                SHIELDS_FRONT,
                ArtemisNpc::shieldsFront,
                ArtemisNpc.Dsl::shieldsFront,
                ArtemisNpc.Dsl,
            ),
            PartialUpdateTestSuite(
                "Front shields max",
                arbObject,
                SHIELDS_FRONT_MAX,
                ArtemisNpc::shieldsFrontMax,
                ArtemisNpc.Dsl::shieldsFrontMax,
                ArtemisNpc.Dsl,
            ),
            PartialUpdateTestSuite(
                "Rear shields",
                arbObject,
                SHIELDS_REAR,
                ArtemisNpc::shieldsRear,
                ArtemisNpc.Dsl::shieldsRear,
                ArtemisNpc.Dsl,
            ),
            PartialUpdateTestSuite(
                "Rear shields max",
                arbObject,
                SHIELDS_REAR_MAX,
                ArtemisNpc::shieldsRearMax,
                ArtemisNpc.Dsl::shieldsRearMax,
                ArtemisNpc.Dsl,
            ),
            PartialUpdateTestSuite(
                "Hull ID",
                arbObject,
                HULL_ID,
                ArtemisNpc::hullId,
                ArtemisNpc.Dsl::hullId,
                ArtemisNpc.Dsl,
            ),
            PartialUpdateTestSuite(
                "Impulse",
                arbObject,
                IMPULSE,
                ArtemisNpc::impulse,
                ArtemisNpc.Dsl::impulse,
                ArtemisNpc.Dsl,
            ),
            PartialUpdateTestSuite(
                "Is enemy",
                arbObject,
                IS_ENEMY,
                ArtemisNpc::isEnemy,
                ArtemisNpc.Dsl::isEnemy,
                ArtemisNpc.Dsl,
            ),
            PartialUpdateTestSuite(
                "Is surrendered",
                arbObject,
                IS_SURRENDERED,
                ArtemisNpc::isSurrendered,
                ArtemisNpc.Dsl::isSurrendered,
                ArtemisNpc.Dsl,
            ),
            PartialUpdateTestSuite(
                "Is in nebula",
                arbObject,
                IN_NEBULA,
                ArtemisNpc::isInNebula,
                ArtemisNpc.Dsl::isInNebula,
                ArtemisNpc.Dsl,
            ),
            PartialUpdateTestSuite(
                "Scan bits",
                arbObject,
                SCAN_BITS,
                ArtemisNpc::scanBits,
                ArtemisNpc.Dsl::scanBits,
                ArtemisNpc.Dsl,
            ),
            PartialUpdateTestSuite(
                "Side",
                arbObject,
                SIDE,
                ArtemisNpc::side,
                ArtemisNpc.Dsl::side,
                ArtemisNpc.Dsl,
            ),
            PartialUpdateTestSuite(
                "X",
                arbObject,
                X,
                ArtemisNpc::x,
                ArtemisNpc.Dsl::x,
                ArtemisNpc.Dsl,
            ),
            PartialUpdateTestSuite(
                "Y",
                arbObject,
                Y,
                ArtemisNpc::y,
                ArtemisNpc.Dsl::y,
                ArtemisNpc.Dsl,
            ),
            PartialUpdateTestSuite(
                "Z",
                arbObject,
                Z,
                ArtemisNpc::z,
                ArtemisNpc.Dsl::z,
                ArtemisNpc.Dsl,
            ),
        )

        override suspend fun testCreateUnknown() {
            arbObject.checkAll {
                it.shouldBeUnknownObject(it.id, objectType)

                it.isEnemy.shouldBeUnspecified()
                it.isInNebula.shouldBeUnspecified()
                it.scanBits.shouldBeUnspecified()
            }
        }

        override suspend fun testCreateFromDsl() {
            checkAll(
                Arb.int(),
                Arb.long(),
                Arb.bind(
                    NAME,
                    SHIELDS,
                    HULL_ID,
                    IMPULSE,
                    IS_ENEMY,
                    IS_SURRENDERED,
                    IN_NEBULA,
                    SCAN_BITS,
                    SIDE,
                    LOCATION,
                    ::Properties,
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
                    NAME,
                    SHIELDS,
                    HULL_ID,
                    IMPULSE,
                    IS_ENEMY,
                    IS_SURRENDERED,
                    IN_NEBULA,
                    SCAN_BITS,
                    SIDE,
                    LOCATION,
                    ::Properties,
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
                    NAME,
                    SHIELDS,
                    HULL_ID,
                    IMPULSE,
                    IS_ENEMY,
                    IS_SURRENDERED,
                    IN_NEBULA,
                    SCAN_BITS,
                    SIDE,
                    LOCATION,
                    ::Properties,
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
                    NAME,
                    SHIELDS,
                    HULL_ID,
                    IMPULSE,
                    IS_ENEMY,
                    IS_SURRENDERED,
                    IN_NEBULA,
                    SCAN_BITS,
                    SIDE,
                    LOCATION,
                    ::Properties,
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
                    NAME,
                    SHIELDS,
                    HULL_ID,
                    IMPULSE,
                    IS_ENEMY,
                    IS_SURRENDERED,
                    IN_NEBULA,
                    SCAN_BITS,
                    SIDE,
                    LOCATION,
                    ::Properties,
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
                    NAME,
                    SHIELDS,
                    HULL_ID,
                    IMPULSE,
                    IS_ENEMY,
                    IS_SURRENDERED,
                    IN_NEBULA,
                    SCAN_BITS,
                    SIDE,
                    LOCATION,
                    ::Properties,
                ),
            ) { npc, test ->
                test.updateDirectly(npc)
                shouldThrowUnit<IllegalArgumentException> { test.updateThroughDsl(npc) }
            }
        }

        override suspend fun DescribeSpecContainerScope.describeMore() {
            describe("Scanned by") {
                describe("Known") {
                    it("Sides") {
                        checkAll(arbObject, SCAN_BITS) { npc, scanBits ->
                            npc.scanBits.value = scanBits
                            BooleanArray(Int.SIZE_BITS) {
                                scanBits and 1.shl(it) != 0
                            }.forEachIndexed { index, expected ->
                                val scanned = npc.hasBeenScannedBy(index.toByte())
                                if (expected) {
                                    scanned shouldBeEqual BoolState.True
                                } else {
                                    scanned shouldBeEqual BoolState.False
                                }
                            }
                        }
                    }

                    it("Ships") {
                        checkAll(
                            arbObjectPair,
                            SCAN_BITS,
                            SIDE,
                        ) { (npc1, npc2), scanBits, side ->
                            npc1.scanBits.value = scanBits
                            npc2.side.value = side

                            val scanned = npc1.hasBeenScannedBy(npc2)
                            if (scanBits and 1.shl(side.toInt()) != 0) {
                                scanned shouldBeEqual BoolState.True
                            } else {
                                scanned shouldBeEqual BoolState.False
                            }
                        }
                    }
                }

                describe("Unknown") {
                    it("Sides") {
                        arbObject.checkAll { npc ->
                            repeat(Int.SIZE_BITS) { side ->
                                npc.hasBeenScannedBy(side.toByte()) shouldBeEqual BoolState.Unknown
                            }
                        }
                    }

                    it("Ships") {
                        checkAll(
                            arbObjectPair,
                            SCAN_BITS,
                        ) { (npc1, npc2), scanBits ->
                            npc1.scanBits.value = scanBits
                            npc1.hasBeenScannedBy(npc2) shouldBeEqual BoolState.Unknown
                        }
                    }
                }
            }

            describeVesselDataTests(arbObject, HULL_ID)
        }
    }

    data object Player : ObjectTestSuite<ArtemisPlayer>(ObjectType.PLAYER_SHIP) {
        private val NAME = Arb.string()
        private val SHIELDS_FRONT = Arb.numericFloat()
        private val SHIELDS_FRONT_MAX = Arb.numericFloat()
        private val SHIELDS_REAR = Arb.numericFloat()
        private val SHIELDS_REAR_MAX = Arb.numericFloat()
        private val SHIELDS = Arb.pair(
            Arb.pair(SHIELDS_FRONT, SHIELDS_FRONT_MAX),
            Arb.pair(SHIELDS_REAR, SHIELDS_REAR_MAX),
        )
        private val HULL_ID = Arb.int().filter { it != -1 }
        private val IMPULSE = Arb.numericFloat()
        private val SIDE = Arb.byte().filter { it.toInt() != -1 }
        private val SHIP_INDEX = Arb.byte().filter { it != Byte.MIN_VALUE }
        private val CAPITAL_SHIP_ID = Arb.int().filter { it != -1 }
        private val ALERT_STATUS = Arb.enum<AlertStatus>()
        private val DRIVE_TYPE = Arb.enum<DriveType>()
        private val ENUMS = Arb.pair(
            ALERT_STATUS,
            DRIVE_TYPE,
        )
        private val WARP = Arb.byte(min = 0, max = Artemis.MAX_WARP)
        private val DOCKING_BASE = Arb.int().filter { it != -1 }
        private val DOUBLE_AGENT_ACTIVE = Arb.boolState()
        private val DOUBLE_AGENT_COUNT = Arb.byte().filter { it.toInt() != -1 }
        private val DOUBLE_AGENT_SECONDS = Arb.int().filter { it != -1 }
        private val DOUBLE_AGENT = Arb.triple(
            DOUBLE_AGENT_ACTIVE,
            DOUBLE_AGENT_COUNT,
            DOUBLE_AGENT_SECONDS,
        )
        private val ORDNANCE_COUNTS = Arb.list(
            Arb.byte(min = 0),
            OrdnanceType.size..OrdnanceType.size,
        )
        private val TUBES = Arb.list(
            Arb.pair(
                Arb.enum<TubeState>(),
                Arb.enum<OrdnanceType>(),
            ),
            Artemis.MAX_TUBES..Artemis.MAX_TUBES,
        )

        private class Properties(
            private val name: String,
            private val shields: Pair<Pair<Float, Float>, Pair<Float, Float>>,
            private val hullId: Int,
            private val impulse: Float,
            private val side: Byte,
            private val shipIndex: Byte,
            private val capitalShipID: Int,
            private val enumStates: Pair<AlertStatus, DriveType>,
            private val warp: Byte,
            private val dockingBase: Int,
            private val doubleAgentStatus: Triple<BoolState, Byte, Int>,
            location: Location,
            private val ordnanceCounts: List<Byte>,
            private val tubes: List<Pair<TubeState, OrdnanceType>>,
        ) : BaseProperties<ArtemisPlayer>(location) {
            override fun updateDirectly(obj: ArtemisPlayer) {
                super.updateDirectly(obj)
                obj.name.value = name
                obj.shieldsFront.value = shields.first.first
                obj.shieldsFrontMax.value = shields.first.second
                obj.shieldsRear.value = shields.second.first
                obj.shieldsRearMax.value = shields.second.second
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
                    obj.tubes[index].also {
                        it.state.value = state
                        it.contents = contents
                    }
                }
            }

            override fun updateThroughDsl(obj: ArtemisPlayer) {
                updateThroughPlayerDsl(obj)
                updateThroughWeaponsDsl(obj)
                updateThroughUpgradesDsl(obj)
            }

            override fun createThroughDsl(id: Int, timestamp: Long): ArtemisPlayer =
                ArtemisPlayer.PlayerDsl.let {
                    it.name = name
                    it.shieldsFront = shields.first.first
                    it.shieldsFrontMax = shields.first.second
                    it.shieldsRear = shields.second.first
                    it.shieldsRearMax = shields.second.second
                    it.hullId = hullId
                    it.impulse = impulse
                    it.side = side
                    it.x = location.x
                    it.y = location.y
                    it.z = location.z
                    ArtemisPlayer.PlayerDsl.shipIndex = shipIndex
                    ArtemisPlayer.PlayerDsl.capitalShipID = capitalShipID
                    ArtemisPlayer.PlayerDsl.alertStatus = enumStates.first
                    ArtemisPlayer.PlayerDsl.driveType = enumStates.second
                    ArtemisPlayer.PlayerDsl.warp = warp
                    ArtemisPlayer.PlayerDsl.dockingBase = dockingBase

                    it.create(id, timestamp).also { player ->
                        updateThroughWeaponsDsl(player)
                        updateThroughUpgradesDsl(player)
                        it.shouldBeReset()
                    }
                }

            override fun testKnownObject(obj: ArtemisPlayer) {
                obj.shouldBeKnownObject(
                    obj.id,
                    objectType,
                    name,
                    location.x,
                    location.y,
                    location.z,
                    hullId,
                    shields.first.first,
                    shields.first.second,
                    shields.second.first,
                    shields.second.second,
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
                ArtemisPlayer.PlayerDsl.also {
                    it.name = name
                    it.shieldsFront = shields.first.first
                    it.shieldsFrontMax = shields.first.second
                    it.shieldsRear = shields.second.first
                    it.shieldsRearMax = shields.second.second
                    it.hullId = hullId
                    it.impulse = impulse
                    it.side = side
                    it.x = location.x
                    it.y = location.y
                    it.z = location.z
                    ArtemisPlayer.PlayerDsl.shipIndex = shipIndex
                    ArtemisPlayer.PlayerDsl.capitalShipID = capitalShipID
                    ArtemisPlayer.PlayerDsl.alertStatus = enumStates.first
                    ArtemisPlayer.PlayerDsl.driveType = enumStates.second
                    ArtemisPlayer.PlayerDsl.warp = warp
                    ArtemisPlayer.PlayerDsl.dockingBase = dockingBase

                    it updates player
                }.shouldBeReset()
            }

            fun updateThroughWeaponsDsl(player: ArtemisPlayer) {
                ArtemisPlayer.WeaponsDsl.also {
                    ordnanceCounts.forEachIndexed { index, count ->
                        ArtemisPlayer.WeaponsDsl.ordnanceCounts[OrdnanceType.entries[index]] = count
                    }
                    tubes.forEachIndexed { index, (state, contents) ->
                        ArtemisPlayer.WeaponsDsl.tubeStates[index] = state
                        ArtemisPlayer.WeaponsDsl.tubeContents[index] = contents
                    }

                    it updates player
                }.shouldBeReset()
            }

            fun updateThroughUpgradesDsl(player: ArtemisPlayer) {
                ArtemisPlayer.UpgradesDsl.also {
                    ArtemisPlayer.UpgradesDsl.doubleAgentActive = doubleAgentStatus.first
                    ArtemisPlayer.UpgradesDsl.doubleAgentCount = doubleAgentStatus.second
                    ArtemisPlayer.UpgradesDsl.doubleAgentSecondsLeft = doubleAgentStatus.third

                    it updates player
                }.shouldBeReset()
            }
        }

        override val arbObject: Arb<ArtemisPlayer> = Arb.bind()
        override val arbObjectPair: Arb<Pair<ArtemisPlayer, ArtemisPlayer>> = Arb.bind(
            Arb.int(),
            Arb.long(),
            Arb.long(),
        ) { id, timestampA, timestampB ->
            Pair(
                ArtemisPlayer(id, min(timestampA, timestampB)),
                ArtemisPlayer(id, max(timestampA, timestampB)),
            )
        }

        override val partialUpdateTestSuites = listOf(
            PartialUpdateTestSuite(
                "Name",
                arbObject,
                NAME,
                ArtemisPlayer::name,
                ArtemisPlayer.PlayerDsl::name,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "Front shields",
                arbObject,
                SHIELDS_FRONT,
                ArtemisPlayer::shieldsFront,
                ArtemisPlayer.PlayerDsl::shieldsFront,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "Front shields max",
                arbObject,
                SHIELDS_FRONT_MAX,
                ArtemisPlayer::shieldsFrontMax,
                ArtemisPlayer.PlayerDsl::shieldsFrontMax,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "Rear shields",
                arbObject,
                SHIELDS_REAR,
                ArtemisPlayer::shieldsRear,
                ArtemisPlayer.PlayerDsl::shieldsRear,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "Rear shields max",
                arbObject,
                SHIELDS_REAR_MAX,
                ArtemisPlayer::shieldsRearMax,
                ArtemisPlayer.PlayerDsl::shieldsRearMax,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "Hull ID",
                arbObject,
                HULL_ID,
                ArtemisPlayer::hullId,
                ArtemisPlayer.PlayerDsl::hullId,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "Impulse",
                arbObject,
                IMPULSE,
                ArtemisPlayer::impulse,
                ArtemisPlayer.PlayerDsl::impulse,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "Warp",
                arbObject,
                WARP,
                ArtemisPlayer::warp,
                ArtemisPlayer.PlayerDsl::warp,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "Side",
                arbObject,
                SIDE,
                ArtemisPlayer::side,
                ArtemisPlayer.PlayerDsl::side,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "Ship index",
                arbObject,
                SHIP_INDEX,
                ArtemisPlayer::shipIndex,
                ArtemisPlayer.PlayerDsl::shipIndex,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "Capital ship ID",
                arbObject,
                CAPITAL_SHIP_ID,
                ArtemisPlayer::capitalShipID,
                ArtemisPlayer.PlayerDsl::capitalShipID,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "Docking base",
                arbObject,
                DOCKING_BASE,
                ArtemisPlayer::dockingBase,
                ArtemisPlayer.PlayerDsl::dockingBase,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "Alert status",
                arbObject,
                ALERT_STATUS,
                ArtemisPlayer::alertStatus,
                ArtemisPlayer.PlayerDsl::alertStatus,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "Drive type",
                arbObject,
                DRIVE_TYPE,
                ArtemisPlayer::driveType,
                ArtemisPlayer.PlayerDsl::driveType,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "X",
                arbObject,
                X,
                ArtemisPlayer::x,
                ArtemisPlayer.PlayerDsl::x,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "Y",
                arbObject,
                Y,
                ArtemisPlayer::y,
                ArtemisPlayer.PlayerDsl::y,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "Z",
                arbObject,
                Z,
                ArtemisPlayer::z,
                ArtemisPlayer.PlayerDsl::z,
                ArtemisPlayer.PlayerDsl,
            ),
            PartialUpdateTestSuite(
                "Double agent active",
                arbObject,
                DOUBLE_AGENT_ACTIVE,
                ArtemisPlayer::doubleAgentActive,
                ArtemisPlayer.UpgradesDsl::doubleAgentActive,
                ArtemisPlayer.UpgradesDsl,
            ),
            PartialUpdateTestSuite(
                "Double agent count",
                arbObject,
                DOUBLE_AGENT_COUNT,
                ArtemisPlayer::doubleAgentCount,
                ArtemisPlayer.UpgradesDsl::doubleAgentCount,
                ArtemisPlayer.UpgradesDsl,
            ),
            PartialUpdateTestSuite(
                "Double agent seconds left",
                arbObject,
                DOUBLE_AGENT_SECONDS,
                ArtemisPlayer::doubleAgentSecondsLeft,
                ArtemisPlayer.UpgradesDsl::doubleAgentSecondsLeft,
                ArtemisPlayer.UpgradesDsl,
            ),
        )

        override suspend fun testCreateUnknown() {
            arbObject.checkAll {
                it.shouldBeUnknownObject(it.id, objectType)

                it.shipIndex.shouldBeUnspecified(Byte.MIN_VALUE)
                it.capitalShipID.shouldBeUnspecified()
                it.doubleAgentActive.shouldBeUnspecified()
                it.doubleAgentCount.shouldBeUnspecified()
                it.doubleAgentSecondsLeft.shouldBeUnspecified()
                it.alertStatus.shouldBeUnspecified()
                it.driveType.shouldBeUnspecified()
                it.warp.shouldBeUnspecified()
                it.dockingBase.shouldBeUnspecified()
                it.ordnanceCounts.forEach { prop -> prop.shouldBeUnspecified() }
                it.tubes.forEach { tube ->
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
                    NAME,
                    SHIELDS,
                    HULL_ID,
                    IMPULSE,
                    SIDE,
                    SHIP_INDEX,
                    CAPITAL_SHIP_ID,
                    ENUMS,
                    WARP,
                    DOCKING_BASE,
                    DOUBLE_AGENT,
                    LOCATION,
                    ORDNANCE_COUNTS,
                    TUBES,
                    ::Properties,
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
                    NAME,
                    SHIELDS,
                    HULL_ID,
                    IMPULSE,
                    SIDE,
                    SHIP_INDEX,
                    CAPITAL_SHIP_ID,
                    ENUMS,
                    WARP,
                    DOCKING_BASE,
                    DOUBLE_AGENT,
                    LOCATION,
                    ORDNANCE_COUNTS,
                    TUBES,
                    ::Properties,
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
                    NAME,
                    SHIELDS,
                    HULL_ID,
                    IMPULSE,
                    SIDE,
                    SHIP_INDEX,
                    CAPITAL_SHIP_ID,
                    ENUMS,
                    WARP,
                    DOCKING_BASE,
                    DOUBLE_AGENT,
                    LOCATION,
                    ORDNANCE_COUNTS,
                    TUBES,
                    ::Properties,
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
                    NAME,
                    SHIELDS,
                    HULL_ID,
                    IMPULSE,
                    SIDE,
                    SHIP_INDEX,
                    CAPITAL_SHIP_ID,
                    ENUMS,
                    WARP,
                    DOCKING_BASE,
                    DOUBLE_AGENT,
                    LOCATION,
                    ORDNANCE_COUNTS,
                    TUBES,
                    ::Properties,
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
                    NAME,
                    SHIELDS,
                    HULL_ID,
                    IMPULSE,
                    SIDE,
                    SHIP_INDEX,
                    CAPITAL_SHIP_ID,
                    ENUMS,
                    WARP,
                    DOCKING_BASE,
                    DOUBLE_AGENT,
                    LOCATION,
                    ORDNANCE_COUNTS,
                    TUBES,
                    ::Properties,
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
                    NAME,
                    SHIELDS,
                    HULL_ID,
                    IMPULSE,
                    SIDE,
                    SHIP_INDEX,
                    CAPITAL_SHIP_ID,
                    ENUMS,
                    WARP,
                    DOCKING_BASE,
                    DOUBLE_AGENT,
                    LOCATION,
                    ORDNANCE_COUNTS,
                    TUBES,
                    ::Properties,
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
                        ArtemisPlayer.WeaponsDsl.ordnanceCounts[ordnanceType] = count
                        ArtemisPlayer.WeaponsDsl updates player
                        player.hasData.shouldBeTrue()
                        player.hasWeaponsData.shouldBeTrue()
                        repeat(OrdnanceType.size) { index ->
                            player.ordnanceCounts[index].hasValue.shouldBeEqual(
                                ordnanceType.ordinal == index
                            )
                        }
                    }
                }
            }

            scope.describe("Tube state") {
                repeat(Artemis.MAX_TUBES) { i ->
                    it("Index: $i") {
                        checkAll(arbObject, Arb.enum<TubeState>()) { player, state ->
                            ArtemisPlayer.WeaponsDsl.tubeStates[i] = state
                            ArtemisPlayer.WeaponsDsl updates player
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
                            ArtemisPlayer.WeaponsDsl.tubeContents[i] = ordnance
                            ArtemisPlayer.WeaponsDsl updates player
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

        enum class DockInvalidatingTestCase(
            val impulseArb: Arb<Float>,
            val warpArb: Arb<Byte>,
        ) : WithDataTestName {
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

        override suspend fun DescribeSpecContainerScope.describeMore() {
            describeVesselDataTests(arbObject, HULL_ID)

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
                        arbObjectPair,
                        Arb.int().filter { it != -1 },
                        test.impulseArb,
                        test.warpArb,
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
    }

    abstract val arbObject: Arb<T>
    abstract val arbObjectPair: Arb<Pair<T, T>>
    protected abstract val partialUpdateTestSuites: List<
        PartialUpdateTestSuite<T, out BaseArtemisObject.Dsl<T>, *, *>
    >
    abstract suspend fun testCreateUnknown()
    abstract suspend fun testCreateFromDsl()
    abstract suspend fun testCreateAndUpdateManually()
    abstract suspend fun testCreateAndUpdateFromDsl()
    abstract suspend fun testUnknownObjectDoesNotProvideUpdates()
    abstract suspend fun testKnownObjectProvidesUpdates()
    abstract suspend fun testDslCannotUpdateKnownObject()

    open suspend fun describeTestCreateAndUpdatePartially(scope: DescribeSpecContainerScope) {
        partialUpdateTestSuites.forEachIndexed { i, testSuite ->
            scope.it(testSuite.name) {
                testSuite.testPartiallyUpdatedObject { base ->
                    partialUpdateTestSuites.forEachIndexed { j, testSuite2 ->
                        testSuite2.property.get(base).hasValue.shouldBeEqual(i == j)
                    }
                }
            }
        }
    }

    private suspend fun DescribeSpecContainerScope.describeTestEquality() {
        describe("Equality") {
            it("Equals itself") {
                arbObject.checkAll { it shouldBeEqual it }
            }

            it("Equal type and ID") {
                arbObjectPair.checkAll { (obj1, obj2) ->
                    obj1 shouldBeEqual obj2
                }
            }

            it("Different ID") {
                arbObject.checkAll { obj ->
                    val mockObj = mockk<ArtemisObject<*>> {
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
                    listOf(
                        Base,
                        BlackHole,
                        Creature,
                        Mine,
                        Npc,
                        Player,
                    ).filter { it.objectType != objectType },
                ) { other ->
                    arbObject.checkAll { obj ->
                        val mockObj = mockk<ArtemisObject<*>> {
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

    private suspend fun DescribeSpecContainerScope.describeTestHashCode() {
        describe("Hash code") {
            it("Equals ID") {
                arbObject.checkAll { it.hashCode() shouldBeEqual it.id }
            }

            it("Equal ID, equal hash code") {
                arbObjectPair.checkAll { (obj1, obj2) ->
                    obj1.hashCode() shouldBeEqual obj2.hashCode()
                }
            }

            it("Different ID, different hash code") {
                arbObject.checkAll { obj ->
                    val mockObj = mockk<BaseArtemisObject<*>> {
                        every { id } returns obj.id.inv()
                    }
                    obj.hashCode() shouldNotBeEqual mockObj.hashCode()
                    clearMocks(mockObj)
                }
            }
        }
    }

    open suspend fun DescribeSpecContainerScope.describeMore() { }

    fun tests(): TestFactory {
        val specName = "Artemis${javaClass.simpleName}"
        return describeSpec {
            describe(specName) {
                it("Can create with no data") {
                    testCreateUnknown()
                }

                it("Can create using Dsl instance") {
                    testCreateFromDsl()
                }

                it("Can populate properties manually") {
                    testCreateAndUpdateManually()
                }

                describe("Can apply partial updates") {
                    describeTestCreateAndUpdatePartially(this)
                }

                it("Can populate properties using Dsl instance") {
                    testCreateAndUpdateFromDsl()
                }

                it("Unpopulated properties do not provide updates to another object") {
                    testUnknownObjectDoesNotProvideUpdates()
                }

                it("Populated properties provide updates to another object") {
                    testKnownObjectProvidesUpdates()
                }

                it("Dsl object cannot populate a non-empty object") {
                    testDslCannotUpdateKnownObject()
                }

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
    }
}
