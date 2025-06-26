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

    private val manager =
        NotificationManagerCompat.from(context).apply {
            val groupSetups =
                listOf(
                    NotificationChannelGroupSetup(
                        CHANNEL_GROUP_IMPORTANT,
                        R.string.channel_group_important,
                        NotificationChannelSetup(
                            NotificationChannelTag.GAME_INFO,
                            R.string.channel_game_info,
                            NotificationManagerCompat.IMPORTANCE_LOW,
                            true,
                        ),
                        NotificationChannelSetup(
                            NotificationChannelTag.GAME_OVER,
                            R.string.channel_game_over,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                        NotificationChannelSetup(
                            NotificationChannelTag.CONNECTION,
                            R.string.channel_connection,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                        NotificationChannelSetup(
                            NotificationChannelTag.BORDER_WAR,
                            R.string.channel_border_war,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                    ),
                    NotificationChannelGroupSetup(
                        CHANNEL_GROUP_STATION,
                        R.string.channel_group_stations,
                        NotificationChannelSetup(
                            NotificationChannelTag.PRODUCTION,
                            R.string.channel_station_production,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                        NotificationChannelSetup(
                            NotificationChannelTag.ATTACK,
                            R.string.channel_station_attack,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                        NotificationChannelSetup(
                            NotificationChannelTag.DESTROYED,
                            R.string.channel_station_destroyed,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                    ),
                    NotificationChannelGroupSetup(
                        CHANNEL_GROUP_MISSION,
                        R.string.channel_group_missions,
                        NotificationChannelSetup(
                            NotificationChannelTag.NEW_MISSION,
                            R.string.channel_mission_new,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                        NotificationChannelSetup(
                            NotificationChannelTag.MISSION_PROGRESS,
                            R.string.channel_mission_progress,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                        NotificationChannelSetup(
                            NotificationChannelTag.MISSION_COMPLETED,
                            R.string.channel_mission_completed,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                    ),
                    NotificationChannelGroupSetup(
                        CHANNEL_GROUP_BIOMECH,
                        R.string.channel_group_biomechs,
                        NotificationChannelSetup(
                            NotificationChannelTag.REANIMATE,
                            R.string.channel_biomech_moving,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                    ),
                    NotificationChannelGroupSetup(
                        CHANNEL_GROUP_ALLIES,
                        R.string.channel_group_allies,
                        NotificationChannelSetup(
                            NotificationChannelTag.DEEP_STRIKE,
                            R.string.channel_allies_deep_string,
                            NotificationManagerCompat.IMPORTANCE_LOW,
                        ),
                    ),
                    NotificationChannelGroupSetup(
                        CHANNEL_GROUP_ENEMIES,
                        R.string.channel_group_enemies,
                        NotificationChannelSetup(
                            NotificationChannelTag.PERFIDY,
                            R.string.channel_enemies_perfidy,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                    ),
                )

            createNotificationChannelGroupsCompat(
                groupSetups.map { group ->
                    NotificationChannelGroupCompat.Builder(group.id)
                        .setName(context.getString(group.nameID))
                        .build()
                }
            )

            createNotificationChannelsCompat(
                groupSetups.flatMap { group ->
                    group.channels.map { channel ->
                        NotificationChannelCompat.Builder(channel.id.tag, channel.importance)
                            .setGroup(group.id)
                            .setName(context.getString(channel.nameID))
                            .setShowBadge(channel.shouldShowBadge)
                            .build()
                    }
                }
            )
        }

    fun createNotification(
        builder: NotificationCompat.Builder,
        info: NotificationInfo,
        context: Context,
    ) {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val (channel, title, message, ongoing) = info
        builder
            .setStyle(NotificationCompat.BigTextStyle().bigText(message).setBigContentTitle(title))
            .setContentTitle(title)
            .setContentText(message)
            .setOngoing(ongoing)

        val index =
            when (channel) {
                NotificationChannelTag.ATTACK -> {
                    attackedStations.getOrPut(title) { attackedStations.size }
                }
                NotificationChannelTag.REANIMATE -> {
                    biomechs.getOrPut(title) { biomechs.size }
                }
                NotificationChannelTag.PERFIDY -> {
                    enemies.getOrPut(title) { enemies.size }
                }
                NotificationChannelTag.DESTROYED -> destroyedStations++
                NotificationChannelTag.NEW_MISSION -> newMissionMessages++
                NotificationChannelTag.MISSION_PROGRESS -> progressMessages++
                NotificationChannelTag.MISSION_COMPLETED -> completionMessages++
                NotificationChannelTag.PRODUCTION -> {
                    val stationIndex = production.getOrPut(title) { production.size }
                    val ordnanceName = message.substring(ORDNANCE_NAME_INDEX, message.indexOf('.'))
                    val ordnanceType = OrdnanceType.entries.find { it.hasLabel(ordnanceName) }
                    stationIndex * ORDNANCE_SIZE + (ordnanceType?.ordinal ?: 0)
                }
                else -> 0
            }

        manager.notify(channel.tag, index, builder.build())
    }

    fun dismissBiomechMessage(name: String) {
        biomechs[name]?.also { manager.cancel(NotificationChannelTag.REANIMATE.tag, it) }
    }

    fun dismissPerfidyMessage(name: String) {
        enemies[name]?.also { manager.cancel(NotificationChannelTag.PERFIDY.tag, it) }
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
        val id: NotificationChannelTag,
        @field:StringRes val nameID: Int,
        val importance: Int,
        val shouldShowBadge: Boolean = false,
    )

    class NotificationChannelGroupSetup(
        val id: String,
        @field:StringRes val nameID: Int,
        vararg val channels: NotificationChannelSetup,
    )

    companion object {
        const val CHANNEL_GROUP_BIOMECH = "biomech"
        const val CHANNEL_GROUP_STATION = "station"
        const val CHANNEL_GROUP_MISSION = "mission"
        const val CHANNEL_GROUP_ALLIES = "allies"
        const val CHANNEL_GROUP_ENEMIES = "enemies"
        const val CHANNEL_GROUP_IMPORTANT = "important"

        private const val ORDNANCE_NAME_INDEX = 23
        private val ORDNANCE_SIZE = OrdnanceType.entries.size
    }
}
