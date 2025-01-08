package com.walkertribe.ian.protocol.core

import com.walkertribe.ian.util.Version

/**
 * Sent by a client that wishes to activate an upgrade.
 *
 * @author rjwut
 */
sealed class ActivateUpgradePacket private constructor(subtype: Byte) :
    ValueIntPacket(subtype, DOUBLE_AGENT_VALUE) {
    data object Current : ActivateUpgradePacket(Subtype.ACTIVATE_UPGRADE_CURRENT)

    data object Old : ActivateUpgradePacket(Subtype.ACTIVATE_UPGRADE_OLD)

    companion object {
        private const val DOUBLE_AGENT_VALUE = 8

        private val DECIDER_VERSION = Version(2, 3, 1)

        operator fun invoke(version: Version): ActivateUpgradePacket =
            if (version <= DECIDER_VERSION) Old else Current
    }
}
