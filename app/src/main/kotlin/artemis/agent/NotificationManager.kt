package artemis.agent

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.walkertribe.ian.enums.OrdnanceType

class NotificationManager(context: Context) {
    private val attackedStations = mutableMapOf<String, Int>()
    private val biomechs = mutableMapOf<String, Int>()
    private val enemies = mutableMapOf<String, Int>()
    private var destroyedStations = 0
    private var newMissionMessages = 0
    private var progressMessages = 0
    private var completionMessages = 0
    private val production = mutableMapOf<String, Int>()

    private val manager = NotificationManagerCompat.from(context).apply {
        val groupSetups = listOf(
            NotificationChannelGroupSetup(
                CHANNEL_GROUP_IMPORTANT,
                R.string.channel_group_important,
                NotificationChannelSetup(
                    CHANNEL_GAME_INFO,
                    R.string.channel_game_info,
                    NotificationManagerCompat.IMPORTANCE_LOW,
                ),
                NotificationChannelSetup(
                    CHANNEL_GAME_OVER,
                    R.string.channel_game_over,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                ),
                NotificationChannelSetup(
                    CHANNEL_CONNECTION,
                    R.string.channel_connection,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                ),
            ),
            NotificationChannelGroupSetup(
                CHANNEL_GROUP_STATION,
                R.string.channel_group_stations,
                NotificationChannelSetup(
                    CHANNEL_PRODUCTION,
                    R.string.channel_station_production,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                ),
                NotificationChannelSetup(
                    CHANNEL_ATTACK,
                    R.string.channel_station_attack,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                ),
                NotificationChannelSetup(
                    CHANNEL_DESTROYED,
                    R.string.channel_station_destroyed,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                ),
            ),
            NotificationChannelGroupSetup(
                CHANNEL_GROUP_MISSION,
                R.string.channel_group_missions,
                NotificationChannelSetup(
                    CHANNEL_NEW_MISSION,
                    R.string.channel_mission_new,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                ),
                NotificationChannelSetup(
                    CHANNEL_MISSION_PROGRESS,
                    R.string.channel_mission_progress,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                ),
                NotificationChannelSetup(
                    CHANNEL_MISSION_COMPLETED,
                    R.string.channel_mission_completed,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                ),
            ),
            NotificationChannelGroupSetup(
                CHANNEL_GROUP_BIOMECH,
                R.string.channel_group_biomechs,
                NotificationChannelSetup(
                    CHANNEL_REANIMATE,
                    R.string.channel_biomech_moving,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                ),
            ),
            NotificationChannelGroupSetup(
                CHANNEL_GROUP_ALLIES,
                R.string.channel_group_allies,
                NotificationChannelSetup(
                    CHANNEL_DEEP_STRIKE,
                    R.string.channel_allies_deep_string,
                    NotificationManagerCompat.IMPORTANCE_LOW,
                ),
            ),
            NotificationChannelGroupSetup(
                CHANNEL_GROUP_ENEMIES,
                R.string.channel_group_enemies,
                NotificationChannelSetup(
                    CHANNEL_PERFIDY,
                    R.string.channel_enemies_perfidy,
                    NotificationManagerCompat.IMPORTANCE_HIGH,
                ),
            ),
        )

        createNotificationChannelGroupsCompat(
            groupSetups.map {
                NotificationChannelGroupCompat.Builder(it.id)
                    .setName(context.getString(it.nameID))
                    .build()
            }
        )

        createNotificationChannelsCompat(
            groupSetups.flatMap { group ->
                group.channels.map {
                    NotificationChannelCompat.Builder(it.id, it.importance)
                        .setGroup(group.id).setName(context.getString(it.nameID))
                        .build()
                }
            }
        )
    }

    fun createNotification(
        builder: NotificationCompat.Builder,
        channelId: String,
        title: String,
        message: String,
        context: Context,
    ) {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        builder.setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(message)
                .setBigContentTitle(title)
        ).setContentTitle(title).setContentText(message)

        val index = when (channelId) {
            CHANNEL_ATTACK -> {
                attackedStations.getOrPut(title) { attackedStations.size }
            }
            CHANNEL_REANIMATE -> {
                biomechs.getOrPut(title) { biomechs.size }
            }
            CHANNEL_PERFIDY -> {
                enemies.getOrPut(title) { enemies.size }
            }
            CHANNEL_DESTROYED -> destroyedStations++
            CHANNEL_NEW_MISSION -> newMissionMessages++
            CHANNEL_MISSION_PROGRESS -> progressMessages++
            CHANNEL_MISSION_COMPLETED -> completionMessages++
            CHANNEL_PRODUCTION -> {
                val stationIndex = production.getOrPut(title) { production.size }
                val ordnanceName = message.substring(ORDNANCE_NAME_INDEX, message.indexOf('.'))
                val ordnanceType = OrdnanceType.entries.find { it.hasLabel(ordnanceName) }
                stationIndex * ORDNANCE_SIZE + (ordnanceType?.ordinal ?: 0)
            }
            else -> 0
        }

        manager.notify(channelId, index, builder.build())
    }

    fun dismissBiomechMessage(name: String) {
        biomechs[name]?.also {
            manager.cancel(CHANNEL_REANIMATE, it)
        }
    }

    fun dismissPerfidyMessage(name: String) {
        enemies[name]?.also {
            manager.cancel(CHANNEL_PERFIDY, it)
        }
    }

    fun reset() {
        production.clear()
        attackedStations.clear()
        biomechs.clear()
        enemies.clear()
        destroyedStations = 0
        newMissionMessages = 0
        progressMessages = 0
        completionMessages = 0
        manager.cancelAll()
    }

    data class NotificationChannelSetup(
        val id: String,
        @StringRes val nameID: Int,
        val importance: Int,
    )

    class NotificationChannelGroupSetup(
        val id: String,
        @StringRes val nameID: Int,
        vararg val channels: NotificationChannelSetup,
    )

    companion object {
        const val CHANNEL_GROUP_BIOMECH = "biomech"
        const val CHANNEL_GROUP_STATION = "station"
        const val CHANNEL_GROUP_MISSION = "mission"
        const val CHANNEL_GROUP_ALLIES = "allies"
        const val CHANNEL_GROUP_ENEMIES = "enemies"
        const val CHANNEL_GROUP_IMPORTANT = "important"

        const val CHANNEL_GAME_INFO = "game info"
        const val CHANNEL_CONNECTION = "connection"
        const val CHANNEL_GAME_OVER = "game over"
        const val CHANNEL_DEEP_STRIKE = "deep strike"
        const val CHANNEL_NEW_MISSION = "new mission"
        const val CHANNEL_MISSION_PROGRESS = "mission progress"
        const val CHANNEL_MISSION_COMPLETED = "mission completed"
        const val CHANNEL_PRODUCTION = "production"
        const val CHANNEL_ATTACK = "attack"
        const val CHANNEL_DESTROYED = "destroyed"
        const val CHANNEL_REANIMATE = "reanimate"
        const val CHANNEL_PERFIDY = "perfidy"

        private const val ORDNANCE_NAME_INDEX = 23
        private val ORDNANCE_SIZE = OrdnanceType.entries.size
    }
}
