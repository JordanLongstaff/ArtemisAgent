package artemis.agent

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object UserSettingsSerializer : Serializer<UserSettingsOuterClass.UserSettings> {
    private const val USER_SETTINGS_FILE_NAME = "user_settings.pb"

    const val DEFAULT_SERVER_PORT = 2010
    const val DEFAULT_CONNECTION_TIMEOUT = 9
    const val DEFAULT_HEARTBEAT_TIMEOUT = 15
    const val DEFAULT_SCAN_TIMEOUT = 5
    const val DEFAULT_ADDRESS_LIMIT = 20
    const val DEFAULT_AUTO_DISMISSAL_SECONDS = 3
    const val DEFAULT_FREEZE_DURATION = 220
    const val DEFAULT_BLACK_HOLE_CLEARANCE = 500
    const val DEFAULT_MINE_CLEARANCE = 1000
    const val DEFAULT_TYPHON_CLEARANCE = 3000
    const val DEFAULT_SOUND_VOLUME = 100
    const val DEFAULT_UPDATE_INTERVAL = 50
    const val DEFAULT_SURRENDER_RANGE = 5000

    val Context.userSettings by dataStore(
        fileName = USER_SETTINGS_FILE_NAME,
        serializer = this,
    )

    override suspend fun readFrom(input: InputStream): UserSettingsOuterClass.UserSettings {
        try {
            return UserSettingsOuterClass.UserSettings.parseFrom(input)
        } catch (ex: InvalidProtocolBufferException) {
            throw CorruptionException("Could not read user settings.", ex)
        }
    }

    override suspend fun writeTo(t: UserSettingsOuterClass.UserSettings, output: OutputStream) {
        t.writeTo(output)
    }

    override val defaultValue: UserSettingsOuterClass.UserSettings = userSettings {
        vesselDataLocation = UserSettingsOuterClass.UserSettings.VesselDataLocation
            .VESSEL_DATA_LOCATION_DEFAULT
        serverPort = DEFAULT_SERVER_PORT
        recentAddressLimit = DEFAULT_ADDRESS_LIMIT
        recentAddressLimitEnabled = false
        updateInterval = DEFAULT_UPDATE_INTERVAL

        connectionTimeoutSeconds = DEFAULT_CONNECTION_TIMEOUT
        serverTimeoutSeconds = DEFAULT_HEARTBEAT_TIMEOUT
        scanTimeoutSeconds = DEFAULT_SCAN_TIMEOUT

        missionsEnabled = true

        displayRewardBattery = true
        displayRewardCoolant = true
        displayRewardNukes = true
        displayRewardProduction = true
        displayRewardShield = true

        completedMissionDismissalEnabled = true
        completedMissionDismissalSeconds = DEFAULT_AUTO_DISMISSAL_SECONDS

        alliesEnabled = true

        allySortClassFirst = false
        allySortStatus = false
        allySortClassSecond = false
        allySortName = false
        allySortEnergyFirst = false

        allyCommandManualReturn = false
        showDestroyedAllies = true

        enemiesEnabled = true

        enemySortFaction = false
        enemySortFactionReversed = false
        enemySortName = false
        enemySortDistance = false
        enemySortSurrendered = false

        surrenderRange = DEFAULT_SURRENDER_RANGE
        surrenderRangeEnabled = true

        showEnemyIntel = true
        showTauntStatuses = true
        disableIneffectiveTaunts = true

        biomechsEnabled = true

        biomechSortClassFirst = false
        biomechSortStatus = false
        biomechSortClassSecond = false
        biomechSortName = false

        freezeDurationSeconds = DEFAULT_FREEZE_DURATION

        routingEnabled = true

        routeMissions = true
        routeNeedsDamcon = true
        routeNeedsEnergy = true
        routeHasEnergy = true
        routeMalfunction = true
        routeAmbassador = true
        routeHostage = true
        routeCommandeered = true

        avoidBlackHoles = true
        avoidMines = true
        avoidTyphon = true

        blackHoleClearance = DEFAULT_BLACK_HOLE_CLEARANCE
        mineClearance = DEFAULT_MINE_CLEARANCE
        typhonClearance = DEFAULT_TYPHON_CLEARANCE

        theme = UserSettingsOuterClass.UserSettings.Theme.THEME_DEFAULT
        threeDigitDirections = true
        soundVolume = DEFAULT_SOUND_VOLUME
    }
}
