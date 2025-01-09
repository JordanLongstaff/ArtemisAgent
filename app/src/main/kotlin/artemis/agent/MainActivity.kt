package artemis.agent

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import artemis.agent.UserSettingsSerializer.userSettings
import artemis.agent.databinding.ActivityMainBinding
import artemis.agent.game.GameFragment
import artemis.agent.game.stations.StationsFragment
import artemis.agent.help.HelpFragment
import artemis.agent.setup.ConnectFragment
import artemis.agent.setup.SetupFragment
import artemis.agent.util.SoundEffect
import artemis.agent.util.collectLatestWhileStarted
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.walkertribe.ian.iface.DisconnectCause
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket
import com.walkertribe.ian.util.Version
import java.io.FileNotFoundException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/** The main application activity. */
class MainActivity : AppCompatActivity() {
    private val viewModel: AgentViewModel by viewModels()

    /** UI sections selected by the three buttons at the bottom of the screen. */
    enum class Section(val sectionClass: Class<out Fragment>, @IdRes val buttonId: Int) {
        SETUP(SetupFragment::class.java, R.id.setupPageButton),
        GAME(GameFragment::class.java, R.id.gamePageButton),
        HELP(HelpFragment::class.java, R.id.helpPageButton),
    }

    private var currentSection: Section? = null
        set(newSection) {
            if (newSection != null && field != newSection) {
                field = newSection
                supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(R.id.fragmentContainer, newSection.sectionClass, null)
                }
            }
        }

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val reviewManager: ReviewManager by lazy { ReviewManagerFactory.create(this) }
    private var shouldAskForReview: Boolean = false

    private var notificationRequests = STOP_NOTIFICATIONS

    private val notificationManager: NotificationManager by lazy {
        NotificationManager(applicationContext)
    }
    private val requestPermissionLauncher: ActivityResultLauncher<String>? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (!granted && shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)) {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage(R.string.permission_rationale)
                        .setCancelable(false)
                        .setNegativeButton(R.string.no) { _, _ ->
                            viewModel.playSound(SoundEffect.BEEP_1)
                            requestPermissionLauncher?.launch(POST_NOTIFICATIONS)
                        }
                        .setPositiveButton(R.string.yes) { _, _ ->
                            viewModel.playSound(SoundEffect.BEEP_1)
                        }
                        .show()
                }
            }
        } else {
            null
        }
    }

    /** Connection to notification service. */
    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                binder as NotificationService.LocalBinder
                binder.notificationManager = notificationManager
                binder.viewModel = viewModel

                notificationRequests = 0

                binder.service?.also { service ->
                    createStationPacketListener(
                        service,
                        viewModel.stationProductionPacket,
                        NotificationManager.CHANNEL_PRODUCTION,
                    )
                    createStationPacketListener(
                        service,
                        viewModel.stationAttackedPacket,
                        NotificationManager.CHANNEL_ATTACK,
                    )
                    createStationPacketListener(
                        service,
                        viewModel.stationDestroyedPacket,
                        NotificationManager.CHANNEL_DESTROYED,
                        false,
                    )

                    createMissionPacketListener(
                        service,
                        viewModel.newMissionPacket,
                        NotificationManager.CHANNEL_NEW_MISSION,
                    )
                    createMissionPacketListener(
                        service,
                        viewModel.missionProgressPacket,
                        NotificationManager.CHANNEL_MISSION_PROGRESS,
                    )
                    createMissionPacketListener(
                        service,
                        viewModel.missionCompletionPacket,
                        NotificationManager.CHANNEL_MISSION_COMPLETED,
                    )

                    setupOngoingNotifications(service)
                    setupBiomechNotifications(service)
                    setupEnemyNotifications(service)
                    setupGameNotifications(service)
                    setupConnectionNotifications(service)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                notificationRequests = STOP_NOTIFICATIONS
                notificationManager.reset()
            }

            private fun setupOngoingNotifications(service: NotificationService) {
                service.collectLatestWhileStarted(viewModel.inventory) { inv ->
                    if (!viewModel.gameIsRunning.value) return@collectLatestWhileStarted

                    val strings =
                        inv.mapIndexedNotNull { index, i ->
                            i?.let {
                                resources.getQuantityString(
                                    AgentViewModel.PLURALS_FOR_INVENTORY[index],
                                    it,
                                    it,
                                )
                            }
                        }

                    buildNotification(
                        channelId = NotificationManager.CHANNEL_GAME_INFO,
                        title = viewModel.connectedUrl.value,
                        message = strings.joinToString(),
                        ongoing = true,
                        onIntent = { putExtra(Section.GAME.name, GAME_PAGE_UNSPECIFIED) },
                    )
                }

                service.collectLatestWhileStarted(viewModel.livingAllies) { allies ->
                    if (viewModel.stationsExist.value) return@collectLatestWhileStarted

                    allies.firstOrNull()?.also { ally ->
                        buildNotification(
                            channelId = NotificationManager.CHANNEL_DEEP_STRIKE,
                            title = viewModel.getFullNameForShip(ally.obj),
                            message =
                                if (viewModel.torpedoesReady) {
                                    getString(R.string.manufacturing_torpedoes_ready)
                                } else {
                                    viewModel.getManufacturingTimer(this@MainActivity)
                                },
                            ongoing = true,
                            onIntent = {
                                putExtra(Section.GAME.name, GameFragment.Page.ALLIES.ordinal)
                            },
                        )
                    }
                }
            }

            private fun setupBiomechNotifications(service: NotificationService) {
                service.collectLatestWhileStarted(viewModel.destroyedBiomechName) {
                    notificationManager.dismissBiomechMessage(it)
                }

                service.collectLatestWhileStarted(viewModel.nextActiveBiomech) { entry ->
                    buildNotification(
                        channelId = NotificationManager.CHANNEL_REANIMATE,
                        title = viewModel.getFullNameForShip(entry.biomech),
                        message = getString(R.string.biomech_notification),
                        onIntent = {
                            putExtra(Section.GAME.name, GameFragment.Page.BIOMECHS.ordinal)
                        },
                        setBuilder = { builder ->
                            if (entry.canFreezeAgain) {
                                val freezeIntent =
                                    Intent(this@MainActivity, NotificationService::class.java)
                                freezeIntent.putExtra(
                                    NotificationService.EXTRA_BIOMECH_ID,
                                    entry.biomech.id,
                                )
                                val actionIntent =
                                    PendingIntent.getService(
                                        this@MainActivity,
                                        entry.biomech.id,
                                        freezeIntent,
                                        PENDING_INTENT_FLAGS,
                                    )
                                builder.addAction(
                                    R.drawable.ic_stat_name,
                                    getString(R.string.refreeze),
                                    actionIntent,
                                )
                            }
                        },
                    )
                }
            }

            private fun setupEnemyNotifications(service: NotificationService) {
                service.collectLatestWhileStarted(viewModel.destroyedEnemyName) {
                    notificationManager.dismissPerfidyMessage(it)
                }

                service.collectLatestWhileStarted(viewModel.perfidiousEnemy) { entry ->
                    buildNotification(
                        channelId = NotificationManager.CHANNEL_PERFIDY,
                        title = viewModel.getFullNameForShip(entry.enemy),
                        message = getString(R.string.enemy_perfidy_notification),
                        onIntent = {
                            putExtra(Section.GAME.name, GameFragment.Page.ENEMIES.ordinal)
                        },
                    )
                }
            }

            private fun setupGameNotifications(service: NotificationService) {
                service.collectLatestWhileStarted(viewModel.borderWarMessage) { packet ->
                    buildNotification(
                        channelId = NotificationManager.CHANNEL_BORDER_WAR,
                        title = packet.sender,
                        message = packet.message,
                        onIntent = { putExtra(Section.GAME.name, GAME_PAGE_UNSPECIFIED) },
                    )
                }

                service.collectLatestWhileStarted(viewModel.gameOverReason) { reason ->
                    if (viewModel.gameIsRunning.value) return@collectLatestWhileStarted
                    buildNotification(
                        channelId = NotificationManager.CHANNEL_GAME_OVER,
                        title = viewModel.connectedUrl.value,
                        message = reason,
                        setBuilder = { notificationManager.reset() },
                    )
                }
            }

            private fun setupConnectionNotifications(service: NotificationService) {
                service.collectLatestWhileStarted(viewModel.disconnectCause) { cause ->
                    val message =
                        when (cause) {
                            is DisconnectCause.UnsupportedVersion ->
                                getString(R.string.artemis_version_not_supported, cause.version)
                            is DisconnectCause.PacketParseError ->
                                getString(R.string.io_parse_error)
                            is DisconnectCause.IOError -> getString(R.string.io_write_error)
                            is DisconnectCause.UnknownError -> getString(R.string.unknown_error)
                            is DisconnectCause.RemoteDisconnect ->
                                getString(R.string.connection_closed)
                            else -> return@collectLatestWhileStarted
                        }
                    buildNotification(
                        channelId = NotificationManager.CHANNEL_CONNECTION,
                        title = viewModel.connectedUrl.value,
                        message = message,
                        onIntent = {
                            putExtra(Section.SETUP.name, SetupFragment.Page.CONNECT.ordinal)
                        },
                        setBuilder = { notificationManager.reset() },
                    )
                }

                service.collectLatestWhileStarted(viewModel.connectionStatus) { status ->
                    val message =
                        when (status) {
                            is ConnectFragment.ConnectionStatus.NotConnected,
                            is ConnectFragment.ConnectionStatus.Connecting ->
                                return@collectLatestWhileStarted

                            else -> getString(status.stringId)
                        }

                    buildNotification(
                        channelId = NotificationManager.CHANNEL_CONNECTION,
                        title = viewModel.lastAttemptedHost,
                        message = message,
                        onIntent = {
                            putExtra(
                                Section.SETUP.name,
                                when (status) {
                                    is ConnectFragment.ConnectionStatus.Connected ->
                                        SetupFragment.Page.SHIPS.ordinal

                                    else -> SetupFragment.Page.CONNECT.ordinal
                                },
                            )
                        },
                    )
                }
            }

            private fun createMissionPacketListener(
                service: NotificationService,
                flow: MutableSharedFlow<CommsIncomingPacket>,
                channelId: String,
            ) {
                service.collectLatestWhileStarted(flow) { packet ->
                    buildNotification(
                        channelId = channelId,
                        title = packet.sender,
                        message = packet.message,
                        onIntent = {
                            putExtra(Section.GAME.name, GameFragment.Page.MISSIONS.ordinal)
                        },
                    )
                }
            }

            private fun createStationPacketListener(
                service: NotificationService,
                flow: MutableSharedFlow<CommsIncomingPacket>,
                channelId: String,
                includeSenderName: Boolean = true,
            ) {
                service.collectLatestWhileStarted(flow) { packet ->
                    buildNotification(
                        channelId = channelId,
                        title = packet.sender,
                        message = packet.message,
                        onIntent = {
                            putExtra(
                                GameFragment.Page.STATIONS.name,
                                if (includeSenderName) {
                                    packet.sender
                                } else {
                                    StationsFragment.Page.FRIENDLY.name
                                },
                            )
                        },
                    )
                }
            }
        }

    private fun buildNotification(
        channelId: String,
        title: String,
        message: String,
        ongoing: Boolean = false,
        onIntent: Intent.() -> Unit = {},
        setBuilder: (NotificationCompat.Builder) -> Unit = {},
    ) {
        if (notificationRequests == STOP_NOTIFICATIONS) return

        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply(onIntent)
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                notificationRequests++,
                launchIntent,
                PENDING_INTENT_FLAGS,
            )

        val builder =
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setLargeIcon(
                    BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground)
                )
                .setContentIntent(pendingIntent)
                .setOngoing(ongoing)
                .also(setBuilder)
        notificationManager.createNotification(
            builder,
            channelId,
            title,
            message,
            applicationContext,
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG

        setupTiramisu()

        with(viewModel) {
            val finishCallback =
                onBackPressedDispatcher.addCallback(this@MainActivity) {
                    supportFinishAfterTransition()
                }

            onBackPressedDispatcher.addCallback(this@MainActivity) {
                val dialog = ifConnected {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage(R.string.exit_message)
                        .setCancelable(false)
                        .setNegativeButton(R.string.no) { _, _ ->
                            playSound(SoundEffect.BEEP_1)
                            isEnabled = true
                        }
                        .setPositiveButton(R.string.yes) { _, _ ->
                            playSound(SoundEffect.CONFIRMATION)
                            networkInterface.stop()
                            finishCallback.handleOnBackPressed()
                        }
                }
                if (dialog != null) {
                    playSound(SoundEffect.BEEP_2)
                    dialog.show()
                    isEnabled = false
                } else {
                    finishCallback.handleOnBackPressed()
                }
            }

            try {
                openFileInput(THEME_RES_FILE_NAME).use { themeIndex = it.read().coerceAtLeast(0) }
            } catch (_: FileNotFoundException) {}

            theme.applyStyle(themeRes, true)
            isThemeChanged.value = false

            collectLatestWhileStarted(connectionStatus) {
                if (isIdle) {
                    selectableShips.value = listOf()
                }
            }

            collectLatestWhileStarted(connectedUrl) { newUrl ->
                if (newUrl.isNotBlank()) {
                    userSettings.updateData {
                        val serversList = it.recentServersList.toMutableList()
                        serversList.remove(newUrl)
                        serversList.add(0, newUrl)

                        it.copy {
                            recentServers.clear()

                            recentServers +=
                                if (recentAddressLimitEnabled) {
                                    serversList.take(recentAddressLimit)
                                } else {
                                    serversList
                                }
                        }
                    }
                }
            }

            collectLatestWhileStarted(userSettings.data) { settings ->
                var newContextIndex = reconcileVesselDataIndex(settings.vesselDataLocationValue)
                checkContext(newContextIndex) { message ->
                    newContextIndex = if (vesselDataIndex == newContextIndex) 0 else vesselDataIndex
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.xml_error)
                        .setMessage(getString(R.string.xml_error_message, message))
                        .setCancelable(true)
                        .show()
                }

                val limit = settings.recentAddressLimit
                val hasLimit = settings.recentAddressLimitEnabled

                val adjustedSettings =
                    settings.copy {
                        vesselDataLocationValue = newContextIndex
                        val recentServersCount = recentServers.size
                        if (hasLimit && recentServersCount > limit) {
                            val min = recentServersCount.coerceAtMost(limit)
                            val serversList = recentServers.take(min)
                            recentServers.clear()
                            recentServers += serversList
                        }
                    }

                if (!isIdle && newContextIndex != vesselDataIndex) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.vessel_data)
                        .setMessage(R.string.xml_location_warning)
                        .setCancelable(false)
                        .setNegativeButton(R.string.no) { _, _ ->
                            launch { userSettings.updateData { revertSettings(it) } }
                        }
                        .setPositiveButton(R.string.yes) { _, _ ->
                            playSound(SoundEffect.BEEP_2)
                            disconnectFromServer()
                            updateFromSettings(adjustedSettings)
                            launch { userSettings.updateData { adjustedSettings } }
                        }
                        .show()
                    return@collectLatestWhileStarted
                } else {
                    updateFromSettings(adjustedSettings)
                    userSettings.updateData { adjustedSettings }
                }
            }

            collectLatestWhileStarted(disconnectCause) {
                var suggestUpdate = false
                val message =
                    when (it) {
                        is DisconnectCause.IOError -> {
                            crashlytics.recordException(it.exception)
                            getString(R.string.disconnect_io_error, it.exception.message)
                        }
                        is DisconnectCause.PacketParseError -> {
                            val ex = it.exception
                            crashlytics.setCustomKeys {
                                key("Version", viewModel.version.toString())
                                key("Packet type", ex.packetType.toHexString())
                                key("Payload", ex.payload?.toHexString() ?: "[]")
                            }
                            crashlytics.recordException(ex)
                            getString(R.string.disconnect_parse, it.exception.message)
                        }
                        is DisconnectCause.RemoteDisconnect -> {
                            getString(R.string.disconnect_remote)
                        }
                        is DisconnectCause.UnsupportedVersion -> {
                            if (it.version < Version.MINIMUM) {
                                getString(
                                    R.string.disconnect_unsupported_version_old,
                                    Version.MINIMUM,
                                    it.version,
                                )
                            } else {
                                suggestUpdate = true
                                getString(R.string.disconnect_unsupported_version_new, it.version)
                            }
                        }
                        is DisconnectCause.UnknownError -> {
                            crashlytics.recordException(it.throwable)
                            getString(R.string.disconnect_unknown_error, it.throwable.message)
                        }
                        is DisconnectCause.LocalDisconnect -> return@collectLatestWhileStarted
                    }
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setCancelable(true)
                    .apply {
                        if (suggestUpdate) {
                            setPositiveButton(R.string.check_for_updates) { _, _ ->
                                openPlayStore()
                            }
                        }
                    }
                    .show()
            }

            collectLatestWhileStarted(gameOverReason) {
                if (shouldAskForReview) askForReview()
                shouldAskForReview = !shouldAskForReview
            }

            with(binding) {
                collectLatestWhileStarted(isThemeChanged) {
                    if (it) {
                        isThemeChanged.value = false
                        recreate()
                    }
                }

                ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
                    val insets =
                        windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars() or
                                WindowInsetsCompat.Type.displayCutout()
                        )
                    view.updatePadding(insets.left, insets.top, insets.right, insets.bottom)
                    WindowInsetsCompat.CONSUMED
                }

                collectLatestWhileStarted(jumping) {
                    jumpInputDisabler.visibility = if (it) View.VISIBLE else View.GONE
                }

                setupPageButton.setOnClickListener { playSound(SoundEffect.BEEP_2) }

                gamePageButton.setOnClickListener { playSound(SoundEffect.BEEP_2) }

                helpPageButton.setOnClickListener { playSound(SoundEffect.BEEP_2) }

                mainPageSelector.setOnCheckedChangeListener { _, checkedId ->
                    currentSection = Section.entries.find { it.buttonId == checkedId }
                }

                super.onCreate(savedInstanceState)
                setContentView(root)

                if (savedInstanceState == null) {
                    setupPageButton.isChecked = true
                }

                collectLatestWhileStarted(shipIndex) {
                    if (it >= 0) {
                        gamePageButton.isChecked = true
                    }
                }
            }
        }
    }

    /**
     * When the app is paused (e.g. by backgrounding it), start up the notification service and
     * connect to it.
     */
    override fun onPause() {
        super.onPause()
        Intent(this, NotificationService::class.java).also {
            startService(it)
            bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
    }

    /** Unbind the notification service when the activity is destroyed to prevent memory leaks. */
    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)

        openFileOutput(THEME_RES_FILE_NAME, Context.MODE_PRIVATE).use {
            it.write(byteArrayOf(viewModel.themeIndex.toByte()))
        }
    }

    /**
     * When the app is resumed, stop the notification service, clear all notifications as well as
     * all channels the service listens to that would trigger more notifications when it starts up
     * again.
     */
    override fun onResume() {
        super.onResume()
        if (notificationRequests != STOP_NOTIFICATIONS) {
            unbindService(connection)
            notificationRequests = STOP_NOTIFICATIONS
            notificationManager.reset()
        }
    }

    /**
     * Handles resumption of the app by clicking a notification with an Intent attached. These
     * Intents tell the app to open to a specified page.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val setupPage = intent.getIntExtra(Section.SETUP.name, NO_NAVIGATION)
        if (setupPage >= 0) {
            viewModel.setupFragmentPage.value = SetupFragment.Page.entries[setupPage]
            binding.mainPageSelector.check(Section.SETUP.buttonId)
        }

        val gamePage = intent.getIntExtra(Section.GAME.name, NO_NAVIGATION)
        if (gamePage >= 0) {
            if (gamePage < GAME_PAGE_UNSPECIFIED) {
                viewModel.currentGamePage.value = GameFragment.Page.entries[gamePage]
            }
            binding.mainPageSelector.check(Section.GAME.buttonId)
        }

        intent.getStringExtra(GameFragment.Page.STATIONS.name)?.also { station ->
            with(viewModel) {
                try {
                    stationPage.value = StationsFragment.Page.valueOf(station)
                } catch (_: IllegalArgumentException) {
                    stationPage.value = StationsFragment.Page.FRIENDLY
                    if (livingStationNameIndex.containsKey(station)) {
                        stationName.value = station
                    }
                }
                currentGamePage.value = GameFragment.Page.STATIONS
                binding.mainPageSelector.check(Section.GAME.buttonId)
            }
        }
    }

    private fun setupTiramisu() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        //        onBackInvokedDispatcher.registerOnBackInvokedCallback(
        //            OnBackInvokedDispatcher.PRIORITY_DEFAULT
        //        ) {
        //            onBackPressed()
        //        }

        if (
            ActivityCompat.checkSelfPermission(this, POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher?.launch(POST_NOTIFICATIONS)
        }
    }

    private fun openPlayStore() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                )
            )
        }
    }

    private fun askForReview() {
        AlertDialog.Builder(this@MainActivity)
            .setTitle(R.string.review_title)
            .setMessage(R.string.review_prompt)
            .setCancelable(true)
            .setNegativeButton(R.string.no) { _, _ -> viewModel.playSound(SoundEffect.BEEP_1) }
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.playSound(SoundEffect.BEEP_2)

                val request = reviewManager.requestReviewFlow()
                request
                    .addOnSuccessListener { reviewInfo ->
                        reviewManager.launchReviewFlow(this, reviewInfo).addOnFailureListener {
                            showReviewErrorDialog(it)
                        }
                    }
                    .addOnFailureListener { showReviewErrorDialog(it) }
            }
            .show()
    }

    private fun showReviewErrorDialog(exception: Exception) {
        val errorCode = exception.message ?: getString(R.string.unknown)
        AlertDialog.Builder(this@MainActivity)
            .setTitle(R.string.review_error)
            .setMessage(getString(R.string.review_error_message, errorCode))
            .setCancelable(true)
            .show()
    }

    private companion object {
        const val STOP_NOTIFICATIONS = -1
        const val NO_NAVIGATION = -1
        const val GAME_PAGE_UNSPECIFIED = 6

        const val THEME_RES_FILE_NAME = "theme_res.dat"

        val PENDING_INTENT_FLAGS =
            PendingIntent.FLAG_UPDATE_CURRENT.or(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE
                else 0
            )
    }
}
