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
import com.walkertribe.ian.world.shouldBeSpecified
import com.walkertribe.ian.world.shouldBeUnspecified
import com.walkertribe.ian.world.shouldContainValue
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeEmpty
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
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.writeIntLe
import kotlin.reflect.KClass
import kotlin.reflect.cast

sealed class ObjectParserTestConfig(val recognizesObjectListeners: Boolean) {
    sealed class ObjectParserData<T : ArtemisObject<T>>(
        val objectID: Int,
        private val objectType: ObjectType,
    ) : PacketTestData.Server<ObjectUpdatePacket> {
        abstract class Real<T : ArtemisObject<T>>(
            objectID: Int,
            private val objectClass: KClass<T>,
            objectType: ObjectType,
        ) : ObjectParserData<T>(objectID, objectType) {
            lateinit var realObject: T

            abstract fun validateObject(obj: T)

            override fun validate(packet: ObjectUpdatePacket) {
                packet.objects.size shouldBeEqual 1
                realObject = packet.objects[0].let { obj ->
                    obj.id shouldBeEqual objectID
                    obj should beInstanceOf(objectClass)
                    objectClass.cast(obj)
                }.also(this::validateObject)
            }
        }

        abstract class Unobserved(
            objectID: Int,
            objectType: ObjectType,
        ) : ObjectParserData<Nothing>(objectID, objectType) {
            override fun validate(packet: ObjectUpdatePacket) {
                packet.objects.shouldBeEmpty()
            }
        }

        abstract fun Sink.buildObject()

        override fun buildPayload(): Source = buildObject {
            writeByte(objectType.id)
            writeIntLe(objectID)
            buildObject()
        }
    }

    data object Empty : ObjectParserTestConfig(false) {
        data object Data : PacketTestData.Server<ObjectUpdatePacket> {
            override val version: Version get() = Version.LATEST

            override fun buildPayload(): Source = buildObject { }

            override fun validate(packet: ObjectUpdatePacket) {
                packet.objects.shouldBeEmpty()
            }
        }

        override val parserName: String = "Empty"
        override val dataGenerator: Gen<Data> = Exhaustive.of(Data)
    }

    data object BaseParser : ObjectParserTestConfig(true) {
        class Data internal constructor(
            objectID: Int,
            private val flags1: BaseFlags1,
            private val flags2: BaseFlags2,
        ) : ObjectParserData.Real<ArtemisBase>(objectID, ArtemisBase::class, ObjectType.BASE) {
            override val version: Version get() = Version.LATEST

            private val nameFlag = flags1.flag1
            private val shieldsFlag = flags1.flag2
            private val maxShieldsFlag = flags1.flag3
            private val hullIdFlag = flags1.flag5
            private val xFlag = flags1.flag6
            private val yFlag = flags1.flag7
            private val zFlag = flags1.flag8

            override fun Sink.buildObject() {
                arrayOf(flags1, flags2).forEach {
                    writeByte(it.byteValue)
                }

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

                if (nameFlag.enabled) {
                    obj.name.shouldBeSpecified()
                    obj.name.value.shouldNotBeNull() shouldBeEqual nameFlag.value
                } else {
                    obj.name.shouldBeUnspecified()
                }

                if (hullIdFlag.enabled) {
                    obj.hullId shouldContainValue hullIdFlag.value
                } else {
                    obj.hullId.shouldBeUnspecified()
                }

                arrayOf(
                    xFlag to obj.x,
                    yFlag to obj.y,
                    zFlag to obj.z,
                    shieldsFlag to obj.shieldsFront,
                    maxShieldsFlag to obj.shieldsFrontMax,
                ).forEach { (flag, property) ->
                    if (flag.enabled) {
                        property shouldContainValue flag.value
                    } else {
                        property.shouldBeUnspecified()
                    }
                }
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
        override val dataGenerator: Gen<Data> = Arb.bind(
            ID,
            Arb.flags(NAME, SHIELDS, MAX_SHIELDS, UNK_1_4, HULL_ID, X, Y, Z),
            Arb.flags(UNK_2_1, UNK_2_2, UNK_2_3, UNK_2_4, UNK_2_5, UNK_2_6),
            ::Data,
        )

    }

    data object BlackHoleParser : ObjectParserTestConfig(true) {
        class Data internal constructor(
            objectID: Int,
            private val flags: PositionFlags,
        ) : ObjectParserData.Real<ArtemisBlackHole>(
            objectID,
            ArtemisBlackHole::class,
            ObjectType.BLACK_HOLE,
        ) {
            override val version: Version get() = Version.LATEST

            private val xFlag = flags.flag1
            private val yFlag = flags.flag2
            private val zFlag = flags.flag3

            override fun Sink.buildObject() {
                writeByte(flags.byteValue)
                writeFloatFlags(xFlag, yFlag, zFlag)
            }

            override fun validateObject(obj: ArtemisBlackHole) {
                testHasPosition(obj, xFlag, zFlag)

                arrayOf(
                    xFlag to obj.x,
                    yFlag to obj.y,
                    zFlag to obj.z,
                ).forEach { (flag, property) ->
                    if (flag.enabled) {
                        property shouldContainValue flag.value
                    } else {
                        property.shouldBeUnspecified()
                    }
                }
            }
        }

        override val parserName: String = "Black hole"
        override val dataGenerator: Gen<Data> = Arb.bind(
            ID,
            Arb.flags(X, Y, Z),
            ::Data,
        )
    }

    sealed class CreatureParser(
        override val specName: String,
        versionArb: Arb<Version>,
    ) : ObjectParserTestConfig(true) {
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
            ): Arb<Data> = Arb.bind(
                ID,
                arbVersion,
                Arb.flags(X, Y, Z, UNK_1_4, UNK_1_5, UNK_1_6, UNK_1_7, arbCreatureType),
                Arb.flags(UNK_2_1, UNK_2_2, UNK_2_3, UNK_2_4, UNK_2_5, UNK_2_6, UNK_2_7, UNK_2_8),
                Arb.flags(UNK_3_1, UNK_3_2),
                arbForceCreatureType,
                ::Data,
            )
        }

        class Data internal constructor(
            objectID: Int,
            override val version: Version,
            private val flags1: CreatureFlags1,
            private val flags2: CreatureFlags2,
            private val flags3: CreatureFlags3,
            private val forceCreatureType: Boolean,
        ) : ObjectParserData.Real<ArtemisCreature>(
            objectID,
            ArtemisCreature::class,
            ObjectType.CREATURE,
        ) {
            private val xFlag = flags1.flag1
            private val yFlag = flags1.flag2
            private val zFlag = flags1.flag3
            private val creatureTypeFlag = flags1.flag8

            override fun Sink.buildObject() {
                var flagByte1 = flags1.byteValue.toInt()
                if (forceCreatureType) flagByte1 = flagByte1 or CREATURE_TYPE_BIT
                writeByte(flagByte1.toByte())

                arrayOf(flags2, flags3).forEach {
                    writeByte(it.byteValue)
                }

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

                arrayOf(
                    xFlag to obj.x,
                    yFlag to obj.y,
                    zFlag to obj.z,
                ).forEach { (flag, property) ->
                    if (flag.enabled) {
                        property shouldContainValue flag.value
                    } else {
                        property.shouldBeUnspecified()
                    }
                }

                when {
                    forceCreatureType -> obj.isNotTyphon shouldContainValue true
                    creatureTypeFlag.enabled -> obj.isNotTyphon shouldContainValue false
                    else -> obj.isNotTyphon.shouldBeUnspecified()
                }
            }
        }

        data object V1 : CreatureParser(
            "Before 2.6.0",
            Arb.version(major = 2, minorRange = 3..5),
        )

        data object V2 : CreatureParser(
            "Since 2.6.0",
            Arb.version(major = 2, minorArb = Arb.int(min = 6)),
        )

        override val parserName: String = "Creature"
        override val dataGenerator: Gen<Data> = arbData(versionArb, Arb.of(0), Arb.of(false))

        private val nonTyphonDataGenerator: Gen<Data> = arbData(
            versionArb,
            Arb.int().filter { it != 0 },
            Arb.of(true),
        )

        override suspend fun describeMore(scope: DescribeSpecContainerScope) {
            scope.it("Rejects non-typhons") {
                val readChannel = ByteChannel()
                val reader = PacketReader(
                    readChannel,
                    ListenerRegistry().apply { register(TestListener.module) }
                )

                nonTyphonDataGenerator.checkAll {
                    readChannel.writePacketWithHeader(
                        TestPacketTypes.OBJECT_BIT_STREAM,
                        it.buildPayload(),
                    )

                    reader.version = it.version

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
    }

    data object MineParser : ObjectParserTestConfig(true) {
        class Data internal constructor(
            objectID: Int,
            private val flags: PositionFlags,
        ) : ObjectParserData.Real<ArtemisMine>(
            objectID,
            ArtemisMine::class,
            ObjectType.MINE,
        ) {
            override val version: Version get() = Version.LATEST

            private val xFlag = flags.flag1
            private val yFlag = flags.flag2
            private val zFlag = flags.flag3

            override fun Sink.buildObject() {
                writeByte(flags.byteValue)
                writeFloatFlags(xFlag, yFlag, zFlag)
            }

            override fun validateObject(obj: ArtemisMine) {
                testHasPosition(obj, xFlag, zFlag)

                arrayOf(
                    xFlag to obj.x,
                    yFlag to obj.y,
                    zFlag to obj.z,
                ).forEach { (flag, property) ->
                    if (flag.enabled) {
                        property shouldContainValue flag.value
                    } else {
                        property.shouldBeUnspecified()
                    }
                }
            }
        }
        override val parserName: String = "Mine"
        override val dataGenerator: Gen<Data> = Arb.bind(
            ID,
            Arb.flags(X, Y, Z),
            ::Data,
        )
    }

    sealed class NpcShipParser(override val specName: String) : ObjectParserTestConfig(true) {
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
        
        abstract class NpcData internal constructor(
            objectID: Int,
            private val flags1: NpcFlags1,
            private val flags2: NpcFlags2,
            private val flags3: NpcFlags3,
            private val flags4: NpcFlags4,
        ) : ObjectParserData.Real<ArtemisNpc>(
            objectID,
            ArtemisNpc::class,
            ObjectType.NPC_SHIP,
        ) {
            private val nameFlag: Flag<String> = flags1.flag1
            private val impulseFlag: Flag<Float> = flags1.flag2
            private val isEnemyFlag: Flag<Int> = flags1.flag6
            private val hullIdFlag: Flag<Int> = flags1.flag7
            private val xFlag: Flag<Float> = flags1.flag8
            private val yFlag: Flag<Float> = flags2.flag1
            private val zFlag: Flag<Float> = flags2.flag2
            private val surrenderedFlag: Flag<Byte> = flags2.flag7
            internal abstract val inNebulaFlag: Flag<out Number>
            private val shieldsFrontFlag: Flag<Float> = flags3.flag1
            private val maxShieldsFrontFlag: Flag<Float> = flags3.flag2
            private val shieldsRearFlag: Flag<Float> = flags3.flag3
            private val maxShieldsRearFlag: Flag<Float> = flags3.flag4
            private val scanBitsFlag: Flag<Int> = flags4.flag1
            private val sideFlag: Flag<Byte> = flags4.flag4
            
            internal abstract val allFlagBytes: Array<AnyFlagByte>

            abstract fun Sink.writeInNebulaFlag()
            abstract fun Sink.writeRemainingFlags()

            override fun Sink.buildObject() {
                allFlagBytes.forEach { writeByte(it.byteValue) }

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
                writeIntFlags(
                    flags3.flag7,
                    flags3.flag8,
                    scanBitsFlag,
                    flags4.flag2,
                    flags4.flag3,
                )
                writeByteFlags(sideFlag, flags4.flag5, flags4.flag6, flags4.flag7)
                writeFloatFlags(flags4.flag8)
                writeRemainingFlags()
            }

            override fun validateObject(obj: ArtemisNpc) {
                testHasPosition(obj, xFlag, zFlag)

                arrayOf(
                    xFlag to obj.x,
                    yFlag to obj.y,
                    zFlag to obj.z,
                    impulseFlag to obj.impulse,
                    shieldsFrontFlag to obj.shieldsFront,
                    maxShieldsFrontFlag to obj.shieldsFrontMax,
                    shieldsRearFlag to obj.shieldsRear,
                    maxShieldsRearFlag to obj.shieldsRearMax,
                ).forEach { (flag, property) ->
                    if (flag.enabled) {
                        property shouldContainValue flag.value
                    } else {
                        property.shouldBeUnspecified()
                    }
                }

                if (hullIdFlag.enabled) {
                    obj.hullId shouldContainValue hullIdFlag.value
                } else {
                    obj.hullId.shouldBeUnspecified()
                }

                if (sideFlag.enabled) {
                    obj.side shouldContainValue sideFlag.value
                } else {
                    obj.side.shouldBeUnspecified()
                }

                if (scanBitsFlag.enabled) {
                    obj.scanBits shouldContainValue scanBitsFlag.value
                } else {
                    obj.scanBits.shouldBeUnspecified()
                }

                arrayOf(
                    isEnemyFlag to obj.isEnemy,
                    surrenderedFlag to obj.isSurrendered,
                    inNebulaFlag to obj.isInNebula,
                ).forEach { (flag, property) ->
                    if (flag.enabled) {
                        property.shouldContainValue(flag.value.toInt() != 0)
                    } else {
                        property.shouldBeUnspecified()
                    }
                }

                if (nameFlag.enabled) {
                    obj.name.shouldBeSpecified()
                    obj.name.value.shouldNotBeNull() shouldBeEqual nameFlag.value
                } else {
                    obj.name.shouldBeUnspecified()
                }
            }
        }

        override val parserName: String = "NPC ship"

        data object V1 : NpcShipParser("Before 2.6.3") {
            class Data internal constructor(
                objectID: Int,
                override val version: Version,
                flags1: NpcFlags1,
                flags2: NpcFlags2Old,
                flags3: NpcFlags3,
                flags4: NpcFlags4,
                private val flags5: NpcFlags5Old,
                private val flags6: NpcFlags6Old,
            ) : NpcData(objectID, flags1, flags2, flags3, flags4) {
                override val allFlagBytes: Array<AnyFlagByte> = arrayOf(
                    flags1,
                    flags2,
                    flags3,
                    flags4,
                    flags5,
                    flags6,
                )
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

            override val dataGenerator: Gen<Data> = Arb.bind(
                ID,
                Arb.choose(
                    3 to Arb.version(major = 2, minor = 6, patchRange = 0..2),
                    997 to Arb.version(major = 2, minorRange = 3..5),
                ),
                Arb.flags(NAME, IMPULSE, UNK_1_3, UNK_1_4, UNK_1_5, IS_ENEMY, HULL_ID, X),
                Arb.flags(Y, Z, UNK_2_3, UNK_2_4, UNK_2_5, UNK_2_6, SURRENDERED, IN_NEBULA_OLD),
                Arb.flags(FRONT, FRONT_MAX, REAR, REAR_MAX, UNK_3_5, UNK_3_6, UNK_3_7, UNK_3_8),
                Arb.flags(SCAN_BITS, UNK_4_2, UNK_4_3, SIDE, UNK_4_5, UNK_4_6, UNK_4_7, UNK_4_8),
                Arb.flags(UNK_5_1, UNK_5_2, DAMAGE, DAMAGE, DAMAGE, DAMAGE, DAMAGE, DAMAGE),
                Arb.flags(DAMAGE, DAMAGE, FREQ, FREQ, FREQ, FREQ, FREQ),
                ::Data,
            )
        }

        data object V2 : NpcShipParser("From 2.6.3 until 2.7.0") {
            class Data internal constructor(
                objectID: Int,
                override val version: Version,
                flags1: NpcFlags1,
                flags2: NpcFlags2Old,
                flags3: NpcFlags3,
                flags4: NpcFlags4,
                private val flags5: NpcFlags5New,
                private val flags6: NpcFlags6New,
                private val flags7: NpcFlags7,
            ) : NpcData(objectID, flags1, flags2, flags3, flags4) {
                override val allFlagBytes: Array<AnyFlagByte> = arrayOf(
                    flags1,
                    flags2,
                    flags3,
                    flags4,
                    flags5,
                    flags6,
                    flags7,
                )
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

            override val dataGenerator: Gen<Data> = Arb.bind(
                ID,
                Arb.version(major = 2, minor = 6, patchArb = Arb.int(min = 3)),
                Arb.flags(NAME, IMPULSE, UNK_1_3, UNK_1_4, UNK_1_5, IS_ENEMY, HULL_ID, X),
                Arb.flags(Y, Z, UNK_2_3, UNK_2_4, UNK_2_5, UNK_2_6, SURRENDERED, IN_NEBULA_OLD),
                Arb.flags(FRONT, FRONT_MAX, REAR, REAR_MAX, UNK_3_5, UNK_3_6, UNK_3_7, UNK_3_8),
                Arb.flags(SCAN_BITS, UNK_4_2, UNK_4_3, SIDE, UNK_4_5, UNK_4_6, UNK_4_7, UNK_4_8),
                Arb.flags(UNK_5_1, UNK_5_2, UNK_5_3, UNK_5_4, DAMAGE, DAMAGE, DAMAGE, DAMAGE),
                Arb.flags(DAMAGE, DAMAGE, DAMAGE, DAMAGE, FREQ, FREQ, FREQ, FREQ),
                Arb.flags(FREQ),
                ::Data,
            )
        }

        data object V3 : NpcShipParser("Since 2.7.0") {
            class Data internal constructor(
                objectID: Int,
                override val version: Version,
                flags1: NpcFlags1,
                flags2: NpcFlags2New,
                flags3: NpcFlags3,
                flags4: NpcFlags4,
                private val flags5: NpcFlags5New,
                private val flags6: NpcFlags6New,
                private val flags7: NpcFlags7,
            ) : NpcData(objectID, flags1, flags2, flags3, flags4) {
                override val allFlagBytes: Array<AnyFlagByte> = arrayOf(
                    flags1,
                    flags2,
                    flags3,
                    flags4,
                    flags5,
                    flags6,
                    flags7,
                )
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

            override val dataGenerator: Gen<Data> = Arb.bind(
                ID,
                Arb.version(2, Arb.int(min = 7)),
                Arb.flags(NAME, IMPULSE, UNK_1_3, UNK_1_4, UNK_1_5, IS_ENEMY, HULL_ID, X),
                Arb.flags(Y, Z, UNK_2_3, UNK_2_4, UNK_2_5, UNK_2_6, SURRENDERED, IN_NEBULA_NEW),
                Arb.flags(FRONT, FRONT_MAX, REAR, REAR_MAX, UNK_3_5, UNK_3_6, UNK_3_7, UNK_3_8),
                Arb.flags(SCAN_BITS, UNK_4_2, UNK_4_3, SIDE, UNK_4_5, UNK_4_6, UNK_4_7, UNK_4_8),
                Arb.flags(UNK_5_1, UNK_5_2, UNK_5_3, UNK_5_4, DAMAGE, DAMAGE, DAMAGE, DAMAGE),
                Arb.flags(DAMAGE, DAMAGE, DAMAGE, DAMAGE, FREQ, FREQ, FREQ, FREQ),
                Arb.flags(FREQ),
                ::Data,
            )
        }
    }

    sealed class PlayerShipParser(
        override val specName: String,
        val versionArb: Arb<Version>,
    ) : ObjectParserTestConfig(true) {
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

            internal val PLAYER_FLAGS_1 = Arb.flags(
                UNK_1_1,
                IMPULSE,
                UNK_1_3,
                UNK_1_4,
                UNK_1_5,
                UNK_1_6,
                WARP,
                UNK_1_8,
            )

            internal val PLAYER_FLAGS_2 = Arb.flags(
                UNK_2_1,
                UNK_2_2,
                HULL_ID,
                X,
                Y,
                Z,
                UNK_2_7,
                UNK_2_8,
            )

            internal val PLAYER_FLAGS_4 = Arb.flags(
                DOCKING_BASE,
                ALERT,
                UNK_4_3,
                UNK_4_4,
                UNK_4_5,
                UNK_4_6,
                UNK_4_7,
                UNK_4_8,
            )

            internal val PLAYER_FLAGS_5 = Arb.flags(
                DRIVE_TYPE,
                UNK_5_2,
                UNK_5_3,
                UNK_5_4,
                UNK_5_5,
                SIDE,
                UNK_5_7,
                SHIP_INDEX,
            )

            internal val PLAYER_FLAGS_6 = Arb.flags(
                CAPITAL_SHIP_ID,
                UNK_6_2,
                UNK_6_3,
                UNK_6_4,
                UNK_6_5,
            )
        }

        sealed class Data private constructor(
            objectID: Int,
            override val version: Version,
            private val flags1: PlayerFlags1,
            private val flags2: PlayerFlags2,
            private val flags3: PlayerFlags3,
            private val flags4: PlayerFlags4,
            private val flags5: PlayerFlags5,
            private val flags6: PlayerFlags6,
        ) : ObjectParserData.Real<ArtemisPlayer>(
            objectID,
            ArtemisPlayer::class,
            ObjectType.PLAYER_SHIP,
        ) {
            private val impulseFlag: Flag<Float> = flags1.flag2
            private val warpFlag: Flag<Byte> = flags1.flag7
            private val hullIdFlag: Flag<Int> = flags2.flag3
            private val xFlag: Flag<Float> = flags2.flag4
            private val yFlag: Flag<Float> = flags2.flag5
            private val zFlag: Flag<Float> = flags2.flag6
            private val nameFlag: Flag<String> = flags3.flag4
            private val shieldsFrontFlag: Flag<Float> = flags3.flag5
            private val maxShieldsFrontFlag: Flag<Float> = flags3.flag6
            private val shieldsRearFlag: Flag<Float> = flags3.flag7
            private val maxShieldsRearFlag: Flag<Float> = flags3.flag8
            private val dockingBaseFlag: Flag<Int> = flags4.flag1
            private val alertStatusFlag: Flag<AlertStatus> = flags4.flag2
            private val driveTypeFlag: Flag<DriveType> = flags5.flag1
            private val sideFlag: Flag<Byte> = flags5.flag6
            private val shipIndexFlag: Flag<Byte> = flags5.flag8
            private val capitalShipFlag: Flag<Int> = flags6.flag1

            internal abstract val nebulaTypeFlag: Flag<out Number>

            class Old internal constructor(
                objectID: Int,
                version: Version,
                flags1: PlayerFlags1,
                flags2: PlayerFlags2,
                flags3: PlayerFlags3Old,
                flags4: PlayerFlags4,
                flags5: PlayerFlags5,
                flags6: PlayerFlags6,
            ) : Data(objectID, version, flags1, flags2, flags3, flags4, flags5, flags6) {
                override val nebulaTypeFlag: Flag<Short> = flags3.flag3

                override fun Sink.writeNebulaTypeFlag() {
                    writeShortFlags(nebulaTypeFlag)
                }
            }

            class New internal constructor(
                objectID: Int,
                version: Version,
                flags1: PlayerFlags1,
                flags2: PlayerFlags2,
                flags3: PlayerFlags3New,
                flags4: PlayerFlags4,
                flags5: PlayerFlags5,
                flags6: PlayerFlags6,
            ) : Data(objectID, version, flags1, flags2, flags3, flags4, flags5, flags6) {
                override val nebulaTypeFlag: Flag<Byte> = flags3.flag3

                override fun Sink.writeNebulaTypeFlag() {
                    writeByteFlags(nebulaTypeFlag)
                }
            }

            abstract fun Sink.writeNebulaTypeFlag()

            override fun Sink.buildObject() {
                arrayOf(flags1, flags2, flags3, flags4, flags5, flags6).forEach {
                    writeByte(it.byteValue)
                }

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

                obj.hasPlayerData shouldBeEqual arrayOf(
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
                ).any { flag -> flag.enabled }

                arrayOf(
                    impulseFlag to obj.impulse,
                    xFlag to obj.x,
                    yFlag to obj.y,
                    zFlag to obj.z,
                    shieldsFrontFlag to obj.shieldsFront,
                    maxShieldsFrontFlag to obj.shieldsFrontMax,
                    shieldsRearFlag to obj.shieldsRear,
                    maxShieldsRearFlag to obj.shieldsRearMax,
                ).forEach { (flag, property) ->
                    if (flag.enabled) {
                        property shouldContainValue flag.value
                    } else {
                        property.shouldBeUnspecified()
                    }
                }

                arrayOf(
                    Triple(warpFlag, obj.warp, (-1).toByte()),
                    Triple(sideFlag, obj.side, (-1).toByte()),
                    Triple(shipIndexFlag, obj.shipIndex, Byte.MIN_VALUE),
                ).forEach { (flag, property, unknownValue) ->
                    if (flag.enabled) {
                        property shouldContainValue flag.value
                    } else {
                        property.shouldBeUnspecified(unknownValue)
                    }
                }

                arrayOf(
                    hullIdFlag to obj.hullId,
                    dockingBaseFlag to obj.dockingBase,
                    capitalShipFlag to obj.capitalShipID,
                ).forEach { (flag, property) ->
                    if (flag.enabled) {
                        property shouldContainValue flag.value
                    } else {
                        property.shouldBeUnspecified()
                    }
                }

                if (alertStatusFlag.enabled) {
                    obj.alertStatus shouldContainValue alertStatusFlag.value
                } else {
                    obj.alertStatus.shouldBeUnspecified()
                }

                if (driveTypeFlag.enabled) {
                    obj.driveType shouldContainValue driveTypeFlag.value
                } else {
                    obj.driveType.shouldBeUnspecified()
                }

                if (nameFlag.enabled) {
                    obj.name.shouldBeSpecified()
                    obj.name.value.shouldNotBeNull() shouldBeEqual nameFlag.value
                } else {
                    obj.name.shouldBeUnspecified()
                }
            }
        }

        override val parserName: String = "Player ship"
        override val dataGenerator: Gen<Data> = Arb.bind(
            ID,
            versionArb,
            PLAYER_FLAGS_1,
            PLAYER_FLAGS_2,
            Arb.flags(UNK_3_1, UNK_3_2, UNK_3_3_OLD, NAME, FRONT, FRONT_MAX, REAR, REAR_MAX),
            PLAYER_FLAGS_4,
            PLAYER_FLAGS_5,
            PLAYER_FLAGS_6,
            Data::Old,
        )

        data object V1 : PlayerShipParser(
            "Before 2.4.0",
            Arb.version(major = 2, minor = 3),
        )
        data object V2 : PlayerShipParser(
            "From 2.4.0 until 2.6.3",
            Arb.choose(
                3 to Arb.version(major = 2, minor = 6, patchRange = 0..2),
                997 to Arb.version(major = 2, minorRange = 4..5),
            ),
        )
        data object V3 : PlayerShipParser(
            "From 2.6.3 until 2.7.0",
            Arb.version(major = 2, minor = 6, patchArb = Arb.int(min = 3)),
        )
        data object V4 : PlayerShipParser(
            "Since 2.7.0",
            Arb.version(major = 2, minorArb = Arb.int(min = 7)),
        ) {
            override val dataGenerator: Gen<Data> = Arb.bind(
                ID,
                versionArb,
                PLAYER_FLAGS_1,
                PLAYER_FLAGS_2,
                Arb.flags(UNK_3_1, UNK_3_2, UNK_3_3_NEW, NAME, FRONT, FRONT_MAX, REAR, REAR_MAX),
                PLAYER_FLAGS_4,
                PLAYER_FLAGS_5,
                PLAYER_FLAGS_6,
                Data::New,
            )
        }
    }

    data object UpgradesParser : ObjectParserTestConfig(true) {
        class Data internal constructor(
            objectID: Int,
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
        ) : ObjectParserData.Real<ArtemisPlayer>(
            objectID,
            ArtemisPlayer::class,
            ObjectType.UPGRADES,
        ) {
            override val version: Version = Version.LATEST

            private val activeFlag: Flag<Byte> = a2.flag1
            private val countFlag: Flag<Byte> = c2.flag5
            private val timeFlag: Flag<Short> = t2.flag1

            override fun Sink.buildObject() {
                arrayOf(a1, a2, a3, ac, c2, c3, c4, t1, t2, t3, t4).forEach {
                    writeByte(it.byteValue)
                }

                arrayOf(a1, a2, a3, ac, c2, c3, c4).forEach {
                    writeByteFlags(
                        it.flag1,
                        it.flag2,
                        it.flag3,
                        it.flag4,
                        it.flag5,
                        it.flag6,
                        it.flag7,
                        it.flag8,
                    )
                }

                arrayOf(t1, t2, t3).forEach {
                    writeShortFlags(
                        it.flag1,
                        it.flag2,
                        it.flag3,
                        it.flag4,
                        it.flag5,
                        it.flag6,
                        it.flag7,
                        it.flag8,
                    )
                }

                writeShortFlags(t4.flag1, t4.flag2, t4.flag3, t4.flag4)
            }

            override fun validateObject(obj: ArtemisPlayer) {
                obj.hasPosition.shouldBeFalse()

                obj.hasUpgradeData shouldBeEqual arrayOf(
                    activeFlag,
                    countFlag,
                    timeFlag,
                ).any { flag -> flag.enabled }

                if (activeFlag.enabled) {
                    obj.doubleAgentActive shouldContainValue (activeFlag.value != 0.toByte())
                } else {
                    obj.doubleAgentActive.shouldBeUnspecified()
                }

                if (countFlag.enabled) {
                    obj.doubleAgentCount shouldContainValue countFlag.value
                } else {
                    obj.doubleAgentCount.shouldBeUnspecified()
                }

                if (timeFlag.enabled) {
                    obj.doubleAgentSecondsLeft shouldContainValue timeFlag.value.toInt()
                } else {
                    obj.doubleAgentSecondsLeft.shouldBeUnspecified()
                }
            }
        }

        private val ACTIVE = Arb.byte()
        private val COUNT = Arb.byte().filter { it.toInt() != -1 }
        private val TIME = Arb.short().filter { it.toInt() != -1 }

        override val parserName: String = "Player ship upgrades"
        override val dataGenerator: Gen<Data> = Arb.bind(
            ID,
            allFlags(ACTIVE),
            allFlags(ACTIVE),
            allFlags(ACTIVE),
            Arb.flags(ACTIVE, ACTIVE, ACTIVE, ACTIVE, COUNT, COUNT, COUNT, COUNT),
            allFlags(COUNT),
            allFlags(COUNT),
            allFlags(COUNT),
            allFlags(TIME),
            allFlags(TIME),
            allFlags(TIME),
            Arb.flags(TIME, TIME, TIME, TIME),
            ::Data,
        )

        private fun <T> allFlags(arb: Arb<T>): Arb<FlagByte<T, T, T, T, T, T, T, T>> =
            Arb.flags(arb, arb, arb, arb, arb, arb, arb, arb)
    }

    sealed class WeaponsParser(override val specName: String) : ObjectParserTestConfig(true) {
        protected companion object {
            val COUNT = Arb.byte().filter { it.toInt() != -1 }
            val UNKNOWN = Arb.byte()
            val TIME = Arb.numericFloat()
            val STATUS = Arb.enum<TubeState>()
        }

        abstract class WeaponsData internal constructor(
            objectID: Int,
            override val version: Version,
        ) : ObjectParserData.Real<ArtemisPlayer>(
            objectID,
            ArtemisPlayer::class,
            ObjectType.WEAPONS_CONSOLE,
        ) {
            internal abstract val countFlags: Array<Flag<Byte>>
            internal abstract val unknownFlag: Flag<Byte>
            internal abstract val timeFlags: Array<Flag<Float>>
            internal abstract val statusFlags: Array<Flag<TubeState>>
            internal abstract val typeFlags: Array<Flag<OrdnanceType>>

            internal abstract val allFlagBytes: Array<AnyFlagByte>

            override fun Sink.buildObject() {
                allFlagBytes.forEach {
                    writeByte(it.byteValue)
                }

                writeByteFlags(*countFlags)
                writeByteFlags(unknownFlag)
                writeFloatFlags(*timeFlags)
                writeEnumFlags(*statusFlags)
                writeEnumFlags(*typeFlags)
            }

            override fun validateObject(obj: ArtemisPlayer) {
                obj.hasPosition.shouldBeFalse()

                obj.hasWeaponsData shouldBeEqual arrayOf(
                    countFlags,
                    statusFlags,
                ).any { flags -> flags.any { flag -> flag.enabled } }

                countFlags.zip(obj.ordnanceCounts).forEach { (flag, property) ->
                    if (flag.enabled) {
                        property shouldContainValue flag.value
                    } else {
                        property.shouldBeUnspecified()
                    }
                }

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

        override val parserName: String = "Player ship weapons"

        data object V1 : WeaponsParser("Before 2.6.3") {
            class Data internal constructor(
                objectID: Int,
                version: Version,
                flags1: WeaponsV1Flags1,
                flags2: WeaponsV1Flags2,
                flags3: WeaponsV1Flags3,
            ) : WeaponsData(objectID, version) {
                override val countFlags: Array<Flag<Byte>> = arrayOf(
                    flags1.flag1,
                    flags1.flag2,
                    flags1.flag3,
                    flags1.flag4,
                    flags1.flag5,
                )

                override val unknownFlag: Flag<Byte> = flags1.flag6

                override val timeFlags: Array<Flag<Float>> = arrayOf(
                    flags1.flag7,
                    flags1.flag8,
                    flags2.flag1,
                    flags2.flag2,
                    flags2.flag3,
                    flags2.flag4,
                )

                override val statusFlags: Array<Flag<TubeState>> = arrayOf(
                    flags2.flag5,
                    flags2.flag6,
                    flags2.flag7,
                    flags2.flag8,
                    flags3.flag1,
                    flags3.flag2,
                )

                override val typeFlags: Array<Flag<OrdnanceType>> = arrayOf(
                    flags3.flag3,
                    flags3.flag4,
                    flags3.flag5,
                    flags3.flag6,
                    flags3.flag7,
                    flags3.flag8,
                )

                override val allFlagBytes: Array<AnyFlagByte> = arrayOf(
                    flags1,
                    flags2,
                    flags3,
                    FlagByte(dummy, dummy, dummy, dummy, dummy, dummy, dummy, dummy),
                )
            }

            private val typeArb = Arb.enum<OrdnanceType>().filter { it < OrdnanceType.BEACON }

            override val dataGenerator: Gen<Data> = Arb.bind(
                ID,
                Arb.choose(
                    3 to Arb.version(major = 2, minor = 6, patchRange = 0..2),
                    997 to Arb.version(major = 2, minorRange = 3..5),
                ),
                Arb.flags(COUNT, COUNT, COUNT, COUNT, COUNT, UNKNOWN, TIME, TIME),
                Arb.flags(TIME, TIME, TIME, TIME, STATUS, STATUS, STATUS, STATUS),
                Arb.flags(STATUS, STATUS, typeArb, typeArb, typeArb, typeArb, typeArb, typeArb),
                ::Data,
            )
        }

        data object V2 : WeaponsParser("Since 2.6.3") {
            class Data internal constructor(
                objectID: Int,
                version: Version,
                flags1: WeaponsV2Flags1,
                flags2: WeaponsV2Flags2,
                flags3: WeaponsV2Flags3,
                flags4: WeaponsV2Flags4,
            ) : WeaponsData(objectID, version) {
                override val countFlags: Array<Flag<Byte>> = arrayOf(
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

                override val timeFlags: Array<Flag<Float>> = arrayOf(
                    flags2.flag1,
                    flags2.flag2,
                    flags2.flag3,
                    flags2.flag4,
                    flags2.flag5,
                    flags2.flag6,
                )

                override val statusFlags: Array<Flag<TubeState>> = arrayOf(
                    flags2.flag7,
                    flags2.flag8,
                    flags3.flag1,
                    flags3.flag2,
                    flags3.flag3,
                    flags3.flag4,
                )

                override val typeFlags: Array<Flag<OrdnanceType>> = arrayOf(
                    flags3.flag5,
                    flags3.flag6,
                    flags3.flag7,
                    flags3.flag8,
                    flags4.flag1,
                    flags4.flag2,
                )

                override val allFlagBytes: Array<AnyFlagByte> = arrayOf(
                    flags1,
                    flags2,
                    flags3,
                    flags4,
                )
            }

            private val typeArb = Arb.enum<OrdnanceType>()

            override val dataGenerator: Gen<Data> = Arb.bind(
                ID,
                Arb.choose(
                    1 to
                        Arb.version(major = 2, minor = 6, patchArb = Arb.int(min = 3)),
                    PropertyTesting.defaultIterationCount - 1 to
                        Arb.version(major = 2, minorArb = Arb.int(min = 7)),
                ),
                Arb.flags(COUNT, COUNT, COUNT, COUNT, COUNT, COUNT, COUNT, COUNT),
                Arb.flags(TIME, TIME, TIME, TIME, TIME, TIME, STATUS, STATUS),
                Arb.flags(STATUS, STATUS, STATUS, STATUS, typeArb, typeArb, typeArb, typeArb),
                Arb.flags(typeArb, typeArb),
                ::Data,
            )
        }

        override suspend fun describeMore(scope: DescribeSpecContainerScope) {
            scope.describe("Bits") {
                describe("OrdnanceCountBit") {
                    withData(OrdnanceType.entries) { ordnanceType ->
                        OrdnanceCountBit(ordnanceType).ordnanceType shouldBeEqual ordnanceType
                    }
                }

                describe("TubeTimeBit") {
                    withData(nameFn = { "Index: $it" }, 0 until Artemis.MAX_TUBES) { index ->
                        TubeTimeBit(index).index shouldBeEqual index
                    }
                }

                describe("TubeStateBit") {
                    withData(nameFn = { "Index: $it" }, 0 until Artemis.MAX_TUBES) { index ->
                        TubeStateBit(index).index shouldBeEqual index
                    }
                }

                describe("TubeContentsBit") {
                    withData(nameFn = { "Index: $it" }, 0 until Artemis.MAX_TUBES) { index ->
                        TubeContentsBit(index).index shouldBeEqual index
                    }
                }
            }
        }
    }

    sealed class Unobserved : ObjectParserTestConfig(false) {
        data object Engineering : Unobserved() {
            class Data internal constructor(
                objectID: Int,
                private val heatFlags: EngineeringFloatFlags,
                private val enFlags: EngineeringFloatFlags,
                private val coolFlags: EngineeringByteFlags,
            ) : ObjectParserData.Unobserved(objectID, ObjectType.ENGINEERING_CONSOLE) {
                override val version: Version get() = Version.LATEST

                override fun Sink.buildObject() {
                    arrayOf(heatFlags, enFlags, coolFlags).forEach { flags ->
                        writeByte(flags.byteValue)
                    }

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
            override val dataGenerator: Gen<Data> = Arb.bind(
                ID,
                systemFlags(Arb.numericFloat()),
                systemFlags(Arb.numericFloat()),
                systemFlags(Arb.byte()),
                ::Data,
            )

            private fun <T> systemFlags(arb: Arb<T>): Arb<FlagByte<T, T, T, T, T, T, T, T>> =
                Arb.flags(arb, arb, arb, arb, arb, arb, arb, arb)
        }

        sealed class Anomaly(
            override val specName: String,
            versionArb: Arb<Version>,
        ) : Unobserved() {
            class Data internal constructor(
                objectID: Int,
                override val version: Version,
                private val flags: AnomalyFlags,
            ) : ObjectParserData.Unobserved(objectID, ObjectType.ANOMALY) {
                override fun Sink.buildObject() {
                    val beaconVersion = version >= Version.BEACON

                    writeByte(flags.byteValue)
                    if (beaconVersion) writeByte(0)

                    writeFloatFlags(flags.flag1, flags.flag2, flags.flag3)
                    writeIntFlags(flags.flag4, flags.flag5, flags.flag6)

                    if (beaconVersion) {
                        writeByteFlags(flags.flag7, flags.flag8)
                    }
                }
            }

            data object V1 : Anomaly(
                "Before 2.6.3",
                Arb.choose(
                    3 to Arb.version(major = 2, minor = 6, patchRange = 0..2),
                    997 to Arb.version(major = 2, minorRange = 3..5),
                ),
            )

            data object V2 : Anomaly(
                "Since 2.6.3",
                Arb.choose(
                    1 to
                        Arb.version(major = 2, minor = 6, patchArb = Arb.int(min = 3)),
                    PropertyTesting.defaultIterationCount - 1 to
                        Arb.version(major = 2, minorArb = Arb.int(min = 7)),
                ),
            )

            override val parserName: String = "Anomaly"
            override val dataGenerator: Gen<Data> = Arb.bind(
                ID,
                versionArb,
                Arb.flags(
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.int(),
                    Arb.int(),
                    Arb.int(),
                    Arb.byte(),
                    Arb.byte(),
                ),
                ::Data,
            )
        }

        sealed class Nebula(
            override val specName: String,
            versionArb: Arb<Version>,
        ) : Unobserved() {
            class Data internal constructor(
                objectID: Int,
                override val version: Version,
                private val flags: NebulaFlags,
            ) : ObjectParserData.Unobserved(objectID, ObjectType.NEBULA) {
                override fun Sink.buildObject() {
                    writeByte(flags.byteValue)

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

            data object V1 : Nebula(
                "Before 2.7.0",
                Arb.version(major = 2, minorRange = 3..6),
            )

            data object V2 : Nebula(
                "Since 2.7.0",
                Arb.version(major = 2, minorArb = Arb.int(min = 7)),
            )

            override val parserName: String = "Nebula"
            override val dataGenerator: Gen<Data> = Arb.bind(
                ID,
                versionArb,
                Arb.flags(
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.byte(),
                ),
                ::Data,
            )
        }

        data object Torpedo : Unobserved() {
            class Data internal constructor(
                objectID: Int,
                private val flags: TorpedoFlags,
            ) : ObjectParserData.Unobserved(objectID, ObjectType.TORPEDO) {
                override val version: Version get() = Version.LATEST

                override fun Sink.buildObject() {
                    writeByte(flags.byteValue)
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
            override val dataGenerator: Gen<Data> = Arb.bind(
                ID,
                Arb.flags(
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.int(),
                    Arb.int(),
                ),
                ::Data,
            )
        }

        data object Asteroid : Unobserved() {
            class Data internal constructor(
                objectID: Int,
                private val flags: AsteroidFlags,
            ) : ObjectParserData.Unobserved(objectID, ObjectType.ASTEROID) {
                override val version: Version get() = Version.LATEST

                override fun Sink.buildObject() {
                    writeByte(flags.byteValue)

                    writeFloatFlags(flags.flag1, flags.flag2, flags.flag3)
                }
            }

            override val parserName: String = "Asteroid"
            override val dataGenerator: Gen<Data> = Arb.bind(
                ID,
                Arb.flags(
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                ),
                ::Data,
            )
        }

        sealed class GenericMesh(
            override val specName: String,
            versionArb: Arb<Version>,
        ) : Unobserved() {
            class Data internal constructor(
                objectID: Int,
                override val version: Version,
                private val flags1: GenericMeshFlags1,
                private val flags2: GenericMeshFlags2,
                private val flags3: GenericMeshFlags3,
                private val flags4: GenericMeshFlags4,
            ) : ObjectParserData.Unobserved(objectID, ObjectType.GENERIC_MESH) {
                override fun Sink.buildObject() {
                    arrayOf(flags1, flags2, flags3, flags4).forEach {
                        writeByte(it.byteValue)
                    }

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

            data object V1 : GenericMesh(
                "Before 2.7.0",
                Arb.version(major = 2, minorRange = 3..6),
            )

            data object V2 : GenericMesh(
                "Since 2.7.0",
                Arb.version(major = 2, minorArb = Arb.int(min = 7)),
            )

            override val parserName: String = "Generic mesh"
            override val dataGenerator: Gen<Data> = Arb.bind(
                ID,
                versionArb,
                Arb.flags(
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.int(),
                    Arb.int(),
                    Arb.int(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                ),
                Arb.flags(
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.string(),
                    Arb.string(),
                    Arb.string(),
                    Arb.numericFloat(),
                ),
                Arb.flags(
                    Arb.byte(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.byte(),
                ),
                Arb.flags(
                    Arb.string(),
                    Arb.string(),
                    Arb.int(),
                ),
                ::Data,
            )
        }

        data object Drone : Unobserved() {
            class Data internal constructor(
                objectID: Int,
                private val flags1: DroneFlags1,
                private val flags2: DroneFlags2,
            ) : ObjectParserData.Unobserved(objectID, ObjectType.DRONE) {
                override val version: Version get() = Version.LATEST

                override fun Sink.buildObject() {
                    arrayOf(flags1, flags2).forEach {
                        writeByte(it.byteValue)
                    }

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
            override val dataGenerator: Gen<Data> = Arb.bind(
                ID,
                Arb.flags(
                    Arb.int(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.int(),
                ),
                Arb.flags(Arb.numericFloat()),
                ::Data,
            )
        }
    }

    abstract val dataGenerator: Gen<PacketTestData.Server<ObjectUpdatePacket>>
    abstract val parserName: String
    open val specName: String get() = toString()

    open suspend fun describeMore(scope: DescribeSpecContainerScope) { }

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

        fun buildObject(block: Sink.() -> Unit): Source = buildPacket {
            block()
            writeIntLe(0)
        }

        fun testHasPosition(obj: ArtemisObject<*>, xFlag: Flag<Float>, zFlag: Flag<Float>) {
            obj.hasPosition.shouldBeEqual(xFlag.enabled && zFlag.enabled)
        }
    }
}
