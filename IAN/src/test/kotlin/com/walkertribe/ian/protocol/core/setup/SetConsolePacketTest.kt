package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.enums.Console
import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.protocol.core.CorePacketType
import com.walkertribe.ian.protocol.core.PacketTestSpec
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.protocol.core.ValueIntPacket
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.exhaustive.of
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readIntLittleEndian

class SetConsolePacketTest : PacketTestSpec.Client<SetConsolePacket>(
    specName = "SetConsolePacket",
    packetType = TestPacketTypes.VALUE_INT,
    packetTypeName = CorePacketType.VALUE_INT,
    expectedPayloadSize = Int.SIZE_BYTES * 3,
) {
    private var expectedValue: Int = 0
    private var expectedConsole: Console = Console.MAIN_SCREEN

    override suspend fun DescribeSpecContainerScope.organizeTests(
        describe: suspend DescribeSpecContainerScope.(Gen<SetConsolePacket>) -> Unit,
    ) {
        allTestCases.map { it.first } shouldContainExactlyInAnyOrder Console.entries

        allTestCases.forEach { (console, consoleName, expected) ->
            describe(consoleName) {
                expectedValue = expected
                expectedConsole = console
                describe(Exhaustive.of(SetConsolePacket(console)))
            }
        }
    }

    override suspend fun runTest(packet: SetConsolePacket, payload: ByteReadPacket) {
        payload.readIntLittleEndian() shouldBeEqual ValueIntPacket.Subtype.SET_CONSOLE.toInt()

        val consoleValue = payload.readIntLittleEndian()
        consoleValue shouldBeEqual expectedValue

        val console = Console.entries.find { it.index == consoleValue }.shouldNotBeNull()
        console shouldBeEqual expectedConsole

        payload.readIntLittleEndian() shouldBeEqual 1
    }

    override suspend fun runClientTest(
        scope: ContainerScope,
        client: ArtemisNetworkInterface,
        readChannel: ByteReadChannel,
    ) {
        scope.withData(nameFn = { it.second }, allTestCases) {
            expectedValue = it.third
            expectedConsole = it.first
            super.runClientTest(this, client, readChannel)
        }
    }

    override fun clientPacketGen(client: ArtemisNetworkInterface): Gen<SetConsolePacket> {
        return Exhaustive.of(SetConsolePacket(expectedConsole))
    }

    private companion object {
        val allTestCases = listOf(
            Triple(Console.MAIN_SCREEN, "Main screen", 0),
            Triple(Console.COMMUNICATIONS, "Communications", 5),
            Triple(Console.SINGLE_SEAT_CRAFT, "Single-seat craft", 6),
        )
    }
}
