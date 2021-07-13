package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.util.Version

/**
 * Sent by a client that wishes to activate an upgrade.
 * @author rjwut
 */
sealed class ActivateUpgradePacket : ValueIntPacket(DOUBLE_AGENT_VALUE) {
    @Packet(
        origin = Origin.CLIENT,
        type = CorePacketType.VALUE_INT,
        subtype = Subtype.ACTIVATE_UPGRADE_CURRENT
    )
    data object Current : ActivateUpgradePacket()

    @Packet(
        origin = Origin.CLIENT,
        type = CorePacketType.VALUE_INT,
        subtype = Subtype.ACTIVATE_UPGRADE_OLD
    )
    data object Old : ActivateUpgradePacket()

    companion object {
        private const val DOUBLE_AGENT_VALUE = 8

        private val DECIDER_VERSION = Version(2, 3, 1)

        operator fun invoke(version: Version): ActivateUpgradePacket =
            if (version <= DECIDER_VERSION) Old else Current
    }
}
