package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.AlertStatus
import com.walkertribe.ian.enums.DriveType
import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.enums.TubeState
import com.walkertribe.ian.iface.ListenerRegistry
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.ParseResult
import com.walkertribe.ian.iface.TestListener
import com.walkertribe.ian.protocol.core.PacketTestData
import com.walkertribe.ian.protocol.core.PacketTestFixture.Companion.writePacketWithHeader
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.protocol.core.world.WeaponsParser.OrdnanceCountBit
import com.walkertribe.ian.protocol.core.world.WeaponsParser.TubeContentsBit
import com.walkertribe.ian.protocol.core.world.WeaponsParser.TubeStateBit
import com.walkertribe.ian.protocol.core.world.WeaponsParser.TubeTimeBit
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.version
import com.walkertribe.ian.world.Artemis
import com.walkertribe.ian.world.ArtemisBase
import com.walkertribe.ian.world.ArtemisBlackHole
import com.walkertribe.ian.world.ArtemisCreature
import com.walkertribe.ian.world.ArtemisMine
import com.walkertribe.ian.world.ArtemisNpc
import com.walkertribe.ian.world.ArtemisObject
import com.walkertribe.ian.world.ArtemisPlayer
import com.walkertribe.ian.world.shouldBeUnspecified
import com.walkertribe.ian.world.shouldContainValue
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.types.beInstanceOf
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.choose
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.short
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.of
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.core.buildPacket
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.writeIntLe

sealed class ObjectParserTestConfig(val recognizesObjectListeners: Boolean) {
    sealed class ObjectParserData<T : ArtemisObject<T>>(private val objectType: ObjectType) :
        PacketTestData.Server<ObjectUpdatePacket> {
        abstract class Real<T : ArtemisObject<T>>(
            private val objectClass: KClass<T>,
            objectType: ObjectType,
        ) : ObjectParserData<T>(objectType) {
            lateinit var realObject: T

            abstract fun validateObject(obj: T)

            override fun validate(packet: ObjectUpdatePacket) {
                packet.objects.shouldBeSingleton { obj ->
                    obj.id shouldBeEqual objectID
                    obj should beInstanceOf(objectClass)
                    realObject = objectClass.cast(obj).also(this::validateObject)
                }
            }
        }

        abstract class Unobserved(objectType: ObjectType) : ObjectParserData<Nothing>(objectType) {
            override fun validate(packet: ObjectUpdatePacket) {
                packet.objects.shouldBeEmpty()
            }
        }

        abstract val objectID: Int

        abstract fun Sink.buildObject()

        override fun buildPayload(): Source = buildObject {
            writeByte(objectType.id)
            writeIntLe(objectID)
            buildObject()
        }
    }

    data object Empty : ObjectParserTestConfig(false) {
        data object Data : PacketTestData.Server<ObjectUpdatePacket> {
            override val version: Version
                get() = Version.DEFAULT

            override fun buildPayload(): Source = buildObject {}

            override fun validate(packet: ObjectUpdatePacket) {
                packet.objects.shouldBeEmpty()
            }
        }

        override val parserName: String = "Empty"
        override val dataGenerator: Gen<Data> = Exhaustive.of(Data)
    }

    data object BaseParser : ObjectParserTestConfig(true) {
        data class Data
        internal constructor(
            override val objectID: Int,
            private val flags1: BaseFlags1,
            private val flags2: BaseFlags2,
        ) : ObjectParserData.Real<ArtemisBase>(ArtemisBase::class, ObjectType.BASE) {
            override val version: Version
                get() = Version.DEFAULT

            private val nameFlag = flags1.flag1
            private val shieldsFlag = flags1.flag2
            private val maxShieldsFlag = flags1.flag3
            private val hullIdFlag = flags1.flag5
            private val xFlag = flags1.flag6
            private val yFlag = flags1.flag7
            private val zFlag = flags1.flag8

            override fun Sink.buildObject() {
                writeFlagBytes(flags1, flags2)

                writeStringFlags(nameFlag)
                writeFloatFlags(shieldsFlag, maxShieldsFlag)
                writeIntFlags(flags1.flag4, hullIdFlag)
                writeFloatFlags(
                    xFlag,
                    yFlag,
                    zFlag,
                    flags2.flag1,
                    flags2.flag2,
                    flags2.flag3,
                    flags2.flag4,
                )
                writeByteFlags(flags2.flag5, flags2.flag6)
            }

            override fun validateObject(obj: ArtemisBase) {
                testHasPosition(obj, xFlag, zFlag)

                obj.name shouldMatch nameFlag
                obj.hullId shouldMatch hullIdFlag
                testFloatPropertyFlags(
                    xFlag to obj.x,
                    yFlag to obj.y,
                    zFlag to obj.z,
                    shieldsFlag to obj.shieldsFront.strength,
                    maxShieldsFlag to obj.shieldsFront.maxStrength,
                )
            }
        }

        private val NAME = Arb.string()
        private val SHIELDS = Arb.numericFloat()
        private val MAX_SHIELDS = Arb.numericFloat()
        private val HULL_ID = Arb.int().filter { it != -1 }

        private val UNK_1_4 = Arb.int()
        private val UNK_2_1 = Arb.float()
        private val UNK_2_2 = Arb.float()
        private val UNK_2_3 = Arb.float()
        private val UNK_2_4 = Arb.float()
        private val UNK_2_5 = Arb.byte()
        private val UNK_2_6 = Arb.byte()

        override val parserName: String = "Base"
        override val dataGenerator: Gen<Data> =
            Arb.bind(
                genA = ID,
                genB =
                    Arb.flags(
                        arb1 = NAME,
                        arb2 = SHIELDS,
                        arb3 = MAX_SHIELDS,
                        arb4 = UNK_1_4,
                        arb5 = HULL_ID,
                        arb6 = X,
                        arb7 = Y,
                        arb8 = Z,
                    ),
                genC =
                    Arb.flags(
                        arb1 = UNK_2_1,
                        arb2 = UNK_2_2,
                        arb3 = UNK_2_3,
                        arb4 = UNK_2_4,
                        arb5 = UNK_2_5,
                        arb6 = UNK_2_6,
                    ),
                bindFn = ::Data,
            )
    }

    data object BlackHoleParser : ObjectParserTestConfig(true) {
        data class Data
        internal constructor(override val objectID: Int, private val flags: PositionFlags) :
            ObjectParserData.Real<ArtemisBlackHole>(
                ArtemisBlackHole::class,
                ObjectType.BLACK_HOLE,
            ) {
            override val version: Version
                get() = Version.DEFAULT

            private val xFlag = flags.flag1
            private val yFlag = flags.flag2
            private val zFlag = flags.flag3

            override fun Sink.buildObject() {
                writeFlagBytes(flags)
                writeFloatFlags(xFlag, yFlag, zFlag)
            }

            override fun validateObject(obj: ArtemisBlackHole) {
                testHasPosition(obj, xFlag, zFlag)

                testFloatPropertyFlags(xFlag to obj.x, yFlag to obj.y, zFlag to obj.z)
            }
        }

        override val parserName: String = "Black hole"
        override val dataGenerator: Gen<Data> = Arb.bind(ID, Arb.flags(X, Y, Z), ::Data)
    }

    sealed class CreatureParser(override val specName: String, versionArb: Arb<Version>) :
        ObjectParserTestConfig(true) {
        data class Data
        internal constructor(
            override val objectID: Int,
            override val version: Version,
            private val flags1: CreatureFlags1,
            private val flags2: CreatureFlags2,
            private val flags3: CreatureFlags3,
            private val forceCreatureType: Boolean,
        ) : ObjectParserData.Real<ArtemisCreature>(ArtemisCreature::class, ObjectType.CREATURE) {
            private val xFlag = flags1.flag1
            private val yFlag = flags1.flag2
            private val zFlag = flags1.flag3
            private val creatureTypeFlag = flags1.flag8

            override fun Sink.buildObject() {
                var flagByte1 = flags1.byteValue.toInt()
                if (forceCreatureType) flagByte1 = flagByte1 or CREATURE_TYPE_BIT
                writeByte(flagByte1.toByte())

                writeFlagBytes(flags2, flags3)

                writeFloatFlags(xFlag, yFlag, zFlag)
                writeStringFlags(flags1.flag4)
                writeFloatFlags(flags1.flag5, flags1.flag6, flags1.flag7)

                if (creatureTypeFlag.enabled || forceCreatureType) {
                    writeIntLe(creatureTypeFlag.value)
                }

                writeIntFlags(
                    flags2.flag1,
                    flags2.flag2,
                    flags2.flag3,
                    flags2.flag4,
                    flags2.flag5,
                    flags2.flag6,
                )
                writeFloatFlags(flags2.flag7, flags2.flag8)

                if (version >= Version.BEACON) {
                    writeByteFlags(flags3.flag1)

                    if (version >= Version.NEBULA_TYPES) {
                        writeIntFlags(flags3.flag2)
                    }
                }
            }

            override fun validateObject(obj: ArtemisCreature) {
                testHasPosition(obj, xFlag, zFlag)

                testFloatPropertyFlags(xFlag to obj.x, yFlag to obj.y, zFlag to obj.z)

                when {
                    forceCreatureType -> obj.isNotTyphon shouldContainValue true
                    creatureTypeFlag.enabled -> obj.isNotTyphon shouldContainValue false
                    else -> obj.isNotTyphon.shouldBeUnspecified()
                }
            }
        }

        data object V1 :
            CreatureParser(before(VERSION_2_6_0), Arb.version(major = 2, minorRange = 3..5))

        data object V2 :
            CreatureParser(
                since(VERSION_2_6_0),
                Arb.version(major = 2, minorArb = Arb.int(min = 6)),
            )

        override val parserName: String = "Creature"
        override val dataGenerator: Gen<Data> = arbData(versionArb, Arb.of(0), Arb.of(false))

        private val nonTyphonDataGenerator: Gen<Data> =
            arbData(versionArb, Arb.int().filter { it != 0 }, Arb.of(true))

        override suspend fun describeMore(scope: DescribeSpecContainerScope) {
            scope.it("Rejects non-typhons") {
                val readChannel = ByteChannel()
                val reader =
                    PacketReader(
                        readChannel,
                        ListenerRegistry().apply { register(TestListener.module) },
                    )

                nonTyphonDataGenerator.checkAll { data ->
                    readChannel.writePacketWithHeader(
                        TestPacketTypes.OBJECT_BIT_STREAM,
                        data.buildPayload(),
                    )

                    reader.version = data.version

                    val result = reader.readPacket()
                    result.shouldBeInstanceOf<ParseResult.Success>()

                    val packet = result.packet
                    packet.shouldBeInstanceOf<ObjectUpdatePacket>()
                    packet.objects.shouldBeEmpty()

                    reader.isAcceptingCurrentObject.shouldBeFalse()
                }

                reader.close()
            }
        }

        protected companion object {
            private val UNK_1_4 = Arb.string()
            private val UNK_1_5 = Arb.float()
            private val UNK_1_6 = Arb.float()
            private val UNK_1_7 = Arb.float()

            private val UNK_2_1 = Arb.int()
            private val UNK_2_2 = Arb.int()
            private val UNK_2_3 = Arb.int()
            private val UNK_2_4 = Arb.int()
            private val UNK_2_5 = Arb.int()
            private val UNK_2_6 = Arb.int()
            private val UNK_2_7 = Arb.float()
            private val UNK_2_8 = Arb.float()

            private val UNK_3_1 = Arb.byte()
            private val UNK_3_2 = Arb.int()

            private const val CREATURE_TYPE_BIT = 0x80

            private fun arbData(
                arbVersion: Arb<Version>,
                arbCreatureType: Arb<Int>,
                arbForceCreatureType: Arb<Boolean>,
            ): Arb<Data> =
                Arb.bind(
                    genA = ID,
                    genB = arbVersion,
                    genC =
                        Arb.flags(
                            arb1 = X,
                            arb2 = Y,
                            arb3 = Z,
                            arb4 = UNK_1_4,
                            arb5 = UNK_1_5,
                            arb6 = UNK_1_6,
                            arb7 = UNK_1_7,
                            arb8 = arbCreatureType,
                        ),
                    genD =
                        Arb.flags(
                            arb1 = UNK_2_1,
                            arb2 = UNK_2_2,
                            arb3 = UNK_2_3,
                            arb4 = UNK_2_4,
                            arb5 = UNK_2_5,
                            arb6 = UNK_2_6,
                            arb7 = UNK_2_7,
                            arb8 = UNK_2_8,
                        ),
                    genE = Arb.flags(arb1 = UNK_3_1, arb2 = UNK_3_2),
                    genF = arbForceCreatureType,
                    bindFn = ::Data,
                )
        }
    }

    data object MineParser : ObjectParserTestConfig(true) {
        data class Data
        internal constructor(override val objectID: Int, private val flags: PositionFlags) :
            ObjectParserData.Real<ArtemisMine>(ArtemisMine::class, ObjectType.MINE) {
            override val version: Version
                get() = Version.DEFAULT

            private val xFlag = flags.flag1
            private val yFlag = flags.flag2
            private val zFlag = flags.flag3

            override fun Sink.buildObject() {
                writeFlagBytes(flags)
                writeFloatFlags(xFlag, yFlag, zFlag)
            }

            override fun validateObject(obj: ArtemisMine) {
                testHasPosition(obj, xFlag, zFlag)

                testFloatPropertyFlags(xFlag to obj.x, yFlag to obj.y, zFlag to obj.z)
            }
        }

        override val parserName: String = "Mine"
        override val dataGenerator: Gen<Data> = Arb.bind(ID, Arb.flags(X, Y, Z), ::Data)
    }

    sealed class NpcShipParser(override val specName: String) : ObjectParserTestConfig(true) {
        override val parserName: String = "NPC ship"

        abstract class NpcData internal constructor() :
            ObjectParserData.Real<ArtemisNpc>(ArtemisNpc::class, ObjectType.NPC_SHIP) {
            internal abstract val flags1: NpcFlags1
            internal abstract val flags2: NpcFlags2
            internal abstract val flags3: NpcFlags3
            internal abstract val flags4: NpcFlags4

            private val nameFlag: Flag<String>
                get() = flags1.flag1

            private val impulseFlag: Flag<Float>
                get() = flags1.flag2

            private val isEnemyFlag: Flag<Int>
                get() = flags1.flag6

            private val hullIdFlag: Flag<Int>
                get() = flags1.flag7

            private val xFlag: Flag<Float>
                get() = flags1.flag8

            private val yFlag: Flag<Float>
                get() = flags2.flag1

            private val zFlag: Flag<Float>
                get() = flags2.flag2

            private val surrenderedFlag: Flag<Byte>
                get() = flags2.flag7

            internal abstract val inNebulaFlag: Flag<out Number>
            private val shieldsFrontFlag: Flag<Float>
                get() = flags3.flag1

            private val maxShieldsFrontFlag: Flag<Float>
                get() = flags3.flag2

            private val shieldsRearFlag: Flag<Float>
                get() = flags3.flag3

            private val maxShieldsRearFlag: Flag<Float>
                get() = flags3.flag4

            private val scanBitsFlag: Flag<Int>
                get() = flags4.flag1

            private val sideFlag: Flag<Byte>
                get() = flags4.flag4

            internal abstract val allFlagBytes: Array<AnyFlagByte>

            abstract fun Sink.writeInNebulaFlag()

            abstract fun Sink.writeRemainingFlags()

            override fun Sink.buildObject() {
                writeFlagBytes(allFlagBytes)

                writeStringFlags(nameFlag)
                writeFloatFlags(impulseFlag, flags1.flag3, flags1.flag4, flags1.flag5)
                writeIntFlags(isEnemyFlag, hullIdFlag)
                writeFloatFlags(
                    xFlag,
                    yFlag,
                    zFlag,
                    flags2.flag3,
                    flags2.flag4,
                    flags2.flag5,
                    flags2.flag6,
                )
                writeByteFlags(surrenderedFlag)
                writeInNebulaFlag()
                writeFloatFlags(
                    shieldsFrontFlag,
                    maxShieldsFrontFlag,
                    shieldsRearFlag,
                    maxShieldsRearFlag,
                )
                writeShortFlags(flags3.flag5)
                writeByteFlags(flags3.flag6)
                writeIntFlags(flags3.flag7, flags3.flag8, scanBitsFlag, flags4.flag2, flags4.flag3)
                writeByteFlags(sideFlag, flags4.flag5, flags4.flag6, flags4.flag7)
                writeFloatFlags(flags4.flag8)
                writeRemainingFlags()
            }

            override fun validateObject(obj: ArtemisNpc) {
                testHasPosition(obj, xFlag, zFlag)

                testFloatPropertyFlags(
                    xFlag to obj.x,
                    yFlag to obj.y,
                    zFlag to obj.z,
                    impulseFlag to obj.impulse,
                    shieldsFrontFlag to obj.shieldsFront.strength,
                    maxShieldsFrontFlag to obj.shieldsFront.maxStrength,
                    shieldsRearFlag to obj.shieldsRear.strength,
                    maxShieldsRearFlag to obj.shieldsRear.maxStrength,
                )

                obj.name shouldMatch nameFlag
                obj.hullId shouldMatch hullIdFlag
                obj.side shouldMatch sideFlag
                obj.scanBits shouldMatch scanBitsFlag

                testBoolPropertyFlags(
                    isEnemyFlag to obj.isEnemy,
                    surrenderedFlag to obj.isSurrendered,
                    inNebulaFlag to obj.isInNebula,
                )
            }
        }

        data object V1 : NpcShipParser(before(VERSION_2_6_3)) {
            data class Data
            internal constructor(
                override val objectID: Int,
                override val version: Version,
                override val flags1: NpcFlags1,
                override val flags2: NpcFlags2Old,
                override val flags3: NpcFlags3,
                override val flags4: NpcFlags4,
                private val flags5: NpcFlags5Old,
                private val flags6: NpcFlags6Old,
            ) : NpcData() {
                override val allFlagBytes: Array<AnyFlagByte> =
                    arrayOf(flags1, flags2, flags3, flags4, flags5, flags6)
                override val inNebulaFlag: Flag<Short> = flags2.flag8

                override fun Sink.writeInNebulaFlag() {
                    writeShortFlags(inNebulaFlag)
                }

                override fun Sink.writeRemainingFlags() {
                    writeFloatFlags(
                        flags5.flag1,
                        flags5.flag2,
                        flags5.flag3,
                        flags5.flag4,
                        flags5.flag5,
                        flags5.flag6,
                        flags5.flag7,
                        flags5.flag8,
                        flags6.flag1,
                        flags6.flag2,
                        flags6.flag3,
                        flags6.flag4,
                        flags6.flag5,
                        flags6.flag6,
                        flags6.flag7,
                    )
                }
            }

            override val dataGenerator: Gen<Data> =
                Arb.bind(
                    genA = ID,
                    genB =
                        Arb.choose(
                            3 to Arb.version(major = 2, minor = 6, patchRange = 0..2),
                            997 to Arb.version(major = 2, minorRange = 3..5),
                        ),
                    genC =
                        Arb.flags(
                            arb1 = NAME,
                            arb2 = IMPULSE,
                            arb3 = UNK_1_3,
                            arb4 = UNK_1_4,
                            arb5 = UNK_1_5,
                            arb6 = IS_ENEMY,
                            arb7 = HULL_ID,
                            arb8 = X,
                        ),
                    genD =
                        Arb.flags(
                            arb1 = Y,
                            arb2 = Z,
                            arb3 = UNK_2_3,
                            arb4 = UNK_2_4,
                            arb5 = UNK_2_5,
                            arb6 = UNK_2_6,
                            arb7 = SURRENDERED,
                            arb8 = IN_NEBULA_OLD,
                        ),
                    genE =
                        Arb.flags(
                            arb1 = FRONT,
                            arb2 = FRONT_MAX,
                            arb3 = REAR,
                            arb4 = REAR_MAX,
                            arb5 = UNK_3_5,
                            arb6 = UNK_3_6,
                            arb7 = UNK_3_7,
                            arb8 = UNK_3_8,
                        ),
                    genF =
                        Arb.flags(
                            arb1 = SCAN_BITS,
                            arb2 = UNK_4_2,
                            arb3 = UNK_4_3,
                            arb4 = SIDE,
                            arb5 = UNK_4_5,
                            arb6 = UNK_4_6,
                            arb7 = UNK_4_7,
                            arb8 = UNK_4_8,
                        ),
                    genG =
                        Arb.flags(
                            arb1 = UNK_5_1,
                            arb2 = UNK_5_2,
                            arb3 = DAMAGE,
                            arb4 = DAMAGE,
                            arb5 = DAMAGE,
                            arb6 = DAMAGE,
                            arb7 = DAMAGE,
                            arb8 = DAMAGE,
                        ),
                    genH =
                        Arb.flags(
                            arb1 = DAMAGE,
                            arb2 = DAMAGE,
                            arb3 = FREQ,
                            arb4 = FREQ,
                            arb5 = FREQ,
                            arb6 = FREQ,
                            arb7 = FREQ,
                        ),
                    bindFn = ::Data,
                )
        }

        data object V2 : NpcShipParser(between(VERSION_2_6_3, VERSION_2_7_0)) {
            data class Data
            internal constructor(
                override val objectID: Int,
                override val version: Version,
                override val flags1: NpcFlags1,
                override val flags2: NpcFlags2Old,
                override val flags3: NpcFlags3,
                override val flags4: NpcFlags4,
                private val flags5: NpcFlags5New,
                private val flags6: NpcFlags6New,
                private val flags7: NpcFlags7,
            ) : NpcData() {
                override val allFlagBytes: Array<AnyFlagByte> =
                    arrayOf(flags1, flags2, flags3, flags4, flags5, flags6, flags7)
                override val inNebulaFlag: Flag<Short> = flags2.flag8

                override fun Sink.writeInNebulaFlag() {
                    writeShortFlags(inNebulaFlag)
                }

                override fun Sink.writeRemainingFlags() {
                    writeFloatFlags(flags5.flag1, flags5.flag2)
                    writeByteFlags(flags5.flag3, flags5.flag4)
                    writeFloatFlags(
                        flags5.flag5,
                        flags5.flag6,
                        flags5.flag7,
                        flags5.flag8,
                        flags6.flag1,
                        flags6.flag2,
                        flags6.flag3,
                        flags6.flag4,
                        flags6.flag5,
                        flags6.flag6,
                        flags6.flag7,
                        flags6.flag8,
                        flags7.flag1,
                    )
                }
            }

            override val dataGenerator: Gen<Data> =
                Arb.bind(
                    genA = ID,
                    genB = Arb.version(major = 2, minor = 6, patchArb = Arb.int(min = 3)),
                    genC =
                        Arb.flags(
                            arb1 = NAME,
                            arb2 = IMPULSE,
                            arb3 = UNK_1_3,
                            arb4 = UNK_1_4,
                            arb5 = UNK_1_5,
                            arb6 = IS_ENEMY,
                            arb7 = HULL_ID,
                            arb8 = X,
                        ),
                    genD =
                        Arb.flags(
                            arb1 = Y,
                            arb2 = Z,
                            arb3 = UNK_2_3,
                            arb4 = UNK_2_4,
                            arb5 = UNK_2_5,
                            arb6 = UNK_2_6,
                            arb7 = SURRENDERED,
                            arb8 = IN_NEBULA_OLD,
                        ),
                    genE =
                        Arb.flags(
                            arb1 = FRONT,
                            arb2 = FRONT_MAX,
                            arb3 = REAR,
                            arb4 = REAR_MAX,
                            arb5 = UNK_3_5,
                            arb6 = UNK_3_6,
                            arb7 = UNK_3_7,
                            arb8 = UNK_3_8,
                        ),
                    genF =
                        Arb.flags(
                            arb1 = SCAN_BITS,
                            arb2 = UNK_4_2,
                            arb3 = UNK_4_3,
                            arb4 = SIDE,
                            arb5 = UNK_4_5,
                            arb6 = UNK_4_6,
                            arb7 = UNK_4_7,
                            arb8 = UNK_4_8,
                        ),
                    genG =
                        Arb.flags(
                            arb1 = UNK_5_1,
                            arb2 = UNK_5_2,
                            arb3 = UNK_5_3,
                            arb4 = UNK_5_4,
                            arb5 = DAMAGE,
                            arb6 = DAMAGE,
                            arb7 = DAMAGE,
                            arb8 = DAMAGE,
                        ),
                    genH =
                        Arb.flags(
                            arb1 = DAMAGE,
                            arb2 = DAMAGE,
                            arb3 = DAMAGE,
                            arb4 = DAMAGE,
                            arb5 = FREQ,
                            arb6 = FREQ,
                            arb7 = FREQ,
                            arb8 = FREQ,
                        ),
                    genI = Arb.flags(arb1 = FREQ),
                    bindFn = ::Data,
                )
        }

        data object V3 : NpcShipParser(since(VERSION_2_7_0)) {
            data class Data
            internal constructor(
                override val objectID: Int,
                override val version: Version,
                override val flags1: NpcFlags1,
                override val flags2: NpcFlags2New,
                override val flags3: NpcFlags3,
                override val flags4: NpcFlags4,
                private val flags5: NpcFlags5New,
                private val flags6: NpcFlags6New,
                private val flags7: NpcFlags7,
            ) : NpcData() {
                override val allFlagBytes: Array<AnyFlagByte> =
                    arrayOf(flags1, flags2, flags3, flags4, flags5, flags6, flags7)
                override val inNebulaFlag: Flag<Byte> = flags2.flag8

                override fun Sink.writeInNebulaFlag() {
                    writeByteFlags(inNebulaFlag)
                }

                override fun Sink.writeRemainingFlags() {
                    writeFloatFlags(flags5.flag1, flags5.flag2)
                    writeByteFlags(flags5.flag3, flags5.flag4)
                    writeFloatFlags(
                        flags5.flag5,
                        flags5.flag6,
                        flags5.flag7,
                        flags5.flag8,
                        flags6.flag1,
                        flags6.flag2,
                        flags6.flag3,
                        flags6.flag4,
                        flags6.flag5,
                        flags6.flag6,
                        flags6.flag7,
                        flags6.flag8,
                        flags7.flag1,
                    )
                }
            }

            override val dataGenerator: Gen<Data> =
                Arb.bind(
                    genA = ID,
                    genB = Arb.version(2, Arb.int(min = 7)),
                    genC =
                        Arb.flags(
                            arb1 = NAME,
                            arb2 = IMPULSE,
                            arb3 = UNK_1_3,
                            arb4 = UNK_1_4,
                            arb5 = UNK_1_5,
                            arb6 = IS_ENEMY,
                            arb7 = HULL_ID,
                            arb8 = X,
                        ),
                    genD =
                        Arb.flags(
                            arb1 = Y,
                            arb2 = Z,
                            arb3 = UNK_2_3,
                            arb4 = UNK_2_4,
                            arb5 = UNK_2_5,
                            arb6 = UNK_2_6,
                            arb7 = SURRENDERED,
                            arb8 = IN_NEBULA_NEW,
                        ),
                    genE =
                        Arb.flags(
                            arb1 = FRONT,
                            arb2 = FRONT_MAX,
                            arb3 = REAR,
                            arb4 = REAR_MAX,
                            arb5 = UNK_3_5,
                            arb6 = UNK_3_6,
                            arb7 = UNK_3_7,
                            arb8 = UNK_3_8,
                        ),
                    genF =
                        Arb.flags(
                            arb1 = SCAN_BITS,
                            arb2 = UNK_4_2,
                            arb3 = UNK_4_3,
                            arb4 = SIDE,
                            arb5 = UNK_4_5,
                            arb6 = UNK_4_6,
                            arb7 = UNK_4_7,
                            arb8 = UNK_4_8,
                        ),
                    genG =
                        Arb.flags(
                            arb1 = UNK_5_1,
                            arb2 = UNK_5_2,
                            arb3 = UNK_5_3,
                            arb4 = UNK_5_4,
                            arb5 = DAMAGE,
                            arb6 = DAMAGE,
                            arb7 = DAMAGE,
                            arb8 = DAMAGE,
                        ),
                    genH =
                        Arb.flags(
                            arb1 = DAMAGE,
                            arb2 = DAMAGE,
                            arb3 = DAMAGE,
                            arb4 = DAMAGE,
                            arb5 = FREQ,
                            arb6 = FREQ,
                            arb7 = FREQ,
                            arb8 = FREQ,
                        ),
                    genI = Arb.flags(arb1 = FREQ),
                    bindFn = ::Data,
                )
        }

        protected companion object {
            val NAME = Arb.string()
            val IMPULSE = Arb.numericFloat()
            val IS_ENEMY = Arb.int()
            val HULL_ID = Arb.int().filter { it != -1 }
            val SURRENDERED = Arb.byte()
            val IN_NEBULA_OLD = Arb.short()
            val IN_NEBULA_NEW = Arb.byte()
            val FRONT = Arb.numericFloat()
            val FRONT_MAX = Arb.numericFloat()
            val REAR = Arb.numericFloat()
            val REAR_MAX = Arb.numericFloat()
            val SCAN_BITS = Arb.int()
            val SIDE = Arb.byte().filter { it.toInt() != -1 }

            val DAMAGE = Arb.float()
            val FREQ = Arb.float()

            val UNK_1_3 = Arb.float()
            val UNK_1_4 = Arb.float()
            val UNK_1_5 = Arb.float()

            val UNK_2_3 = Arb.float()
            val UNK_2_4 = Arb.float()
            val UNK_2_5 = Arb.float()
            val UNK_2_6 = Arb.float()

            val UNK_3_5 = Arb.short()
            val UNK_3_6 = Arb.byte()
            val UNK_3_7 = Arb.int()
            val UNK_3_8 = Arb.int()

            val UNK_4_2 = Arb.int()
            val UNK_4_3 = Arb.int()
            val UNK_4_5 = Arb.byte()
            val UNK_4_6 = Arb.byte()
            val UNK_4_7 = Arb.byte()
            val UNK_4_8 = Arb.float()

            val UNK_5_1 = Arb.float()
            val UNK_5_2 = Arb.float()
            val UNK_5_3 = Arb.byte()
            val UNK_5_4 = Arb.byte()
        }
    }

    sealed class PlayerShipParser(override val specName: String, val versionArb: Arb<Version>) :
        ObjectParserTestConfig(true) {
        sealed class Data private constructor() :
            ObjectParserData.Real<ArtemisPlayer>(ArtemisPlayer::class, ObjectType.PLAYER_SHIP) {
            internal abstract val flags1: PlayerFlags1
            internal abstract val flags2: PlayerFlags2
            internal abstract val flags3: PlayerFlags3
            internal abstract val flags4: PlayerFlags4
            internal abstract val flags5: PlayerFlags5
            internal abstract val flags6: PlayerFlags6

            private val impulseFlag: Flag<Float>
                get() = flags1.flag2

            private val warpFlag: Flag<Byte>
                get() = flags1.flag7

            private val hullIdFlag: Flag<Int>
                get() = flags2.flag3

            private val xFlag: Flag<Float>
                get() = flags2.flag4

            private val yFlag: Flag<Float>
                get() = flags2.flag5

            private val zFlag: Flag<Float>
                get() = flags2.flag6

            private val nameFlag: Flag<String>
                get() = flags3.flag4

            private val shieldsFrontFlag: Flag<Float>
                get() = flags3.flag5

            private val maxShieldsFrontFlag: Flag<Float>
                get() = flags3.flag6

            private val shieldsRearFlag: Flag<Float>
                get() = flags3.flag7

            private val maxShieldsRearFlag: Flag<Float>
                get() = flags3.flag8

            private val dockingBaseFlag: Flag<Int>
                get() = flags4.flag1

            private val alertStatusFlag: Flag<AlertStatus>
                get() = flags4.flag2

            private val driveTypeFlag: Flag<DriveType>
                get() = flags5.flag1

            private val sideFlag: Flag<Byte>
                get() = flags5.flag6

            private val shipIndexFlag: Flag<Byte>
                get() = flags5.flag8

            private val capitalShipFlag: Flag<Int>
                get() = flags6.flag1

            internal abstract val nebulaTypeFlag: Flag<out Number>

            data class Old
            internal constructor(
                override val objectID: Int,
                override val version: Version,
                override val flags1: PlayerFlags1,
                override val flags2: PlayerFlags2,
                override val flags3: PlayerFlags3Old,
                override val flags4: PlayerFlags4,
                override val flags5: PlayerFlags5,
                override val flags6: PlayerFlags6,
            ) : Data() {
                override val nebulaTypeFlag: Flag<Short> = flags3.flag3

                override fun Sink.writeNebulaTypeFlag() {
                    writeShortFlags(nebulaTypeFlag)
                }
            }

            data class New
            internal constructor(
                override val objectID: Int,
                override val version: Version,
                override val flags1: PlayerFlags1,
                override val flags2: PlayerFlags2,
                override val flags3: PlayerFlags3New,
                override val flags4: PlayerFlags4,
                override val flags5: PlayerFlags5,
                override val flags6: PlayerFlags6,
            ) : Data() {
                override val nebulaTypeFlag: Flag<Byte> = flags3.flag3

                override fun Sink.writeNebulaTypeFlag() {
                    writeByteFlags(nebulaTypeFlag)
                }
            }

            abstract fun Sink.writeNebulaTypeFlag()

            override fun Sink.buildObject() {
                writeFlagBytes(flags1, flags2, flags3, flags4, flags5, flags6)

                writeIntFlags(flags1.flag1)
                writeFloatFlags(impulseFlag, flags1.flag3, flags1.flag4, flags1.flag5)
                writeByteFlags(flags1.flag6, flags1.flag7)
                writeFloatFlags(flags1.flag8)
                writeShortFlags(flags2.flag1)
                writeIntFlags(flags2.flag2, hullIdFlag)
                writeFloatFlags(
                    xFlag,
                    yFlag,
                    zFlag,
                    flags2.flag7,
                    flags2.flag8,
                    flags3.flag1,
                    flags3.flag2,
                )
                writeNebulaTypeFlag()
                writeStringFlags(nameFlag)
                writeFloatFlags(
                    shieldsFrontFlag,
                    maxShieldsFrontFlag,
                    shieldsRearFlag,
                    maxShieldsRearFlag,
                )
                writeIntFlags(dockingBaseFlag)
                writeEnumFlags(alertStatusFlag)
                writeFloatFlags(flags4.flag3)
                writeByteFlags(flags4.flag4, flags4.flag5, flags4.flag6)
                writeIntFlags(flags4.flag7, flags4.flag8)
                writeEnumFlags(driveTypeFlag)
                writeIntFlags(flags5.flag2)
                writeFloatFlags(flags5.flag3)
                writeByteFlags(flags5.flag4)
                writeFloatFlags(flags5.flag5)
                writeByteFlags(sideFlag)
                writeIntFlags(flags5.flag7)
                writeByteFlags(shipIndexFlag)
                writeIntFlags(capitalShipFlag)

                if (version >= Version.ACCENT_COLOR) {
                    writeFloatFlags(flags6.flag2, flags6.flag3)

                    if (version >= Version.BEACON) {
                        writeByteFlags(flags6.flag4, flags6.flag5)
                    }
                }
            }

            override fun validateObject(obj: ArtemisPlayer) {
                testHasPosition(obj, xFlag, zFlag)

                obj.hasPlayerData shouldBeEqual
                    arrayOf(
                            impulseFlag,
                            warpFlag,
                            hullIdFlag,
                            xFlag,
                            yFlag,
                            zFlag,
                            nameFlag,
                            shieldsFrontFlag,
                            shieldsRearFlag,
                            maxShieldsFrontFlag,
                            maxShieldsRearFlag,
                            dockingBaseFlag,
                            alertStatusFlag,
                            driveTypeFlag,
                            sideFlag,
                            shipIndexFlag,
                            capitalShipFlag,
                        )
                        .any { flag -> flag.enabled }

                testFloatPropertyFlags(
                    xFlag to obj.x,
                    yFlag to obj.y,
                    zFlag to obj.z,
                    impulseFlag to obj.impulse,
                    shieldsFrontFlag to obj.shieldsFront.strength,
                    maxShieldsFrontFlag to obj.shieldsFront.maxStrength,
                    shieldsRearFlag to obj.shieldsRear.strength,
                    maxShieldsRearFlag to obj.shieldsRear.maxStrength,
                )

                testBytePropertyFlags(
                    Triple(warpFlag, obj.warp, null),
                    Triple(sideFlag, obj.side, null),
                    Triple(shipIndexFlag, obj.shipIndex, Byte.MIN_VALUE),
                )

                testIntPropertyFlags(
                    hullIdFlag to obj.hullId,
                    dockingBaseFlag to obj.dockingBase,
                    capitalShipFlag to obj.capitalShipID,
                )

                obj.alertStatus shouldMatch alertStatusFlag
                obj.driveType shouldMatch driveTypeFlag
                obj.name shouldMatch nameFlag
            }
        }

        override val parserName: String = "Player ship"
        override val dataGenerator: Gen<Data> =
            Arb.bind(
                genA = ID,
                genB = versionArb,
                genC = PLAYER_FLAGS_1,
                genD = PLAYER_FLAGS_2,
                genE = playerFlags3(UNK_3_3_OLD),
                genF = PLAYER_FLAGS_4,
                genG = PLAYER_FLAGS_5,
                genH = PLAYER_FLAGS_6,
                bindFn = Data::Old,
            )

        data object V1 : PlayerShipParser(before(VERSION_2_4_0), Arb.version(major = 2, minor = 3))

        data object V2 :
            PlayerShipParser(
                between(VERSION_2_4_0, VERSION_2_6_3),
                Arb.choose(
                    3 to Arb.version(major = 2, minor = 6, patchRange = 0..2),
                    997 to Arb.version(major = 2, minorRange = 4..5),
                ),
            )

        data object V3 :
            PlayerShipParser(
                between(VERSION_2_6_3, VERSION_2_7_0),
                Arb.version(major = 2, minor = 6, patchArb = Arb.int(min = 3)),
            )

        data object V4 :
            PlayerShipParser(
                since(VERSION_2_7_0),
                Arb.version(major = 2, minorArb = Arb.int(min = 7)),
            ) {
            override val dataGenerator: Gen<Data> =
                Arb.bind(
                    genA = ID,
                    genB = versionArb,
                    genC = PLAYER_FLAGS_1,
                    genD = PLAYER_FLAGS_2,
                    genE = playerFlags3(UNK_3_3_NEW),
                    genF = PLAYER_FLAGS_4,
                    genG = PLAYER_FLAGS_5,
                    genH = PLAYER_FLAGS_6,
                    bindFn = Data::New,
                )
        }

        protected companion object {
            private val IMPULSE = Arb.numericFloat()
            private val WARP = Arb.byte(min = 0, max = Artemis.MAX_WARP)
            private val HULL_ID = Arb.int().filter { it != -1 }
            val NAME = Arb.string()
            val FRONT = Arb.numericFloat()
            val FRONT_MAX = Arb.numericFloat()
            val REAR = Arb.numericFloat()
            val REAR_MAX = Arb.numericFloat()
            private val DOCKING_BASE = Arb.int().filter { it != -1 }
            private val ALERT = Arb.enum<AlertStatus>()
            private val DRIVE_TYPE = Arb.enum<DriveType>()
            private val SIDE = Arb.byte().filter { it.toInt() != -1 }
            private val SHIP_INDEX = Arb.byte(min = 0x81.toByte())
            private val CAPITAL_SHIP_ID = Arb.int().filter { it != -1 }

            private val UNK_1_1 = Arb.int()
            private val UNK_1_3 = Arb.float()
            private val UNK_1_4 = Arb.float()
            private val UNK_1_5 = Arb.float()
            private val UNK_1_6 = Arb.byte()
            private val UNK_1_8 = Arb.float()

            private val UNK_2_1 = Arb.short()
            private val UNK_2_2 = Arb.int()
            private val UNK_2_7 = Arb.float()
            private val UNK_2_8 = Arb.float()

            val UNK_3_1 = Arb.float()
            val UNK_3_2 = Arb.float()
            private val UNK_3_3_OLD = Arb.short()
            val UNK_3_3_NEW = Arb.byte()

            private val UNK_4_3 = Arb.float()
            private val UNK_4_4 = Arb.byte()
            private val UNK_4_5 = Arb.byte()
            private val UNK_4_6 = Arb.byte()
            private val UNK_4_7 = Arb.int()
            private val UNK_4_8 = Arb.int()

            private val UNK_5_2 = Arb.int()
            private val UNK_5_3 = Arb.float()
            private val UNK_5_4 = Arb.byte()
            private val UNK_5_5 = Arb.float()
            private val UNK_5_7 = Arb.int()

            private val UNK_6_2 = Arb.float()
            private val UNK_6_3 = Arb.float()
            private val UNK_6_4 = Arb.byte()
            private val UNK_6_5 = Arb.byte()

            internal val PLAYER_FLAGS_1 =
                Arb.flags(
                    arb1 = UNK_1_1,
                    arb2 = IMPULSE,
                    arb3 = UNK_1_3,
                    arb4 = UNK_1_4,
                    arb5 = UNK_1_5,
                    arb6 = UNK_1_6,
                    arb7 = WARP,
                    arb8 = UNK_1_8,
                )

            internal val PLAYER_FLAGS_2 =
                Arb.flags(
                    arb1 = UNK_2_1,
                    arb2 = UNK_2_2,
                    arb3 = HULL_ID,
                    arb4 = X,
                    arb5 = Y,
                    arb6 = Z,
                    arb7 = UNK_2_7,
                    arb8 = UNK_2_8,
                )

            internal val PLAYER_FLAGS_4 =
                Arb.flags(
                    arb1 = DOCKING_BASE,
                    arb2 = ALERT,
                    arb3 = UNK_4_3,
                    arb4 = UNK_4_4,
                    arb5 = UNK_4_5,
                    arb6 = UNK_4_6,
                    arb7 = UNK_4_7,
                    arb8 = UNK_4_8,
                )

            internal val PLAYER_FLAGS_5 =
                Arb.flags(
                    arb1 = DRIVE_TYPE,
                    arb2 = UNK_5_2,
                    arb3 = UNK_5_3,
                    arb4 = UNK_5_4,
                    arb5 = UNK_5_5,
                    arb6 = SIDE,
                    arb7 = UNK_5_7,
                    arb8 = SHIP_INDEX,
                )

            internal val PLAYER_FLAGS_6 =
                Arb.flags(
                    arb1 = CAPITAL_SHIP_ID,
                    arb2 = UNK_6_2,
                    arb3 = UNK_6_3,
                    arb4 = UNK_6_4,
                    arb5 = UNK_6_5,
                )

            internal fun <T> playerFlags3(flag3: Arb<T>) =
                Arb.flags(
                    arb1 = UNK_3_1,
                    arb2 = UNK_3_2,
                    arb3 = flag3,
                    arb4 = NAME,
                    arb5 = FRONT,
                    arb6 = FRONT_MAX,
                    arb7 = REAR,
                    arb8 = REAR_MAX,
                )
        }
    }

    data object UpgradesParser : ObjectParserTestConfig(true) {
        data class Data
        internal constructor(
            override val objectID: Int,
            private val a1: UpgradesByteFlags,
            private val a2: UpgradesByteFlags,
            private val a3: UpgradesByteFlags,
            private val ac: UpgradesByteFlags,
            private val c2: UpgradesByteFlags,
            private val c3: UpgradesByteFlags,
            private val c4: UpgradesByteFlags,
            private val t1: UpgradesShortFlags,
            private val t2: UpgradesShortFlags,
            private val t3: UpgradesShortFlags,
            private val t4: UpgradesEndFlags,
        ) : ObjectParserData.Real<ArtemisPlayer>(ArtemisPlayer::class, ObjectType.UPGRADES) {
            override val version: Version = Version.DEFAULT

            private val activeFlag: Flag<Byte> = a2.flag1
            private val countFlag: Flag<Byte> = c2.flag5
            private val timeFlag: Flag<Short> = t2.flag1

            override fun Sink.buildObject() {
                writeFlagBytes(a1, a2, a3, ac, c2, c3, c4, t1, t2, t3, t4)

                arrayOf(a1, a2, a3, ac, c2, c3, c4).forEach { flagByte ->
                    writeByteFlags(
                        flagByte.flag1,
                        flagByte.flag2,
                        flagByte.flag3,
                        flagByte.flag4,
                        flagByte.flag5,
                        flagByte.flag6,
                        flagByte.flag7,
                        flagByte.flag8,
                    )
                }

                arrayOf(t1, t2, t3).forEach { flagByte ->
                    writeShortFlags(
                        flagByte.flag1,
                        flagByte.flag2,
                        flagByte.flag3,
                        flagByte.flag4,
                        flagByte.flag5,
                        flagByte.flag6,
                        flagByte.flag7,
                        flagByte.flag8,
                    )
                }

                writeShortFlags(t4.flag1, t4.flag2, t4.flag3, t4.flag4)
            }

            override fun validateObject(obj: ArtemisPlayer) {
                obj.hasPosition.shouldBeFalse()

                obj.hasUpgradeData shouldBeEqual
                    arrayOf(activeFlag, countFlag, timeFlag).any { flag -> flag.enabled }

                obj.doubleAgentActive shouldMatch activeFlag
                obj.doubleAgentCount shouldMatch countFlag
                obj.doubleAgentSecondsLeft shouldMatch timeFlag
            }
        }

        private val ACTIVE = Arb.byte()
        private val COUNT = Arb.byte().filter { it.toInt() != -1 }
        private val TIME = Arb.short().filter { it.toInt() != -1 }

        override val parserName: String = "Player ship upgrades"
        override val dataGenerator: Gen<Data> =
            Arb.bind(
                genA = ID,
                genB = allFlags(ACTIVE),
                genC = allFlags(ACTIVE),
                genD = allFlags(ACTIVE),
                genE =
                    Arb.flags(
                        arb1 = ACTIVE,
                        arb2 = ACTIVE,
                        arb3 = ACTIVE,
                        arb4 = ACTIVE,
                        arb5 = COUNT,
                        arb6 = COUNT,
                        arb7 = COUNT,
                        arb8 = COUNT,
                    ),
                genF = allFlags(COUNT),
                genG = allFlags(COUNT),
                genH = allFlags(COUNT),
                genI = allFlags(TIME),
                genJ = allFlags(TIME),
                genK = allFlags(TIME),
                genL = Arb.flags(arb1 = TIME, arb2 = TIME, arb3 = TIME, arb4 = TIME),
                bindFn = ::Data,
            )

        private fun <T> allFlags(arb: Arb<T>): Arb<FlagByte<T, T, T, T, T, T, T, T>> =
            Arb.flags(
                arb1 = arb,
                arb2 = arb,
                arb3 = arb,
                arb4 = arb,
                arb5 = arb,
                arb6 = arb,
                arb7 = arb,
                arb8 = arb,
            )
    }

    sealed class WeaponsParser(override val specName: String) : ObjectParserTestConfig(true) {
        override val parserName: String = "Player ship weapons"

        override suspend fun describeMore(scope: DescribeSpecContainerScope) {
            scope.describe("Bits") {
                describe("OrdnanceCountBit") {
                    withData(OrdnanceType.entries) { ordnanceType ->
                        OrdnanceCountBit(ordnanceType).ordnanceType shouldBeEqual ordnanceType
                    }
                }

                describe("TubeTimeBit") {
                    withData(nameFn = { testName(it) }, 0 until Artemis.MAX_TUBES) { index ->
                        TubeTimeBit(index).index shouldBeEqual index
                    }
                }

                describe("TubeStateBit") {
                    withData(nameFn = { testName(it) }, 0 until Artemis.MAX_TUBES) { index ->
                        TubeStateBit(index).index shouldBeEqual index
                    }
                }

                describe("TubeContentsBit") {
                    withData(nameFn = { testName(it) }, 0 until Artemis.MAX_TUBES) { index ->
                        TubeContentsBit(index).index shouldBeEqual index
                    }
                }
            }
        }

        abstract class WeaponsData internal constructor() :
            ObjectParserData.Real<ArtemisPlayer>(ArtemisPlayer::class, ObjectType.WEAPONS_CONSOLE) {
            internal abstract val countFlags: Array<Flag<Byte>>
            internal abstract val unknownFlag: Flag<Byte>
            internal abstract val timeFlags: Array<Flag<Float>>
            internal abstract val statusFlags: Array<Flag<TubeState>>
            internal abstract val typeFlags: Array<Flag<OrdnanceType>>

            internal abstract val allFlagBytes: Array<AnyFlagByte>

            override fun Sink.buildObject() {
                writeFlagBytes(allFlagBytes)

                writeByteFlags(countFlags)
                writeByteFlags(unknownFlag)
                writeFloatFlags(timeFlags)
                writeEnumFlags(statusFlags)
                writeEnumFlags(typeFlags)
            }

            override fun validateObject(obj: ArtemisPlayer) {
                obj.hasPosition.shouldBeFalse()

                obj.hasWeaponsData shouldBeEqual
                    arrayOf(countFlags, statusFlags).any { flags ->
                        flags.any { flag -> flag.enabled }
                    }

                testBytePropertyFlags(countFlags.zip(obj.ordnanceCounts))

                obj.tubes.forEachIndexed { index, tube ->
                    val statusFlag = statusFlags[index]
                    val contentsFlag = typeFlags[index]

                    val status = statusFlag.value.takeIf { statusFlag.enabled }
                    val contents = contentsFlag.value

                    if (status != null) {
                        tube.state shouldContainValue status
                    } else {
                        tube.state.shouldBeUnspecified()
                    }

                    if (contentsFlag.enabled && status != null && status != TubeState.UNLOADED) {
                        tube.lastContents shouldContainValue contents
                        if (status == TubeState.LOADING || status == TubeState.LOADED) {
                            tube.contents.shouldNotBeNull() shouldBeEqual contents
                        } else {
                            tube.contents.shouldBeNull()
                        }
                    } else {
                        tube.lastContents.shouldBeUnspecified()
                        tube.contents.shouldBeNull()
                    }

                    tube.hasData.shouldBeEqual(statusFlag.enabled)
                }
            }
        }

        data object V1 : WeaponsParser(before(VERSION_2_6_3)) {
            data class Data
            internal constructor(
                override val objectID: Int,
                override val version: Version,
                private val flags1: WeaponsV1Flags1,
                private val flags2: WeaponsV1Flags2,
                private val flags3: WeaponsV1Flags3,
            ) : WeaponsData() {
                override val countFlags: Array<Flag<Byte>> =
                    arrayOf(flags1.flag1, flags1.flag2, flags1.flag3, flags1.flag4, flags1.flag5)

                override val unknownFlag: Flag<Byte> = flags1.flag6

                override val timeFlags: Array<Flag<Float>> =
                    arrayOf(
                        flags1.flag7,
                        flags1.flag8,
                        flags2.flag1,
                        flags2.flag2,
                        flags2.flag3,
                        flags2.flag4,
                    )

                override val statusFlags: Array<Flag<TubeState>> =
                    arrayOf(
                        flags2.flag5,
                        flags2.flag6,
                        flags2.flag7,
                        flags2.flag8,
                        flags3.flag1,
                        flags3.flag2,
                    )

                override val typeFlags: Array<Flag<OrdnanceType>> =
                    arrayOf(
                        flags3.flag3,
                        flags3.flag4,
                        flags3.flag5,
                        flags3.flag6,
                        flags3.flag7,
                        flags3.flag8,
                    )

                override val allFlagBytes: Array<AnyFlagByte> =
                    arrayOf(
                        flags1,
                        flags2,
                        flags3,
                        FlagByte(
                            flag1 = dummy,
                            flag2 = dummy,
                            flag3 = dummy,
                            flag4 = dummy,
                            flag5 = dummy,
                            flag6 = dummy,
                            flag7 = dummy,
                            flag8 = dummy,
                        ),
                    )
            }

            private val typeArb = Arb.enum<OrdnanceType>().filter { it < OrdnanceType.BEACON }

            override val dataGenerator: Gen<Data> =
                Arb.bind(
                    genA = ID,
                    genB =
                        Arb.choose(
                            3 to Arb.version(major = 2, minor = 6, patchRange = 0..2),
                            997 to Arb.version(major = 2, minorRange = 3..5),
                        ),
                    genC =
                        Arb.flags(
                            arb1 = COUNT,
                            arb2 = COUNT,
                            arb3 = COUNT,
                            arb4 = COUNT,
                            arb5 = COUNT,
                            arb6 = UNKNOWN,
                            arb7 = TIME,
                            arb8 = TIME,
                        ),
                    genD =
                        Arb.flags(
                            arb1 = TIME,
                            arb2 = TIME,
                            arb3 = TIME,
                            arb4 = TIME,
                            arb5 = STATUS,
                            arb6 = STATUS,
                            arb7 = STATUS,
                            arb8 = STATUS,
                        ),
                    genE =
                        Arb.flags(
                            arb1 = STATUS,
                            arb2 = STATUS,
                            arb3 = typeArb,
                            arb4 = typeArb,
                            arb5 = typeArb,
                            arb6 = typeArb,
                            arb7 = typeArb,
                            arb8 = typeArb,
                        ),
                    bindFn = ::Data,
                )
        }

        data object V2 : WeaponsParser(since(VERSION_2_6_3)) {
            data class Data
            internal constructor(
                override val objectID: Int,
                override val version: Version,
                private val flags1: WeaponsV2Flags1,
                private val flags2: WeaponsV2Flags2,
                private val flags3: WeaponsV2Flags3,
                private val flags4: WeaponsV2Flags4,
            ) : WeaponsData() {
                override val countFlags: Array<Flag<Byte>> =
                    arrayOf(
                        flags1.flag1,
                        flags1.flag2,
                        flags1.flag3,
                        flags1.flag4,
                        flags1.flag5,
                        flags1.flag6,
                        flags1.flag7,
                        flags1.flag8,
                    )

                override val unknownFlag: Flag<Byte> = Flag(false, 0)

                override val timeFlags: Array<Flag<Float>> =
                    arrayOf(
                        flags2.flag1,
                        flags2.flag2,
                        flags2.flag3,
                        flags2.flag4,
                        flags2.flag5,
                        flags2.flag6,
                    )

                override val statusFlags: Array<Flag<TubeState>> =
                    arrayOf(
                        flags2.flag7,
                        flags2.flag8,
                        flags3.flag1,
                        flags3.flag2,
                        flags3.flag3,
                        flags3.flag4,
                    )

                override val typeFlags: Array<Flag<OrdnanceType>> =
                    arrayOf(
                        flags3.flag5,
                        flags3.flag6,
                        flags3.flag7,
                        flags3.flag8,
                        flags4.flag1,
                        flags4.flag2,
                    )

                override val allFlagBytes: Array<AnyFlagByte> =
                    arrayOf(flags1, flags2, flags3, flags4)
            }

            private val typeArb = Arb.enum<OrdnanceType>()

            override val dataGenerator: Gen<Data> =
                Arb.bind(
                    genA = ID,
                    genB =
                        Arb.choose(
                            1 to Arb.version(major = 2, minor = 6, patchArb = Arb.int(min = 3)),
                            PropertyTesting.defaultIterationCount - 1 to
                                Arb.version(major = 2, minorArb = Arb.int(min = 7)),
                        ),
                    genC =
                        Arb.flags(
                            arb1 = COUNT,
                            arb2 = COUNT,
                            arb3 = COUNT,
                            arb4 = COUNT,
                            arb5 = COUNT,
                            arb6 = COUNT,
                            arb7 = COUNT,
                            arb8 = COUNT,
                        ),
                    genD =
                        Arb.flags(
                            arb1 = TIME,
                            arb2 = TIME,
                            arb3 = TIME,
                            arb4 = TIME,
                            arb5 = TIME,
                            arb6 = TIME,
                            arb7 = STATUS,
                            arb8 = STATUS,
                        ),
                    genE =
                        Arb.flags(
                            arb1 = STATUS,
                            arb2 = STATUS,
                            arb3 = STATUS,
                            arb4 = STATUS,
                            arb5 = typeArb,
                            arb6 = typeArb,
                            arb7 = typeArb,
                            arb8 = typeArb,
                        ),
                    genF = Arb.flags(arb1 = typeArb, arb2 = typeArb),
                    bindFn = ::Data,
                )
        }

        protected companion object {
            val COUNT = Arb.byte().filter { it.toInt() != -1 }
            val UNKNOWN = Arb.byte()
            val TIME = Arb.numericFloat()
            val STATUS = Arb.enum<TubeState>()

            private fun testName(index: Int): String = "Index: $index"
        }
    }

    sealed class Unobserved : ObjectParserTestConfig(false) {
        data object Engineering : Unobserved() {
            data class Data
            internal constructor(
                override val objectID: Int,
                private val heatFlags: EngineeringFloatFlags,
                private val enFlags: EngineeringFloatFlags,
                private val coolFlags: EngineeringByteFlags,
            ) : ObjectParserData.Unobserved(ObjectType.ENGINEERING_CONSOLE) {
                override val version: Version
                    get() = Version.DEFAULT

                override fun Sink.buildObject() {
                    writeFlagBytes(heatFlags, enFlags, coolFlags)

                    arrayOf(heatFlags, enFlags).forEach { flags ->
                        writeFloatFlags(
                            flags.flag1,
                            flags.flag2,
                            flags.flag3,
                            flags.flag4,
                            flags.flag5,
                            flags.flag6,
                            flags.flag7,
                            flags.flag8,
                        )
                    }

                    writeByteFlags(
                        coolFlags.flag1,
                        coolFlags.flag2,
                        coolFlags.flag3,
                        coolFlags.flag4,
                        coolFlags.flag5,
                        coolFlags.flag6,
                        coolFlags.flag7,
                        coolFlags.flag8,
                    )
                }
            }

            override val parserName: String = "Player ship engineering"
            override val dataGenerator: Gen<Data> =
                Arb.bind(
                    genA = ID,
                    genB = systemFlags(Arb.numericFloat()),
                    genC = systemFlags(Arb.numericFloat()),
                    genD = systemFlags(Arb.byte()),
                    bindFn = ::Data,
                )

            private fun <T> systemFlags(arb: Arb<T>): Arb<FlagByte<T, T, T, T, T, T, T, T>> =
                Arb.flags(
                    arb1 = arb,
                    arb2 = arb,
                    arb3 = arb,
                    arb4 = arb,
                    arb5 = arb,
                    arb6 = arb,
                    arb7 = arb,
                    arb8 = arb,
                )
        }

        sealed class Anomaly(override val specName: String, versionArb: Arb<Version>) :
            Unobserved() {
            data class Data
            internal constructor(
                override val objectID: Int,
                override val version: Version,
                private val flags: AnomalyFlags,
            ) : ObjectParserData.Unobserved(ObjectType.ANOMALY) {
                override fun Sink.buildObject() {
                    val beaconVersion = version >= Version.BEACON

                    writeFlagBytes(flags)
                    if (beaconVersion) writeByte(0)

                    writeFloatFlags(flags.flag1, flags.flag2, flags.flag3)
                    writeIntFlags(flags.flag4, flags.flag5, flags.flag6)

                    if (beaconVersion) {
                        writeByteFlags(flags.flag7, flags.flag8)
                    }
                }
            }

            data object V1 :
                Anomaly(
                    before(VERSION_2_6_3),
                    Arb.choose(
                        3 to Arb.version(major = 2, minor = 6, patchRange = 0..2),
                        997 to Arb.version(major = 2, minorRange = 3..5),
                    ),
                )

            data object V2 :
                Anomaly(
                    since(VERSION_2_6_3),
                    Arb.choose(
                        1 to Arb.version(major = 2, minor = 6, patchArb = Arb.int(min = 3)),
                        PropertyTesting.defaultIterationCount - 1 to
                            Arb.version(major = 2, minorArb = Arb.int(min = 7)),
                    ),
                )

            override val parserName: String = "Anomaly"
            override val dataGenerator: Gen<Data> =
                Arb.bind(
                    genA = ID,
                    genB = versionArb,
                    genC =
                        Arb.flags(
                            arb1 = Arb.numericFloat(),
                            arb2 = Arb.numericFloat(),
                            arb3 = Arb.numericFloat(),
                            arb4 = Arb.int(),
                            arb5 = Arb.int(),
                            arb6 = Arb.int(),
                            arb7 = Arb.byte(),
                            arb8 = Arb.byte(),
                        ),
                    bindFn = ::Data,
                )
        }

        sealed class Nebula(override val specName: String, versionArb: Arb<Version>) :
            Unobserved() {
            data class Data
            internal constructor(
                override val objectID: Int,
                override val version: Version,
                private val flags: NebulaFlags,
            ) : ObjectParserData.Unobserved(ObjectType.NEBULA) {
                override fun Sink.buildObject() {
                    writeFlagBytes(flags)

                    writeFloatFlags(
                        flags.flag1,
                        flags.flag2,
                        flags.flag3,
                        flags.flag4,
                        flags.flag5,
                        flags.flag6,
                    )

                    if (version >= Version.NEBULA_TYPES) {
                        writeByteFlags(flags.flag7)
                    }
                }
            }

            data object V1 :
                Nebula(before(VERSION_2_7_0), Arb.version(major = 2, minorRange = 3..6))

            data object V2 :
                Nebula(since(VERSION_2_7_0), Arb.version(major = 2, minorArb = Arb.int(min = 7)))

            override val parserName: String = "Nebula"
            override val dataGenerator: Gen<Data> =
                Arb.bind(
                    genA = ID,
                    genB = versionArb,
                    genC =
                        Arb.flags(
                            arb1 = Arb.numericFloat(),
                            arb2 = Arb.numericFloat(),
                            arb3 = Arb.numericFloat(),
                            arb4 = Arb.numericFloat(),
                            arb5 = Arb.numericFloat(),
                            arb6 = Arb.numericFloat(),
                            arb7 = Arb.byte(),
                        ),
                    bindFn = ::Data,
                )
        }

        data object Torpedo : Unobserved() {
            data class Data
            internal constructor(override val objectID: Int, private val flags: TorpedoFlags) :
                ObjectParserData.Unobserved(ObjectType.TORPEDO) {
                override val version: Version
                    get() = Version.DEFAULT

                override fun Sink.buildObject() {
                    writeFlagBytes(flags)
                    writeByte(0)

                    writeFloatFlags(
                        flags.flag1,
                        flags.flag2,
                        flags.flag3,
                        flags.flag4,
                        flags.flag5,
                        flags.flag6,
                    )
                    writeIntFlags(flags.flag7, flags.flag8)
                }
            }

            override val parserName: String = "Torpedo"
            override val dataGenerator: Gen<Data> =
                Arb.bind(
                    ID,
                    Arb.flags(
                        arb1 = Arb.numericFloat(),
                        arb2 = Arb.numericFloat(),
                        arb3 = Arb.numericFloat(),
                        arb4 = Arb.numericFloat(),
                        arb5 = Arb.numericFloat(),
                        arb6 = Arb.numericFloat(),
                        arb7 = Arb.int(),
                        arb8 = Arb.int(),
                    ),
                    ::Data,
                )
        }

        data object Asteroid : Unobserved() {
            data class Data
            internal constructor(override val objectID: Int, private val flags: AsteroidFlags) :
                ObjectParserData.Unobserved(ObjectType.ASTEROID) {
                override val version: Version
                    get() = Version.DEFAULT

                override fun Sink.buildObject() {
                    writeFlagBytes(flags)

                    writeFloatFlags(flags.flag1, flags.flag2, flags.flag3)
                }
            }

            override val parserName: String = "Asteroid"
            override val dataGenerator: Gen<Data> =
                Arb.bind(
                    ID,
                    Arb.flags(Arb.numericFloat(), Arb.numericFloat(), Arb.numericFloat()),
                    ::Data,
                )
        }

        sealed class GenericMesh(override val specName: String, versionArb: Arb<Version>) :
            Unobserved() {
            data class Data
            internal constructor(
                override val objectID: Int,
                override val version: Version,
                private val flags1: GenericMeshFlags1,
                private val flags2: GenericMeshFlags2,
                private val flags3: GenericMeshFlags3,
                private val flags4: GenericMeshFlags4,
            ) : ObjectParserData.Unobserved(ObjectType.GENERIC_MESH) {
                override fun Sink.buildObject() {
                    writeFlagBytes(flags1, flags2, flags3, flags4)

                    writeFloatFlags(flags1.flag1, flags1.flag2, flags1.flag3)
                    writeIntFlags(flags1.flag4, flags1.flag5, flags1.flag6)
                    writeFloatFlags(
                        flags1.flag7,
                        flags1.flag8,
                        flags2.flag1,
                        flags2.flag2,
                        flags2.flag3,
                        flags2.flag4,
                    )
                    writeStringFlags(flags2.flag5, flags2.flag6, flags2.flag7)
                    writeFloatFlags(flags2.flag8)
                    writeByteFlags(flags3.flag1)
                    writeFloatFlags(
                        flags3.flag2,
                        flags3.flag3,
                        flags3.flag4,
                        flags3.flag5,
                        flags3.flag6,
                        flags3.flag7,
                    )
                    writeByteFlags(flags3.flag8)
                    writeStringFlags(flags4.flag1, flags4.flag2)

                    if (version >= Version.NEBULA_TYPES) {
                        writeIntFlags(flags4.flag3)
                    }
                }
            }

            data object V1 :
                GenericMesh(before(VERSION_2_7_0), Arb.version(major = 2, minorRange = 3..6))

            data object V2 :
                GenericMesh(
                    since(VERSION_2_7_0),
                    Arb.version(major = 2, minorArb = Arb.int(min = 7)),
                )

            override val parserName: String = "Generic mesh"
            override val dataGenerator: Gen<Data> =
                Arb.bind(
                    genA = ID,
                    genB = versionArb,
                    genC =
                        Arb.flags(
                            arb1 = Arb.numericFloat(),
                            arb2 = Arb.numericFloat(),
                            arb3 = Arb.numericFloat(),
                            arb4 = Arb.int(),
                            arb5 = Arb.int(),
                            arb6 = Arb.int(),
                            arb7 = Arb.numericFloat(),
                            arb8 = Arb.numericFloat(),
                        ),
                    genD =
                        Arb.flags(
                            arb1 = Arb.numericFloat(),
                            arb2 = Arb.numericFloat(),
                            arb3 = Arb.numericFloat(),
                            arb4 = Arb.numericFloat(),
                            arb5 = Arb.string(),
                            arb6 = Arb.string(),
                            arb7 = Arb.string(),
                            arb8 = Arb.numericFloat(),
                        ),
                    genE =
                        Arb.flags(
                            arb1 = Arb.byte(),
                            arb2 = Arb.numericFloat(),
                            arb3 = Arb.numericFloat(),
                            arb4 = Arb.numericFloat(),
                            arb5 = Arb.numericFloat(),
                            arb6 = Arb.numericFloat(),
                            arb7 = Arb.numericFloat(),
                            arb8 = Arb.byte(),
                        ),
                    genF = Arb.flags(arb1 = Arb.string(), arb2 = Arb.string(), arb3 = Arb.int()),
                    bindFn = ::Data,
                )
        }

        data object Drone : Unobserved() {
            data class Data
            internal constructor(
                override val objectID: Int,
                private val flags1: DroneFlags1,
                private val flags2: DroneFlags2,
            ) : ObjectParserData.Unobserved(ObjectType.DRONE) {
                override val version: Version
                    get() = Version.DEFAULT

                override fun Sink.buildObject() {
                    writeFlagBytes(flags1, flags2)

                    writeIntFlags(flags1.flag1)
                    writeFloatFlags(
                        flags1.flag2,
                        flags1.flag3,
                        flags1.flag4,
                        flags1.flag5,
                        flags1.flag6,
                        flags1.flag7,
                    )
                    writeIntFlags(flags1.flag8)
                    writeFloatFlags(flags2.flag1)
                }
            }

            override val parserName: String = "Drone"
            override val dataGenerator: Gen<Data> =
                Arb.bind(
                    genA = ID,
                    genB =
                        Arb.flags(
                            arb1 = Arb.int(),
                            arb2 = Arb.numericFloat(),
                            arb3 = Arb.numericFloat(),
                            arb4 = Arb.numericFloat(),
                            arb5 = Arb.numericFloat(),
                            arb6 = Arb.numericFloat(),
                            arb7 = Arb.numericFloat(),
                            arb8 = Arb.int(),
                        ),
                    genC = Arb.flags(arb1 = Arb.numericFloat()),
                    bindFn = ::Data,
                )
        }
    }

    abstract val dataGenerator: Gen<PacketTestData.Server<ObjectUpdatePacket>>
    abstract val parserName: String
    open val specName: String
        get() = toString()

    open suspend fun describeMore(scope: DescribeSpecContainerScope) {}

    fun afterTest(
        fixture: ObjectUpdatePacketFixture,
        data: PacketTestData.Server<ObjectUpdatePacket>,
    ) {
        if (data is ObjectParserData.Real<*>) {
            fixture.objects.add(data.realObject)
        }
    }

    internal companion object {
        val ID = Arb.int()
        val X = Arb.numericFloat()
        val Y = Arb.numericFloat()
        val Z = Arb.numericFloat()

        const val VERSION_2_4_0 = "2.4.0"
        const val VERSION_2_6_0 = "2.6.0"
        const val VERSION_2_6_3 = "2.6.3"
        const val VERSION_2_7_0 = "2.7.0"

        private fun before(version: String): String = "Before $version"

        private fun since(version: String): String = "Since $version"

        private fun between(from: String, until: String) = "From $from until $until"

        fun buildObject(block: Sink.() -> Unit): Source = buildPacket {
            block()
            writeIntLe(0)
        }

        fun testHasPosition(obj: ArtemisObject<*>, xFlag: Flag<Float>, zFlag: Flag<Float>) {
            obj.hasPosition.shouldBeEqual(xFlag.enabled && zFlag.enabled)
        }

        fun Sink.writeFlagBytes(firstFlags: AnyFlagByte, vararg flagBytes: AnyFlagByte) {
            writeByte(firstFlags.byteValue)
            writeFlagBytes(flagBytes.iterator())
        }

        fun Sink.writeFlagBytes(flagBytes: Array<AnyFlagByte>) {
            writeFlagBytes(flagBytes.iterator())
        }

        private fun Sink.writeFlagBytes(flagBytes: Iterator<AnyFlagByte>) {
            flagBytes.forEach { writeByte(it.byteValue) }
        }
    }
}
