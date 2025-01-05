package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.comm.ToggleRedAlertPacket
import com.walkertribe.ian.protocol.core.setup.ReadyPacket
import com.walkertribe.ian.protocol.core.setup.SetConsolePacket
import com.walkertribe.ian.protocol.core.setup.SetShipPacket
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import kotlin.reflect.KClass

class ValueIntPacketTest :
    DescribeSpec({
        describe("ValueIntPacket") {
            describe("Subtypes") {
                val nameRegex = Regex("\\.[A-Z].+")

                withData<Triple<KClass<out Packet>, Byte, Byte>>(
                    nameFn = {
                        val name = it.first.qualifiedName?.let(nameRegex::find)?.value?.substring(1)
                        "$name = ${it.third}"
                    },
                    Triple(
                        ToggleRedAlertPacket::class,
                        ValueIntPacket.Subtype.TOGGLE_RED_ALERT,
                        0x0a,
                    ),
                    Triple(SetShipPacket::class, ValueIntPacket.Subtype.SET_SHIP, 0x0d),
                    Triple(SetConsolePacket::class, ValueIntPacket.Subtype.SET_CONSOLE, 0x0e),
                    Triple(ReadyPacket::class, ValueIntPacket.Subtype.READY, 0x0f),
                    Triple(ButtonClickPacket::class, ValueIntPacket.Subtype.BUTTON_CLICK, 0x15),
                    Triple(
                        ActivateUpgradePacket.Old::class,
                        ValueIntPacket.Subtype.ACTIVATE_UPGRADE_OLD,
                        0x1b,
                    ),
                    Triple(
                        ActivateUpgradePacket.Current::class,
                        ValueIntPacket.Subtype.ACTIVATE_UPGRADE_CURRENT,
                        0x1c,
                    ),
                    Triple(
                        HeartbeatPacket.Client::class,
                        ValueIntPacket.Subtype.CLIENT_HEARTBEAT,
                        0x24,
                    ),
                ) {
                    it.second shouldBeEqual it.third
                }
            }
        }
    })
