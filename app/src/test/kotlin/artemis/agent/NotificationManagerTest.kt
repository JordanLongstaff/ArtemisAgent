package artemis.agent

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.walkertribe.ian.enums.OrdnanceType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeSameSizeAs
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.ints.shouldBeZero
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll

class NotificationManagerTest :
    DescribeSpec({
        describe("NotificationManager") {
            val groups =
                listOf(
                    NotificationTestChannelGroup(
                        "important",
                        "Important",
                        R.string.channel_group_important,
                        NotificationTestChannel(
                            "game info",
                            "Game info",
                            R.string.channel_game_info,
                            NotificationManagerCompat.IMPORTANCE_LOW,
                        ),
                        NotificationTestChannel(
                            "game over",
                            "Game over",
                            R.string.channel_game_over,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                        NotificationTestChannel(
                            "connection",
                            "Connection",
                            R.string.channel_connection,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                        NotificationTestChannel(
                            "border war",
                            "Border war",
                            R.string.channel_border_war,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                    ),
                    NotificationTestChannelGroup(
                        "station",
                        "Stations",
                        R.string.channel_group_stations,
                        NotificationTestChannel(
                            "production",
                            "Missile production",
                            R.string.channel_station_production,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                        NotificationTestChannel(
                            "attack",
                            "Station under attack",
                            R.string.channel_station_attack,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                        NotificationTestChannel(
                            "destroyed",
                            "Station destroyed",
                            R.string.channel_station_destroyed,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                    ),
                    NotificationTestChannelGroup(
                        "mission",
                        "Missions",
                        R.string.channel_group_missions,
                        NotificationTestChannel(
                            "new mission",
                            "New mission",
                            R.string.channel_mission_new,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                        NotificationTestChannel(
                            "mission progress",
                            "Mission progress",
                            R.string.channel_mission_progress,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                        NotificationTestChannel(
                            "mission completed",
                            "Mission completed",
                            R.string.channel_mission_completed,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                    ),
                    NotificationTestChannelGroup(
                        "biomech",
                        "Biomechs",
                        R.string.channel_group_biomechs,
                        NotificationTestChannel(
                            "reanimate",
                            "Reanimation alert",
                            R.string.channel_biomech_moving,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                    ),
                    NotificationTestChannelGroup(
                        "allies",
                        "Allies",
                        R.string.channel_group_allies,
                        NotificationTestChannel(
                            "deep strike",
                            "Deep Strike production",
                            R.string.channel_allies_deep_strike,
                            NotificationManagerCompat.IMPORTANCE_LOW,
                        ),
                    ),
                    NotificationTestChannelGroup(
                        "enemies",
                        "Enemies",
                        R.string.channel_group_enemies,
                        NotificationTestChannel(
                            "perfidy",
                            "Perfidy alert",
                            R.string.channel_enemies_perfidy,
                            NotificationManagerCompat.IMPORTANCE_HIGH,
                        ),
                    ),
                )

            val tagSlot = slot<String>()
            val indexSlot = slot<Int>()

            val titleSlot = slot<CharSequence>()
            val textSlot = slot<CharSequence>()
            val ongoingSlot = slot<Boolean>()

            val channelGroups = mutableListOf<NotificationChannelGroupCompat>()
            val channels = mutableListOf<NotificationChannelCompat>()

            val notifications = mutableMapOf<String, Int>()
            var notificationCount = 0

            val mockContext =
                mockk<Context> {
                    groups.forEach { group ->
                        every { getString(group.nameRes) } returns group.name
                        group.channels.forEach { channel ->
                            every { getString(channel.nameRes) } returns channel.name
                        }
                    }

                    every { getSystemService(any()) } returns
                        mockk<android.app.NotificationManager>()
                }

            mockkStatic(ContextCompat::class)

            mockkStatic(NotificationManagerCompat::class)
            val mockNotificationManager =
                mockk<NotificationManagerCompat> {
                    every { createNotificationChannelGroupsCompat(any()) } answers
                        {
                            channelGroups.addAll(firstArg())
                        }
                    every { createNotificationChannelsCompat(any()) } answers
                        {
                            channels.addAll(firstArg())
                        }

                    every { notify(capture(tagSlot), capture(indexSlot), any()) } answers
                        {
                            notifications[tagSlot.captured] = indexSlot.captured
                        }

                    every { cancel(capture(tagSlot), any()) } answers
                        {
                            notifications.remove(tagSlot.captured)
                        }

                    every { cancelAll() } answers
                        {
                            notifications.clear()
                            tagSlot.clear()
                            indexSlot.clear()
                        }
                }
            every { NotificationManagerCompat.from(any()) } returns mockNotificationManager

            val notificationBuilder =
                mockk<NotificationCompat.Builder> {
                    every { setStyle(any()) } returns this
                    every { setContentTitle(capture(titleSlot)) } returns this
                    every { setContentText(capture(textSlot)) } returns this
                    every { setOngoing(capture(ongoingSlot)) } returns this
                    every { build() } returns mockk()
                }

            afterSpec {
                clearAllMocks()
                unmockkAll()
            }

            val notificationManager = NotificationManager(mockContext)

            describe("Initial state") {
                it("No attacked stations") { notificationManager.attackedStations.shouldBeEmpty() }

                it("No biomechs") { notificationManager.biomechs.shouldBeEmpty() }

                it("No enemies") { notificationManager.enemies.shouldBeEmpty() }

                it("No destroyed stations") { notificationManager.destroyedStations.shouldBeZero() }

                it("No new missions") { notificationManager.newMissionMessages.shouldBeZero() }

                it("No missions in progress") {
                    notificationManager.progressMessages.shouldBeZero()
                }

                it("No completed missions") {
                    notificationManager.completionMessages.shouldBeZero()
                }

                it("No missile production messages") {
                    notificationManager.production.shouldBeEmpty()
                }
            }

            describe("Notification manager setup") {
                notificationManager.manager shouldBe mockNotificationManager
            }

            describe("Channel setup") {
                it("All groups") {
                    channelGroups shouldBeSameSizeAs groups

                    val groupMap = groups.associateBy { it.id }
                    channelGroups.forEach { group ->
                        val expectedGroup = groupMap[group.id]!!
                        group.name.shouldNotBeNull() shouldBeEqual expectedGroup.name
                    }
                }

                it("All channels") {
                    val testChannels =
                        groups.flatMap { group -> group.channels.map { it to group.id } }
                    channels shouldBeSameSizeAs testChannels

                    val channelMap = testChannels.associateBy { it.first.id }
                    channels.forEach { channel ->
                        val (expectedChannel, expectedGroup) = channelMap[channel.id]!!
                        channel.name.shouldNotBeNull() shouldBeEqual expectedChannel.name
                        channel.importance shouldBeEqual expectedChannel.importance
                        channel.group.shouldNotBeNull() shouldBeEqual expectedGroup

                        val canShowBadge = expectedChannel.id == "game info"
                        channel.canShowBadge() shouldBeEqual canShowBadge
                    }
                }
            }

            every { ContextCompat.checkSelfPermission(any(), any()) } returns
                PackageManager.PERMISSION_DENIED

            it("Requires permission") {
                checkAll<NotificationInfo> { info ->
                    notificationManager.createNotification(notificationBuilder, info, mockContext)
                    tagSlot.isCaptured.shouldBeFalse()
                    indexSlot.isCaptured.shouldBeFalse()
                }
            }

            every { ContextCompat.checkSelfPermission(any(), any()) } returns
                PackageManager.PERMISSION_GRANTED

            describe("Important messages") {
                withData(
                    nameFn = { it.second },
                    NotificationChannelTag.GAME_INFO to "Game info",
                    NotificationChannelTag.GAME_OVER to "Game over",
                    NotificationChannelTag.CONNECTION to "Connection",
                    NotificationChannelTag.BORDER_WAR to "Border war",
                ) { (channel, tag) ->
                    notificationCount++

                    withData("First time", "Second time") { _ ->
                        checkAll<String, String, Boolean>(iterations = 1) { title, text, ongoing ->
                            val info = NotificationInfo(channel, title, text, ongoing)
                            notificationManager.createNotification(
                                notificationBuilder,
                                info,
                                mockContext,
                            )

                            tagSlot.captured shouldBeEqual tag.lowercase()
                            titleSlot.captured shouldBeEqual title
                            textSlot.captured shouldBeEqual text
                            ongoingSlot.captured shouldBeEqual ongoing
                            indexSlot.captured.shouldBeZero()

                            notifications shouldHaveSize notificationCount
                        }
                    }
                }
            }

            describe("Station messages") {
                describe("Production") {
                    notificationCount++

                    withData(nameFn = { it.first }, "DS1" to 0, "DS1" to 0, "DS2" to 1) {
                        (station, index) ->
                        checkAll<OrdnanceType, Boolean>(iterations = 1) { ordnance, ongoing ->
                            val message = "We've produced another $ordnance."
                            val info =
                                NotificationInfo(
                                    NotificationChannelTag.PRODUCTION,
                                    station,
                                    message,
                                    ongoing,
                                )
                            notificationManager.createNotification(
                                notificationBuilder,
                                info,
                                mockContext,
                            )

                            tagSlot.captured shouldBeEqual "production"
                            titleSlot.captured shouldBeEqual station
                            textSlot.captured shouldBeEqual message
                            ongoingSlot.captured shouldBeEqual ongoing
                            indexSlot.captured.shouldBeEqual(index * 8 + ordnance.ordinal)

                            notificationManager.production shouldHaveSize index + 1
                            notificationManager.production.shouldContain(station, index)

                            notifications shouldHaveSize notificationCount
                        }
                    }
                }

                describe("Attack") {
                    notificationCount++

                    withData(nameFn = { it.first }, "DS1" to 0, "DS1" to 0, "DS2" to 1) {
                        (station, index) ->
                        checkAll<String, Boolean>(iterations = 1) { message, ongoing ->
                            val info =
                                NotificationInfo(
                                    NotificationChannelTag.ATTACK,
                                    station,
                                    message,
                                    ongoing,
                                )
                            notificationManager.createNotification(
                                notificationBuilder,
                                info,
                                mockContext,
                            )

                            tagSlot.captured shouldBeEqual "attack"
                            titleSlot.captured shouldBeEqual station
                            textSlot.captured shouldBeEqual message
                            ongoingSlot.captured shouldBeEqual ongoing
                            indexSlot.captured shouldBeEqual index

                            notificationManager.attackedStations shouldHaveSize index + 1
                            notificationManager.attackedStations.shouldContain(station, index)

                            notifications shouldHaveSize notificationCount
                        }
                    }
                }

                describe("Destroyed") {
                    notificationCount++

                    listOf("DS1", "DS2").forEachIndexed { index, station ->
                        it(station) {
                            checkAll<String, Boolean>(iterations = 1) { message, ongoing ->
                                val info =
                                    NotificationInfo(
                                        NotificationChannelTag.DESTROYED,
                                        station,
                                        message,
                                        ongoing,
                                    )
                                notificationManager.createNotification(
                                    notificationBuilder,
                                    info,
                                    mockContext,
                                )

                                tagSlot.captured shouldBeEqual "destroyed"
                                titleSlot.captured shouldBeEqual station
                                textSlot.captured shouldBeEqual message
                                ongoingSlot.captured shouldBeEqual ongoing
                                indexSlot.captured shouldBeEqual index

                                notificationManager.destroyedStations shouldBeEqual index + 1

                                notifications shouldHaveSize notificationCount
                            }
                        }
                    }
                }
            }

            describe("Mission messages") {
                withData(
                    nameFn = { it.second },
                    Triple(
                        NotificationChannelTag.NEW_MISSION,
                        "New mission",
                        notificationManager::newMissionMessages,
                    ),
                    Triple(
                        NotificationChannelTag.MISSION_PROGRESS,
                        "Mission progress",
                        notificationManager::progressMessages,
                    ),
                    Triple(
                        NotificationChannelTag.MISSION_COMPLETED,
                        "Mission completed",
                        notificationManager::completionMessages,
                    ),
                ) { (channel, tag, counter) ->
                    notificationCount++

                    listOf("First", "Second").forEachIndexed { index, ordinal ->
                        it("$ordinal time") {
                            checkAll<String, String, Boolean>(iterations = 1) {
                                title,
                                message,
                                ongoing ->
                                val info = NotificationInfo(channel, title, message, ongoing)
                                notificationManager.createNotification(
                                    notificationBuilder,
                                    info,
                                    mockContext,
                                )

                                tagSlot.captured shouldBeEqual tag.lowercase()
                                titleSlot.captured shouldBeEqual title
                                textSlot.captured shouldBeEqual message
                                ongoingSlot.captured shouldBeEqual ongoing
                                indexSlot.captured shouldBeEqual index

                                counter.get() shouldBeEqual index + 1

                                notifications shouldHaveSize notificationCount
                            }
                        }
                    }
                }
            }

            withData(
                nameFn = { "${it.first} messages" },
                Triple(
                    "BioMech",
                    NotificationChannelTag.REANIMATE to "Reanimate",
                    notificationManager.biomechs to notificationManager::dismissBiomechMessage,
                ),
                Triple(
                    "Enemy",
                    NotificationChannelTag.PERFIDY to "Perfidy",
                    notificationManager.enemies to notificationManager::dismissPerfidyMessage,
                ),
            ) { (_, tag, data) ->
                val (channel, test) = tag
                val (counterMap, dismiss) = data
                describe(test) {
                    describe("Create notification") {
                        withData(nameFn = { it.first }, "A01" to 0, "A01" to 0, "B02" to 1) {
                            (name, index) ->
                            checkAll<String, Boolean>(iterations = 1) { message, ongoing ->
                                val info = NotificationInfo(channel, name, message, ongoing)
                                notificationManager.createNotification(
                                    notificationBuilder,
                                    info,
                                    mockContext,
                                )

                                tagSlot.captured shouldBeEqual test.lowercase()
                                titleSlot.captured shouldBeEqual name
                                textSlot.captured shouldBeEqual message
                                ongoingSlot.captured shouldBeEqual ongoing
                                indexSlot.captured shouldBeEqual index

                                counterMap shouldHaveSize index + 1
                                counterMap.shouldContain(name, index)

                                notifications shouldHaveSize notificationCount + 1
                            }
                        }
                    }

                    describe("Dismiss notification") {
                        it("Name not found") {
                            dismiss("A02")
                            notifications shouldHaveSize notificationCount + 1
                        }

                        it("Name found") {
                            dismiss("A01")
                            counterMap shouldHaveSize 2
                            counterMap shouldContainKey "A01"
                            notifications shouldHaveSize notificationCount
                        }
                    }
                }
            }

            describe("Ally messages") {
                describe("Deep Strike") {
                    notificationCount++

                    withData("First time", "Second time") { _ ->
                        checkAll<String, String, Boolean>(iterations = 1) { title, text, ongoing ->
                            val info =
                                NotificationInfo(
                                    NotificationChannelTag.DEEP_STRIKE,
                                    title,
                                    text,
                                    ongoing,
                                )
                            notificationManager.createNotification(
                                notificationBuilder,
                                info,
                                mockContext,
                            )

                            tagSlot.captured shouldBeEqual "deep strike"
                            titleSlot.captured shouldBeEqual title
                            textSlot.captured shouldBeEqual text
                            ongoingSlot.captured shouldBeEqual ongoing
                            indexSlot.captured.shouldBeZero()

                            notifications shouldHaveSize notificationCount
                        }
                    }
                }
            }

            describe("Reset") {
                notificationManager.reset()

                it("Clears data") {
                    notificationManager.attackedStations.shouldBeEmpty()
                    notificationManager.biomechs.shouldBeEmpty()
                    notificationManager.enemies.shouldBeEmpty()
                    notificationManager.destroyedStations.shouldBeZero()
                    notificationManager.newMissionMessages.shouldBeZero()
                    notificationManager.progressMessages.shouldBeZero()
                    notificationManager.completionMessages.shouldBeZero()
                    notificationManager.production.shouldBeEmpty()
                }

                it("Clears notifications") {
                    notifications.shouldBeEmpty()
                    tagSlot.isCaptured.shouldBeFalse()
                    indexSlot.isCaptured.shouldBeFalse()
                }
            }
        }
    })
