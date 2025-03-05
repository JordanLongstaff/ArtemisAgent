package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.enums.Console
import com.walkertribe.ian.enums.entries
import com.walkertribe.ian.protocol.core.PacketTestData
import com.walkertribe.ian.protocol.core.PacketTestFixture
import com.walkertribe.ian.protocol.core.TestPacketTypes
import com.walkertribe.ian.protocol.core.ValueIntPacket
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.exhaustive.of
import kotlinx.io.Source
import kotlinx.io.readIntLe

class SetConsolePacketFixture
private constructor(override val specName: String, console: Console, expectedValue: Int) :
    PacketTestFixture.Client<SetConsolePacket>(
        packetType = TestPacketTypes.VALUE_INT,
        expectedPayloadSize = Int.SIZE_BYTES * 3,
    ) {
    class Data internal constructor(private val console: Console, private val expectedValue: Int) :
        PacketTestData.Client<SetConsolePacket>(SetConsolePacket(console)) {
        override fun validatePayload(payload: Source) {
            payload.readIntLe() shouldBeEqual ValueIntPacket.Subtype.SET_CONSOLE.toInt()

            val consoleValue = payload.readIntLe()
            consoleValue shouldBeEqual expectedValue

            val readConsole = Console.entries.find { it.index == consoleValue }.shouldNotBeNull()
            readConsole shouldBeEqual console

            payload.readIntLe() shouldBeEqual 1
        }
    }

    override val generator: Gen<Data> = Exhaustive.of(Data(console, expectedValue))

    companion object {
        val ALL =
            listOf(
                SetConsolePacketFixture("Main screen", Console.MAIN_SCREEN, 0),
                SetConsolePacketFixture("Communications", Console.COMMUNICATIONS, 5),
                SetConsolePacketFixture("Single-seat craft", Console.SINGLE_SEAT_CRAFT, 6),
            )
    }
}
