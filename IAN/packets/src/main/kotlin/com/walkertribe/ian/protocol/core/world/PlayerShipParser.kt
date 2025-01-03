package com.walkertribe.ian.protocol.core.world

import com.walkertribe.ian.enums.AlertStatus
import com.walkertribe.ian.enums.DriveType
import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.iface.PacketReader
import com.walkertribe.ian.util.Bit
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.world.ArtemisPlayer

/**
 * ObjectParser implementation for player ships
 * @author rjwut
 */
object PlayerShipParser : AbstractObjectParser<ArtemisPlayer>(ObjectType.PLAYER_SHIP) {
    private enum class PlayerBit : Bit {
        WEAPONS_TARGET,
        IMPULSE,
        RUDDER,
        TOP_SPEED,
        TURN_RATE,
        TARGETING_MODE,
        WARP,
        ENERGY,

        SHIELD_STATE,
        UNK_2_2,
        SHIP_TYPE,
        X,
        Y,
        Z,
        PITCH,
        ROLL,

        HEADING,
        VELOCITY,
        NEBULA_TYPE,
        NAME,
        FORE_SHIELDS,
        FORE_SHIELDS_MAX,
        AFT_SHIELDS,
        AFT_SHIELDS_MAX,

        DOCKING_BASE,
        ALERT_STATUS,
        UNK_4_3,
        MAIN_SCREEN,
        BEAM_FREQUENCY,
        AVAILABLE_COOLANT_OR_MISSILES,
        SCIENCE_TARGET,
        CAPTAIN_TARGET,

        DRIVE_TYPE,
        SCAN_OBJECT_ID,
        SCAN_PROGRESS,
        REVERSE_STATE,
        CLIMB_DIVE,
        SIDE,
        VISIBILITY,
        SHIP_INDEX,

        CAPITAL_SHIP_ID,
        ACCENT_COLOR {
            override fun getIndex(version: Version): Int =
                if (version < Version.ACCENT_COLOR) -1 else ordinal
        },
        EMERGENCY_JUMP_COOLDOWN {
            override fun getIndex(version: Version): Int =
                if (version < Version.ACCENT_COLOR) -1 else ordinal
        },
        BEACON_TYPE {
            override fun getIndex(version: Version): Int =
                if (version < Version.BEACON) -1 else ordinal
        },
        BEACON_MODE {
            override fun getIndex(version: Version): Int =
                if (version < Version.BEACON) -1 else ordinal
        };

        override fun getIndex(version: Version): Int = ordinal
    }

    override fun parseDsl(reader: PacketReader) = ArtemisPlayer.Dsl.Player.apply {
        reader.readInt(PlayerBit.WEAPONS_TARGET, -1)
        impulse = reader.readFloat(PlayerBit.IMPULSE)
        reader.readFloat(PlayerBit.RUDDER)
        reader.readFloat(PlayerBit.TOP_SPEED)
        reader.readFloat(PlayerBit.TURN_RATE)
        reader.readByte(PlayerBit.TARGETING_MODE)
        warp = reader.readByte(PlayerBit.WARP)
        reader.readFloat(PlayerBit.ENERGY)
        reader.readBool(PlayerBit.SHIELD_STATE, 2)
        reader.readBool(PlayerBit.UNK_2_2, Int.SIZE_BYTES)
        hullId = reader.readInt(PlayerBit.SHIP_TYPE, -1)
        x = reader.readFloat(PlayerBit.X)
        y = reader.readFloat(PlayerBit.Y)
        z = reader.readFloat(PlayerBit.Z)
        reader.readFloat(PlayerBit.PITCH)
        reader.readFloat(PlayerBit.ROLL)
        reader.readFloat(PlayerBit.HEADING)
        reader.readFloat(PlayerBit.VELOCITY)
        reader.readBool(
            PlayerBit.NEBULA_TYPE,
            if (reader.version < Version.NEBULA_TYPES) 2 else 1
        )
        name = reader.readString(PlayerBit.NAME)
        shieldsFront = reader.readFloat(PlayerBit.FORE_SHIELDS)
        shieldsFrontMax = reader.readFloat(PlayerBit.FORE_SHIELDS_MAX)
        shieldsRear = reader.readFloat(PlayerBit.AFT_SHIELDS)
        shieldsRearMax = reader.readFloat(PlayerBit.AFT_SHIELDS_MAX)
        dockingBase = reader.readInt(PlayerBit.DOCKING_BASE, -1)
        alertStatus = reader.readByteAsEnum<AlertStatus>(PlayerBit.ALERT_STATUS)
        reader.readBool(PlayerBit.UNK_4_3, Float.SIZE_BYTES)
        reader.readByte(PlayerBit.MAIN_SCREEN)
        reader.readByte(PlayerBit.BEAM_FREQUENCY)
        reader.readByte(PlayerBit.AVAILABLE_COOLANT_OR_MISSILES)
        reader.readInt(PlayerBit.SCIENCE_TARGET, -1)
        reader.readInt(PlayerBit.CAPTAIN_TARGET, -1)
        driveType = reader.readByteAsEnum<DriveType>(PlayerBit.DRIVE_TYPE)
        reader.readInt(PlayerBit.SCAN_OBJECT_ID, -1)
        reader.readFloat(PlayerBit.SCAN_PROGRESS)
        reader.readBool(PlayerBit.REVERSE_STATE, 1)
        reader.readFloat(PlayerBit.CLIMB_DIVE)
        side = reader.readByte(PlayerBit.SIDE)
        reader.readInt(PlayerBit.VISIBILITY, 0)
        shipIndex = reader.readByte(PlayerBit.SHIP_INDEX, Byte.MIN_VALUE)
        capitalShipID = reader.readInt(PlayerBit.CAPITAL_SHIP_ID, -1)
        reader.readFloat(PlayerBit.ACCENT_COLOR)
        reader.readFloat(PlayerBit.EMERGENCY_JUMP_COOLDOWN)
        reader.readByte(PlayerBit.BEACON_TYPE)
        reader.readByte(PlayerBit.BEACON_MODE)
    }

    override fun getBitCount(version: Version): Int = PlayerBit.entries.count {
        it.getIndex(version) >= 0
    }
}
