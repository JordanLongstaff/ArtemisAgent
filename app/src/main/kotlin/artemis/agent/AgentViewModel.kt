package artemis.agent

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.StyleRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import artemis.agent.UserSettingsOuterClass.UserSettings
import artemis.agent.cpu.BiomechManager
import artemis.agent.cpu.CPU
import artemis.agent.cpu.EnemiesManager
import artemis.agent.cpu.MiscManager
import artemis.agent.cpu.MissionManager
import artemis.agent.cpu.RoutingGraph
import artemis.agent.cpu.RoutingGraph.Companion.calculateRouteCost
import artemis.agent.cpu.VesselDataManager
import artemis.agent.cpu.listeners
import artemis.agent.game.GameFragment
import artemis.agent.game.ObjectEntry
import artemis.agent.game.WarStatus
import artemis.agent.game.allies.AllySorter
import artemis.agent.game.route.RouteEntry
import artemis.agent.game.route.RouteObjective
import artemis.agent.game.route.RouteTaskIncentive
import artemis.agent.game.stations.StationsFragment
import artemis.agent.help.HelpFragment
import artemis.agent.setup.SetupFragment
import artemis.agent.setup.settings.SettingsFragment
import artemis.agent.util.HapticEffect
import artemis.agent.util.SoundEffect
import artemis.agent.util.TimerText
import artemis.agent.util.TimerText.timerString
import com.walkertribe.ian.enums.AlertStatus
import com.walkertribe.ian.enums.Console
import com.walkertribe.ian.enums.GameType
import com.walkertribe.ian.iface.ArtemisNetworkInterface
import com.walkertribe.ian.iface.ConnectionEvent
import com.walkertribe.ian.iface.DisconnectCause
import com.walkertribe.ian.iface.KtorArtemisNetworkInterface
import com.walkertribe.ian.iface.Listener
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.core.ActivateUpgradePacket
import com.walkertribe.ian.protocol.core.BayStatusPacket
import com.walkertribe.ian.protocol.core.EndGamePacket
import com.walkertribe.ian.protocol.core.GameOverReasonPacket
import com.walkertribe.ian.protocol.core.GameStartPacket
import com.walkertribe.ian.protocol.core.JumpEndPacket
import com.walkertribe.ian.protocol.core.PausePacket
import com.walkertribe.ian.protocol.core.PlayerShipDamagePacket
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket
import com.walkertribe.ian.protocol.core.setup.AllShipSettingsPacket
import com.walkertribe.ian.protocol.core.setup.ReadyPacket
import com.walkertribe.ian.protocol.core.setup.SetConsolePacket
import com.walkertribe.ian.protocol.core.setup.SetShipPacket
import com.walkertribe.ian.protocol.core.setup.Ship
import com.walkertribe.ian.protocol.core.setup.VersionPacket
import com.walkertribe.ian.protocol.core.world.DockedPacket
import com.walkertribe.ian.protocol.udp.Server
import com.walkertribe.ian.protocol.udp.ServerDiscoveryRequester
import com.walkertribe.ian.util.BoolState
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.world.Artemis
import com.walkertribe.ian.world.ArtemisBlackHole
import com.walkertribe.ian.world.ArtemisCreature
import com.walkertribe.ian.world.ArtemisMine
import com.walkertribe.ian.world.ArtemisObject
import com.walkertribe.ian.world.ArtemisPlayer
import com.walkertribe.ian.world.ArtemisShielded
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The view model containing all running client data and utility functions used by the UI. */
class AgentViewModel(application: Application) :
    AndroidViewModel(application), ServerDiscoveryRequester.Listener {
    // Connection status
    val networkInterface: ArtemisNetworkInterface by lazy {
        KtorArtemisNetworkInterface(maxVersion = if (BuildConfig.DEBUG) null else maxVersion).also {
            it.addListeners(
                listeners +
                    cpu.listeners +
                    enemiesManager.listeners +
                    missionManager.listeners +
                    biomechManager.listeners +
                    miscManager.listeners
            )
        }
    }

    val connectionStatus: MutableStateFlow<ConnectionStatus> by lazy {
        MutableStateFlow(ConnectionStatus.NotConnected)
    }
    val connectedUrl: MutableStateFlow<String> by lazy { MutableStateFlow("") }
    var attemptingConnection: Boolean = false
    var lastAttemptedHost: String = ""

    val isIdle: Boolean
        get() =
            connectionStatus.value == ConnectionStatus.NotConnected ||
                connectionStatus.value == ConnectionStatus.Failed

    val isConnected: Boolean
        get() = !isIdle && connectionStatus.value != ConnectionStatus.Connecting

    // UDP discovered servers
    val discoveredServers: MutableStateFlow<List<Server>> by lazy { MutableStateFlow(emptyList()) }
    val isScanningUDP: MutableSharedFlow<Boolean> by lazy {
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    var showingNetworkInfo: Boolean = true
        private set

    var alwaysScanPublicBroadcasts: Boolean = true
        private set

    // Saved copy of address bar text in connect fragment
    var addressBarText: String = ""

    // UI variables - app theme, opacity, back press callback
    val isThemeChanged: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }

    @StyleRes var themeRes: Int = R.style.Theme_ArtemisAgent
    var themeIndex: Int
        get() = ALL_THEMES.indexOf(themeRes)
        set(index) {
            themeRes = ALL_THEMES[index]
        }

    val rootOpacity: MutableStateFlow<Float> by lazy { MutableStateFlow(1f) }
    val jumping: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }

    private var damageVisJob: Job? = null

    // Ship settings from packet
    val selectableShips: MutableStateFlow<List<Ship>> by lazy { MutableStateFlow(emptyList()) }

    // Game status
    val gameIsRunning: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }
    var isDeepStrikePossible: Boolean = false
    var isBorderWarPossible: Boolean = false
    val isBorderWar: StateFlow<Boolean> by lazy {
        stationsExist
            .combine(enemyStationsExist) { friendly, enemy ->
                friendly && enemy && isBorderWarPossible
            }
            .stateIn(scope = viewModelScope, started = SharingStarted.Lazily, initialValue = false)
    }
    val borderWarStatus: MutableStateFlow<WarStatus> by lazy { MutableStateFlow(WarStatus.TENSION) }
    val borderWarMessage: MutableSharedFlow<CommsIncomingPacket> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val gameOverReason: MutableSharedFlow<String> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val disconnectCause: MutableSharedFlow<DisconnectCause> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    // List of selectable game fragment pages, mapped to flashing status
    val gamePages: MutableStateFlow<Map<GameFragment.Page, Boolean>> by lazy {
        MutableStateFlow(emptyMap())
    }
    val currentGamePage: MutableStateFlow<GameFragment.Page?> by lazy { MutableStateFlow(null) }

    // Page activator data
    var alliesExist: Boolean = false
    val stationsExist: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }
    val enemyStationsExist: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }

    // Miscellaneous Comms actions
    val miscManager = MiscManager()

    // Current player ship data
    val shipIndex: MutableStateFlow<Int> by lazy { MutableStateFlow(-1) }
    val playerShip: ArtemisPlayer?
        get() = shipIndex.value.coerceAtLeast(0).let(playerIndex::get).let(players::get)

    val playerName: String?
        get() = playerShip?.run { name.value }

    internal val playerIndex = IntArray(Artemis.SHIP_COUNT) { -1 }
    internal val players = ConcurrentHashMap<Int, ArtemisPlayer>()
    private var playerChange = false
    private var fightersInBays: Int = 0
    internal val fighterIDs = mutableSetOf<Int>()
    val totalFighters: MutableStateFlow<Int> by lazy { MutableStateFlow(0) }
    val ordnanceUpdated: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }

    // Double agent button UI data
    internal var doubleAgentSecondsLeft = -1
    val doubleAgentEnabled: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }
    val doubleAgentActive: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }
    val doubleAgentText: MutableStateFlow<String> by lazy { MutableStateFlow("") }

    // Alert status
    val alertStatus: MutableStateFlow<AlertStatus> by lazy { MutableStateFlow(AlertStatus.NORMAL) }

    // Side mission data
    val missionManager = MissionManager(this)

    // Friendly ship data
    var alliesEnabled: Boolean = true
    val allyShipIndex = ConcurrentHashMap<String, Int>()
    val allyShips = ConcurrentHashMap<Int, ObjectEntry.Ally>()
    val livingAllies: MutableSharedFlow<List<ObjectEntry.Ally>> by lazy {
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    // Allies page UI data
    var allySorter: AllySorter = AllySorter()
        private set

    var showAllySelector = false
        set(value) {
            field = value
            if (!value) showingDestroyedAllies.value = false
        }

    val showingDestroyedAllies: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }
    var scrollToAlly: ObjectEntry.Ally? = null
    val focusedAlly: MutableStateFlow<ObjectEntry.Ally?> by lazy { MutableStateFlow(null) }
    val defendableTargets: MutableSharedFlow<List<ArtemisShielded<*>>> by lazy {
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    var manuallyReturnFromCommands: Boolean = false

    // Single-ally UI data
    val isDeepStrike: Boolean
        get() = isDeepStrikePossible && !stationsExist.value && allyShips.size <= 1

    val isSingleAlly: Boolean
        get() = allyShips.size == 1 && allyShips.values.any { it.isInstructable }

    var torpedoesReady: Boolean = false
    var torpedoFinishTime: Long = 0L

    // Friendly station data
    val stationsRemain: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }
    val livingStationNameIndex =
        ConcurrentSkipListMap<String, Int>(ObjectEntry.Station.FRIENDLY_COMPARATOR)
    val livingStationFullNameIndex = ConcurrentHashMap<String, Int>()
    val livingStations = ConcurrentHashMap<Int, ObjectEntry.Station>()

    // Friendly station navigation data
    val flashingStations: MutableStateFlow<List<Pair<ObjectEntry.Station, Boolean>>> by lazy {
        MutableStateFlow(emptyList())
    }
    val stationName: MutableStateFlow<String> by lazy { MutableStateFlow("") }
    val currentStation: MutableSharedFlow<ObjectEntry.Station> by lazy {
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val closestStationName: MutableStateFlow<String> by lazy { MutableStateFlow("") }

    // Enemy station data
    val enemyStationNameIndex =
        ConcurrentSkipListMap<String, Int>(ObjectEntry.Station.ENEMY_COMPARATOR)
    val livingEnemyStations = ConcurrentHashMap<Int, ObjectEntry.Station>()
    val enemyStations: MutableSharedFlow<List<ObjectEntry.Station>> by lazy {
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    // Stations page UI data
    val stationPage: MutableStateFlow<StationsFragment.Page> by lazy {
        MutableStateFlow(StationsFragment.Page.FRIENDLY)
    }
    val stationSelectorFlashPercent: MutableStateFlow<Float> by lazy { MutableStateFlow(1f) }

    // Friendly station message packet data
    val stationProductionPacket: MutableSharedFlow<CommsIncomingPacket> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val stationAttackedPacket: MutableSharedFlow<CommsIncomingPacket> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val stationDestroyedPacket: MutableSharedFlow<CommsIncomingPacket> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    // Destroyed objects data
    val destroyedAllies: MutableStateFlow<List<String>> by lazy { MutableStateFlow(emptyList()) }
    val destroyedStations: MutableStateFlow<List<String>> by lazy { MutableStateFlow(emptyList()) }

    // Biomech data
    val biomechManager = BiomechManager()

    // Enemy ship data
    val enemiesManager = EnemiesManager()

    // Routing data
    var routingEnabled: Boolean = true
    var routeIncludesMissions: Boolean = true
    var routeIncentives: List<RouteTaskIncentive> = RouteTaskIncentive.entries
    private var routeRunning: Boolean = false
    private var routeJob: Job? = null
    val routeObjective: MutableStateFlow<RouteObjective> by lazy {
        MutableStateFlow(RouteObjective.Tasks)
    }
    var routeSuppliesIndex: Int = 0
    val routeMap = ConcurrentHashMap<RouteObjective, List<RouteEntry>>()
    val routeList: MutableSharedFlow<List<RouteEntry>> by lazy {
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    private var graph: RoutingGraph? = null

    // Object avoidance flags - modified in Settings
    internal var avoidMines: Boolean = true
    internal var avoidBlackHoles: Boolean = true
    internal var avoidTyphons: Boolean = true

    // Object clearance variables - modified in Settings
    internal var mineClearance: Float = DEFAULT_MINE_CLEARANCE
    internal var blackHoleClearance: Float = DEFAULT_BLACK_HOLE_CLEARANCE
    internal var typhonClearance: Float = DEFAULT_TYPHON_CLEARANCE

    // Lists of obstacles
    internal val mines = ConcurrentHashMap<Int, ArtemisMine>()
    internal val blackHoles = ConcurrentHashMap<Int, ArtemisBlackHole>()
    internal val typhons = ConcurrentHashMap<Int, ArtemisCreature>()

    // CPU and inventory data
    internal val cpu = CPU(this)
    val inventory: MutableStateFlow<Array<Int?>> by lazy {
        MutableStateFlow(arrayOfNulls(PLURALS_FOR_INVENTORY.size))
    }
    private var updateJob: Job? = null

    // Determines whether directions are shown as padded three-digit numbers
    var threeDigitDirections = true
        private set

    // Haptics
    private val vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    var hapticsEnabled = true
        private set

    // Setup fragment page
    val setupFragmentPage: MutableStateFlow<SetupFragment.Page> by lazy {
        MutableStateFlow(SetupFragment.Page.CONNECT)
    }

    // Settings fragment page
    val settingsPage: MutableStateFlow<SettingsFragment.Page?> by lazy { MutableStateFlow(null) }
    val settingsReset: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }

    // Help topic index
    val helpTopicIndex: MutableStateFlow<Int> by lazy { MutableStateFlow(HelpFragment.MENU) }

    // Various numerical settings
    var port: Int = DEFAULT_PORT
    var updateObjectsInterval: Int = DEFAULT_UPDATE_INTERVAL
    var connectTimeout: Int = DEFAULT_CONNECT_TIMEOUT
    var scanTimeout: Int = DEFAULT_SCAN_TIMEOUT
    var heartbeatTimeout: Int = DEFAULT_HEARTBEAT_TIMEOUT
        set(value) {
            field = value
            if (isConnected) {
                networkInterface.setTimeout(field.seconds.inWholeMilliseconds)
            }
        }

    var autoDismissCompletedMissions: Boolean = true

    // UDP server discovery requester
    private val serverDiscoveryRequester: ServerDiscoveryRequester
        get() =
            ServerDiscoveryRequester(
                listener = this@AgentViewModel,
                timeoutMs = scanTimeout.seconds.inWholeMilliseconds,
            )

    // Artemis version
    var version: Version = Version.DEFAULT
        private set

    var maxVersion: Version = Version.DEFAULT

    // Vessel data
    val vesselDataManager = VesselDataManager(application)

    val vesselData: VesselData
        get() = vesselDataManager.vesselData

    // Sound effects players
    private val playSounds: Boolean
        get() = volume > 0f && !soundsMuted

    private val sounds: MutableList<MediaPlayer?> =
        SoundEffect.entries
            .map { MediaPlayer.create(application.applicationContext, it.soundId) }
            .toMutableList()
    var volume: Float = 1f
        set(value) {
            field = value / VOLUME_SCALE
        }

    var soundsMuted: Boolean = false

    /** Populates the RecyclerView in the route fragment. */
    private suspend fun calculateRoute() {
        routeObjective.value.also { objective ->
            routeMap[objective] =
                when (objective) {
                    is RouteObjective.Tasks -> {
                        routeMap[objective].orEmpty()
                    }
                    is RouteObjective.ReplacementFighters -> {
                        livingStations.values
                            .filter { it.fighters > 0 }
                            .let { stations ->
                                stations.zip(
                                    stations.map {
                                        playerShip?.let { player ->
                                            withContext(cpu.coroutineContext) {
                                                graph.calculateRouteCost(player, it.obj)
                                            }
                                        } ?: Float.POSITIVE_INFINITY
                                    }
                                )
                            }
                            .sortedBy { it.second }
                            .map { RouteEntry(it.first) }
                    }
                    is RouteObjective.Ordnance -> {
                        livingStations.values
                            .let { stations ->
                                stations.zip(
                                    stations.map {
                                        if (it.ordnanceStock[objective.ordnanceType] == 0)
                                            Float.POSITIVE_INFINITY
                                        else
                                            playerShip?.let { player ->
                                                withContext(cpu.coroutineContext) {
                                                    graph.calculateRouteCost(player, it.obj)
                                                }
                                            } ?: Float.POSITIVE_INFINITY
                                    }
                                )
                            }
                            .sortedBy { it.second }
                            .map { RouteEntry(it.first) }
                    }
                }
        }
    }

    /** Returns the string that displays time left for an ally to finish building torpedoes. */
    fun getManufacturingTimer(context: Context): String =
        context.getString(
            R.string.manufacturing_torpedoes,
            TimerText.getTimeUntil(torpedoFinishTime),
        )

    /** Checks to see whether the given object still exists. */
    private fun checkRoutePointExists(entry: ObjectEntry<*>): Boolean =
        entry.obj.id.let { id -> allyShips.containsKey(id) || livingStations.containsKey(id) }

    fun formattedHeading(heading: Int): String =
        heading.toString().padStart(if (threeDigitDirections) PADDED_ZEROES else 0, '0')

    /**
     * Calculates the heading from the player ship to the given object and formats it as a string.
     */
    private fun calculatePlayerHeadingTo(obj: ArtemisObject<*>): String {
        val heading = playerShip?.run { headingTo(obj).toDouble().toInt() } ?: 0
        return formattedHeading(heading)
    }

    /** Calculates the distance from the player ship to the given object. */
    private fun calculatePlayerRangeTo(obj: ArtemisObject<*>): Float =
        playerShip?.distanceTo(obj) ?: 0f

    /** Selects a player ship by its index. */
    fun selectShip(index: Int) {
        playSound(SoundEffect.CONFIRMATION)
        cpu.launch {
            if (shipIndex.value != index) {
                playerChange = true
                shipIndex.value = index
            }
            sendToServer(
                SetShipPacket(index),
                SetConsolePacket(Console.COMMUNICATIONS),
                SetConsolePacket(Console.MAIN_SCREEN),
                SetConsolePacket(Console.SINGLE_SEAT_CRAFT),
                ReadyPacket(),
            )
            graph = null
        }
    }

    /**
     * Signals a connection attempt to the UI, while also terminating the current server connection,
     * if any.
     */
    fun connectToServer() {
        disconnectFromServer(resetUrl = false)
        connectionStatus.value = ConnectionStatus.Connecting
        playSound(SoundEffect.CONFIRMATION)
    }

    /**
     * Attempts to connect to a running Artemis server, then sends to result of the attempt to the
     * UI.
     */
    fun tryConnect(url: String) {
        cpu.launch {
            // Allow only one connection attempt at a time
            attemptingConnection = true

            val connected =
                networkInterface.connect(
                    host = url,
                    port = port,
                    timeoutMs = connectTimeout.seconds.inWholeMilliseconds,
                )
            lastAttemptedHost = url
            attemptingConnection = false

            if (connected) {
                networkInterface.start()
            } else {
                connectionStatus.value = ConnectionStatus.Failed
                playSound(SoundEffect.DISCONNECTED)
            }
        }
    }

    /** Terminates the current server connection. */
    fun disconnectFromServer(resetUrl: Boolean = true) {
        playerChange = false
        endGame()
        if (resetUrl) {
            playSound(SoundEffect.DISCONNECTED)
            connectedUrl.value = ""
            shipIndex.value = -1
        }
        if (isConnected) {
            networkInterface.stop()
        }
    }

    /** Sends one or more packets to the server. */
    fun sendToServer(vararg packets: Packet.Client) {
        if (isConnected) {
            cpu.launch { packets.forEach(networkInterface::sendPacket) }
        }
    }

    /** Plays a sound effect if sound effects are enabled. */
    fun playSound(sound: SoundEffect) {
        if (playSounds) {
            sounds[sound.ordinal]?.also { player ->
                player.setVolume(volume, volume)
                player.start()
            }
        }
    }

    fun activateHaptic(effect: HapticEffect = HapticEffect.CLICK) {
        if (!hapticsEnabled) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(effect.vibration)
        } else {
            @Suppress("DEPRECATION") vibrator.vibrate(effect.duration)
        }
    }

    /** Begins scanning for servers via UDP. */
    fun scanForServers(broadcastAddress: String?) {
        isScanningUDP.tryEmit(true)
        discoveredServers.value = emptyList()
        cpu.launch {
            try {
                serverDiscoveryRequester.run(
                    broadcastAddress?.takeUnless { alwaysScanPublicBroadcasts }
                        ?: ServerDiscoveryRequester.DEFAULT_BROADCAST_ADDRESS
                )
            } catch (_: Exception) {
                isScanningUDP.emit(false)
            }
        }
    }

    /** When a server is discovered via UDP, adds it to the current list of discovered servers. */
    override suspend fun onDiscovered(server: Server) {
        val servers = discoveredServers.value.toMutableList()
        servers.add(server)
        discoveredServers.value = servers
    }

    /** Called when the UDP server discovery requester is finished listening. */
    override suspend fun onQuit() {
        isScanningUDP.emit(false)
    }

    /** Called at game end or server disconnect. Clears all data related to the last game. */
    private fun endGame() {
        routeJob?.also {
            it.cancel()
            routeJob = null
            routeRunning = false
        }
        graph = null
        isBorderWarPossible = false
        isDeepStrikePossible = false
        cpu.clear()
        playerIndex.fill(-1)
        players.clear()
        fighterIDs.clear()
        fightersInBays = 0
        missionManager.reset()
        focusedAlly.value = null
        allyShipIndex.clear()
        allyShips.clear()
        destroyedAllies.value = emptyList()
        destroyedStations.value = emptyList()
        livingStationNameIndex.clear()
        livingStationFullNameIndex.clear()
        enemyStationNameIndex.clear()
        livingStations.clear()
        livingEnemyStations.clear()
        stationsRemain.value = false
        enemiesManager.reset()
        biomechManager.reset()
        miscManager.reset()
        onPlayerShipDisposed()
        alliesExist = false
        stationsExist.value = false
        enemyStationsExist.value = false
        stationName.value = ""
        cpu.launch { updateObjects() }
    }

    private suspend fun updateObjects() {
        if (jumping.value) return

        val includingAllies = alliesEnabled || isDeepStrike

        val startTime = System.currentTimeMillis()
        if (playerChange) {
            playerShip?.getVessel(vesselData)?.also { vessel ->
                val isPirate = vessel.side == PIRATE_SIDE
                allyShips.values.forEach {
                    it.status = it.status.getPirateSensitiveEquivalent(isPirate)
                }
                playerChange = false
            }
        }

        val missionList =
            missionManager.run {
                if (enabled) {
                    if (autoDismissCompletedMissions) {
                        allMissions.removeAll(
                            allMissions.filter { it.completionTimestamp < startTime }.toSet()
                        )
                    }
                    allMissions.filter {
                        displayedRewards.any { reward -> it.rewards[reward.ordinal] > 0 } &&
                            (!it.isStarted || it.associatedShipName == playerName)
                    }
                } else {
                    emptyList()
                }
            }

        val allyShipList =
            if (includingAllies) {
                allyShips.values.sortedWith(allySorter).onEach {
                    it.heading = calculatePlayerHeadingTo(it.obj)
                    it.range = calculatePlayerRangeTo(it.obj)
                }
            } else {
                emptyList()
            }

        val focusedStation =
            if (livingStations.isEmpty()) {
                null
            } else {
                val closestName =
                    livingStationNameIndex
                        .minByOrNull { (_, id) ->
                            livingStations[id]?.let { entry ->
                                calculatePlayerRangeTo(entry.obj).also {
                                    entry.heading = calculatePlayerHeadingTo(entry.obj)
                                    entry.range = it
                                }
                            } ?: Float.POSITIVE_INFINITY
                        }
                        ?.key ?: ""
                closestStationName.value = closestName

                livingStationNameIndex[stationName.value]
                    ?.let(livingStations::get)
                    ?.also(currentStation::tryEmit)
            }
        val enemyStationList =
            enemyStationNameIndex.values
                .mapNotNull { livingEnemyStations[it] }
                .onEach {
                    it.heading = calculatePlayerHeadingTo(it.obj)
                    it.range = calculatePlayerRangeTo(it.obj)
                }

        val selectedEnemyEntry = enemiesManager.selection.value
        val enemyShipList = enemiesManager.allEnemies.values.filter { !it.vessel.isSingleseat }
        val enemySorter = enemiesManager.sorter
        val scannedEnemies =
            enemyShipList
                .filter { playerShip?.let(it.enemy::hasBeenScannedBy)?.booleanValue == true }
                .sortedWith(enemySorter)
                .onEach { entry ->
                    val enemy = entry.enemy
                    entry.heading = calculatePlayerHeadingTo(enemy)
                    entry.range = calculatePlayerRangeTo(enemy)
                }
        val enemyNavOptions = enemySorter.buildCategoryMap(scannedEnemies)

        val biomechList =
            if (biomechManager.enabled) {
                biomechManager.scanned.sortedWith(biomechManager.sorter).onEach {
                    if (it.onFreezeTimeExpired(startTime - biomechManager.freezeTime)) {
                        biomechManager.nextActiveBiomech.tryEmit(it)
                        biomechManager.notifyUpdate()
                    }
                }
            } else {
                emptyList()
            }

        if (isDeepStrike && !torpedoesReady && torpedoFinishTime < startTime) {
            torpedoesReady = true
        }

        when (currentGamePage.value) {
            GameFragment.Page.MISSIONS -> missionManager.hasUpdate = false
            GameFragment.Page.ENEMIES -> enemiesManager.hasUpdate = false
            GameFragment.Page.BIOMECHS -> biomechManager.resetUpdate()
            GameFragment.Page.MISC -> miscManager.resetUpdate()
            else -> {}
        }

        val (surrendered, hostile) =
            enemyShipList.partition { it.enemy.isSurrendered.value.booleanValue }

        val postedInventory =
            arrayOf(
                livingStations.size.takeIf { stationsExist.value },
                enemyStationList.size.takeIf { enemyStationsExist.value },
                allyShipList.size.takeIf { alliesExist },
                missionList.size.takeIf { missionManager.confirmed },
                biomechList.size.takeIf { biomechManager.confirmed },
                hostile.size,
                surrendered.size.takeIf { it > 0 },
            )

        if (!postedInventory.contentEquals(inventory.value)) {
            inventory.value = postedInventory
        }

        totalFighters.value = fightersInBays + fighterIDs.size
        ordnanceUpdated.value = false

        val flashTime = startTime % SECONDS_TO_MILLIS
        val flashOn = flashTime < FLASH_INTERVAL

        val stationShieldPercents =
            livingStationNameIndex.mapNotNull {
                livingStations[it.value]?.let { station ->
                    station to station.obj.shieldsFront.percentage
                }
            }
        val stationMinimumShieldPercent =
            stationShieldPercents
                .takeIf { flashOn }
                ?.minOfOrNull { (station, percent) ->
                    if (station == focusedStation) 1f else percent
                } ?: 1f
        val stationFlashOn = flashOn && stationMinimumShieldPercent < 1f

        val currentFlashOn = flashOn && focusedStation?.run { obj.shieldsFront.isDamaged } == true

        val pagesWithFlash = sortedMapOf<GameFragment.Page, Boolean>()

        if (gameIsRunning.value) {
            gamePages.value.also(pagesWithFlash::putAll)
            GameFragment.Page.entries.forEach { page ->
                val oldFlash = pagesWithFlash[page]
                when {
                    oldFlash == true -> pagesWithFlash[page] = flashOn
                    flashOn -> {
                        when (page) {
                            GameFragment.Page.STATIONS -> currentFlashOn || stationFlashOn
                            GameFragment.Page.ALLIES ->
                                allyShips
                                    .takeIf { includingAllies && alliesExist }
                                    ?.values
                                    ?.any { it.isDamaged }
                            GameFragment.Page.MISSIONS -> missionManager.shouldFlash
                            GameFragment.Page.ENEMIES -> enemiesManager.shouldFlash
                            GameFragment.Page.BIOMECHS -> biomechManager.shouldFlash
                            GameFragment.Page.ROUTE ->
                                false.takeIf { stationsExist.value && routingEnabled }
                            GameFragment.Page.MISC -> miscManager.shouldFlash
                        }?.also { pagesWithFlash[page] = it }
                    }
                }
            }
        }

        val ally = if (isSingleAlly) allyShipList.firstOrNull() else focusedAlly.value
        focusedAlly.value = ally
        defendableTargets.tryEmit(
            mutableListOf<ArtemisShielded<*>>().apply {
                if (ally != null) {
                    addAll(livingStationNameIndex.values.mapNotNull { livingStations[it]?.obj })
                    addAll(allyShipList.filter { it != ally }.map { it.obj })
                    addAll(players.values)
                }
            }
        )

        missionManager.missions.tryEmit(missionList)
        livingAllies.tryEmit(allyShipList)
        enemyStations.tryEmit(enemyStationList)
        enemiesManager.displayedEnemies.tryEmit(scannedEnemies)
        enemiesManager.categories.tryEmit(enemyNavOptions)
        biomechManager.allBiomechs.tryEmit(biomechList)

        enemiesManager.refreshTaunts()
        enemiesManager.intel.value = selectedEnemyEntry?.intel
        enemiesManager.selectionIndex.tryEmit(
            selectedEnemyEntry?.let { entry ->
                scannedEnemies.indexOfFirst { it.enemy == entry.enemy }
            } ?: -1
        )

        gamePages.value = pagesWithFlash
        flashingStations.value =
            stationShieldPercents.map { (station, percent) ->
                Pair(station, flashOn && percent < 1f)
            }
        stationSelectorFlashPercent.value = stationMinimumShieldPercent

        doubleAgentText.value =
            doubleAgentSecondsLeft.let {
                if (it < 0) "${playerShip?.doubleAgentCount?.value?.coerceAtLeast(0) ?: 0}"
                else it.seconds.timerString(false)
            }

        if (routingEnabled && gameIsRunning.value) {
            val objective = routeObjective.value

            if (!routeRunning) {
                routeRunning = true
                routeJob =
                    cpu.launch {
                        while (routeRunning) {
                            val routeGraph =
                                graph
                                    ?: playerShip?.let { RoutingGraph(this@AgentViewModel, it) }
                                    ?: continue

                            if (graph == null) {
                                graph = routeGraph
                            }
                            if (objective == RouteObjective.Tasks) {
                                routeGraph.preprocessObjectsToAvoid()
                                routeGraph.resetGraph()

                                if (routeIncludesMissions) {
                                    missionManager.allMissions.forEach { mission ->
                                        if (
                                            missionManager.displayedRewards.none {
                                                mission.rewards[it.ordinal] > 0
                                            } ||
                                                mission.isCompleted ||
                                                !checkRoutePointExists(mission.destination)
                                        ) {
                                            return@forEach
                                        }
                                        if (mission.isStarted) {
                                            if (mission.associatedShipName != playerName) {
                                                return@forEach
                                            }
                                            routeGraph.addPath(mission.destination)
                                        } else if (checkRoutePointExists(mission.source)) {
                                            routeGraph.addPath(mission.source, mission.destination)
                                        }
                                    }
                                }

                                allyShips.values
                                    .filter { ally ->
                                        !ally.isTrap && routeIncentives.any { it.matches(ally) }
                                    }
                                    .forEach { routeGraph.addPath(it) }

                                routeGraph.purgePaths()
                                routeGraph.testRoute(routeMap[objective])

                                routeGraph.preprocessCosts()
                                routeGraph.searchForRoute()?.also { routeMap[objective] = it }
                            }
                        }
                    }
            }

            calculateRoute()
            routeMap[objective]?.also(routeList::tryEmit)
        }

        delay(0L.coerceAtLeast(updateObjectsInterval + startTime - System.currentTimeMillis()))
    }

    internal fun checkGameStart() {
        if (gameIsRunning.value) return
        gameIsRunning.value = true
        if (updateJob == null) {
            updateJob =
                cpu.launch {
                    while (gameIsRunning.value) {
                        updateObjects()
                    }
                }
        }
    }

    internal fun onPlayerShipDisposed() {
        if (playerShip != null || !gameIsRunning.value) return
        gameIsRunning.value = false
        updateJob?.also {
            it.cancel()
            updateJob = null
        }
    }

    fun activateDoubleAgent() {
        sendToServer(ActivateUpgradePacket(version))
    }

    @Listener
    fun onPacket(packet: VersionPacket) {
        version = packet.version
    }

    @Listener
    fun onPacket(packet: BayStatusPacket) {
        fightersInBays = packet.fighterCount
    }

    @Listener
    fun onConnect(@Suppress("UNUSED_PARAMETER") event: ConnectionEvent.Success) {
        connectionStatus.value = ConnectionStatus.Connected
        playSound(SoundEffect.CONNECTED)

        if (lastAttemptedHost != connectedUrl.value) {
            connectedUrl.value = lastAttemptedHost
        } else if (shipIndex.value >= 0) {
            selectShip(shipIndex.value)
        }
    }

    @Listener
    fun onDisconnect(event: ConnectionEvent.Disconnect) {
        disconnectCause.tryEmit(event.cause)
        connectionStatus.value = ConnectionStatus.NotConnected

        if (event.cause !is DisconnectCause.LocalDisconnect) {
            disconnectFromServer()
        }
    }

    @Listener
    fun onHeartbeatLost(@Suppress("UNUSED_PARAMETER") event: ConnectionEvent.HeartbeatLost) {
        connectionStatus.value = ConnectionStatus.HeartbeatLost
        playSound(SoundEffect.HEARTBEAT_LOST)
    }

    @Listener
    fun onHeartbeatRegained(
        @Suppress("UNUSED_PARAMETER") event: ConnectionEvent.HeartbeatRegained
    ) {
        connectionStatus.value = ConnectionStatus.Connected
        playSound(SoundEffect.BEEP_2)
    }

    @Listener
    fun onPacket(packet: AllShipSettingsPacket) {
        selectableShips.value = packet.ships
    }

    @Listener
    fun onPacket(packet: PlayerShipDamagePacket) {
        if (packet.shipIndex == shipIndex.value) {
            val durationInMillis = (SECONDS_TO_MILLIS * packet.duration).toLong()
            damageVisJob?.cancel()
            damageVisJob =
                viewModelScope.launch {
                    rootOpacity.value = DAMAGED_ALPHA
                    delay(durationInMillis)
                    rootOpacity.value = 1f
                }
        }
    }

    @Listener
    fun onPacket(packet: DockedPacket) {
        players[packet.objectId]?.docked = BoolState.True
    }

    @Listener
    fun onPacket(packet: GameStartPacket) {
        playerChange = false
        when (packet.gameType) {
            GameType.BORDER_WAR -> {
                borderWarStatus.value = WarStatus.TENSION
                isBorderWarPossible = true
            }
            GameType.DEEP_STRIKE -> isDeepStrikePossible = true
            else -> {} // make `when` exhaustive
        }
    }

    @Listener
    fun onPacket(packet: GameOverReasonPacket) {
        endGame()
        gameOverReason.tryEmit(packet.text.joinToString("\n").substring(GAME_OVER_REASON_INDEX))
    }

    @Listener
    fun onPacket(@Suppress("UNUSED_PARAMETER") packet: EndGamePacket) {
        endGame()
    }

    @Listener
    fun onPacket(packet: PausePacket) {
        val isPaused = packet.isPaused.booleanValue
        livingStations.values.forEach { it.isPaused = isPaused }
    }

    @Listener
    fun onPacket(@Suppress("UNUSED_PARAMETER") packet: JumpEndPacket) {
        viewModelScope.launch {
            jumping.value = true
            delay(JUMP_DURATION)
            jumping.value = false
        }
    }

    internal fun <Obj : ArtemisObject<Obj>> onDeleteObstacle(
        id: Int,
        map: ConcurrentHashMap<Int, Obj>,
    ) {
        map.remove(id)?.also { graph?.removeObstacle(it) }
    }

    override fun onCleared() {
        disconnectFromServer(resetUrl = false)
        networkInterface.dispose()

        volume = 0f
        sounds.forEach { it?.release() }
        sounds.clear()

        super.onCleared()
    }

    fun updateFromSettings(settings: UserSettings) {
        vesselDataManager.index = settings.vesselDataLocationValue
        port = settings.serverPort
        updateObjectsInterval = settings.updateInterval

        connectTimeout = settings.connectionTimeoutSeconds
        scanTimeout = settings.scanTimeoutSeconds
        heartbeatTimeout = settings.serverTimeoutSeconds
        showingNetworkInfo = settings.showNetworkInfo
        alwaysScanPublicBroadcasts = settings.alwaysScanPublic

        missionManager.updateFromSettings(settings)

        alliesEnabled = settings.alliesEnabled
        allySorter =
            AllySorter(
                sortByClassFirst = settings.allySortClassFirst,
                sortByEnergy = settings.allySortEnergyFirst,
                sortByStatus = settings.allySortStatus,
                sortByClassSecond = settings.allySortClassSecond,
                sortByName = settings.allySortName,
            )
        showAllySelector = settings.showDestroyedAllies
        manuallyReturnFromCommands = settings.allyCommandManualReturn

        biomechManager.updateFromSettings(settings)

        routingEnabled = settings.routingEnabled
        routeIncludesMissions = settings.routeMissions
        routeIncentives =
            listOfNotNull(
                RouteTaskIncentive.NEEDS_ENERGY.takeIf { settings.routeNeedsEnergy },
                RouteTaskIncentive.NEEDS_DAMCON.takeIf { settings.routeNeedsDamcon },
                RouteTaskIncentive.RESET_COMPUTER.takeIf { settings.routeMalfunction },
                RouteTaskIncentive.AMBASSADOR_PICKUP.takeIf { settings.routeAmbassador },
                RouteTaskIncentive.HOSTAGE.takeIf { settings.routeHostage },
                RouteTaskIncentive.COMMANDEERED.takeIf { settings.routeCommandeered },
                RouteTaskIncentive.HAS_ENERGY.takeIf { settings.routeHasEnergy },
            )

        enemiesManager.updateFromSettings(settings)

        avoidBlackHoles = settings.avoidBlackHoles
        avoidMines = settings.avoidMines
        avoidTyphons = settings.avoidTyphon

        blackHoleClearance = settings.blackHoleClearance
        mineClearance = settings.mineClearance
        typhonClearance = settings.typhonClearance

        threeDigitDirections = settings.threeDigitDirections
        volume = settings.soundVolume.toFloat()
        soundsMuted = settings.soundMuted
        hapticsEnabled = settings.hapticsEnabled

        val newThemeRes = ALL_THEMES[settings.themeValue]
        if (themeRes != newThemeRes) {
            themeRes = newThemeRes
            isThemeChanged.value = true
        }
    }

    fun revertSettings(settings: UserSettings): UserSettings =
        settings.copy {
            vesselDataLocationValue = vesselDataManager.index
            serverPort = port
            updateInterval = updateObjectsInterval

            connectionTimeoutSeconds = connectTimeout
            scanTimeoutSeconds = scanTimeout
            serverTimeoutSeconds = heartbeatTimeout

            missionManager.revertSettings(this)

            alliesEnabled = this@AgentViewModel.alliesEnabled
            allySortClassFirst = allySorter.sortByClassFirst
            allySortEnergyFirst = allySorter.sortByEnergy
            allySortStatus = allySorter.sortByStatus
            allySortClassSecond = allySorter.sortByClassSecond
            allySortName = allySorter.sortByName
            showDestroyedAllies = showAllySelector
            allyCommandManualReturn = manuallyReturnFromCommands

            biomechManager.revertSettings(this)

            routingEnabled = this@AgentViewModel.routingEnabled
            routeMissions = routeIncludesMissions

            enemiesManager.revertSettings(this)

            val incentiveSettings =
                mapOf(
                    RouteTaskIncentive.NEEDS_ENERGY to this::routeNeedsEnergy,
                    RouteTaskIncentive.NEEDS_DAMCON to this::routeNeedsDamcon,
                    RouteTaskIncentive.RESET_COMPUTER to this::routeMalfunction,
                    RouteTaskIncentive.AMBASSADOR_PICKUP to this::routeAmbassador,
                    RouteTaskIncentive.HOSTAGE to this::routeHostage,
                    RouteTaskIncentive.COMMANDEERED to this::routeCommandeered,
                    RouteTaskIncentive.HAS_ENERGY to this::routeHasEnergy,
                )
            incentiveSettings.values.forEach { it.set(false) }
            routeIncentives.forEach { incentiveSettings[it]?.set(true) }

            avoidBlackHoles = this@AgentViewModel.avoidBlackHoles
            avoidMines = this@AgentViewModel.avoidMines
            avoidTyphon = this@AgentViewModel.avoidTyphons

            blackHoleClearance = this@AgentViewModel.blackHoleClearance
            mineClearance = this@AgentViewModel.mineClearance
            typhonClearance = this@AgentViewModel.typhonClearance

            threeDigitDirections = this@AgentViewModel.threeDigitDirections
            soundVolume = (volume * VOLUME_SCALE).toInt()
            soundMuted = this@AgentViewModel.soundsMuted
            themeValue = ALL_THEMES.indexOf(themeRes)
            showNetworkInfo = showingNetworkInfo
            alwaysScanPublic = alwaysScanPublicBroadcasts
            hapticsEnabled = this@AgentViewModel.hapticsEnabled
        }

    companion object {
        private const val DEFAULT_PORT = 2010
        private const val DEFAULT_SCAN_TIMEOUT = 5
        private const val DEFAULT_CONNECT_TIMEOUT = 9
        private const val DEFAULT_HEARTBEAT_TIMEOUT = 15

        private const val DEFAULT_BLACK_HOLE_CLEARANCE = 500f
        private const val DEFAULT_MINE_CLEARANCE = 1000f
        private const val DEFAULT_TYPHON_CLEARANCE = 3000f

        const val DEFAULT_UPDATE_INTERVAL = 50
        const val FLASH_INTERVAL = 500L
        const val SECONDS_TO_MILLIS = 1000
        private const val PIRATE_SIDE = 8
        private const val DAMAGED_ALPHA = 0.5f
        private const val JUMP_DURATION = 3000L
        const val FULL_HEADING_RANGE = 360
        const val VOLUME_SCALE = 100f
        private const val PADDED_ZEROES = 3

        private const val GAME_OVER_REASON_INDEX = 13

        val PLURALS_FOR_INVENTORY =
            arrayOf(
                R.plurals.friendly_stations,
                R.plurals.enemy_stations,
                R.plurals.allies,
                R.plurals.side_missions,
                R.plurals.biomechs,
                R.plurals.enemies,
                R.plurals.surrenders,
            )

        private val ALL_THEMES =
            arrayOf(
                R.style.Theme_ArtemisAgent,
                R.style.Theme_ArtemisAgent_Red,
                R.style.Theme_ArtemisAgent_Green,
                R.style.Theme_ArtemisAgent_Yellow,
                R.style.Theme_ArtemisAgent_Blue,
                R.style.Theme_ArtemisAgent_Purple,
                R.style.Theme_ArtemisAgent_Orange,
            )

        fun Number.formatString(): String = toString().format(Locale.getDefault())
    }
}
