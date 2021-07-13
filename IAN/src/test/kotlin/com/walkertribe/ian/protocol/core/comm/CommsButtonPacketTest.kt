package com.walkertribe.ian.protocol.core.comm

import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.PacketTestProtocol
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.exhaustive.bytes
import io.kotest.property.exhaustive.filterNot
import io.kotest.property.exhaustive.map
import io.kotest.property.exhaustive.of
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket

class CommsButtonPacketTest : PacketTestSpec.Server<CommsButtonPacket>(
    specName = "CommsButtonPacket",
    packetType = TestPacketTypes.COMMS_BUTTON,
    packetTypeName = CorePacketType.COMMS_BUTTON,
) {
    abstract class CommsButtonActionTestCase(
        val testName: String,
        val actionValue: Byte,
        private val shouldWriteLabel: Boolean = true,
    ) {
        protected var label: String = ""

        abstract val payloadGen: Gen<ByteReadPacket>

        abstract fun test(action: CommsButtonPacket.Action)

        protected fun buildPacket(): ByteReadPacket = buildPacket {
            writeByte(actionValue)
            if (shouldWriteLabel) {
                writeString(label)
            }
        }
    }

    private val allTestCases = listOf(
        object : CommsButtonActionTestCase("Remove", 0x00) {
            override val payloadGen: Gen<ByteReadPacket> = Arb.string().map {
                label = it
                buildPacket()
            }

            override fun test(action: CommsButtonPacket.Action) {
                action.shouldBeInstanceOf<CommsButtonPacket.Action.Remove>()
                action.label.toString() shouldBeEqual label
            }
        },
        object : CommsButtonActionTestCase("Create", 0x02) {
            override val payloadGen: Gen<ByteReadPacket> = Arb.string().map {
                label = it
                buildPacket()
            }

            override fun test(action: CommsButtonPacket.Action) {
                action.shouldBeInstanceOf<CommsButtonPacket.Action.Create>()
                action.label.toString() shouldBeEqual label
            }
        },
        object : CommsButtonActionTestCase("Remove All", 0x64, false) {
            override val payloadGen: Gen<ByteReadPacket> = Exhaustive.of(buildPacket())

            override fun test(action: CommsButtonPacket.Action) {
                action.shouldBeInstanceOf<CommsButtonPacket.Action.RemoveAll>()
            }
        },
    ).apply {
        size shouldBeEqual CommsButtonPacket.Action::class.sealedSubclasses.size
    }

    private lateinit var actionTestCase: CommsButtonActionTestCase

    override val protocol: PacketTestProtocol<CommsButtonPacket> = PacketTestProtocol()

    override val payloadGen: Gen<ByteReadPacket> get() = actionTestCase.payloadGen

    override val failures: List<Failure> = listOf(
        object : Failure("Fails to parse invalid action") {
            private val validActions = allTestCases.map { it.actionValue }.toSet()

            override val payloadGen: Gen<ByteReadPacket> = Exhaustive.bytes().filterNot {
                validActions.contains(it)
            }.map {
                ByteReadPacket(byteArrayOf(it))
            }
        }
    )

    override suspend fun testType(packet: ArtemisPacket): CommsButtonPacket =
        packet.shouldBeInstanceOf()

    override suspend fun testPayload(packet: CommsButtonPacket) {
        actionTestCase.test(packet.action)
    }

    override suspend fun DescribeSpecContainerScope.organizeTests(
        describeTests: suspend DescribeSpecContainerScope.() -> Unit,
    ) {
        allTestCases.forEach {
            describe(it.testName) {
                actionTestCase = it

                describeTests()
            }
        }
    }

    override suspend fun describeClientTests(
        scope: DescribeSpecContainerScope,
        client: ArtemisNetworkInterface,
        sendChannel: ByteWriteChannel,
        afterTest: () -> Unit
    ) {
        scope.describe(specName) {
            withData(nameFn = { it.testName }, this@CommsButtonPacketTest.allTestCases) {
                actionTestCase = it
                runClientTest(client, sendChannel)
                afterTest()
            }
        }
    }
}
