package com.walkertribe.ian.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.util.BoolState

/**
 * An NPC ship; they may have special abilities, and can be scanned.
 *
 * The Artemis server sometimes sends garbage data for the special abilities properties when a
 * vessel does not have special abilities. The [.getSpecialBits] and [.getSpecialStateBits] methods
 * preserve the exact data sent by the server. The other methods related to special abilities will
 * attempt to reconcile this with the information parsed from vesselData.xml. If the vessel spec
 * indicates that it can't have special abilities, then these methods will never report that it has
 * them, regardless of what is reported by received packets. If the vessel cannot be determined
 * (because the hull ID is unspecified or does not correspond to a known vessel), these methods will
 * indicate that it cannot reliably make a determination.
 *
 * @author dhleong
 */
class ArtemisNpc(id: Int, timestamp: Long) : BaseArtemisShip<ArtemisNpc>(id, timestamp) {
    /**
     * Is the NPC understood to be an enemy? Always true in PvP and scripted games. Unspecified:
     * BoolState.Unknown
     */
    val isEnemy = Property.BoolProperty(timestamp)

    /** Has the NPC surrendered? Unspecified: BoolState.Unknown */
    val isSurrendered = Property.BoolProperty(timestamp)

    /** Is the NPC in a nebula? Unspecified: BoolState.Unknown */
    val isInNebula = Property.BoolProperty(timestamp)

    /** A bitmask indicating which sides have scanned this NPC at least once. Unspecified: null */
    val scanBits = Property.ObjectProperty<Int>(timestamp)

    fun hasBeenScannedBy(side: Byte): BoolState =
        BoolState(scanBits.value?.let { it and (1 shl side.toInt()) != 0 })

    fun hasBeenScannedBy(ship: BaseArtemisShip<*>): BoolState =
        if (ship.side.hasValue) hasBeenScannedBy(ship.side.value) else BoolState.Unknown

    override val type = ObjectType.NPC_SHIP

    override val hasData
        get() =
            super.hasData ||
                isEnemy.hasValue ||
                isSurrendered.hasValue ||
                isInNebula.hasValue ||
                scanBits.hasValue

    override fun updates(other: ArtemisNpc) {
        super.updates(other)

        isEnemy updates other.isEnemy
        isSurrendered updates other.isSurrendered
        isInNebula updates other.isInNebula
        scanBits updates other.scanBits
    }

    object Dsl : BaseArtemisShip.Dsl<ArtemisNpc>() {
        var isEnemy: BoolState = BoolState.Unknown
        var isSurrendered: BoolState = BoolState.Unknown
        var isInNebula: BoolState = BoolState.Unknown
        var scanBits: Int? = null

        override fun create(id: Int, timestamp: Long) = ArtemisNpc(id, timestamp)

        override fun updates(obj: ArtemisNpc) {
            super.updates(obj)

            obj.isEnemy.value = isEnemy
            obj.isSurrendered.value = isSurrendered
            obj.isInNebula.value = isInNebula
            obj.scanBits.value = scanBits

            isEnemy = BoolState.Unknown
            isSurrendered = BoolState.Unknown
            isInNebula = BoolState.Unknown
            scanBits = null
        }
    }
}
