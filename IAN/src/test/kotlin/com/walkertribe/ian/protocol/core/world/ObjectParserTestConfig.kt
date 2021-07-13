package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.AlertStatus
import com.walkertribe.ian.enums.DriveType
import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.enums.TubeState
import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.iface.ParseResult
import com.walkertribe.ian.iface.TestListener
import com.walkertribe.ian.protocol.PacketTestProtocol
import com.walkertribe.ian.protocol.core.PacketTestSpec.Companion.prepareMockPacket
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.util.Version
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
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.boolean
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.choose
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.nonNegativeInt
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.short
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.BytePacketBuilder
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.writeFloatLittleEndian
import io.ktor.utils.io.core.writeIntLittleEndian
import io.ktor.utils.io.core.writeShort
import io.ktor.utils.io.core.writeShortLittleEndian
import io.ktor.utils.io.core.writeText
import io.mockk.mockk

sealed class ObjectParserTestConfig {
    protected data class Flag<T>(val enabled: Boolean, val value: T) {
        constructor(data: Pair<Boolean, T>) : this(data.first, data.second)
    }

    protected data class FlagByte<T1, T2, T3, T4, T5, T6, T7, T8>(
        val flag1: Flag<T1>,
        val flag2: Flag<T2>,
        val flag3: Flag<T3>,
        val flag4: Flag<T4>,
        val flag5: Flag<T5>,
        val flag6: Flag<T6>,
        val flag7: Flag<T7>,
        val flag8: Flag<T8>,
    ) {
        val byteValue: Byte by lazy {
            var value = 0

            arrayOf(flag1, flag2, flag3, flag4, flag5, flag6, flag7, flag8)
                .forEachIndexed { index, flag ->
                    if (flag.enabled) {
                        value += 1 shl index
                    }
                }

            value.toByte()
        }
    }

    data object Empty : ObjectParserTestConfig() {
        override val shouldYieldObject: Boolean = false

        override val objectPacketGen: Gen<ByteReadPacket> = versionArb.map {
            version = it
            buildObject { }
        }
    }

    data object BaseParser : ObjectParserTestConfig() {
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

        private lateinit var nameFlag: Flag<String>
        private lateinit var shieldsFlag: Flag<Float>
        private lateinit var maxShieldsFlag: Flag<Float>
        private lateinit var hullIdFlag: Flag<Int>
        private lateinit var xFlag: Flag<Float>
        private lateinit var yFlag: Flag<Float>
        private lateinit var zFlag: Flag<Float>

        override val objectPacketGen: Gen<ByteReadPacket> = Arb.bind(
            ID,
            versionArb,
            Arb.flags(NAME, SHIELDS, MAX_SHIELDS, UNK_1_4, HULL_ID, X, Y, Z),
            Arb.flags(UNK_2_1, UNK_2_2, UNK_2_3, UNK_2_4, UNK_2_5, UNK_2_6),
        ) { id, ver, flags1, flags2 ->
            objectID = id
            version = ver

            nameFlag = flags1.flag1
            shieldsFlag = flags1.flag2
            maxShieldsFlag = flags1.flag3
            hullIdFlag = flags1.flag5
            xFlag = flags1.flag6
            yFlag = flags1.flag7
            zFlag = flags1.flag8

            buildObject {
                writeByte(ObjectType.BASE.id)
                writeIntLittleEndian(id)
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
        }

        override val shouldYieldObject: Boolean = true

        override fun test(obj: ArtemisObject) {
            obj.shouldBeInstanceOf<ArtemisBase>()
            obj.id shouldBeEqual objectID

            testHasPosition(obj, xFlag, zFlag)

            if (nameFlag.enabled) {
                obj.name.shouldBeSpecified()

                val name = obj.name.value
                name.shouldNotBeNull()
                name.toString() shouldBeEqual nameFlag.value

                val nameString = obj.nameString
                nameString.shouldNotBeNull()
                nameString shouldBeEqual nameFlag.value
            } else {
                obj.name.shouldBeUnspecified()
                obj.nameString.shouldBeNull()
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

    data object BlackHoleParser : ObjectParserTestConfig() {
        private lateinit var xFlag: Flag<Float>
        private lateinit var yFlag: Flag<Float>
        private lateinit var zFlag: Flag<Float>

        override val objectPacketGen: Gen<ByteReadPacket> = Arb.bind(
            ID,
            versionArb,
            Arb.flags(X, Y, Z),
        ) { id, ver, flags ->
            objectID = id
            version = ver
            xFlag = flags.flag1
            yFlag = flags.flag2
            zFlag = flags.flag3

            buildObject {
                writeByte(ObjectType.BLACK_HOLE.id)
                writeIntLittleEndian(id)
                writeByte(flags.byteValue)

                writeFloatFlags(xFlag, yFlag, zFlag)
            }
        }

        override val shouldYieldObject: Boolean = true

        override fun test(obj: ArtemisObject) {
            obj.shouldBeInstanceOf<ArtemisBlackHole>()
            obj.id shouldBeEqual objectID

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

    data object CreatureParser : ObjectParserTestConfig() {
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

        private lateinit var xFlag: Flag<Float>
        private lateinit var yFlag: Flag<Float>
        private lateinit var zFlag: Flag<Float>
        private lateinit var creatureTypeFlag: Flag<Int>

        private var creatureTypeArb: Arb<Int> = Arb.of(0)
        private var forceCreatureType: Boolean = false

        override val objectPacketGen: Gen<ByteReadPacket> get() = Arb.bind(
            ID,
            versionArb,
            Arb.flags(X, Y, Z, UNK_1_4, UNK_1_5, UNK_1_6, UNK_1_7, creatureTypeArb),
            Arb.flags(UNK_2_1, UNK_2_2, UNK_2_3, UNK_2_4, UNK_2_5, UNK_2_6, UNK_2_7, UNK_2_8),
            Arb.flags(UNK_3_1, UNK_3_2),
        ) { id, ver, flags1, flags2, flags3 ->
            objectID = id
            version = ver

            val (flag11, flag12, flag13, flag14, flag15, flag16, flag17, flag18) = flags1
            val (flag21, flag22, flag23, flag24, flag25, flag26, flag27, flag28) = flags2
            val (flag31, flag32) = flags3

            xFlag = flag11
            yFlag = flag12
            zFlag = flag13
            creatureTypeFlag = flag18

            buildObject {
                writeByte(ObjectType.CREATURE.id)
                writeIntLittleEndian(id)

                var flagByte1 = flags1.byteValue.toInt()
                if (forceCreatureType) flagByte1 = flagByte1 or 0x80
                writeByte(flagByte1.toByte())

                arrayOf(flags2, flags3).forEach {
                    writeByte(it.byteValue)
                }

                writeFloatFlags(xFlag, yFlag, zFlag)
                writeStringFlags(flag14)
                writeFloatFlags(flag15, flag16, flag17)

                if (flag18.enabled || forceCreatureType) {
                    writeIntLittleEndian(flag18.value)
                }

                writeIntFlags(flag21, flag22, flag23, flag24, flag25, flag26)
                writeFloatFlags(flag27, flag28)

                if (ver >= Version.BEACON) {
                    writeByteFlags(flag31)

                    if (ver >= Version.NEBULA_TYPES) {
                        writeIntFlags(flag32)
                    }
                }
            }
        }

        override val shouldYieldObject: Boolean get() = !forceCreatureType

        override fun test(obj: ArtemisObject) {
            obj.shouldBeInstanceOf<ArtemisCreature>()
            obj.id shouldBeEqual objectID

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

        override suspend fun split(
            baseSpec: DescribeSpecContainerScope,
            describeTests: suspend DescribeSpecContainerScope.() -> Unit,
        ) {
            val readChannel = mockk<ByteReadChannel>()
            val reader = PacketReader(
                readChannel,
                PacketTestProtocol<ObjectUpdatePacket>(),
                TestListener.registry,
            )

            arrayOf(
                "Before 2.6.0" to Arb.bind(
                    Arb.int(3..5),
                    Arb.nonNegativeInt(),
                ) { minor, patch -> Version(2, minor, patch) },
                "Since 2.6.0" to Arb.bind(
                    Arb.int(min = 6),
                    Arb.nonNegativeInt(),
                ) { minor, patch -> Version(2, minor, patch) },
            ).forEach { (specName, arb) ->
                baseSpec.describe(specName) {
                    versionArb = arb
                    forceCreatureType = false
                    creatureTypeArb = Arb.of(0)

                    describeTests()

                    it("Rejects non-typhons") {
                        forceCreatureType = true
                        creatureTypeArb = Arb.int().filter { it != 0 }

                        objectPacketGen.checkAll {
                            readChannel.prepareMockPacket(
                                it,
                                TestPacketTypes.OBJECT_BIT_STREAM,
                            )

                            reader.version = version

                            val result = reader.readPacket()
                            result.shouldBeInstanceOf<ParseResult.Success>()

                            val packet = result.packet
                            packet.shouldBeInstanceOf<ObjectUpdatePacket>()
                            packet.objects.shouldBeEmpty()

                            reader.isAcceptingCurrentObject.shouldBeFalse()
                        }
                    }
                }
            }
        }
    }

    data object MineParser : ObjectParserTestConfig() {
        private lateinit var xFlag: Flag<Float>
        private lateinit var yFlag: Flag<Float>
        private lateinit var zFlag: Flag<Float>

        override val objectPacketGen: Gen<ByteReadPacket> = Arb.bind(
            ID,
            versionArb,
            Arb.flags(X, Y, Z),
        ) { id, ver, flags ->
            objectID = id
            version = ver
            xFlag = flags.flag1
            yFlag = flags.flag2
            zFlag = flags.flag3

            buildObject {
                writeByte(ObjectType.MINE.id)
                writeIntLittleEndian(id)
                writeByte(flags.byteValue)

                writeFloatFlags(xFlag, yFlag, zFlag)
            }
        }

        override val shouldYieldObject: Boolean = true

        override fun test(obj: ArtemisObject) {
            obj.shouldBeInstanceOf<ArtemisMine>()
            obj.id shouldBeEqual objectID

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

    data object NpcShipParser : ObjectParserTestConfig() {
        private val NAME = Arb.string()
        private val IMPULSE = Arb.numericFloat()
        private val IS_ENEMY = Arb.int()
        private val HULL_ID = Arb.int().filter { it != -1 }
        private val IN_NEBULA_OLD = Arb.short()
        private val IN_NEBULA_NEW = Arb.byte()
        private val FRONT = Arb.numericFloat()
        private val FRONT_MAX = Arb.numericFloat()
        private val REAR = Arb.numericFloat()
        private val REAR_MAX = Arb.numericFloat()
        private val SCAN_BITS = Arb.int()
        private val SIDE = Arb.byte().filter { it.toInt() != -1 }

        private val DAMAGE = Arb.float()
        private val FREQ = Arb.float()

        private val UNK_1_3 = Arb.float()
        private val UNK_1_4 = Arb.float()
        private val UNK_1_5 = Arb.float()

        private val UNK_2_3 = Arb.float()
        private val UNK_2_4 = Arb.float()
        private val UNK_2_5 = Arb.float()
        private val UNK_2_6 = Arb.float()
        private val UNK_2_7 = Arb.byte()

        private val UNK_3_5 = Arb.short()
        private val UNK_3_6 = Arb.byte()
        private val UNK_3_7 = Arb.int()
        private val UNK_3_8 = Arb.int()

        private val UNK_4_2 = Arb.int()
        private val UNK_4_3 = Arb.int()
        private val UNK_4_5 = Arb.byte()
        private val UNK_4_6 = Arb.byte()
        private val UNK_4_7 = Arb.byte()
        private val UNK_4_8 = Arb.float()

        private val UNK_5_1 = Arb.float()
        private val UNK_5_2 = Arb.float()
        private val UNK_5_3 = Arb.byte()
        private val UNK_5_4 = Arb.byte()

        private lateinit var nameFlag: Flag<String>
        private lateinit var shieldsFrontFlag: Flag<Float>
        private lateinit var maxShieldsFrontFlag: Flag<Float>
        private lateinit var shieldsRearFlag: Flag<Float>
        private lateinit var maxShieldsRearFlag: Flag<Float>
        private lateinit var hullIdFlag: Flag<Int>
        private lateinit var impulseFlag: Flag<Float>
        private lateinit var isEnemyFlag: Flag<Int>
        private lateinit var inNebulaFlag: Flag<Int>
        private lateinit var sideFlag: Flag<Byte>
        private lateinit var scanBitsFlag: Flag<Int>
        private lateinit var xFlag: Flag<Float>
        private lateinit var yFlag: Flag<Float>
        private lateinit var zFlag: Flag<Float>

        private sealed interface TestSpec {
            val specName: String
            val objectPacketGen: Gen<ByteReadPacket>

            data object V1 : TestSpec {
                override val specName: String = "Before 2.6.3"

                override val objectPacketGen: Gen<ByteReadPacket> = Arb.bind(
                    ID,
                    Arb.choose(
                        3 to Arb.int(0..2).map { Version(2, 6, it) },
                        997 to Arb.bind(
                            Arb.int(3..5),
                            Arb.nonNegativeInt(),
                        ) { minor, patch -> Version(2, minor, patch) },
                    ),
                    Arb.flags(NAME, IMPULSE, UNK_1_3, UNK_1_4, UNK_1_5, IS_ENEMY, HULL_ID, X),
                    Arb.flags(Y, Z, UNK_2_3, UNK_2_4, UNK_2_5, UNK_2_6, UNK_2_7, IN_NEBULA_OLD),
                    Arb.flags(FRONT, FRONT_MAX, REAR, REAR_MAX, UNK_3_5, UNK_3_6, UNK_3_7, UNK_3_8),
                    Arb.flags(SCAN_BITS, UNK_4_2, UNK_4_3, SIDE, UNK_4_5, UNK_4_6, UNK_4_7, UNK_4_8),
                    Arb.flags(UNK_5_1, UNK_5_2, DAMAGE, DAMAGE, DAMAGE, DAMAGE, DAMAGE, DAMAGE),
                    Arb.flags(DAMAGE, DAMAGE, FREQ, FREQ, FREQ, FREQ, FREQ),
                ) { id, ver, flags1, flags2, flags3, flags4, flags5, flags6 ->
                    objectID = id
                    version = ver

                    nameFlag = flags1.flag1
                    impulseFlag = flags1.flag2
                    isEnemyFlag = flags1.flag6
                    hullIdFlag = flags1.flag7
                    xFlag = flags1.flag8
                    yFlag = flags2.flag1
                    zFlag = flags2.flag2
                    inNebulaFlag = Flag(flags2.flag8.enabled, flags2.flag8.value.toInt())
                    shieldsFrontFlag = flags3.flag1
                    maxShieldsFrontFlag = flags3.flag2
                    shieldsRearFlag = flags3.flag3
                    maxShieldsRearFlag = flags3.flag4
                    scanBitsFlag = flags4.flag1
                    sideFlag = flags4.flag4

                    buildObject {
                        writeByte(ObjectType.NPC_SHIP.id)
                        writeIntLittleEndian(objectID)

                        arrayOf(flags1, flags2, flags3, flags4, flags5, flags6).forEach {
                            writeByte(it.byteValue)
                        }

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
                        writeByteFlags(flags2.flag7)
                        writeShortFlags(flags2.flag8)
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
                        writeFloatFlags(
                            flags4.flag8,
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
            }

            data object V2 : TestSpec {
                override val specName: String = "From 2.6.3 until 2.7.0"

                override val objectPacketGen: Gen<ByteReadPacket> = Arb.bind(
                    ID,
                    Arb.int(min = 3).map { Version(2, 6, it) },
                    Arb.flags(NAME, IMPULSE, UNK_1_3, UNK_1_4, UNK_1_5, IS_ENEMY, HULL_ID, X),
                    Arb.flags(Y, Z, UNK_2_3, UNK_2_4, UNK_2_5, UNK_2_6, UNK_2_7, IN_NEBULA_OLD),
                    Arb.flags(FRONT, FRONT_MAX, REAR, REAR_MAX, UNK_3_5, UNK_3_6, UNK_3_7, UNK_3_8),
                    Arb.flags(SCAN_BITS, UNK_4_2, UNK_4_3, SIDE, UNK_4_5, UNK_4_6, UNK_4_7, UNK_4_8),
                    Arb.flags(UNK_5_1, UNK_5_2, UNK_5_3, UNK_5_4, DAMAGE, DAMAGE, DAMAGE, DAMAGE),
                    Arb.flags(DAMAGE, DAMAGE, DAMAGE, DAMAGE, FREQ, FREQ, FREQ, FREQ),
                    Arb.flags(FREQ),
                ) { id, ver, flags1, flags2, flags3, flags4, flags5, flags6, flags7 ->
                    objectID = id
                    version = ver

                    nameFlag = flags1.flag1
                    impulseFlag = flags1.flag2
                    isEnemyFlag = flags1.flag6
                    hullIdFlag = flags1.flag7
                    xFlag = flags1.flag8
                    yFlag = flags2.flag1
                    zFlag = flags2.flag2
                    inNebulaFlag = Flag(flags2.flag8.enabled, flags2.flag8.value.toInt())
                    shieldsFrontFlag = flags3.flag1
                    maxShieldsFrontFlag = flags3.flag2
                    shieldsRearFlag = flags3.flag3
                    maxShieldsRearFlag = flags3.flag4
                    scanBitsFlag = flags4.flag1
                    sideFlag = flags4.flag4

                    buildObject {
                        writeByte(ObjectType.NPC_SHIP.id)
                        writeIntLittleEndian(objectID)

                        arrayOf(flags1, flags2, flags3, flags4, flags5, flags6, flags7).forEach {
                            writeByte(it.byteValue)
                        }

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
                        writeByteFlags(flags2.flag7)
                        writeShortFlags(flags2.flag8)
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
                        writeFloatFlags(
                            flags4.flag8,
                            flags5.flag1,
                            flags5.flag2,
                        )
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
            }

            data object V3 : TestSpec {
                override val specName: String = "Since 2.7.0"

                override val objectPacketGen: Gen<ByteReadPacket> = Arb.bind(
                    ID,
                    Arb.bind(
                        Arb.int(min = 7),
                        Arb.nonNegativeInt(),
                    ) { minor, patch -> Version(2, minor, patch) },
                    Arb.flags(NAME, IMPULSE, UNK_1_3, UNK_1_4, UNK_1_5, IS_ENEMY, HULL_ID, X),
                    Arb.flags(Y, Z, UNK_2_3, UNK_2_4, UNK_2_5, UNK_2_6, UNK_2_7, IN_NEBULA_NEW),
                    Arb.flags(FRONT, FRONT_MAX, REAR, REAR_MAX, UNK_3_5, UNK_3_6, UNK_3_7, UNK_3_8),
                    Arb.flags(SCAN_BITS, UNK_4_2, UNK_4_3, SIDE, UNK_4_5, UNK_4_6, UNK_4_7, UNK_4_8),
                    Arb.flags(UNK_5_1, UNK_5_2, UNK_5_3, UNK_5_4, DAMAGE, DAMAGE, DAMAGE, DAMAGE),
                    Arb.flags(DAMAGE, DAMAGE, DAMAGE, DAMAGE, FREQ, FREQ, FREQ, FREQ),
                    Arb.flags(FREQ),
                ) { id, ver, flags1, flags2, flags3, flags4, flags5, flags6, flags7 ->
                    objectID = id
                    version = ver

                    nameFlag = flags1.flag1
                    impulseFlag = flags1.flag2
                    isEnemyFlag = flags1.flag6
                    hullIdFlag = flags1.flag7
                    xFlag = flags1.flag8
                    yFlag = flags2.flag1
                    zFlag = flags2.flag2
                    inNebulaFlag = Flag(flags2.flag8.enabled, flags2.flag8.value.toInt())
                    shieldsFrontFlag = flags3.flag1
                    maxShieldsFrontFlag = flags3.flag2
                    shieldsRearFlag = flags3.flag3
                    maxShieldsRearFlag = flags3.flag4
                    scanBitsFlag = flags4.flag1
                    sideFlag = flags4.flag4

                    buildObject {
                        writeByte(ObjectType.NPC_SHIP.id)
                        writeIntLittleEndian(objectID)

                        arrayOf(flags1, flags2, flags3, flags4, flags5, flags6, flags7).forEach {
                            writeByte(it.byteValue)
                        }

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
                        writeByteFlags(flags2.flag7, flags2.flag8)
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
                        writeFloatFlags(
                            flags4.flag8,
                            flags5.flag1,
                            flags5.flag2,
                        )
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
            }
        }

        private var testSpec: TestSpec = TestSpec.V3
        override val objectPacketGen: Gen<ByteReadPacket> get() = testSpec.objectPacketGen

        override val shouldYieldObject: Boolean = true

        override fun test(obj: ArtemisObject) {
            obj.shouldBeInstanceOf<ArtemisNpc>()
            obj.id shouldBeEqual objectID

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
                inNebulaFlag to obj.isInNebula,
            ).forEach { (flag, property) ->
                if (flag.enabled) {
                    property.shouldContainValue(flag.value != 0)
                } else {
                    property.shouldBeUnspecified()
                }
            }

            if (nameFlag.enabled) {
                obj.name.shouldBeSpecified()

                val name = obj.name.value
                name.shouldNotBeNull()
                name.toString() shouldBeEqual nameFlag.value

                val nameString = obj.nameString
                nameString.shouldNotBeNull()
                nameString shouldBeEqual nameFlag.value
            } else {
                obj.name.shouldBeUnspecified()
                obj.nameString.shouldBeNull()
            }
        }

        override suspend fun split(
            baseSpec: DescribeSpecContainerScope,
            describeTests: suspend DescribeSpecContainerScope.() -> Unit
        ) {
            arrayOf(TestSpec.V1, TestSpec.V2, TestSpec.V3).forEach {
                baseSpec.describe(it.specName) {
                    testSpec = it

                    describeTests()
                }
            }
        }
    }

    data object PlayerShipParser : ObjectParserTestConfig() {
        private val IMPULSE = Arb.numericFloat()
        private val WARP = Arb.byte(min = 0, max = Artemis.MAX_WARP)
        private val HULL_ID = Arb.int().filter { it != -1 }
        private val NAME = Arb.string()
        private val FRONT = Arb.numericFloat()
        private val FRONT_MAX = Arb.numericFloat()
        private val REAR = Arb.numericFloat()
        private val REAR_MAX = Arb.numericFloat()
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

        private val UNK_3_1 = Arb.float()
        private val UNK_3_2 = Arb.float()
        private val UNK_3_3_OLD = Arb.short()
        private val UNK_3_3_NEW = Arb.byte()

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

        private lateinit var impulseFlag: Flag<Float>
        private lateinit var warpFlag: Flag<Byte>
        private lateinit var hullIdFlag: Flag<Int>
        private lateinit var xFlag: Flag<Float>
        private lateinit var yFlag: Flag<Float>
        private lateinit var zFlag: Flag<Float>
        private lateinit var nameFlag: Flag<String>
        private lateinit var shieldsFrontFlag: Flag<Float>
        private lateinit var maxShieldsFrontFlag: Flag<Float>
        private lateinit var shieldsRearFlag: Flag<Float>
        private lateinit var maxShieldsRearFlag: Flag<Float>
        private lateinit var dockingBaseFlag: Flag<Int>
        private lateinit var alertStatusFlag: Flag<AlertStatus>
        private lateinit var driveTypeFlag: Flag<DriveType>
        private lateinit var sideFlag: Flag<Byte>
        private lateinit var shipIndexFlag: Flag<Byte>
        private lateinit var capitalShipFlag: Flag<Int>

        override val objectPacketGen: Gen<ByteReadPacket> get() = Arb.bind(
            ID,
            versionArb,
            Arb.flags(UNK_1_1, IMPULSE, UNK_1_3, UNK_1_4, UNK_1_5, UNK_1_6, WARP, UNK_1_8),
            Arb.flags(UNK_2_1, UNK_2_2, HULL_ID, X, Y, Z, UNK_2_7, UNK_2_8),
            Arb.flags(UNK_3_1, UNK_3_2, UNK_3_3_NEW, NAME, FRONT, FRONT_MAX, REAR, REAR_MAX),
            Arb.flags(DOCKING_BASE, ALERT, UNK_4_3, UNK_4_4, UNK_4_5, UNK_4_6, UNK_4_7, UNK_4_8),
            Arb.flags(DRIVE_TYPE, UNK_5_2, UNK_5_3, UNK_5_4, UNK_5_5, SIDE, UNK_5_7, SHIP_INDEX),
            Arb.flags(CAPITAL_SHIP_ID, UNK_6_2, UNK_6_3, UNK_6_4, UNK_6_5),
            Arb.flag(UNK_3_3_OLD),
        ) { id, ver, flags1, flags2, f3, flags4, flags5, flags6, flag33Old ->
            objectID = id
            version = ver

            val flags3 = if (ver < Version.NEBULA_TYPES) {
                FlagByte(
                    f3.flag1,
                    f3.flag2,
                    flag33Old,
                    f3.flag4,
                    f3.flag5,
                    f3.flag6,
                    f3.flag7,
                    f3.flag8,
                )
            } else {
                f3
            }

            impulseFlag = flags1.flag2
            warpFlag = flags1.flag7
            hullIdFlag = flags2.flag3
            xFlag = flags2.flag4
            yFlag = flags2.flag5
            zFlag = flags2.flag6
            nameFlag = flags3.flag4
            shieldsFrontFlag = flags3.flag5
            maxShieldsFrontFlag = flags3.flag6
            shieldsRearFlag = flags3.flag7
            maxShieldsRearFlag = flags3.flag8
            dockingBaseFlag = flags4.flag1
            alertStatusFlag = flags4.flag2
            driveTypeFlag = flags5.flag1
            sideFlag = flags5.flag6
            shipIndexFlag = flags5.flag8
            capitalShipFlag = flags6.flag1

            buildObject {
                writeByte(ObjectType.PLAYER_SHIP.id)
                writeIntLittleEndian(objectID)

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
                if (version < Version.NEBULA_TYPES) {
                    writeShortFlags(flag33Old)
                } else {
                    writeByteFlags(f3.flag3)
                }
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

                if (ver >= Version.ACCENT_COLOR) {
                    writeFloatFlags(flags6.flag2, flags6.flag3)

                    if (ver >= Version.BEACON) {
                        writeByteFlags(flags6.flag4, flags6.flag5)
                    }
                }
            }
        }

        override val shouldYieldObject: Boolean = true

        override fun test(obj: ArtemisObject) {
            obj.shouldBeInstanceOf<ArtemisPlayer>()
            obj.id shouldBeEqual objectID

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

                val name = obj.name.value
                name.shouldNotBeNull()
                name.toString() shouldBeEqual nameFlag.value

                val nameString = obj.nameString
                nameString.shouldNotBeNull()
                nameString shouldBeEqual nameFlag.value
            } else {
                obj.name.shouldBeUnspecified()
                obj.nameString.shouldBeNull()
            }
        }

        override suspend fun split(
            baseSpec: DescribeSpecContainerScope,
            describeTests: suspend DescribeSpecContainerScope.() -> Unit
        ) {
            arrayOf(
                "Before 2.4.0" to Arb.nonNegativeInt().map { Version(2, 3, it) },
                "From 2.4.0 until 2.6.3" to Arb.choose(
                    3 to Arb.int(0..2).map { Version(2, 6, it) },
                    997 to Arb.bind(
                        Arb.int(4..5),
                        Arb.nonNegativeInt(),
                    ) { minor, patch -> Version(2, minor, patch) },
                ),
                "From 2.6.3 until 2.7.0" to Arb.int(min = 3).map { Version(2, 6, it) },
                "Since 2.7.0" to Arb.bind(
                    Arb.int(min = 7),
                    Arb.nonNegativeInt(),
                ) { minor, patch -> Version(2, minor, patch) },
            ).forEach { (specName, arb) ->
                baseSpec.describe(specName) {
                    versionArb = arb

                    describeTests()
                }
            }
        }
    }

    data object UpgradesParser : ObjectParserTestConfig() {
        private val ACTIVE = Arb.byte()
        private val COUNT = Arb.byte().filter { it.toInt() != -1 }
        private val TIME = Arb.short().filter { it.toInt() != -1 }

        private lateinit var activeFlag: Flag<Byte>
        private lateinit var countFlag: Flag<Byte>
        private lateinit var timeFlag: Flag<Short>

        override val objectPacketGen: Gen<ByteReadPacket> = Arb.bind(
            ID,
            versionArb,
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
        ) { id, ver, a1, a2, a3, ac, c2, c3, c4, t1, t2, t3, t4 ->
            objectID = id
            version = ver

            activeFlag = a2.flag1
            countFlag = c2.flag5
            timeFlag = t2.flag1

            buildObject {
                writeByte(ObjectType.UPGRADES.id)
                writeIntLittleEndian(id)

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
        }

        override val shouldYieldObject: Boolean = true

        override fun test(obj: ArtemisObject) {
            obj.shouldBeInstanceOf<ArtemisPlayer>()
            obj.id shouldBeEqual objectID

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

        private fun <T> allFlags(arb: Arb<T>): Arb<FlagByte<T, T, T, T, T, T, T, T>> =
            Arb.flags(arb, arb, arb, arb, arb, arb, arb, arb)
    }

    data object WeaponsParser : ObjectParserTestConfig() {
        private val COUNT = Arb.byte().filter { it.toInt() != -1 }
        private val UNKNOWN = Arb.byte()
        private val TIME = Arb.numericFloat()
        private val STATUS = Arb.enum<TubeState>()

        private lateinit var countFlags: Array<Flag<Byte>>
        private var unknownFlag: Flag<Byte>? = null
        private lateinit var timeFlags: Array<Flag<Float>>
        private lateinit var statusFlags: Array<Flag<TubeState>>
        private lateinit var typeFlags: Array<Flag<OrdnanceType>>

        private sealed interface TestSpec {
            val specName: String
            val objectPacketGen: Gen<ByteReadPacket>

            data object V1 : TestSpec {
                override val specName: String = "Before 2.6.3"

                private val typeArb = Arb.enum<OrdnanceType>().filter { it < OrdnanceType.BEACON }

                override val objectPacketGen: Gen<ByteReadPacket> = Arb.bind(
                    ID,
                    Arb.choose(
                        3 to Arb.int(0..2).map { Version(2, 6, it) },
                        997 to Arb.bind(
                            Arb.int(3..5),
                            Arb.nonNegativeInt(),
                        ) { minor, patch -> Version(2, minor, patch) },
                    ),
                    Arb.flags(COUNT, COUNT, COUNT, COUNT, COUNT, UNKNOWN, TIME, TIME),
                    Arb.flags(TIME, TIME, TIME, TIME, STATUS, STATUS, STATUS, STATUS),
                    Arb.flags(STATUS, STATUS, typeArb, typeArb, typeArb, typeArb, typeArb, typeArb),
                ) { id, ver, flags1, flags2, flags3 ->
                    objectID = id
                    version = ver

                    countFlags = arrayOf(
                        flags1.flag1,
                        flags1.flag2,
                        flags1.flag3,
                        flags1.flag4,
                        flags1.flag5,
                    )
                    unknownFlag = flags1.flag6
                    timeFlags = arrayOf(
                        flags1.flag7,
                        flags1.flag8,
                        flags2.flag1,
                        flags2.flag2,
                        flags2.flag3,
                        flags2.flag4,
                    )
                    statusFlags = arrayOf(
                        flags2.flag5,
                        flags2.flag6,
                        flags2.flag7,
                        flags2.flag8,
                        flags3.flag1,
                        flags3.flag2,
                    )
                    typeFlags = arrayOf(
                        flags3.flag3,
                        flags3.flag4,
                        flags3.flag5,
                        flags3.flag6,
                        flags3.flag7,
                        flags3.flag8,
                    )

                    buildObject(flags1, flags2, flags3)
                }
            }

            data object V2 : TestSpec {
                override val specName: String = "Since 2.6.3"

                private val typeArb = Arb.enum<OrdnanceType>()

                override val objectPacketGen: Gen<ByteReadPacket> = Arb.bind(
                    ID,
                    Arb.choose(
                        1 to Arb.int(min = 3).map { Version(2, 6, it) },
                        999 to Arb.bind(
                            Arb.int(min = 7),
                            Arb.nonNegativeInt(),
                        ) { minor, patch -> Version(2, minor, patch) },
                    ),
                    Arb.flags(COUNT, COUNT, COUNT, COUNT, COUNT, COUNT, COUNT, COUNT),
                    Arb.flags(TIME, TIME, TIME, TIME, TIME, TIME, STATUS, STATUS),
                    Arb.flags(STATUS, STATUS, STATUS, STATUS, typeArb, typeArb, typeArb, typeArb),
                    Arb.flags(typeArb, typeArb),
                ) { id, ver, flags1, flags2, flags3, flags4 ->
                    objectID = id
                    version = ver

                    countFlags = arrayOf(
                        flags1.flag1,
                        flags1.flag2,
                        flags1.flag3,
                        flags1.flag4,
                        flags1.flag5,
                        flags1.flag6,
                        flags1.flag7,
                        flags1.flag8,
                    )
                    unknownFlag = null
                    timeFlags = arrayOf(
                        flags2.flag1,
                        flags2.flag2,
                        flags2.flag3,
                        flags2.flag4,
                        flags2.flag5,
                        flags2.flag6,
                    )
                    statusFlags = arrayOf(
                        flags2.flag7,
                        flags2.flag8,
                        flags3.flag1,
                        flags3.flag2,
                        flags3.flag3,
                        flags3.flag4,
                    )
                    typeFlags = arrayOf(
                        flags3.flag5,
                        flags3.flag6,
                        flags3.flag7,
                        flags3.flag8,
                        flags4.flag1,
                        flags4.flag2,
                    )

                    buildObject(flags1, flags2, flags3, flags4)
                }
            }
        }

        private var testSpec: TestSpec = TestSpec.V2
        override val objectPacketGen: Gen<ByteReadPacket> get() = testSpec.objectPacketGen

        override val shouldYieldObject: Boolean = true

        override fun test(obj: ArtemisObject) {
            obj.shouldBeInstanceOf<ArtemisPlayer>()
            obj.id shouldBeEqual objectID

            obj.hasPosition.shouldBeFalse()

            obj.hasWeaponsData shouldBeEqual arrayOf(
                countFlags,
                statusFlags,
                typeFlags,
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

                if (contentsFlag.enabled && status != TubeState.UNLOADED) {
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

                tube.hasData.shouldBeEqual(statusFlag.enabled || contentsFlag.enabled)
            }
        }

        override suspend fun split(
            baseSpec: DescribeSpecContainerScope,
            describeTests: suspend DescribeSpecContainerScope.() -> Unit,
        ) {
            arrayOf(TestSpec.V1, TestSpec.V2).forEach {
                baseSpec.describe(it.specName) {
                    testSpec = it

                    describeTests()
                }
            }
        }

        private fun buildObject(
            flags1: FlagByte<*, *, *, *, *, *, *, *>,
            flags2: FlagByte<*, *, *, *, *, *, *, *>,
            flags3: FlagByte<*, *, *, *, *, *, *, *>,
            flags4: FlagByte<*, *, *, *, *, *, *, *> =
                FlagByte(dummy, dummy, dummy, dummy, dummy, dummy, dummy, dummy),
        ): ByteReadPacket = buildObject {
            writeByte(ObjectType.WEAPONS_CONSOLE.id)
            writeIntLittleEndian(objectID)
            arrayOf(flags1, flags2, flags3, flags4).forEach {
                writeByte(it.byteValue)
            }

            writeByteFlags(*countFlags)

            unknownFlag?.also {
                writeByteFlags(it)
            }

            writeFloatFlags(*timeFlags)
            writeEnumFlags(*statusFlags)
            writeEnumFlags(*typeFlags)
        }
    }

    sealed class Unobserved : ObjectParserTestConfig() {
        data object Engineering : Unobserved() {
            override val objectPacketGen: Gen<ByteReadPacket> = Arb.bind(
                ID,
                versionArb,
                systemFlags(Arb.numericFloat()),
                systemFlags(Arb.numericFloat()),
                systemFlags(Arb.byte()),
            ) { id, ver, heatFlags, enFlags, coolFlags ->
                objectID = id
                version = ver

                buildObject {
                    writeByte(ObjectType.ENGINEERING_CONSOLE.id)
                    writeIntLittleEndian(id)

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

            private fun <T> systemFlags(arb: Arb<T>): Arb<FlagByte<T, T, T, T, T, T, T, T>> =
                Arb.flags(arb, arb, arb, arb, arb, arb, arb, arb)
        }

        data object Anomaly : Unobserved() {
            override val objectPacketGen: Gen<ByteReadPacket> get() = Arb.bind(
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
            ) { id, ver, flags ->
                objectID = id
                version = ver

                val beaconVersion = ver >= Version.BEACON

                buildObject {
                    writeByte(ObjectType.ANOMALY.id)
                    writeIntLittleEndian(id)

                    writeByte(flags.byteValue)
                    if (beaconVersion) writeByte(0)

                    writeFloatFlags(flags.flag1, flags.flag2, flags.flag3)
                    writeIntFlags(flags.flag4, flags.flag5, flags.flag6)

                    if (beaconVersion) {
                        writeByteFlags(flags.flag7, flags.flag8)
                    }
                }
            }

            override suspend fun split(
                baseSpec: DescribeSpecContainerScope,
                describeTests: suspend DescribeSpecContainerScope.() -> Unit,
            ) {
                arrayOf(
                    "Before 2.6.3" to Arb.choose(
                        3 to Arb.int(0..2).map { Version(2, 6, it) },
                        997 to Arb.bind(
                            Arb.int(3..5),
                            Arb.nonNegativeInt(),
                        ) { minor, patch -> Version(2, minor, patch) },
                    ),
                    "Since 2.6.3" to Arb.choose(
                        1 to Arb.int(min = 3).map { Version(2, 6, it) },
                        999 to Arb.bind(
                            Arb.int(min = 7),
                            Arb.nonNegativeInt(),
                        ) { minor, patch -> Version(2, minor, patch) },
                    ),
                ).forEach { (specName, arb) ->
                    baseSpec.describe(specName) {
                        versionArb = arb

                        describeTests()
                    }
                }
            }
        }

        data object Nebula : Unobserved() {
            override val objectPacketGen: Gen<ByteReadPacket> get() = Arb.bind(
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
            ) { id, ver, flags ->
                objectID = id
                version = ver

                buildObject {
                    writeByte(ObjectType.NEBULA.id)
                    writeIntLittleEndian(id)

                    writeByte(flags.byteValue)

                    writeFloatFlags(
                        flags.flag1,
                        flags.flag2,
                        flags.flag3,
                        flags.flag4,
                        flags.flag5,
                        flags.flag6,
                    )

                    if (ver >= Version.NEBULA_TYPES) {
                        writeByteFlags(flags.flag7)
                    }
                }
            }

            override suspend fun split(
                baseSpec: DescribeSpecContainerScope,
                describeTests: suspend DescribeSpecContainerScope.() -> Unit,
            ) {
                arrayOf(
                    "Before 2.7.0" to Arb.bind(
                        Arb.int(3..6),
                        Arb.nonNegativeInt(),
                    ) { minor, patch -> Version(2, minor, patch) },
                    "Since 2.7.0" to Arb.bind(
                        Arb.int(min = 7),
                        Arb.nonNegativeInt(),
                    ) { minor, patch -> Version(2, minor, patch) },
                ).forEach { (specName, arb) ->
                    baseSpec.describe(specName) {
                        versionArb = arb

                        describeTests()
                    }
                }
            }
        }

        data object Torpedo : Unobserved() {
            override val objectPacketGen: Gen<ByteReadPacket> = Arb.bind(
                ID,
                versionArb,
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
            ) { id, ver, flags ->
                objectID = id
                version = ver

                buildObject {
                    writeByte(ObjectType.TORPEDO.id)
                    writeIntLittleEndian(id)

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
        }

        data object Asteroid : Unobserved() {
            override val objectPacketGen: Gen<ByteReadPacket> = Arb.bind(
                ID,
                versionArb,
                Arb.flags(
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                    Arb.numericFloat(),
                ),
            ) { id, ver, flags ->
                objectID = id
                version = ver

                buildObject {
                    writeByte(ObjectType.ASTEROID.id)
                    writeIntLittleEndian(id)

                    writeByte(flags.byteValue)

                    writeFloatFlags(flags.flag1, flags.flag2, flags.flag3)
                }
            }
        }

        data object GenericMesh : Unobserved() {
            override val objectPacketGen: Gen<ByteReadPacket> get() = Arb.bind(
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
            ) { id, ver, flags1, flags2, flags3, flags4 ->
                objectID = id
                version = ver

                buildObject {
                    writeByte(ObjectType.GENERIC_MESH.id)
                    writeIntLittleEndian(id)

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

                    if (ver >= Version.NEBULA_TYPES) {
                        writeIntFlags(flags4.flag3)
                    }
                }
            }

            override suspend fun split(
                baseSpec: DescribeSpecContainerScope,
                describeTests: suspend DescribeSpecContainerScope.() -> Unit,
            ) {
                arrayOf(
                    "Before 2.7.0" to Arb.bind(
                        Arb.int(3..6),
                        Arb.nonNegativeInt(),
                    ) { minor, patch -> Version(2, minor, patch) },
                    "Since 2.7.0" to Arb.bind(
                        Arb.int(min = 7),
                        Arb.nonNegativeInt(),
                    ) { minor, patch -> Version(2, minor, patch) },
                ).forEach { (specName, arb) ->
                    baseSpec.describe(specName) {
                        versionArb = arb

                        describeTests()
                    }
                }
            }
        }

        data object Drone : Unobserved() {
            override val objectPacketGen: Gen<ByteReadPacket> = Arb.bind(
                ID,
                versionArb,
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
            ) { id, ver, flags1, flags2 ->
                objectID = id
                version = ver

                buildObject {
                    writeByte(ObjectType.DRONE.id)
                    writeIntLittleEndian(id)

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
        }

        override val shouldYieldObject: Boolean = false
    }

    protected var objectID: Int = 0
    var version: Version = ArtemisNetworkInterface.LATEST_VERSION

    var versionArb: Arb<Version> = Arb.bind(
        Arb.int(min = 3),
        Arb.nonNegativeInt(),
    ) { minor, patch -> Version(2, minor, patch) }

    abstract val objectPacketGen: Gen<ByteReadPacket>
    abstract val shouldYieldObject: Boolean

    open fun test(obj: ArtemisObject) { }

    open suspend fun split(
        baseSpec: DescribeSpecContainerScope,
        describeTests: suspend DescribeSpecContainerScope.() -> Unit,
    ) {
        baseSpec.describeTests()
    }

    protected companion object {
        val ID = Arb.int()
        val X = Arb.numericFloat()
        val Y = Arb.numericFloat()
        val Z = Arb.numericFloat()

        private val dummy = Flag(false, 0)

        private fun <T> Arb.Companion.flag(arb: Arb<T>): Arb<Flag<T>> =
            Arb.pair(Arb.boolean(), arb).map { Flag(it) }

        fun <T> Arb.Companion.flags(
            arb: Arb<T>
        ): Arb<FlagByte<T, Int, Int, Int, Int, Int, Int, Int>> = Arb.flag(arb).map {
            FlagByte(it, dummy, dummy, dummy, dummy, dummy, dummy, dummy)
        }

        fun <T1, T2> Arb.Companion.flags(
            arb1: Arb<T1>,
            arb2: Arb<T2>,
        ): Arb<FlagByte<T1, T2, Int, Int, Int, Int, Int, Int>> = Arb.bind(
            Arb.flag(arb1),
            Arb.flag(arb2),
        ) { flag1, flag2 ->
            FlagByte(flag1, flag2, dummy, dummy, dummy, dummy, dummy, dummy)
        }

        fun <T1, T2, T3> Arb.Companion.flags(
            arb1: Arb<T1>,
            arb2: Arb<T2>,
            arb3: Arb<T3>,
        ): Arb<FlagByte<T1, T2, T3, Int, Int, Int, Int, Int>> = Arb.bind(
            Arb.flag(arb1),
            Arb.flag(arb2),
            Arb.flag(arb3),
        ) { flag1, flag2, flag3 ->
            FlagByte(flag1, flag2, flag3, dummy, dummy, dummy, dummy, dummy)
        }

        fun <T1, T2, T3, T4> Arb.Companion.flags(
            arb1: Arb<T1>,
            arb2: Arb<T2>,
            arb3: Arb<T3>,
            arb4: Arb<T4>,
        ): Arb<FlagByte<T1, T2, T3, T4, Int, Int, Int, Int>> = Arb.bind(
            Arb.flag(arb1),
            Arb.flag(arb2),
            Arb.flag(arb3),
            Arb.flag(arb4),
        ) { flag1, flag2, flag3, flag4 ->
            FlagByte(flag1, flag2, flag3, flag4, dummy, dummy, dummy, dummy)
        }

        fun <T1, T2, T3, T4, T5> Arb.Companion.flags(
            arb1: Arb<T1>,
            arb2: Arb<T2>,
            arb3: Arb<T3>,
            arb4: Arb<T4>,
            arb5: Arb<T5>,
        ): Arb<FlagByte<T1, T2, T3, T4, T5, Int, Int, Int>> = Arb.bind(
            Arb.flag(arb1),
            Arb.flag(arb2),
            Arb.flag(arb3),
            Arb.flag(arb4),
            Arb.flag(arb5),
        ) { flag1, flag2, flag3, flag4, flag5 ->
            FlagByte(flag1, flag2, flag3, flag4, flag5, dummy, dummy, dummy)
        }

        fun <T1, T2, T3, T4, T5, T6> Arb.Companion.flags(
            arb1: Arb<T1>,
            arb2: Arb<T2>,
            arb3: Arb<T3>,
            arb4: Arb<T4>,
            arb5: Arb<T5>,
            arb6: Arb<T6>,
        ): Arb<FlagByte<T1, T2, T3, T4, T5, T6, Int, Int>> = Arb.bind(
            Arb.flag(arb1),
            Arb.flag(arb2),
            Arb.flag(arb3),
            Arb.flag(arb4),
            Arb.flag(arb5),
            Arb.flag(arb6),
        ) { flag1, flag2, flag3, flag4, flag5, flag6 ->
            FlagByte(flag1, flag2, flag3, flag4, flag5, flag6, dummy, dummy)
        }

        fun <T1, T2, T3, T4, T5, T6, T7> Arb.Companion.flags(
            arb1: Arb<T1>,
            arb2: Arb<T2>,
            arb3: Arb<T3>,
            arb4: Arb<T4>,
            arb5: Arb<T5>,
            arb6: Arb<T6>,
            arb7: Arb<T7>,
        ): Arb<FlagByte<T1, T2, T3, T4, T5, T6, T7, Int>> = Arb.bind(
            Arb.flag(arb1),
            Arb.flag(arb2),
            Arb.flag(arb3),
            Arb.flag(arb4),
            Arb.flag(arb5),
            Arb.flag(arb6),
            Arb.flag(arb7),
        ) { flag1, flag2, flag3, flag4, flag5, flag6, flag7 ->
            FlagByte(flag1, flag2, flag3, flag4, flag5, flag6, flag7, dummy)
        }

        fun <T1, T2, T3, T4, T5, T6, T7, T8> Arb.Companion.flags(
            arb1: Arb<T1>,
            arb2: Arb<T2>,
            arb3: Arb<T3>,
            arb4: Arb<T4>,
            arb5: Arb<T5>,
            arb6: Arb<T6>,
            arb7: Arb<T7>,
            arb8: Arb<T8>,
        ): Arb<FlagByte<T1, T2, T3, T4, T5, T6, T7, T8>> = Arb.bind(
            Arb.flag(arb1),
            Arb.flag(arb2),
            Arb.flag(arb3),
            Arb.flag(arb4),
            Arb.flag(arb5),
            Arb.flag(arb6),
            Arb.flag(arb7),
            Arb.flag(arb8),
        ) { flag1, flag2, flag3, flag4, flag5, flag6, flag7, flag8 ->
            FlagByte(flag1, flag2, flag3, flag4, flag5, flag6, flag7, flag8)
        }

        fun BytePacketBuilder.writeFloatFlags(vararg flags: Flag<Float>) {
            flags.forEach {
                if (it.enabled) {
                    writeFloatLittleEndian(it.value)
                }
            }
        }

        fun BytePacketBuilder.writeIntFlags(vararg flags: Flag<Int>) {
            flags.forEach {
                if (it.enabled) {
                    writeIntLittleEndian(it.value)
                }
            }
        }

        fun <E : Enum<E>> BytePacketBuilder.writeEnumFlags(vararg flags: Flag<E>) {
            flags.forEach {
                if (it.enabled) {
                    writeByte(it.value.ordinal.toByte())
                }
            }
        }

        fun BytePacketBuilder.writeByteFlags(vararg flags: Flag<Byte>) {
            flags.forEach {
                if (it.enabled) {
                    writeByte(it.value)
                }
            }
        }

        fun BytePacketBuilder.writeShortFlags(vararg flags: Flag<Short>) {
            flags.forEach {
                if (it.enabled) {
                    writeShortLittleEndian(it.value)
                }
            }
        }

        fun BytePacketBuilder.writeStringFlags(vararg flags: Flag<String>) {
            flags.forEach {
                if (it.enabled) {
                    val str = it.value
                    writeIntLittleEndian(str.length + 1)
                    writeText(str, charset = Charsets.UTF_16LE)
                    writeShort(0)
                }
            }
        }

        fun buildObject(block: BytePacketBuilder.() -> Unit): ByteReadPacket = buildPacket {
            block()
            writeIntLittleEndian(0)
        }

        fun testHasPosition(obj: ArtemisObject, xFlag: Flag<Float>, zFlag: Flag<Float>) {
            obj.hasPosition.shouldBeEqual(xFlag.enabled && zFlag.enabled)
        }
    }
}
