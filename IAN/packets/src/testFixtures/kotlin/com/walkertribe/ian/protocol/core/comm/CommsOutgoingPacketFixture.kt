package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.enums.BaseMessage
import com.walkertribe.ian.enums.CommsMessage
import com.walkertribe.ian.enums.CommsRecipientType
import com.walkertribe.ian.enums.EnemyMessage
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.enums.OtherMessage
import com.walkertribe.ian.protocol.core.PacketTestData
import com.walkertribe.ian.protocol.core.PacketTestFixture
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.util.BoolState
import com.walkertribe.ian.vesseldata.Empty
import com.walkertribe.ian.vesseldata.TestFaction
import com.walkertribe.ian.vesseldata.TestVessel
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.vesseldata.vesselData
import com.walkertribe.ian.vesseldata.vesselKeys
import com.walkertribe.ian.world.ArtemisBase
import com.walkertribe.ian.world.ArtemisNpc
import com.walkertribe.ian.world.ArtemisObject
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.PropertyTesting
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.choose
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.of
import io.kotest.property.exhaustive.enum
import io.kotest.property.exhaustive.map
import io.kotest.property.exhaustive.merge
import io.kotest.property.exhaustive.of
import kotlinx.io.Source
import kotlinx.io.readIntLe

sealed class CommsOutgoingPacketFixture
private constructor(
    val recipientGen: Gen<ArtemisObject<*>>,
    val messageGen: Gen<CommsMessage>,
    protected val vesselDataGen: Gen<VesselData>,
    val expectedRecipientType: CommsRecipientType,
    specQualifier: String = "",
) :
    PacketTestFixture.Client<CommsOutgoingPacket>(
        packetType = TestPacketTypes.COMMS_MESSAGE,
        expectedPayloadSize = PAYLOAD_SIZE,
    ) {
    class Data
    internal constructor(
        private val recipient: ArtemisObject<*>,
        private val message: CommsMessage,
        private val expectedRecipientType: CommsRecipientType,
        vesselData: VesselData,
    ) :
        PacketTestData.Client<CommsOutgoingPacket>(
            CommsOutgoingPacket(recipient, message, vesselData)
        ) {
        private val expectedArgument =
            if (message is OtherMessage.GoDefend) message.targetID else UNKNOWN_ARG

        init {
            message.recipientType shouldBeEqual expectedRecipientType
            packet.recipientType shouldBeEqual expectedRecipientType
            packet.message shouldBeEqual message
            packet.recipientId shouldBeEqual recipient.id
        }

        override fun validatePayload(payload: Source) {
            payload.readIntLe() shouldBeEqual expectedRecipientType.ordinal
            payload.readIntLe() shouldBeEqual recipient.id
            payload.readIntLe() shouldBeEqual message.id
            payload.readIntLe() shouldBeEqual expectedArgument
            payload.readIntLe() shouldBeEqual UNKNOWN_ARG_2
        }
    }

    data object Enemy :
        CommsOutgoingPacketFixture(
            Arb.bind<ArtemisNpc>().map { it.apply { isEnemy.value = BoolState.True } },
            Arb.enum<EnemyMessage>(),
            Arb.of(VesselData.Empty),
            CommsRecipientType.ENEMY,
        )

    data object EnemyVessel :
        CommsOutgoingPacketFixture(
            recipientGen = Arb.bind<ArtemisNpc>(),
            messageGen = Arb.enum<EnemyMessage>(),
            vesselDataGen =
                Arb.vesselData(
                    vessels = TestVessel.arbitrary(Arb.enum<TestFaction>().filter { it.isEnemy }),
                    numVessels = 1..1,
                ),
            expectedRecipientType = CommsRecipientType.ENEMY,
            specQualifier = "from vessel data",
        ) {
        override val generator: Gen<Data> =
            Arb.bind(recipientGen, messageGen, vesselDataGen) { recipient, message, vesselData ->
                recipient.shouldBeInstanceOf<ArtemisNpc>()
                vesselData.shouldBeInstanceOf<VesselData.Loaded>()

                val hullID = vesselData.vesselKeys.first()
                recipient.hullId.value = hullID

                Data(recipient, message, expectedRecipientType, vesselData)
            }
    }

    data object Base :
        CommsOutgoingPacketFixture(
            Arb.bind<ArtemisBase>(),
            Exhaustive.of(
                    BaseMessage.StandByForDockingOrCeaseOperation,
                    BaseMessage.PleaseReportStatus,
                )
                .merge(Exhaustive.enum<OrdnanceType>().map(BaseMessage.Build::invoke)),
            Arb.of(VesselData.Empty),
            CommsRecipientType.BASE,
        )

    data object Other :
        CommsOutgoingPacketFixture(
            Arb.bind<ArtemisNpc>(),
            Arb.choose(
                otherMessageObjects.size to Arb.of(otherMessageObjects),
                goDefendCount to Arb.bind<OtherMessage.GoDefend>(),
            ),
            Arb.vesselData(factions = TestFaction.entries.filterNot { it.isEnemy }),
            CommsRecipientType.OTHER,
        )

    override val generator: Gen<Data> =
        Arb.bind(recipientGen, messageGen, vesselDataGen) { recipient, message, vesselData ->
            Data(recipient, message, expectedRecipientType, vesselData)
        }

    override val specName: String = "Recipient type: $expectedRecipientType $specQualifier".trim()

    companion object {
        private const val PAYLOAD_SIZE = Int.SIZE_BYTES * 5
        private const val UNKNOWN_ARG = 0x00730078
        private const val UNKNOWN_ARG_2 = 0x004f005e

        private val otherMessageObjects =
            listOf(
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
            )
        private val goDefendCount = PropertyTesting.defaultIterationCount - otherMessageObjects.size

        val ALL = listOf(Enemy, EnemyVessel, Base, Other)
    }
}
