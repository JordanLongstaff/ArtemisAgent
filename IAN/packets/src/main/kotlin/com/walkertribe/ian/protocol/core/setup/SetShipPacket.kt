package com.walkertribe.ian.protocol.core.setup

import com.walkertribe.ian.protocol.core.ValueIntPacket
import com.walkertribe.ian.world.Artemis

/**
 * Set the ship you want to be on. You must send this packet before [SetConsolePacket].
 *
 * @author dhleong
 */
class SetShipPacket(
    /** The ship index being selected (0-based). */
    val shipIndex: Int
) : ValueIntPacket(Subtype.SET_SHIP, shipIndex) {
    init {
        require(shipIndex in 0 until Artemis.SHIP_COUNT) {
            "Ship index $shipIndex is not in range [0, ${Artemis.SHIP_COUNT})"
        }
    }
}
