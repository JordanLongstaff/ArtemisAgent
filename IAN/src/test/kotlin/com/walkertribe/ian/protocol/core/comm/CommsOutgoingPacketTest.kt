package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.enums.BaseMessage
import com.walkertribe.ian.enums.CommsMessage
import com.walkertribe.ian.enums.CommsRecipientType
import com.walkertribe.ian.enums.EnemyMessage
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.enums.OtherMessage
import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.util.BoolState
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.world.ArtemisBase
import com.walkertribe.ian.world.ArtemisBlackHole
import com.walkertribe.ian.world.ArtemisCreature
import com.walkertribe.ian.world.ArtemisMine
import com.walkertribe.ian.world.ArtemisNpc
import com.walkertribe.ian.world.ArtemisObject
import com.walkertribe.ian.world.ArtemisPlayer
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.choose
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.of
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.enum
import io.kotest.property.exhaustive.map
import io.kotest.property.exhaustive.merge
import io.kotest.property.exhaustive.of
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readIntLittleEndian
import io.mockk.mockk

class CommsOutgoingPacketTest : PacketTestSpec.Client<CommsOutgoingPacket>(
    specName = "CommsOutgoingPacket",
    packetType = TestPacketTypes.COMMS_MESSAGE,
    packetTypeName = CorePacketType.COMMS_MESSAGE,
    expectedPayloadSize = Int.SIZE_BYTES * 5,
) {
    abstract class CommsOutgoingPacketTestCase(
        val expectedRecipientType: CommsRecipientType,
        val recipientGen: Gen<ArtemisObject>,
        val messageGen: Gen<CommsMessage>,
    ) {
        abstract suspend fun test(payload: ByteReadPacket, message: CommsMessage)

        override fun toString(): String = expectedRecipientType.name
    }

    private lateinit var testCase: CommsOutgoingPacketTestCase
    private lateinit var recipientObject: ArtemisObject
    private lateinit var commsMessage: CommsMessage

    private val unknownArg = 0x004f005e

    override suspend fun DescribeSpecContainerScope.organizeTests(
        describe: suspend DescribeSpecContainerScope.(Gen<CommsOutgoingPacket>) -> Unit,
    ) {
        allTestCases.forEach {
            this@CommsOutgoingPacketTest.testCase = it

            describe("Comms recipient type: $it") {
                describe(
                    Arb.bind(it.recipientGen, it.messageGen) { recipient, message ->
                        recipientObject = recipient
                        commsMessage = message
                        CommsOutgoingPacket(recipient, message, mockVesselData)
                    },
                )
            }
        }
    }

    override suspend fun runTest(packet: CommsOutgoingPacket, payload: ByteReadPacket) {
        payload.readIntLittleEndian() shouldBeEqual testCase.expectedRecipientType.ordinal
        payload.readIntLittleEndian() shouldBeEqual recipientObject.id
        testCase.test(payload, commsMessage)
        payload.readIntLittleEndian() shouldBeEqual unknownArg
    }

    override suspend fun runClientTest(
        scope: ContainerScope,
        client: ArtemisNetworkInterface,
        readChannel: ByteReadChannel,
    ) {
        scope.withData(nameFn = { "Comms recipient type: $it" }, allTestCases) {
            this@CommsOutgoingPacketTest.testCase = it
            super.runClientTest(this, client, readChannel)
        }
    }

    override fun clientPacketGen(client: ArtemisNetworkInterface): Gen<CommsOutgoingPacket> {
        return Arb.bind(testCase.recipientGen, testCase.messageGen) { recipient, message ->
            recipientObject = recipient
            commsMessage = message
            CommsOutgoingPacket(recipient, message, mockVesselData)
        }
    }

    override suspend fun DescribeSpecContainerScope.describeMore() {
        describe("Throws with invalid arguments") {
            describe("Invalid recipient object") {
                withData(
                    nameFn = { it.first },
                    "ArtemisBlackHole" to Arb.bind<ArtemisBlackHole>(),
                    "ArtemisCreature" to Arb.bind<ArtemisCreature>(),
                    "ArtemisMine" to Arb.bind<ArtemisMine>(),
                    "ArtemisPlayer" to Arb.bind<ArtemisPlayer>(),
                ) {
                    allTestCases.forEach { testCase ->
                        checkAll(
                            it.second,
                            testCase.messageGen,
                        ) { recipient, message ->
                            shouldThrow<IllegalArgumentException> {
                                CommsOutgoingPacket(recipient, message, mockVesselData)
                            }
                        }
                    }
                }
            }

            describe("Recipient-message type mismatch") {
                withData(nameFn = { "$it message" }, allTestCases) { testCaseForMessage ->
                    withData(
                        nameFn = { "$it recipient" },
                        allTestCases.filter { it != testCaseForMessage },
                    ) {
                        checkAll(
                            it.recipientGen,
                            testCaseForMessage.messageGen,
                        ) { recipient, message ->
                            shouldThrow<IllegalArgumentException> {
                                CommsOutgoingPacket(recipient, message, mockVesselData)
                            }
                        }
                    }
                }
            }
        }
    }

    private companion object {
        val mockVesselData = mockk<VesselData>(relaxed = true)

        val allTestCases = listOf(
            object : CommsOutgoingPacketTestCase(
                CommsRecipientType.ENEMY,
                Arb.bind<ArtemisNpc>().map {
                    it.apply { isEnemy.value = BoolState.True }
                },
                Exhaustive.enum<EnemyMessage>(),
            ) {
                override suspend fun test(payload: ByteReadPacket, message: CommsMessage) {
                    message.shouldBeInstanceOf<EnemyMessage>()
                    payload.readIntLittleEndian() shouldBeEqual message.id
                    payload.readIntLittleEndian() shouldBeEqual CommsMessage.NO_ARG
                }
            },
            object : CommsOutgoingPacketTestCase(
                CommsRecipientType.BASE,
                Arb.bind<ArtemisBase>(),
                Exhaustive.of(
                    BaseMessage.StandByForDockingOrCeaseOperation,
                    BaseMessage.PleaseReportStatus,
                ).merge(
                    Exhaustive.enum<OrdnanceType>().map(BaseMessage.Build::invoke)
                ),
            ) {
                override suspend fun test(payload: ByteReadPacket, message: CommsMessage) {
                    message.shouldBeInstanceOf<BaseMessage>()
                    payload.readIntLittleEndian() shouldBeEqual message.id
                    payload.readIntLittleEndian() shouldBeEqual CommsMessage.NO_ARG
                }
            },
            object : CommsOutgoingPacketTestCase(
                CommsRecipientType.OTHER,
                Arb.bind<ArtemisNpc>(),
                Arb.choose(
                    11 to Arb.of(
                        OtherMessage.Hail,
                        OtherMessage.TurnToHeading0,
                        OtherMessage.TurnToHeading90,
                        OtherMessage.TurnToHeading180,
                        OtherMessage.TurnToHeading270,
                        OtherMessage.TurnLeft10Degrees,
                        OtherMessage.TurnRight10Degrees,
                        OtherMessage.TurnLeft25Degrees,
                        OtherMessage.TurnRight25Degrees,
                        OtherMessage.AttackNearestEnemy,
                        OtherMessage.ProceedToYourDestination,
                    ),
                    989 to Arb.bind<OtherMessage.GoDefend>(),
                ),
            ) {
                override suspend fun test(payload: ByteReadPacket, message: CommsMessage) {
                    message.shouldBeInstanceOf<OtherMessage>()
                    payload.readIntLittleEndian() shouldBeEqual message.id
                    payload.readIntLittleEndian() shouldBeEqual message.argument
                }
            },
        )
    }
}
