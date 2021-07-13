package com.walkertribe.ian.world

import com.walkertribe.ian.enums.AlertStatus
import com.walkertribe.ian.enums.DriveType
import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.enums.TubeState
import com.walkertribe.ian.util.BoolState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.factory.TestFactory
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.describeSpec
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.floats.shouldBeNaN
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.triple
import io.kotest.property.checkAll
import kotlin.math.max
import kotlin.math.min

class ArtemisObjectTest : DescribeSpec({
    arrayOf(
        ObjectTestSuite.Base,
        ObjectTestSuite.BlackHole,
        ObjectTestSuite.Creature,
        ObjectTestSuite.Mine,
        ObjectTestSuite.Npc,
        ObjectTestSuite.Player,
    ).forEach { include(it.tests()) }
})

sealed class ObjectTestSuite<T : ArtemisObject>(protected val objectType: ObjectType) {
    protected companion object {
        val X = Arb.numericFloat()
        val Y = Arb.numericFloat()
        val Z = Arb.numericFloat()
    }

    protected abstract class BaseProperties<T : ArtemisObject>(
        val x: Float,
        val y: Float,
        val z: Float,
    ) {
        open fun updateDirectly(obj: T) {
            obj.x.value = x
            obj.y.value = y
            obj.z.value = z
        }
        abstract fun updateThroughDsl(obj: T)
        abstract fun testKnownObject(obj: T)
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
            x: Float,
            y: Float,
            z: Float,
        ) : BaseProperties<ArtemisBase>(x, y, z) {
            override fun updateDirectly(obj: ArtemisBase) {
                super.updateDirectly(obj)
                obj.name.value = name
                obj.shieldsFront.value = shields
                obj.shieldsFrontMax.value = shieldsMax
                obj.hullId.value = hullId
            }

            override fun updateThroughDsl(obj: ArtemisBase) {
                ArtemisBase.Dsl.also {
                    it.name = name
                    it.shieldsFront = shields
                    it.shieldsFrontMax = shieldsMax
                    it.hullId = hullId
                    it.x = x
                    it.y = y
                    it.z = z

                    it updates obj
                }.shouldBeReset()
            }

            override fun testKnownObject(obj: ArtemisBase) {
                obj.shouldBeKnownObject(
                    obj.id,
                    objectType,
                    name,
                    x,
                    y,
                    z,
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

        override suspend fun testCreateUnknown() {
            arbObject.checkAll { it.shouldBeUnknownObject(it.id, objectType) }
        }

        override suspend fun testCreateAndUpdateManually() {
            checkAll(
                arbObject,
                Arb.bind(NAME, SHIELDS, SHIELDS_MAX, HULL_ID, X, Y, Z, ::Properties),
            ) { base, test ->
                test.updateDirectly(base)
                test.testKnownObject(base)
            }
        }

        override suspend fun testCreateAndUpdateFromDsl() {
            checkAll(
                arbObject,
                Arb.bind(NAME, SHIELDS, SHIELDS_MAX, HULL_ID, X, Y, Z, ::Properties),
            ) { base, test ->
                test.updateThroughDsl(base)
                test.testKnownObject(base)
            }
        }

        override suspend fun testUnknownObjectDoesNotProvideUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(NAME, SHIELDS, SHIELDS_MAX, HULL_ID, X, Y, Z, ::Properties),
            ) { (oldBase, newBase), test ->
                test.updateDirectly(oldBase)
                newBase updates oldBase
                test.testKnownObject(oldBase)
            }
        }

        override suspend fun testKnownObjectProvidesUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(NAME, SHIELDS, SHIELDS_MAX, HULL_ID, X, Y, Z, ::Properties),
            ) { (oldBase, newBase), test ->
                test.updateDirectly(newBase)
                newBase updates oldBase
                test.testKnownObject(oldBase)
            }
        }

        override suspend fun testDslCannotUpdateKnownObject() {
            checkAll(
                arbObject,
                Arb.bind(NAME, SHIELDS, SHIELDS_MAX, HULL_ID, X, Y, Z, ::Properties),
            ) { base, test ->
                test.updateDirectly(base)
                shouldThrow<IllegalArgumentException> { test.updateThroughDsl(base) }
            }
        }
    }

    data object BlackHole : ObjectTestSuite<ArtemisBlackHole>(ObjectType.BLACK_HOLE) {
        private class Properties(
            x: Float,
            y: Float,
            z: Float,
        ) : BaseProperties<ArtemisBlackHole>(x, y, z) {
            override fun updateThroughDsl(obj: ArtemisBlackHole) {
                ArtemisBlackHole.Dsl.also {
                    it.x = x
                    it.y = y
                    it.z = z

                    it updates obj
                }.shouldBeReset()
            }

            override fun testKnownObject(obj: ArtemisBlackHole) {
                obj.shouldBeKnownObject(
                    obj.id,
                    objectType,
                    x,
                    y,
                    z,
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

        override suspend fun testCreateUnknown() {
            arbObject.checkAll { it.shouldBeUnknownObject(it.id, objectType) }
        }

        override suspend fun testCreateAndUpdateManually() {
            checkAll(
                arbObject,
                Arb.bind(X, Y, Z, ::Properties),
            ) { blackHole, test ->
                test.updateDirectly(blackHole)
                test.testKnownObject(blackHole)
            }
        }

        override suspend fun testCreateAndUpdateFromDsl() {
            checkAll(
                arbObject,
                Arb.bind(X, Y, Z, ::Properties),
            ) { blackHole, test ->
                test.updateThroughDsl(blackHole)
                test.testKnownObject(blackHole)
            }
        }

        override suspend fun testUnknownObjectDoesNotProvideUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(X, Y, Z, ::Properties),
            ) { (oldBlackHole, newBlackHole), test ->
                test.updateDirectly(oldBlackHole)
                newBlackHole updates oldBlackHole
                test.testKnownObject(oldBlackHole)
            }
        }

        override suspend fun testKnownObjectProvidesUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(X, Y, Z, ::Properties),
            ) { (oldBlackHole, newBlackHole), test ->
                test.updateDirectly(newBlackHole)
                newBlackHole updates oldBlackHole
                test.testKnownObject(oldBlackHole)
            }
        }

        override suspend fun testDslCannotUpdateKnownObject() {
            checkAll(
                arbObject,
                Arb.bind(X, Y, Z, ::Properties),
            ) { blackHole, test ->
                test.updateDirectly(blackHole)
                shouldThrow<IllegalArgumentException> { test.updateThroughDsl(blackHole) }
            }
        }
    }

    data object Creature : ObjectTestSuite<ArtemisCreature>(ObjectType.CREATURE) {
        private val IS_NOT_TYPHON = Arb.boolean()

        private class Properties(
            private val isNotTyphon: Boolean,
            x: Float,
            y: Float,
            z: Float,
        ) : BaseProperties<ArtemisCreature>(x, y, z) {
            override fun updateDirectly(obj: ArtemisCreature) {
                super.updateDirectly(obj)
                obj.isNotTyphon.value = BoolState(isNotTyphon)
            }

            override fun updateThroughDsl(obj: ArtemisCreature) {
                ArtemisCreature.Dsl.also {
                    it.isNotTyphon = BoolState(isNotTyphon)
                    it.x = x
                    it.y = y
                    it.z = z

                    it updates obj
                }.shouldBeReset()
            }

            override fun testKnownObject(obj: ArtemisCreature) {
                obj.shouldBeKnownObject(
                    obj.id,
                    objectType,
                    x,
                    y,
                    z,
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

        override suspend fun testCreateUnknown() {
            arbObject.checkAll {
                it.shouldBeUnknownObject(it.id, objectType)
                it.isNotTyphon.shouldBeUnspecified()
            }
        }

        override suspend fun testCreateAndUpdateManually() {
            checkAll(
                arbObject,
                Arb.bind(IS_NOT_TYPHON, X, Y, Z, ::Properties),
            ) { creature, test ->
                test.updateDirectly(creature)
                test.testKnownObject(creature)
            }
        }

        override suspend fun testCreateAndUpdateFromDsl() {
            checkAll(
                arbObject,
                Arb.bind(IS_NOT_TYPHON, X, Y, Z, ::Properties),
            ) { creature, test ->
                test.updateThroughDsl(creature)
                test.testKnownObject(creature)
            }
        }

        override suspend fun testUnknownObjectDoesNotProvideUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(IS_NOT_TYPHON, X, Y, Z, ::Properties),
            ) { (oldCreature, newCreature), test ->
                test.updateDirectly(oldCreature)
                newCreature updates oldCreature
                test.testKnownObject(oldCreature)
            }
        }

        override suspend fun testKnownObjectProvidesUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(IS_NOT_TYPHON, X, Y, Z, ::Properties),
            ) { (oldCreature, newCreature), test ->
                test.updateDirectly(newCreature)
                newCreature updates oldCreature
                test.testKnownObject(oldCreature)
            }
        }

        override suspend fun testDslCannotUpdateKnownObject() {
            checkAll(
                arbObject,
                Arb.bind(IS_NOT_TYPHON, X, Y, Z, ::Properties),
            ) { creature, test ->
                test.updateDirectly(creature)
                shouldThrow<IllegalArgumentException> { test.updateThroughDsl(creature) }
            }
        }
    }

    data object Mine : ObjectTestSuite<ArtemisMine>(ObjectType.MINE) {
        private class Properties(
            x: Float,
            y: Float,
            z: Float,
        ) : BaseProperties<ArtemisMine>(x, y, z) {
            override fun updateThroughDsl(obj: ArtemisMine) {
                ArtemisMine.Dsl.also {
                    it.x = x
                    it.y = y
                    it.z = z

                    it updates obj
                }.shouldBeReset()
            }

            override fun testKnownObject(obj: ArtemisMine) {
                obj.shouldBeKnownObject(
                    obj.id,
                    objectType,
                    x,
                    y,
                    z,
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

        override suspend fun testCreateUnknown() {
            arbObject.checkAll { it.shouldBeUnknownObject(it.id, objectType) }
        }

        override suspend fun testCreateAndUpdateManually() {
            checkAll(
                arbObject,
                Arb.bind(X, Y, Z, ::Properties),
            ) { mine, test ->
                test.updateDirectly(mine)
                test.testKnownObject(mine)
            }
        }

        override suspend fun testCreateAndUpdateFromDsl() {
            checkAll(
                arbObject,
                Arb.bind(X, Y, Z, ::Properties),
            ) { mine, test ->
                test.updateThroughDsl(mine)
                test.testKnownObject(mine)
            }
        }

        override suspend fun testUnknownObjectDoesNotProvideUpdates() {
            checkAll(
                arbObjectPair,
                Arb.bind(X, Y, Z, ::Properties),
            ) { (oldMine, newMine), test ->
                test.updateDirectly(oldMine)
                newMine updates oldMine
                test.testKnownObject(oldMine)
            }
        }

        override suspend fun testKnownObjectProvidesUpdates() {
            Arb.pair(
                arbObject,
                Arb.bind(X, Y, Z, ::Properties),
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
                Arb.bind(X, Y, Z, ::Properties),
            ) { mine, test ->
                test.updateDirectly(mine)
                shouldThrow<IllegalArgumentException> { test.updateThroughDsl(mine) }
            }
        }
    }

    data object Npc : ObjectTestSuite<ArtemisNpc>(ObjectType.NPC_SHIP) {
        private val NAME = Arb.string()
        private val SHIELDS_FRONT = Arb.numericFloat()
        private val SHIELDS_FRONT_MAX = Arb.numericFloat()
        private val SHIELDS_REAR = Arb.numericFloat()
        private val SHIELDS_REAR_MAX = Arb.numericFloat()
        private val HULL_ID = Arb.int().filter { it != -1 }
        private val IMPULSE = Arb.numericFloat()
        private val IS_ENEMY = Arb.boolean()
        private val IN_NEBULA = Arb.boolean()
        private val SCAN_BITS = Arb.int()
        private val SIDE = Arb.byte().filter { it.toInt() != -1 }

        private class Properties(
            private val name: String,
            private val shieldsFront: Float,
            private val shieldsFrontMax: Float,
            private val shieldsRear: Float,
            private val shieldsRearMax: Float,
            private val hullId: Int,
            private val impulse: Float,
            private val isEnemy: Boolean,
            private val inNebula: Boolean,
            private val scanBits: Int,
            private val side: Byte,
            x: Float,
            y: Float,
            z: Float,
        ) : BaseProperties<ArtemisNpc>(x, y, z) {
            override fun updateDirectly(obj: ArtemisNpc) {
                super.updateDirectly(obj)
                obj.name.value = name
                obj.shieldsFront.value = shieldsFront
                obj.shieldsFrontMax.value = shieldsFrontMax
                obj.shieldsRear.value = shieldsRear
                obj.shieldsRearMax.value = shieldsRearMax
                obj.hullId.value = hullId
                obj.impulse.value = impulse
                obj.isEnemy.value = BoolState(isEnemy)
                obj.isInNebula.value = BoolState(inNebula)
                obj.scanBits.value = scanBits
                obj.side.value = side
            }

            override fun updateThroughDsl(obj: ArtemisNpc) {
                ArtemisNpc.Dsl.also {
                    it.name = name
                    it.shieldsFront = shieldsFront
                    it.shieldsFrontMax = shieldsFrontMax
                    it.shieldsRear = shieldsRear
                    it.shieldsRearMax = shieldsRearMax
                    it.hullId = hullId
                    it.impulse = impulse
                    it.isEnemy = BoolState(isEnemy)
                    it.isInNebula = BoolState(inNebula)
                    it.scanBits = scanBits
                    it.side = side
                    it.x = x
                    it.y = y
                    it.z = z

                    it updates obj
                }.shouldBeReset()
            }

            override fun testKnownObject(obj: ArtemisNpc) {
                obj.shouldBeKnownObject(
                    obj.id,
                    objectType,
                    name,
                    x,
                    y,
                    z,
                    hullId,
                    shieldsFront,
                    shieldsFrontMax,
                    shieldsRear,
                    shieldsRearMax,
                    impulse,
                    side,
                )

                obj.isEnemy shouldContainValue isEnemy
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

        override suspend fun testCreateUnknown() {
            arbObject.checkAll {
                it.shouldBeUnknownObject(it.id, objectType)

                it.isEnemy.shouldBeUnspecified()
                it.isInNebula.shouldBeUnspecified()
                it.scanBits.shouldBeUnspecified()
            }
        }

        override suspend fun testCreateAndUpdateManually() {
            checkAll(
                arbObject,
                Arb.bind(
                    NAME,
                    SHIELDS_FRONT,
                    SHIELDS_FRONT_MAX,
                    SHIELDS_REAR,
                    SHIELDS_REAR_MAX,
                    HULL_ID,
                    IMPULSE,
                    IS_ENEMY,
                    IN_NEBULA,
                    SCAN_BITS,
                    SIDE,
                    X,
                    Y,
                    Z,
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
                    SHIELDS_FRONT,
                    SHIELDS_FRONT_MAX,
                    SHIELDS_REAR,
                    SHIELDS_REAR_MAX,
                    HULL_ID,
                    IMPULSE,
                    IS_ENEMY,
                    IN_NEBULA,
                    SCAN_BITS,
                    SIDE,
                    X,
                    Y,
                    Z,
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
                    SHIELDS_FRONT,
                    SHIELDS_FRONT_MAX,
                    SHIELDS_REAR,
                    SHIELDS_REAR_MAX,
                    HULL_ID,
                    IMPULSE,
                    IS_ENEMY,
                    IN_NEBULA,
                    SCAN_BITS,
                    SIDE,
                    X,
                    Y,
                    Z,
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
                    SHIELDS_FRONT,
                    SHIELDS_FRONT_MAX,
                    SHIELDS_REAR,
                    SHIELDS_REAR_MAX,
                    HULL_ID,
                    IMPULSE,
                    IS_ENEMY,
                    IN_NEBULA,
                    SCAN_BITS,
                    SIDE,
                    X,
                    Y,
                    Z,
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
                    SHIELDS_FRONT,
                    SHIELDS_FRONT_MAX,
                    SHIELDS_REAR,
                    SHIELDS_REAR_MAX,
                    HULL_ID,
                    IMPULSE,
                    IS_ENEMY,
                    IN_NEBULA,
                    SCAN_BITS,
                    SIDE,
                    X,
                    Y,
                    Z,
                    ::Properties,
                ),
            ) { npc, test ->
                test.updateDirectly(npc)
                shouldThrow<IllegalArgumentException> { test.updateThroughDsl(npc) }
            }
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
        private val DOUBLE_AGENT_ACTIVE = Arb.boolean()
        private val DOUBLE_AGENT_COUNT = Arb.byte().filter { it.toInt() != -1 }
        private val DOUBLE_AGENT_SECONDS = Arb.int().filter { it != -1 }
        private val DOUBLE_AGENT = Arb.triple(
            DOUBLE_AGENT_ACTIVE,
            DOUBLE_AGENT_COUNT,
            DOUBLE_AGENT_SECONDS,
        )
        private val LOCATION = Arb.triple(X, Y, Z)
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
            private val doubleAgentStatus: Triple<Boolean, Byte, Int>,
            location: Triple<Float, Float, Float>,
            private val ordnanceCounts: List<Byte>,
            private val tubes: List<Pair<TubeState, OrdnanceType>>,
        ) : BaseProperties<ArtemisPlayer>(location.first, location.second, location.third) {
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
                obj.doubleAgentActive.value = BoolState(doubleAgentStatus.first)
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

            override fun testKnownObject(obj: ArtemisPlayer) {
                obj.shouldBeKnownObject(
                    obj.id,
                    objectType,
                    name,
                    x,
                    y,
                    z,
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
                    it.x = x
                    it.y = y
                    it.z = z
                    it.shipIndex = shipIndex
                    it.capitalShipID = capitalShipID
                    it.alertStatus = enumStates.first
                    it.driveType = enumStates.second
                    it.warp = warp
                    it.dockingBase = dockingBase

                    it updates player
                }.shouldBeReset()
            }

            fun updateThroughWeaponsDsl(player: ArtemisPlayer) {
                ArtemisPlayer.WeaponsDsl.also {
                    ordnanceCounts.forEachIndexed { index, count ->
                        it.ordnanceCounts[OrdnanceType.entries[index]] = count
                    }
                    tubes.forEachIndexed { index, (state, contents) ->
                        it.tubeStates[index] = state
                        it.tubeContents[index] = contents
                    }

                    it updates player
                }.shouldBeReset()
            }

            fun updateThroughUpgradesDsl(player: ArtemisPlayer) {
                ArtemisPlayer.UpgradesDsl.also {
                    it.doubleAgentActive = BoolState(doubleAgentStatus.first)
                    it.doubleAgentCount = doubleAgentStatus.second
                    it.doubleAgentSecondsLeft = doubleAgentStatus.third

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
                shouldThrow<IllegalArgumentException> { test.updateThroughPlayerDsl(player) }
                shouldThrow<IllegalArgumentException> { test.updateThroughUpgradesDsl(player) }
            }
        }

        override suspend fun describeMore(scope: DescribeSpecContainerScope) {
            scope.describe("Invalid warp value throws") {
                withData(
                    nameFn = { it.first },
                    "Negative number" to Arb.byte(max = -2),
                    "Higher than 4" to Arb.byte(min = 5),
                ) { (_, testGen) ->
                    checkAll(arbObject, testGen) { player, warp ->
                        shouldThrow<IllegalArgumentException> {
                            player.warp.value = warp
                            null
                        }
                    }
                }
            }

            scope.describe("Undock when moving") {
                it("At impulse") {
                    checkAll(
                        arbObjectPair,
                        Arb.int().filter { it != -1 },
                        Arb.numericFloat(min = Float.MIN_VALUE),
                    ) { (playerA, playerB), dockingBase, impulse ->
                        playerA.dockingBase.value = dockingBase
                        playerA.docked = BoolState.True

                        playerB.impulse.value = impulse
                        playerB updates playerA

                        playerA.dockingBase shouldContainValue 0
                        playerA.docked shouldBeEqual BoolState.False
                    }
                }

                it("At warp") {
                    checkAll(
                        arbObjectPair,
                        Arb.int().filter { it != -1 },
                        Arb.byte(min = 1, max = 4),
                    ) { (playerA, playerB), dockingBase, warp ->
                        playerA.dockingBase.value = dockingBase
                        playerA.docked = BoolState.True

                        playerB.warp.value = warp
                        playerB updates playerA

                        playerA.dockingBase shouldContainValue 0
                        playerA.docked shouldBeEqual BoolState.False
                    }
                }

                it("At impulse and warp") {
                    checkAll(
                        arbObjectPair,
                        Arb.int().filter { it != -1 },
                        Arb.numericFloat(min = Float.MIN_VALUE),
                        Arb.byte(min = 1, max = 4),
                    ) { (playerA, playerB), dockingBase, impulse, warp ->
                        playerA.dockingBase.value = dockingBase
                        playerA.docked = BoolState.True

                        playerB.impulse.value = impulse
                        playerB.warp.value = warp
                        playerB updates playerA

                        playerA.dockingBase shouldContainValue 0
                        playerA.docked shouldBeEqual BoolState.False
                    }
                }
            }
        }
    }

    abstract val arbObject: Arb<T>
    abstract val arbObjectPair: Arb<Pair<T, T>>
    abstract suspend fun testCreateUnknown()
    abstract suspend fun testCreateAndUpdateManually()
    abstract suspend fun testCreateAndUpdateFromDsl()
    abstract suspend fun testUnknownObjectDoesNotProvideUpdates()
    abstract suspend fun testKnownObjectProvidesUpdates()
    abstract suspend fun testDslCannotUpdateKnownObject()

    private suspend fun describeTestCannotUpdateObjectOfDifferentType(scope: DescribeSpecContainerScope) {
        scope.describe("Cannot update objects of different type") {
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
                Arb.pair(arbObject, other.arbObject).checkAll { (thisObj, otherObj) ->
                    shouldThrow<IllegalArgumentException> { thisObj updates otherObj }
                }
            }
        }
    }

    open suspend fun describeMore(scope: DescribeSpecContainerScope) { }

    fun tests(): TestFactory {
        val specName = "Artemis${javaClass.simpleName}"
        return describeSpec {
            describe(specName) {
                it("Can create") {
                    testCreateUnknown()
                }

                it("Can populate properties manually") {
                    testCreateAndUpdateManually()
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

                describeTestCannotUpdateObjectOfDifferentType(this)

                describeMore(this)
            }
        }
    }
}

private fun ArtemisObject.shouldBeUnknownObject(id: Int, type: ObjectType) {
    this.id shouldBeEqual id
    this.type shouldBeEqual type
    this.x.shouldBeUnspecified()
    this.y.shouldBeUnspecified()
    this.z.shouldBeUnspecified()
    this.hasPosition.shouldBeFalse()
}

private fun ArtemisObject.shouldBeKnownObject(
    id: Int,
    type: ObjectType,
    x: Float,
    y: Float,
    z: Float,
) {
    this.id shouldBeEqual id
    this.type shouldBeEqual type
    this.x shouldContainValue x
    this.y shouldContainValue y
    this.z shouldContainValue z
    this.hasPosition.shouldBeTrue()
}

private fun BaseArtemisObject.Dsl<*>.shouldBeReset() {
    this.x.shouldBeNaN()
    this.y.shouldBeNaN()
    this.z.shouldBeNaN()
}

private fun ArtemisShielded.shouldBeUnknownObject(id: Int, type: ObjectType) {
    (this as ArtemisObject).shouldBeUnknownObject(id, type)

    this.name.shouldBeUnspecified()
    this.hullId.shouldBeUnspecified()
    this.shieldsFront.shouldBeUnspecified()
    this.shieldsFrontMax.shouldBeUnspecified()
}

private fun ArtemisShielded.shouldBeKnownObject(
    id: Int,
    type: ObjectType,
    name: String,
    x: Float,
    y: Float,
    z: Float,
    hullId: Int,
    shieldsFront: Float,
    shieldsFrontMax: Float,
) {
    shouldBeKnownObject(id, type, x, y, z)

    this.name shouldContainValue name
    this.hullId shouldContainValue hullId
    this.shieldsFront shouldContainValue shieldsFront
    this.shieldsFrontMax shouldContainValue shieldsFrontMax
}

private fun BaseArtemisShielded.Dsl<*>.shouldBeReset() {
    (this as BaseArtemisObject.Dsl<*>).shouldBeReset()

    this.name.shouldBeNull()
    this.hullId shouldBeEqual -1
    this.shieldsFront.shouldBeNaN()
    this.shieldsFrontMax.shouldBeNaN()
}

private fun BaseArtemisShip.shouldBeUnknownObject(id: Int, type: ObjectType) {
    (this as ArtemisShielded).shouldBeUnknownObject(id, type)

    this.shieldsRear.shouldBeUnspecified()
    this.shieldsRearMax.shouldBeUnspecified()
    this.impulse.shouldBeUnspecified()
    this.side.shouldBeUnspecified()
}

private fun BaseArtemisShip.shouldBeKnownObject(
    id: Int,
    type: ObjectType,
    name: String,
    x: Float,
    y: Float,
    z: Float,
    hullId: Int,
    shieldsFront: Float,
    shieldsFrontMax: Float,
    shieldsRear: Float,
    shieldsRearMax: Float,
    impulse: Float,
    side: Byte,
) {
    shouldBeKnownObject(id, type, name, x, y, z, hullId, shieldsFront, shieldsFrontMax)

    this.shieldsRear shouldContainValue shieldsRear
    this.shieldsRearMax shouldContainValue shieldsRearMax
    this.impulse shouldContainValue impulse
    this.side shouldContainValue side
}

private fun BaseArtemisShip.Dsl<*>.shouldBeReset() {
    (this as BaseArtemisShielded.Dsl<*>).shouldBeReset()

    this.shieldsRear.shouldBeNaN()
    this.shieldsRearMax.shouldBeNaN()
    this.impulse.shouldBeNaN()
    this.side shouldBeEqual -1
}
