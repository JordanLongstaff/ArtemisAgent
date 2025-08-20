package artemis.agent

import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class NotificationManagerTest {
    @Before
    fun beforeEach() {
        NotificationManager(context).reset()
    }

    @Test
    fun channelGroupsTest() {
        val channelGroups = notificationManager.notificationChannelGroups
        Assert.assertEquals(groups.size, channelGroups.size)

        val groupMap = groups.associateBy { it.id }
        channelGroups.forEach { actualGroup ->
            val expectedGroup =
                checkNotNull(groupMap[actualGroup.id]) {
                    "Channel group ID should not exist: ${actualGroup.id}"
                }
            Assert.assertEquals(expectedGroup.name, actualGroup.name)
            Assert.assertEquals(expectedGroup.channels.size, actualGroup.channels.size)
        }
    }

    @Test
    fun channelsTest() {
        val channels = notificationManager.notificationChannels
        val testChannels = groups.flatMap { group -> group.channels.map { it to group.id } }
        Assert.assertEquals(testChannels.size, channels.size)

        val channelMap = testChannels.associateBy { it.first.id }
        channels.forEach { actualChannel ->
            val (expectedChannel, expectedGroup) =
                checkNotNull(channelMap[actualChannel.id]) {
                    "Channel ID should not exist: ${actualChannel.id}"
                }
            Assert.assertEquals(expectedChannel.name, actualChannel.name)
            Assert.assertEquals(expectedChannel.importance, actualChannel.importance)
            Assert.assertEquals(expectedGroup, actualChannel.group)
            Assert.assertEquals(expectedChannel.id == "game info", actualChannel.canShowBadge())
        }
    }

    private companion object {
        val context by lazy {
            checkNotNull(InstrumentationRegistry.getInstrumentation().targetContext) {
                "Failed to get context"
            }
        }

        val notificationManager by lazy { NotificationManagerCompat.from(context) }

        val groups by lazy {
            listOf(
                NotificationTestChannelGroup(
                    "important",
                    "Important",
                    NotificationTestChannel(
                        "game info",
                        "Game info",
                        NotificationManagerCompat.IMPORTANCE_LOW,
                    ),
                    NotificationTestChannel(
                        "game over",
                        "Game over",
                        NotificationManagerCompat.IMPORTANCE_HIGH,
                    ),
                    NotificationTestChannel(
                        "connection",
                        "Connection",
                        NotificationManagerCompat.IMPORTANCE_HIGH,
                    ),
                    NotificationTestChannel(
                        "border war",
                        "Border war",
                        NotificationManagerCompat.IMPORTANCE_HIGH,
                    ),
                ),
                NotificationTestChannelGroup(
                    "station",
                    "Stations",
                    NotificationTestChannel(
                        "production",
                        "Missile production",
                        NotificationManagerCompat.IMPORTANCE_HIGH,
                    ),
                    NotificationTestChannel(
                        "attack",
                        "Station under attack",
                        NotificationManagerCompat.IMPORTANCE_HIGH,
                    ),
                    NotificationTestChannel(
                        "destroyed",
                        "Station destroyed",
                        NotificationManagerCompat.IMPORTANCE_HIGH,
                    ),
                ),
                NotificationTestChannelGroup(
                    "mission",
                    "Missions",
                    NotificationTestChannel(
                        "new mission",
                        "New mission",
                        NotificationManagerCompat.IMPORTANCE_HIGH,
                    ),
                    NotificationTestChannel(
                        "mission progress",
                        "Mission progress",
                        NotificationManagerCompat.IMPORTANCE_HIGH,
                    ),
                    NotificationTestChannel(
                        "mission completed",
                        "Mission completed",
                        NotificationManagerCompat.IMPORTANCE_HIGH,
                    ),
                ),
                NotificationTestChannelGroup(
                    "biomech",
                    "Biomechs",
                    NotificationTestChannel(
                        "reanimate",
                        "Reanimation alert",
                        NotificationManagerCompat.IMPORTANCE_HIGH,
                    ),
                ),
                NotificationTestChannelGroup(
                    "allies",
                    "Allies",
                    NotificationTestChannel(
                        "deep strike",
                        "Deep Strike production",
                        NotificationManagerCompat.IMPORTANCE_LOW,
                    ),
                ),
                NotificationTestChannelGroup(
                    "enemies",
                    "Enemies",
                    NotificationTestChannel(
                        "perfidy",
                        "Perfidy alert",
                        NotificationManagerCompat.IMPORTANCE_HIGH,
                    ),
                ),
            )
        }
    }
}
