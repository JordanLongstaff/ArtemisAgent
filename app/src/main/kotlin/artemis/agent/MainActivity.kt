package artemis.agent

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.viewModelScope
import artemis.agent.UserSettingsSerializer.userSettings
import artemis.agent.databinding.ActivityMainBinding
import artemis.agent.game.GameFragment
import artemis.agent.game.stations.StationsFragment
import artemis.agent.help.HelpFragment
import artemis.agent.setup.SetupFragment
import artemis.agent.util.SoundEffect
import artemis.agent.util.VersionString
import artemis.agent.util.collectLatestWhileStarted
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallException
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.jakewharton.processphoenix.ProcessPhoenix
import com.walkertribe.ian.iface.DisconnectCause
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket
import com.walkertribe.ian.util.Version
import java.io.FileNotFoundException
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.asDeferred

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

    val updateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(this) }

    private val updateResultLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            when (result.resultCode) {
                RESULT_CANCELED -> {
                    R.string.update_declined_title to R.string.update_declined_message
                }

                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    R.string.update_failed_title to R.string.update_failed_message
                }

                else -> null
            }?.also { (titleId, messageId) ->
                AlertDialog.Builder(this)
                    .setTitle(titleId)
                    .setMessage(messageId)
                    .setCancelable(false)
                    .setNegativeButton(R.string.no) { _, _ ->
                        viewModel.playSound(SoundEffect.BEEP_2)
                    }
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.playSound(SoundEffect.BEEP_2)
                        startUpdateFlow()
                    }
                    .show()
            }
        }

    private var isUpdateReady: Boolean = false
    @AppUpdateType private var updateType: Int = AppUpdateType.FLEXIBLE

    private val completeUpdateCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                updateManager.completeUpdate()
            }
        }

    private val exitConfirmationCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                isEnabled = false
                viewModel.playSound(SoundEffect.BEEP_2)
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(R.string.exit_message)
                    .setCancelable(false)
                    .setNegativeButton(R.string.no) { _, _ ->
                        viewModel.playSound(SoundEffect.BEEP_1)
                        isEnabled = true
                    }
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.playSound(SoundEffect.CONFIRMATION)
                        viewModel.networkInterface.stop()
                        onBackPressedDispatcher.onBackPressed()
                    }
                    .show()
            }
        }

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
                        NotificationChannelTag.PRODUCTION,
                    )
                    createStationPacketListener(
                        service,
                        viewModel.stationAttackedPacket,
                        NotificationChannelTag.ATTACK,
                    )
                    createStationPacketListener(
                        service,
                        viewModel.stationDestroyedPacket,
                        NotificationChannelTag.DESTROYED,
                        false,
                    )

                    val missionManager = viewModel.missionManager
                    createMissionPacketListener(
                        service,
                        missionManager.newMissionPacket,
                        NotificationChannelTag.NEW_MISSION,
                    )
                    createMissionPacketListener(
                        service,
                        missionManager.missionProgressPacket,
                        NotificationChannelTag.MISSION_PROGRESS,
                    )
                    createMissionPacketListener(
                        service,
                        missionManager.missionCompletionPacket,
                        NotificationChannelTag.MISSION_COMPLETED,
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
                        info =
                            NotificationInfo(
                                channel = NotificationChannelTag.GAME_INFO,
                                title = viewModel.connectedUrl.value,
                                message = strings.joinToString(),
                                ongoing = true,
                            ),
                        onIntent = { putExtra(Section.GAME.name, GAME_PAGE_UNSPECIFIED) },
                    )
                }

                service.collectLatestWhileStarted(viewModel.livingAllies) { allies ->
                    if (viewModel.stationsExist.value) return@collectLatestWhileStarted

                    allies.firstOrNull()?.also { ally ->
                        buildNotification(
                            info =
                                NotificationInfo(
                                    channel = NotificationChannelTag.DEEP_STRIKE,
                                    title = ally.fullName,
                                    message =
                                        if (viewModel.torpedoesReady)
                                            getString(R.string.manufacturing_torpedoes_ready)
                                        else viewModel.getManufacturingTimer(this@MainActivity),
                                    ongoing = true,
                                ),
                            onIntent = {
                                putExtra(Section.GAME.name, GameFragment.Page.ALLIES.ordinal)
                            },
                        )
                    }
                }
            }

            private fun setupBiomechNotifications(service: NotificationService) {
                val biomechManager = viewModel.biomechManager

                service.collectLatestWhileStarted(biomechManager.destroyedBiomechName) {
                    notificationManager.dismissBiomechMessage(it)
                }

                service.collectLatestWhileStarted(biomechManager.nextActiveBiomech) { entry ->
                    buildNotification(
                        info =
                            NotificationInfo(
                                channel = NotificationChannelTag.REANIMATE,
                                title = entry.getFullName(viewModel),
                                message = getString(R.string.biomech_notification),
                            ),
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
                val enemiesManager = viewModel.enemiesManager

                service.collectLatestWhileStarted(enemiesManager.destroyedEnemyName) {
                    notificationManager.dismissPerfidyMessage(it)
                }

                service.collectLatestWhileStarted(enemiesManager.perfidy) { entry ->
                    buildNotification(
                        info =
                            NotificationInfo(
                                channel = NotificationChannelTag.PERFIDY,
                                title = entry.fullName,
                                message = getString(R.string.enemy_perfidy_notification),
                            ),
                        onIntent = {
                            putExtra(Section.GAME.name, GameFragment.Page.ENEMIES.ordinal)
                        },
                    )
                }
            }

            private fun setupGameNotifications(service: NotificationService) {
                service.collectLatestWhileStarted(viewModel.borderWarMessage) { packet ->
                    buildNotification(
                        info =
                            NotificationInfo(
                                channel = NotificationChannelTag.BORDER_WAR,
                                title = packet.sender,
                                message = packet.message,
                            ),
                        onIntent = { putExtra(Section.GAME.name, GAME_PAGE_UNSPECIFIED) },
                    )
                }

                service.collectLatestWhileStarted(viewModel.gameOverReason) { reason ->
                    if (viewModel.gameIsRunning.value) return@collectLatestWhileStarted
                    buildNotification(
                        info =
                            NotificationInfo(
                                channel = NotificationChannelTag.GAME_OVER,
                                title = viewModel.connectedUrl.value,
                                message = reason,
                            ),
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
                        info =
                            NotificationInfo(
                                channel = NotificationChannelTag.CONNECTION,
                                title = viewModel.connectedUrl.value,
                                message = message,
                            ),
                        onIntent = {
                            putExtra(Section.SETUP.name, SetupFragment.Page.CONNECT.ordinal)
                        },
                        setBuilder = { notificationManager.reset() },
                    )
                }

                service.collectLatestWhileStarted(viewModel.connectionStatus) { status ->
                    val message =
                        when (status) {
                            is ConnectionStatus.NotConnected,
                            is ConnectionStatus.Connecting -> return@collectLatestWhileStarted

                            else -> getString(status.stringId)
                        }

                    buildNotification(
                        info =
                            NotificationInfo(
                                channel = NotificationChannelTag.CONNECTION,
                                title = viewModel.lastAttemptedHost,
                                message = message,
                            ),
                        onIntent = {
                            val openPage =
                                if (status is ConnectionStatus.Connected) SetupFragment.Page.SHIPS
                                else SetupFragment.Page.CONNECT
                            putExtra(Section.SETUP.name, openPage.ordinal)
                        },
                    )
                }
            }

            private fun createMissionPacketListener(
                service: NotificationService,
                flow: MutableSharedFlow<CommsIncomingPacket>,
                channel: NotificationChannelTag,
            ) {
                service.collectLatestWhileStarted(flow) { packet ->
                    buildNotification(
                        info =
                            NotificationInfo(
                                channel = channel,
                                title = packet.sender,
                                message = packet.message,
                            ),
                        onIntent = {
                            putExtra(Section.GAME.name, GameFragment.Page.MISSIONS.ordinal)
                        },
                    )
                }
            }

            private fun createStationPacketListener(
                service: NotificationService,
                flow: MutableSharedFlow<CommsIncomingPacket>,
                channel: NotificationChannelTag,
                includeSenderName: Boolean = true,
            ) {
                service.collectLatestWhileStarted(flow) { packet ->
                    buildNotification(
                        info =
                            NotificationInfo(
                                channel = channel,
                                title = packet.sender,
                                message = packet.message,
                            ),
                        onIntent = {
                            putExtra(
                                GameFragment.Page.STATIONS.name,
                                if (includeSenderName) packet.sender
                                else StationsFragment.Page.FRIENDLY.name,
                            )
                        },
                    )
                }
            }
        }

    private fun buildNotification(
        info: NotificationInfo,
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
            NotificationCompat.Builder(this, info.channel.tag)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setLargeIcon(
                    BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground)
                )
                .setContentIntent(pendingIntent)
                .also(setBuilder)
        notificationManager.createNotification(builder, info, applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupWindowInsets()
        setupFirebase()
        setupTiramisu()
        setupBackPressedCallbacks()
        setupTheme()
        setupConnectionObservers()
        setupUserSettingsObserver()

        collectLatestWhileStarted(viewModel.gameOverReason) {
            if (shouldAskForReview) askForReview()
            shouldAskForReview = !shouldAskForReview
            checkForUpdates()
        }

        collectLatestWhileStarted(viewModel.jumping) {
            binding.jumpInputDisabler.visibility = if (it) View.VISIBLE else View.GONE
        }

        collectLatestWhileStarted(viewModel.helpTopicIndex) {
            binding.updateButton.visibility =
                if (it == HelpFragment.ABOUT_TOPIC_INDEX) View.VISIBLE else View.GONE
        }

        binding.updateButton.setOnClickListener { checkForUpdates() }

        binding.mainPageSelector.children.forEach { view ->
            view.setOnClickListener { viewModel.playSound(SoundEffect.BEEP_2) }
        }

        binding.mainPageSelector.setOnCheckedChangeListener { _, checkedId ->
            currentSection = Section.entries.find { it.buttonId == checkedId }
        }

        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            binding.setupPageButton.isChecked = true
        }

        collectLatestWhileStarted(viewModel.shipIndex) {
            if (it >= 0) {
                binding.gamePageButton.isChecked = true
            }
        }

        checkForUpdates()
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
    override fun onStop() {
        super.onStop()
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

        updateManager.appUpdateInfo.addOnSuccessListener { updateInfo ->
            if (updateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                onUpdateReady()
            }

            if (
                updateInfo.updateAvailability() ==
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            ) {
                // Resume immediate update that was in progress
                updateManager.startUpdateFlowForResult(
                    updateInfo,
                    updateResultLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                )
            }
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
            try {
                viewModel.stationPage.value = StationsFragment.Page.valueOf(station)
            } catch (_: IllegalArgumentException) {
                viewModel.stationPage.value = StationsFragment.Page.FRIENDLY
                if (viewModel.livingStationNameIndex.containsKey(station)) {
                    viewModel.stationName.value = station
                }
            }
            viewModel.currentGamePage.value = GameFragment.Page.STATIONS
            binding.mainPageSelector.check(Section.GAME.buttonId)
        }
    }

    private fun setupWindowInsets() {
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets =
                windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
            view.updatePadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupTiramisu() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        if (
            ActivityCompat.checkSelfPermission(this, POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher?.launch(POST_NOTIFICATIONS)
        }
    }

    private fun setupFirebase() {
        Firebase.crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG

        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 1.minutes.inWholeSeconds
        }
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(configSettings)
            setDefaultsAsync(
                mapOf(RemoteConfigKey.artemisLatestVersion to Version.DEFAULT.toString())
            )
        }
    }

    private fun setupBackPressedCallbacks() {
        // Some Android 10 devices leak memory if this is not called, so we need to register this
        // callback to address it
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            onBackPressedDispatcher.addCallback(this) { supportFinishAfterTransition() }
        }

        onBackPressedDispatcher.addCallback(this, completeUpdateCallback)
        onBackPressedDispatcher.addCallback(this, exitConfirmationCallback)
    }

    private fun setupTheme() {
        with(viewModel) {
            try {
                openFileInput(THEME_RES_FILE_NAME).use { themeIndex = it.read().coerceAtLeast(0) }
            } catch (_: FileNotFoundException) {}

            theme.applyStyle(themeRes, true)
            isThemeChanged.value = false

            collectLatestWhileStarted(isThemeChanged) {
                if (it) {
                    isThemeChanged.value = false
                    recreate()
                }
            }
        }
    }

    private fun setupConnectionObservers() {
        collectLatestWhileStarted(viewModel.connectionStatus) {
            val isConnected = viewModel.isConnected
            exitConfirmationCallback.isEnabled = isConnected
            if (!isConnected) {
                viewModel.selectableShips.value = emptyList()
            }
        }

        collectLatestWhileStarted(viewModel.connectedUrl) { newUrl ->
            if (newUrl.isNotBlank()) {
                userSettings.updateData {
                    val serversList = it.recentServersList.toMutableList()
                    serversList.remove(newUrl)
                    serversList.add(0, newUrl)

                    it.copy {
                        recentServers.clear()

                        recentServers +=
                            if (recentAddressLimitEnabled) serversList.take(recentAddressLimit)
                            else serversList
                    }
                }
            }
        }

        collectLatestWhileStarted(viewModel.disconnectCause) {
            val (message, suggestUpdate) =
                getDisconnectDialogContents(it) ?: return@collectLatestWhileStarted
            AlertDialog.Builder(this@MainActivity)
                .setMessage(message)
                .setCancelable(true)
                .apply {
                    if (suggestUpdate) {
                        setPositiveButton(R.string.update) { _, _ -> checkForUpdates() }
                    }
                }
                .show()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getDisconnectDialogContents(cause: DisconnectCause): Pair<String, Boolean>? {
        val crashlytics = Firebase.crashlytics
        return when (cause) {
            is DisconnectCause.IOError -> {
                crashlytics.recordException(cause.exception)
                getString(R.string.disconnect_io_error, cause.exception.message) to false
            }
            is DisconnectCause.PacketParseError -> {
                val ex = cause.exception
                crashlytics.setCustomKeys {
                    key("Version", viewModel.version.toString())
                    key("Packet type", ex.packetType.toHexString())
                    key("Payload", ex.payload?.toHexString() ?: "[]")
                }
                crashlytics.recordException(ex)
                getString(R.string.disconnect_parse, cause.exception.message) to false
            }
            is DisconnectCause.RemoteDisconnect -> {
                getString(R.string.disconnect_remote) to false
            }
            is DisconnectCause.UnsupportedVersion -> {
                if (cause.version < Version.MINIMUM)
                    getString(
                        R.string.disconnect_unsupported_version_old,
                        Version.MINIMUM,
                        cause.version,
                    ) to false
                else getString(R.string.disconnect_unsupported_version_new, cause.version) to true
            }
            is DisconnectCause.UnknownError -> {
                crashlytics.recordException(cause.throwable)
                getString(R.string.disconnect_unknown_error, cause.throwable.message) to false
            }
            is DisconnectCause.LocalDisconnect -> null
        }
    }

    private fun setupUserSettingsObserver() {
        collectLatestWhileStarted(userSettings.data) { settings ->
            val vesselDataManager = viewModel.vesselDataManager
            var newContextIndex = vesselDataManager.reconcileIndex(settings.vesselDataLocationValue)
            vesselDataManager.checkContext(newContextIndex) { message ->
                newContextIndex =
                    if (vesselDataManager.index == newContextIndex) 0 else vesselDataManager.index
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

            if (!viewModel.isIdle && newContextIndex != vesselDataManager.index) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.vessel_data)
                    .setMessage(R.string.xml_location_warning)
                    .setCancelable(false)
                    .setNegativeButton(R.string.no) { _, _ ->
                        launch { userSettings.updateData { viewModel.revertSettings(it) } }
                    }
                    .setPositiveButton(R.string.yes) { _, _ ->
                        viewModel.playSound(SoundEffect.BEEP_2)
                        viewModel.disconnectFromServer()
                        viewModel.updateFromSettings(adjustedSettings)
                        launch { userSettings.updateData { adjustedSettings } }
                    }
                    .show()
                return@collectLatestWhileStarted
            } else {
                viewModel.updateFromSettings(adjustedSettings)
                userSettings.updateData { adjustedSettings }
            }
        }
    }

    private fun checkForUpdates() {
        viewModel.viewModelScope.launch {
            val results =
                awaitAll(
                    Firebase.remoteConfig
                        .fetchAndActivate()
                        .continueWith { fetchArtemisLatestVersion() }
                        .asDeferred(),
                    async {
                        try {
                            updateManager.appUpdateInfo.asDeferred().await()
                        } catch (_: InstallException) {
                            null
                        }
                    },
                )

            val maxVersion = results[0] as Version
            viewModel.maxVersion = maxVersion

            val updateInfo = results[1] as? AppUpdateInfo
            val latestVersionCode = updateInfo?.availableVersionCode() ?: 0

            val updateAlert = UpdateAlert.check(maxVersion, latestVersionCode) ?: return@launch

            val context = this@MainActivity

            AlertDialog.Builder(context)
                .setTitle(updateAlert.getTitle(context))
                .setMessage(updateAlert.getMessage(context))
                .setCancelable(false)
                .setNegativeButton(R.string.no) { _, _ -> viewModel.playSound(SoundEffect.BEEP_1) }
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.playSound(SoundEffect.BEEP_2)
                    when (updateAlert) {
                        is UpdateAlert.Immediate,
                        is UpdateAlert.ArtemisVersion.Update -> {
                            updateType = AppUpdateType.IMMEDIATE
                            startUpdateFlow()
                        }

                        is UpdateAlert.Flexible -> {
                            updateType = AppUpdateType.FLEXIBLE
                            startUpdateFlow()
                        }

                        is UpdateAlert.ArtemisVersion.Restart -> {
                            deleteFile(MAX_VERSION_FILE_NAME)
                            ProcessPhoenix.triggerRebirth(context)
                        }
                    }
                }
                .show()
        }
    }

    private fun fetchArtemisLatestVersion(): Version =
        try {
                openFileInput(MAX_VERSION_FILE_NAME).use { it.readBytes().decodeToString() }
            } catch (_: FileNotFoundException) {
                Firebase.remoteConfig.getString(RemoteConfigKey.artemisLatestVersion).also { ver ->
                    openFileOutput(MAX_VERSION_FILE_NAME, Context.MODE_PRIVATE).use {
                        it.write(ver.encodeToByteArray())
                    }
                }
            }
            .let { VersionString(it).toVersion() }

    private fun startUpdateFlow() {
        val appUpdateInfoTask = updateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { updateInfo ->
            if (updateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (updateType == AppUpdateType.FLEXIBLE) {
                    val listener =
                        object : InstallStateUpdatedListener {
                            override fun onStateUpdate(installState: InstallState) {
                                when (installState.installStatus()) {
                                    InstallStatus.DOWNLOADED -> onUpdateReady()
                                    InstallStatus.INSTALLED ->
                                        updateManager.unregisterListener(this)
                                    else -> {}
                                }
                            }
                        }
                    updateManager.registerListener(listener)
                }

                if (updateInfo.isUpdateTypeAllowed(updateType)) {
                    updateManager.startUpdateFlowForResult(
                        updateInfo,
                        updateResultLauncher,
                        AppUpdateOptions.newBuilder(updateType).build(),
                    )
                }
            }
        }
    }

    private fun onUpdateReady() {
        isUpdateReady = true
        completeUpdateCallback.isEnabled = true
        AlertDialog.Builder(this@MainActivity)
            .setTitle(R.string.update_ready_title)
            .setMessage(R.string.update_ready_message)
            .setCancelable(false)
            .setNegativeButton(R.string.update_later) { _, _ ->
                viewModel.playSound(SoundEffect.BEEP_2)
            }
            .setPositiveButton(R.string.update_now) { _, _ ->
                viewModel.playSound(SoundEffect.BEEP_2)
                isUpdateReady = false
                updateManager.completeUpdate()
            }
            .show()
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
        const val MAX_VERSION_FILE_NAME = "max_version.dat"

        val PENDING_INTENT_FLAGS =
            PendingIntent.FLAG_UPDATE_CURRENT.or(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE
                else 0
            )
    }
}
