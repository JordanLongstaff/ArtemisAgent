package com.walkertribe.ian.world

import com.walkertribe.ian.enums.AlertStatus
import com.walkertribe.ian.enums.DriveType
import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.enums.TubeState
import com.walkertribe.ian.util.BoolState

/**
 * A player ship.
 *
 * @author dhleong
 */
class ArtemisPlayer(id: Int, timestamp: Long) : BaseArtemisShip<ArtemisPlayer>(id, timestamp) {
    override val type: ObjectType = ObjectType.PLAYER_SHIP

    /** Returns whether this player ship has activated a double agent. */
    val doubleAgentActive = Property.BoolProperty(timestamp)

    /** Returns the number of double agents on this player ship. */
    val doubleAgentCount = Property.ByteProperty(timestamp)

    /**
     * Returns the number of seconds for which the currently active double agent will remain active.
     */
    val doubleAgentSecondsLeft = Property.IntProperty(timestamp)

    /** Returns the alert status of the ship. */
    val alertStatus = Property.ObjectProperty<AlertStatus>(timestamp)

    /**
     * Get this ship's player ship index. Note that this value is zero-based, so the vessel that is
     * named Artemis will have a ship index of 0. If the ship is a single-seat craft, this value
     * will be -1. Unspecified: Byte.MIN_VALUE
     */
    val shipIndex = Property.ByteProperty(timestamp, Byte.MIN_VALUE)

    /**
     * Returns the ID of the capital ship with which this ship can dock. Only applies to single-seat
     * craft. Unspecified: -1
     */
    val capitalShipID = Property.IntProperty(timestamp)

    /**
     * Get the ID of the base at which we're docking. This property is set when a base latches onto
     * the player ship with its tractor beam; [DockedPacket] is sent when docking is complete and
     * resupply commences, watch for the [DockedPacket]. Note that this property is only updated in
     * a packet when the docking process commences; undocking does not update this property.
     * However, if an existing [ArtemisPlayer] object is docked, is updated by another one, and the
     * update has the ship engaging impulse or warp drive, this property will be set to 0 to
     * indicate that the ship has undocked. Unspecified: -1
     */
    val dockingBase = Property.IntProperty(timestamp)

    /** Returns [BoolState.True] if the player ship is docked; [BoolState.False] otherwise. */
    var docked: BoolState = BoolState.False

    /** The type of drive system the ship has. Unspecified: null */
    val driveType = Property.ObjectProperty<DriveType>(timestamp)

    /** Warp factor, between 0 (not at warp) and [Artemis.MAX_WARP]. Unspecified: -1 */
    val warp =
        Property.ByteProperty(timestamp) {
            require(it in -1..Artemis.MAX_WARP) { "Invalid warp factor: $it" }
            if (it > 0) {
                docked = BoolState.False
            }
        }

    /** Ordnance counts. */
    val ordnanceCounts =
        OrdnanceType.entries.map { Property.ByteProperty(timestamp) }.toTypedArray()

    /** Weapons tubes. */
    val tubes = Array(Artemis.MAX_TUBES) { WeaponsTube(timestamp) }

    /** Returns true if this object contains any data that is not upgrades data. */
    val hasPlayerData: Boolean
        get() =
            super.hasData ||
                alertStatus.hasValue ||
                shipIndex.hasValue ||
                capitalShipID.hasValue ||
                dockingBase.hasValue ||
                warp.hasValue ||
                driveType.hasValue

    val hasWeaponsData: Boolean
        get() = tubes.any { it.hasData } || ordnanceCounts.any { it.hasValue }

    /** Returns true if this object contains any upgrades data. */
    val hasUpgradeData: Boolean
        get() =
            doubleAgentActive.hasValue ||
                doubleAgentCount.hasValue ||
                doubleAgentSecondsLeft.hasValue

    override val hasData: Boolean
        get() = hasPlayerData || hasWeaponsData || hasUpgradeData

    init {
        impulse.addListener {
            if (!it.isNaN() && it > 0f) {
                docked = BoolState.False
            }
        }
    }

    override fun updates(other: ArtemisPlayer) {
        super.updates(other)

        updatesPlayer(other)
        updatesWeaponsFor(other)
        updatesUpgradesFor(other)
    }

    /**
     * Updates only data from the given ArtemisPlayer object that is not weapons, engineering or
     * upgrades data.
     */
    private infix fun updatesPlayer(plr: ArtemisPlayer) {
        alertStatus updates plr.alertStatus
        shipIndex updates plr.shipIndex
        capitalShipID updates plr.capitalShipID
        warp updates plr.warp
        driveType updates plr.driveType
        dockingBase.updates(plr.dockingBase) {
            if (impulse.value > 0f || warp.value > 0) {
                Property.IntProperty(timestamp).also {
                    it.value = 0
                    it updates plr.dockingBase
                }
            }
        }
    }

    private infix fun updatesWeaponsFor(plr: ArtemisPlayer) {
        ordnanceCounts.zip(plr.ordnanceCounts).forEach { it.first updates it.second }

        tubes.zip(plr.tubes).forEach { it.first updates it.second }
    }

    /** Updates only upgrades data from the given ArtemisPlayer object. */
    private infix fun updatesUpgradesFor(plr: ArtemisPlayer) {
        doubleAgentActive updates plr.doubleAgentActive
        doubleAgentCount updates plr.doubleAgentCount
        doubleAgentSecondsLeft updates plr.doubleAgentSecondsLeft
    }

    fun getTotalOrdnanceCount(ordnanceType: OrdnanceType): Int {
        return ordnanceCounts[ordnanceType.ordinal].value +
            tubes.count { it.contents == ordnanceType }
    }

    sealed class Dsl private constructor() : BaseArtemisShip.Dsl<ArtemisPlayer>() {
        data object Player : Dsl() {
            var shipIndex: Byte = Byte.MIN_VALUE
            var capitalShipID: Int = -1
            var alertStatus: AlertStatus? = null
            var dockingBase: Int = -1
            var driveType: DriveType? = null
            var warp: Byte = -1

            override fun isObjectEmpty(obj: ArtemisPlayer): Boolean = !obj.hasPlayerData

            override fun updates(obj: ArtemisPlayer) {
                super.updates(obj)

                obj.warp.value = warp
                obj.shipIndex.value = shipIndex
                obj.capitalShipID.value = capitalShipID
                obj.alertStatus.value = alertStatus
                obj.driveType.value = driveType
                obj.dockingBase.value = dockingBase

                shipIndex = Byte.MIN_VALUE
                capitalShipID = -1
                alertStatus = null
                dockingBase = -1
                driveType = null
                warp = -1
            }
        }

        data object Weapons : Dsl() {
            val ordnanceCounts = mutableMapOf<OrdnanceType, Byte>()
            val tubeStates = arrayOfNulls<TubeState>(Artemis.MAX_TUBES)
            val tubeContents = arrayOfNulls<OrdnanceType>(Artemis.MAX_TUBES)

            override fun updates(obj: ArtemisPlayer) {
                require(!obj.hasWeaponsData) { "Cannot apply Dsl to an already-populated object" }

                ordnanceCounts.forEach { (ordnanceType, count) ->
                    obj.ordnanceCounts[ordnanceType.ordinal].value = count
                }
                tubeStates.zip(tubeContents).forEachIndexed { index, (state, contents) ->
                    obj.tubes[index].also {
                        it.state.value = state
                        it.contents = contents
                    }
                }

                ordnanceCounts.clear()
                tubeStates.fill(null)
                tubeContents.fill(null)
            }
        }

        data object Upgrades : Dsl() {
            var doubleAgentActive: BoolState = BoolState.Unknown
            var doubleAgentCount: Byte = -1
            var doubleAgentSecondsLeft: Int = -1

            override fun updates(obj: ArtemisPlayer) {
                require(!obj.hasUpgradeData) { "Cannot apply Dsl to an already-populated object" }

                obj.doubleAgentActive.value = doubleAgentActive
                obj.doubleAgentCount.value = doubleAgentCount
                obj.doubleAgentSecondsLeft.value = doubleAgentSecondsLeft

                doubleAgentActive = BoolState.Unknown
                doubleAgentCount = -1
                doubleAgentSecondsLeft = -1
            }
        }

        final override fun create(id: Int, timestamp: Long) = ArtemisPlayer(id, timestamp)
    }
}
