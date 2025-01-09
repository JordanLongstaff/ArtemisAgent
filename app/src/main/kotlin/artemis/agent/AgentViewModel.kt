package artemis.agent

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.StyleRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import artemis.agent.UserSettingsOuterClass.UserSettings
import artemis.agent.cpu.CPU
import artemis.agent.cpu.RoutingGraph
import artemis.agent.cpu.RoutingGraph.Companion.calculateRouteCost
import artemis.agent.cpu.listeners
import artemis.agent.game.GameFragment
import artemis.agent.game.ObjectEntry
import artemis.agent.game.WarStatus
import artemis.agent.game.allies.AllySorter
import artemis.agent.game.biomechs.BiomechEntry
import artemis.agent.game.biomechs.BiomechRageStatus
import artemis.agent.game.biomechs.BiomechSorter
import artemis.agent.game.enemies.EnemyEntry
import artemis.agent.game.enemies.EnemySortCategory
import artemis.agent.game.enemies.EnemySorter
import artemis.agent.game.enemies.TauntStatus
import artemis.agent.game.misc.AudioEntry
import artemis.agent.game.misc.CommsActionEntry
import artemis.agent.game.missions.RewardType
import artemis.agent.game.missions.SideMissionEntry
import artemis.agent.game.route.RouteEntry
import artemis.agent.game.route.RouteObjective
import artemis.agent.game.route.RouteTaskIncentive
import artemis.agent.game.stations.StationsFragment
import artemis.agent.help.HelpFragment
import artemis.agent.setup.ConnectFragment.ConnectionStatus
import artemis.agent.setup.SetupFragment
import artemis.agent.setup.settings.SettingsFragment
import artemis.agent.util.AssetsResolver
import artemis.agent.util.SoundEffect
import artemis.agent.util.TimerText
import artemis.agent.util.TimerText.timerString
import com.walkertribe.ian.enums.AlertStatus
import com.walkertribe.ian.enums.AudioMode
import com.walkertribe.ian.enums.Console
import com.walkertribe.ian.enums.GameType
import com.walkertribe.ian.enums.ObjectType
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
import com.walkertribe.ian.protocol.core.comm.CommsButtonPacket
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket
import com.walkertribe.ian.protocol.core.comm.IncomingAudioPacket
import com.walkertribe.ian.protocol.core.setup.AllShipSettingsPacket
import com.walkertribe.ian.protocol.core.setup.ReadyPacket
import com.walkertribe.ian.protocol.core.setup.SetConsolePacket
import com.walkertribe.ian.protocol.core.setup.SetShipPacket
import com.walkertribe.ian.protocol.core.setup.Ship
import com.walkertribe.ian.protocol.core.setup.VersionPacket
import com.walkertribe.ian.protocol.core.world.BiomechRagePacket
import com.walkertribe.ian.protocol.core.world.DeleteObjectPacket
import com.walkertribe.ian.protocol.core.world.DockedPacket
import com.walkertribe.ian.protocol.udp.Server
import com.walkertribe.ian.protocol.udp.ServerDiscoveryRequester
import com.walkertribe.ian.util.BoolState
import com.walkertribe.ian.util.FilePathResolver
import com.walkertribe.ian.util.Util.joinSpaceDelimited
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.vesseldata.Taunt
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.vesseldata.VesselDataObject
import com.walkertribe.ian.world.Artemis
import com.walkertribe.ian.world.ArtemisBlackHole
import com.walkertribe.ian.world.ArtemisCreature
import com.walkertribe.ian.world.ArtemisMine
import com.walkertribe.ian.world.ArtemisNpc
import com.walkertribe.ian.world.ArtemisObject
import com.walkertribe.ian.world.ArtemisPlayer
import com.walkertribe.ian.world.ArtemisShielded
import com.walkertribe.ian.world.Property
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.time.Duration.Companion.milliseconds
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
import okio.Path.Companion.toOkioPath

/** The view model containing all running client data and utility functions used by the UI. */
class AgentViewModel(application: Application) :
    AndroidViewModel(application), ServerDiscoveryRequester.Listener {
    // Connection status
    val networkInterface: ArtemisNetworkInterface by lazy {
        KtorArtemisNetworkInterface(debugMode = BuildConfig.DEBUG).also {
            it.addListeners(listeners + cpu.listeners)
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

    // UDP discovered servers
    val discoveredServers: MutableStateFlow<List<Server>> by lazy { MutableStateFlow(listOf()) }
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
        get() {
            return ALL_THEMES.indexOf(themeRes)
        }
        set(index) {
            themeRes = ALL_THEMES[index]
        }

    val rootOpacity: MutableStateFlow<Float> by lazy { MutableStateFlow(1f) }
    val jumping: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }

    // Ship settings from packet
    val selectableShips: MutableStateFlow<List<Ship>> by lazy { MutableStateFlow(listOf()) }

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
        MutableStateFlow(mapOf())
    }
    val currentGamePage: MutableStateFlow<GameFragment.Page?> by lazy { MutableStateFlow(null) }

    // Page activator data
    var alliesExist: Boolean = false
    var biomechsExist: Boolean = false
    val stationsExist: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }
    val enemyStationsExist: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }

    // Miscellaneous Comms actions
    val miscActionsExist: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }
    val miscAudioExists: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }
    val commsActionSet = CopyOnWriteArraySet<CommsActionEntry>()
    val commsAudioSet = CopyOnWriteArraySet<AudioEntry>()
    val miscActions: MutableStateFlow<List<CommsActionEntry>> by lazy { MutableStateFlow(listOf()) }
    val miscAudio: MutableStateFlow<List<AudioEntry>> by lazy { MutableStateFlow(listOf()) }
    val showingAudio: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }

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
    var missionsEnabled: Boolean = true
    var missionsExist: Boolean = false
    internal val allMissions = CopyOnWriteArrayList<SideMissionEntry>()
    val missions: MutableSharedFlow<List<SideMissionEntry>> by lazy {
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    // Completed mission payout data
    val payouts = IntArray(RewardType.entries.size)
    var displayedRewards: Array<RewardType> = arrayOf()
    val displayedPayouts: MutableStateFlow<List<Pair<RewardType, Int>>> by lazy {
        MutableStateFlow(listOf())
    }
    val showingPayouts: MutableStateFlow<Boolean> by lazy { MutableStateFlow(false) }

    // Mission progress packet data
    val newMissionPacket: MutableSharedFlow<CommsIncomingPacket> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val missionProgressPacket: MutableSharedFlow<CommsIncomingPacket> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val missionCompletionPacket: MutableSharedFlow<CommsIncomingPacket> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

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
        ConcurrentSkipListMap<String, Int>(Comparator(this::compareFriendlyStationNames))
    val livingStationFullNameIndex = ConcurrentHashMap<String, Int>()
    val livingStations = ConcurrentHashMap<Int, ObjectEntry.Station>()

    // Friendly station navigation data
    val flashingStations: MutableStateFlow<List<Pair<ObjectEntry.Station, Boolean>>> by lazy {
        MutableStateFlow(listOf())
    }
    val stationName: MutableStateFlow<String> by lazy { MutableStateFlow("") }
    val currentStation: MutableSharedFlow<ObjectEntry.Station> by lazy {
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val closestStationName: MutableStateFlow<String> by lazy { MutableStateFlow("") }

    // Enemy station data
    val enemyStationNameIndex =
        ConcurrentSkipListMap<String, Int>(Comparator(this::compareEnemyStationNames))
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
    val destroyedAllies: MutableStateFlow<List<String>> by lazy { MutableStateFlow(listOf()) }
    val destroyedStations: MutableStateFlow<List<String>> by lazy { MutableStateFlow(listOf()) }

    // Biomech data
    var biomechsEnabled: Boolean = true
    val biomechs: MutableSharedFlow<List<BiomechEntry>> by lazy {
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    var biomechSorter = BiomechSorter()
    val scannedBiomechs = CopyOnWriteArrayList<BiomechEntry>()
    val unscannedBiomechs = ConcurrentHashMap<Int, ArtemisNpc>()

    // Biomech rage data
    private val biomechRageProperty = Property.IntProperty(Long.MIN_VALUE)
    val biomechRage: MutableStateFlow<BiomechRageStatus> by lazy {
        MutableStateFlow(BiomechRageStatus.NEUTRAL)
    }

    // Biomech notification data
    val nextActiveBiomech: MutableSharedFlow<BiomechEntry> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val destroyedBiomechName: MutableSharedFlow<String> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    // Enemy ship data
    var enemiesEnabled: Boolean = true
    val selectedEnemy: MutableStateFlow<EnemyEntry?> by lazy { MutableStateFlow(null) }
    val selectedEnemyIndex: MutableStateFlow<Int> by lazy { MutableStateFlow(-1) }
    val displayedEnemies: MutableSharedFlow<List<EnemyEntry>> by lazy {
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val enemyCategories: MutableStateFlow<List<EnemySortCategory>> by lazy {
        MutableStateFlow(listOf())
    }
    val enemyTaunts: MutableStateFlow<List<Pair<Taunt, TauntStatus>>> by lazy {
        MutableStateFlow(listOf())
    }
    val enemyIntel: MutableStateFlow<String?> by lazy { MutableStateFlow(null) }
    val perfidiousEnemy: MutableSharedFlow<EnemyEntry> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    val destroyedEnemyName: MutableSharedFlow<String> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    var enemySorter = EnemySorter()
    val enemyNameIndex = ConcurrentHashMap<String, Int>()
    val enemies = ConcurrentHashMap<Int, EnemyEntry>()
    var showTauntStatuses: Boolean = true
    var showEnemyIntel: Boolean = true
    var disableIneffectiveTaunts: Boolean = true
    var maxSurrenderDistance: Float? = Float.MAX_VALUE

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

    // Page flash variables
    var missionUpdate: Boolean = false
    var enemiesUpdate: Boolean = false
    var biomechUpdate: Boolean = false
    private var miscUpdate: Boolean = false

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
    var heartbeatTimeout: Long = DEFAULT_HEARTBEAT_TIMEOUT
        set(value) {
            field = value
            ifConnected { networkInterface.setTimeout(field.seconds.inWholeMilliseconds) }
        }

    var biomechFreezeTime: Long = DEFAULT_FREEZE_TIME.seconds.inWholeMilliseconds
        set(value) {
            field = value.seconds.inWholeMilliseconds
        }

    var autoDismissCompletedMissions: Boolean = true
    var completedDismissalTime: Long = DEFAULT_COMPLETED_DISMISSAL.seconds.inWholeMilliseconds
        set(value) {
            field = value.seconds.inWholeMilliseconds
        }

    // UDP server discovery requester
    private val serverDiscoveryRequester: ServerDiscoveryRequester
        get() =
            ServerDiscoveryRequester(
                listener = this@AgentViewModel,
                timeoutMs = scanTimeout.seconds.inWholeMilliseconds,
            )

    // I/O resolver data
    private val assetsResolver: AssetsResolver = AssetsResolver(application.assets)
    val storageDirectories: Array<File> = application.applicationContext.getExternalFilesDirs(null)

    // Artemis version
    var version: Version = Version.LATEST
        private set

    // Vessel data
    private val defaultVesselData: VesselData by lazy { VesselData.load(assetsResolver) }
    private val internalStorageVesselData: VesselData? by lazy {
        if (storageDirectories.isEmpty()) {
            null
        } else {
            setupFilePathResolver(storageDirectories[0])?.let(VesselData.Companion::load)
        }
    }
    private val externalStorageVesselData: VesselData? by lazy {
        if (storageDirectories.size <= 1) {
            null
        } else {
            setupFilePathResolver(storageDirectories[1])?.let(VesselData.Companion::load)
        }
    }

    var vesselDataIndex: Int = 0
        set(index) {
            if (field != index) {
                field = index
                vesselData =
                    when (index) {
                        1 -> internalStorageVesselData
                        2 -> externalStorageVesselData
                        else -> null
                    } ?: defaultVesselData
            }
        }

    var vesselData: VesselData = defaultVesselData
        private set

    private val allVesselData
        get() = arrayOf(defaultVesselData, internalStorageVesselData, externalStorageVesselData)

    // Sound effects players
    private val playSounds: Boolean
        get() = volume > 0f

    private val sounds: MutableList<MediaPlayer?> =
        SoundEffect.entries
            .map { MediaPlayer.create(application.applicationContext, it.soundId) }
            .toMutableList()
    var volume: Float = 1f
        set(value) {
            field = value / VOLUME_SCALE
        }

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
                                                graph.calculateRouteCost(
                                                    player.x.value,
                                                    player.z.value,
                                                    it.obj.x.value,
                                                    it.obj.z.value,
                                                )
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
                                        if (it.ordnanceStock[objective.ordnanceType] == 0) {
                                            Float.POSITIVE_INFINITY
                                        } else {
                                            playerShip?.let { player ->
                                                withContext(cpu.coroutineContext) {
                                                    graph.calculateRouteCost(
                                                        player.x.value,
                                                        player.z.value,
                                                        it.obj.x.value,
                                                        it.obj.z.value,
                                                    )
                                                }
                                            } ?: Float.POSITIVE_INFINITY
                                        }
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

    /** Returns the full name for the given object, including callsign, faction and vessel name. */
    fun getFullNameForShip(obj: ArtemisShielded<*>?): String =
        obj?.run { listOf(name.value, getFullNameForVessel(this)).joinSpaceDelimited() } ?: ""

    /** Returns the faction and model name of the given object's vessel. */
    fun getFullNameForVessel(obj: VesselDataObject?): String =
        obj?.getVessel(vesselData)?.run {
            listOfNotNull(getFaction(vesselData)?.name, name).joinSpaceDelimited()
        } ?: ""

    /** Determines how many currently applicable missions involve the given object. */
    private fun calculateMissionsFor(entry: ObjectEntry<*>, reward: RewardType): Int =
        allMissions
            .filter {
                val isDest = it.destination == entry
                if (it.isStarted) {
                    isDest && it.associatedShipName == playerName
                } else {
                    isDest || it.source == entry
                }
            }
            .sumOf { it.rewards[reward.ordinal] }

    private fun reconcileDisplayedMissions(
        battery: Boolean,
        coolant: Boolean,
        nukes: Boolean,
        production: Boolean,
        shieldBoost: Boolean,
    ) {
        val oldRewards = displayedRewards.copyOf()
        val newRewards =
            listOfNotNull(
                    RewardType.BATTERY.takeIf { battery },
                    RewardType.COOLANT.takeIf { coolant },
                    RewardType.NUKE.takeIf { nukes },
                    RewardType.PRODUCTION.takeIf { production },
                    RewardType.SHIELD.takeIf { shieldBoost },
                )
                .toTypedArray()
        displayedRewards = newRewards

        var oldIndex = 0
        var newIndex = 0
        val allObjects = livingStations.values.toList() + allyShips.values.toList()
        RewardType.entries.forEach { reward ->
            var missionsSignum = 0

            val inOldSet = oldIndex < oldRewards.size && oldRewards[oldIndex] == reward
            if (inOldSet) {
                oldIndex++
                missionsSignum--
            }
            val inNewSet = newIndex < newRewards.size && newRewards[newIndex] == reward
            if (inNewSet) {
                newIndex++
                missionsSignum++
            }

            if (missionsSignum != 0) {
                allObjects.forEach {
                    it.missions += missionsSignum * calculateMissionsFor(it, reward)
                }
            }
        }

        updatePayouts()
    }

    /**
     * Attempts to set up the default vessel data in a given file path if it does not exist, and
     * return a file path resolver using that file if successful, or null if there was an error.
     */
    private fun setupFilePathResolver(storageDir: File): FilePathResolver? {
        val datDir = File(storageDir, "dat")
        if (!datDir.exists()) datDir.mkdirs()

        return if (assetsResolver.copyVesselDataTo(datDir)) {
            FilePathResolver(storageDir.toOkioPath())
        } else {
            null
        }
    }

    fun reconcileVesselDataIndex(index: Int): Int = if (allVesselData[index] == null) 0 else index

    fun checkContext(index: Int, ifError: (String) -> Unit) {
        val vesselDataAtIndex = allVesselData[index]
        if (vesselDataAtIndex is VesselData.Error) {
            ifError(vesselDataAtIndex.message ?: "")
        }
    }

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
        ifConnected { networkInterface.stop() }
    }

    /** Sends one or more packets to the server. */
    fun sendToServer(vararg packets: Packet.Client) {
        ifConnected { cpu.launch { packets.forEach(networkInterface::sendPacket) } }
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

    /** Begins scanning for servers via UDP. */
    fun scanForServers(broadcastAddress: String?) {
        isScanningUDP.tryEmit(true)
        discoveredServers.value = listOf()
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

    /** Dismisses an audio message. */
    fun dismissAudio(entry: AudioEntry) {
        if (commsAudioSet.remove(entry)) {
            miscAudio.value = commsAudioSet.toList()
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
        missionUpdate = false
        enemiesUpdate = false
        biomechUpdate = false
        miscUpdate = false
        isBorderWarPossible = false
        isDeepStrikePossible = false
        cpu.clear()
        playerIndex.fill(-1)
        players.clear()
        fighterIDs.clear()
        fightersInBays = 0
        allMissions.clear()
        payouts.fill(0)
        focusedAlly.value = null
        allyShipIndex.clear()
        allyShips.clear()
        destroyedAllies.value = listOf()
        destroyedStations.value = listOf()
        livingStationNameIndex.clear()
        livingStationFullNameIndex.clear()
        enemyStationNameIndex.clear()
        livingStations.clear()
        livingEnemyStations.clear()
        stationsRemain.value = false
        scannedBiomechs.clear()
        unscannedBiomechs.clear()
        enemies.clear()
        enemyNameIndex.clear()
        selectedEnemy.value = null
        commsActionSet.clear()
        commsAudioSet.clear()
        miscActions.value = listOf()
        miscAudio.value = listOf()
        biomechRageProperty.value = 0
        biomechRage.value = BiomechRageStatus.NEUTRAL
        onPlayerShipDisposed()
        missionsExist = false
        alliesExist = false
        biomechsExist = false
        stationsExist.value = false
        enemyStationsExist.value = false
        stationName.value = ""
        miscActionsExist.value = false
        miscAudioExists.value = false
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
            if (missionsEnabled) {
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
                listOf()
            }

        val allyShipList =
            if (includingAllies) {
                allyShips.values.sortedWith(allySorter).onEach {
                    it.heading = calculatePlayerHeadingTo(it.obj)
                    it.range = calculatePlayerRangeTo(it.obj)
                }
            } else {
                listOf()
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

        val selectedEnemyEntry = selectedEnemy.value
        val enemyShipList = enemies.values.filter { !it.vessel.isSingleseat }
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
            if (biomechsEnabled) {
                scannedBiomechs.sortedWith(biomechSorter).onEach {
                    if (it.onFreezeTimeExpired(startTime - biomechFreezeTime)) {
                        nextActiveBiomech.tryEmit(it)
                        biomechUpdate = true
                    }
                }
            } else {
                listOf()
            }

        if (isDeepStrike && !torpedoesReady && torpedoFinishTime < startTime) {
            torpedoesReady = true
        }

        when (currentGamePage.value) {
            GameFragment.Page.MISSIONS -> missionUpdate = false
            GameFragment.Page.ENEMIES -> enemiesUpdate = false
            GameFragment.Page.BIOMECHS -> biomechUpdate = false
            GameFragment.Page.MISC -> miscUpdate = false
            else -> {}
        }

        val (surrendered, hostile) =
            enemyShipList.partition { it.enemy.isSurrendered.value.booleanValue }

        val postedInventory =
            arrayOf(
                livingStations.size.takeIf { stationsExist.value },
                enemyStationList.size.takeIf { enemyStationsExist.value },
                allyShipList.size.takeIf { alliesExist },
                missionList.size.takeIf { missionsExist },
                biomechList.size.takeIf { biomechsExist },
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
                    Pair(
                        station,
                        station.obj.shieldsFront.value / station.obj.shieldsFrontMax.value,
                    )
                }
            }
        val stationMinimumShieldPercent =
            stationShieldPercents
                .takeIf { flashOn }
                ?.minOfOrNull { (station, percent) ->
                    if (station == focusedStation) 1f else percent
                } ?: 1f
        val stationFlashOn = flashOn && stationMinimumShieldPercent < 1f

        val currentFlashOn =
            if (flashOn) {
                focusedStation?.let { it.obj.shieldsFront < it.obj.shieldsFrontMax } == true
            } else {
                false
            }

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
                            GameFragment.Page.MISSIONS ->
                                missionUpdate.takeIf { missionsEnabled && missionsExist }
                            GameFragment.Page.ENEMIES ->
                                enemiesUpdate.takeIf { enemiesEnabled && enemies.isNotEmpty() }
                            GameFragment.Page.BIOMECHS ->
                                biomechUpdate.takeIf { biomechsEnabled && biomechsExist }
                            GameFragment.Page.ROUTE ->
                                false.takeIf { stationsExist.value && routingEnabled }
                            GameFragment.Page.MISC ->
                                miscUpdate.takeIf {
                                    miscActionsExist.value || miscAudioExists.value
                                }
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

        missions.tryEmit(missionList)
        livingAllies.tryEmit(allyShipList)
        enemyStations.tryEmit(enemyStationList)
        displayedEnemies.tryEmit(scannedEnemies)
        enemyCategories.tryEmit(enemyNavOptions)
        biomechs.tryEmit(biomechList)

        refreshEnemyTaunts()
        enemyIntel.value = selectedEnemyEntry?.intel
        selectedEnemyIndex.tryEmit(
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
                if (it < 0) {
                    "${playerShip?.doubleAgentCount?.value?.coerceAtLeast(0) ?: 0}"
                } else {
                    it.seconds.timerString(false)
                }
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
                                    allMissions.forEach { mission ->
                                        if (
                                            displayedRewards.none {
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

    internal fun updatePayouts() {
        displayedPayouts.value = displayedRewards.map { Pair(it, payouts[it.ordinal]) }
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

    private fun compareFriendlyStationNames(firstNameOpt: String?, secondNameOpt: String?): Int {
        val firstName = firstNameOpt ?: ""
        val secondName = secondNameOpt ?: ""
        return if (STATION_CALLSIGN.matches(firstName) && STATION_CALLSIGN.matches(secondName)) {
            firstName.substring(2).toInt() - secondName.substring(2).toInt()
        } else {
            firstName.compareTo(secondName)
        }
    }

    private fun compareEnemyStationNames(firstNameOpt: String?, secondNameOpt: String?): Int {
        val firstName = firstNameOpt ?: ""
        val secondName = secondNameOpt ?: ""
        return if (ENEMY_STATION.matches(firstName) && ENEMY_STATION.matches(secondName)) {
            val firstIndex = firstName.run { substring(lastIndexOf(" ") + 1).toInt() }
            val secondIndex = secondName.run { substring(lastIndexOf(" ") + 1).toInt() }
            firstIndex - secondIndex
        } else {
            firstName.compareTo(secondName)
        }
    }

    fun refreshEnemyTaunts() {
        val enemy = selectedEnemy.value

        enemyTaunts.value = enemy?.run { faction.taunts.zip(tauntStatuses) }.orEmpty()
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

    private var damageVisJob: Job? = null

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
    fun onPacket(packet: IncomingAudioPacket) {
        val audioMode = packet.audioMode
        if (audioMode is AudioMode.Incoming) {
            commsAudioSet.add(AudioEntry(packet.audioId, audioMode.title))
            miscAudio.value = commsAudioSet.toList()
            miscAudioExists.value = true
            miscUpdate = true
        }
    }

    @Listener
    fun onPacket(packet: CommsButtonPacket) {
        when (val action = packet.action) {
            is CommsButtonPacket.Action.RemoveAll -> {
                commsActionSet.clear()
            }
            is CommsButtonPacket.Action.Create -> {
                commsActionSet.add(CommsActionEntry(action.label))
                miscActionsExist.value = true
                miscUpdate = true
            }
            is CommsButtonPacket.Action.Remove -> {
                if (!commsActionSet.remove(CommsActionEntry(action.label))) {
                    return
                }
            }
        }
        miscActions.value = commsActionSet.toList()
    }

    @Listener
    fun onPacket(packet: BiomechRagePacket) {
        val newRage = Property.IntProperty(packet.timestamp)
        newRage.value = packet.rage
        newRage updates biomechRageProperty
        biomechRage.value =
            BiomechRageStatus[biomechRageProperty.value].also {
                if (
                    biomechRage.value == BiomechRageStatus.NEUTRAL &&
                        it == BiomechRageStatus.HOSTILE
                ) {
                    biomechUpdate = true
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
        updatePayouts()
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

    @Listener
    fun onPacket(packet: DeleteObjectPacket) {
        val id = packet.target

        when (packet.targetType) {
            ObjectType.NPC_SHIP -> cpu.onNpcDelete(id)
            ObjectType.BASE -> cpu.onStationDelete(id)
            ObjectType.PLAYER_SHIP -> cpu.onPlayerDelete(id)
            ObjectType.MINE -> cpu.launch { mines.remove(id)?.also { graph?.removeObstacle(it) } }
            ObjectType.BLACK_HOLE ->
                cpu.launch { blackHoles.remove(id)?.also { graph?.removeObstacle(it) } }
            ObjectType.CREATURE ->
                cpu.launch { typhons.remove(id)?.also { graph?.removeObstacle(it) } }
            else -> {}
        }
    }

    internal inline fun <R> ifConnected(block: () -> R): R? =
        when (connectionStatus.value) {
            is ConnectionStatus.Connected,
            is ConnectionStatus.HeartbeatLost -> block()
            else -> null
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
        vesselDataIndex = settings.vesselDataLocationValue
        port = settings.serverPort
        updateObjectsInterval = settings.updateInterval

        connectTimeout = settings.connectionTimeoutSeconds
        scanTimeout = settings.scanTimeoutSeconds
        heartbeatTimeout = settings.serverTimeoutSeconds.toLong()
        showingNetworkInfo = settings.showNetworkInfo
        alwaysScanPublicBroadcasts = settings.alwaysScanPublic

        missionsEnabled = settings.missionsEnabled
        reconcileDisplayedMissions(
            battery = settings.displayRewardBattery,
            coolant = settings.displayRewardCoolant,
            nukes = settings.displayRewardNukes,
            production = settings.displayRewardProduction,
            shieldBoost = settings.displayRewardShield,
        )

        autoDismissCompletedMissions = settings.completedMissionDismissalEnabled
        completedDismissalTime = settings.completedMissionDismissalSeconds.toLong()

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

        biomechsEnabled = settings.biomechsEnabled
        biomechSorter =
            BiomechSorter(
                sortByClassFirst = settings.biomechSortClassFirst,
                sortByStatus = settings.biomechSortStatus,
                sortByClassSecond = settings.biomechSortClassSecond,
                sortByName = settings.biomechSortName,
            )
        biomechFreezeTime = settings.freezeDurationSeconds.toLong()

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

        enemiesEnabled = settings.enemiesEnabled
        enemySorter =
            EnemySorter(
                sortBySurrendered = settings.enemySortSurrendered,
                sortByFaction = settings.enemySortFaction,
                sortByFactionReversed = settings.enemySortFactionReversed,
                sortByName = settings.enemySortName,
                sortByDistance = settings.enemySortDistance,
            )
        maxSurrenderDistance =
            settings.surrenderRange.toFloat().takeIf { settings.surrenderRangeEnabled }
        showEnemyIntel = settings.showEnemyIntel
        showTauntStatuses = settings.showTauntStatuses
        disableIneffectiveTaunts = settings.disableIneffectiveTaunts

        avoidBlackHoles = settings.avoidBlackHoles
        avoidMines = settings.avoidMines
        avoidTyphons = settings.avoidTyphon

        blackHoleClearance = settings.blackHoleClearance.toFloat()
        mineClearance = settings.mineClearance.toFloat()
        typhonClearance = settings.typhonClearance.toFloat()

        threeDigitDirections = settings.threeDigitDirections
        volume = settings.soundVolume.toFloat()

        val newThemeRes = ALL_THEMES[settings.themeValue]
        if (themeRes != newThemeRes) {
            themeRes = newThemeRes
            isThemeChanged.value = true
        }
    }

    fun revertSettings(settings: UserSettings): UserSettings =
        settings.copy {
            vesselDataLocationValue = vesselDataIndex
            serverPort = port
            updateInterval = updateObjectsInterval

            connectionTimeoutSeconds = connectTimeout
            scanTimeoutSeconds = scanTimeout
            serverTimeoutSeconds = heartbeatTimeout.toInt()

            missionsEnabled = this@AgentViewModel.missionsEnabled

            val rewardSettings =
                mapOf(
                    RewardType.BATTERY to this::displayRewardBattery,
                    RewardType.COOLANT to this::displayRewardCoolant,
                    RewardType.NUKE to this::displayRewardNukes,
                    RewardType.PRODUCTION to this::displayRewardProduction,
                    RewardType.SHIELD to this::displayRewardShield,
                )
            rewardSettings.values.forEach { it.set(false) }
            displayedRewards.forEach { rewardSettings[it]?.set(true) }

            completedMissionDismissalEnabled = autoDismissCompletedMissions
            completedMissionDismissalSeconds = completedDismissalTime.toInt()

            alliesEnabled = this@AgentViewModel.alliesEnabled
            allySortClassFirst = allySorter.sortByClassFirst
            allySortEnergyFirst = allySorter.sortByEnergy
            allySortStatus = allySorter.sortByStatus
            allySortClassSecond = allySorter.sortByClassSecond
            allySortName = allySorter.sortByName
            showDestroyedAllies = showAllySelector
            allyCommandManualReturn = manuallyReturnFromCommands

            biomechsEnabled = this@AgentViewModel.biomechsEnabled
            biomechSortClassFirst = biomechSorter.sortByClassFirst
            biomechSortStatus = biomechSorter.sortByStatus
            biomechSortClassSecond = biomechSorter.sortByClassSecond
            biomechSortName = biomechSorter.sortByName
            freezeDurationSeconds = biomechFreezeTime.milliseconds.inWholeSeconds.toInt()

            routingEnabled = this@AgentViewModel.routingEnabled
            routeMissions = routeIncludesMissions

            enemiesEnabled = this@AgentViewModel.enemiesEnabled
            enemySortSurrendered = enemySorter.sortBySurrendered
            enemySortFaction = enemySorter.sortByFaction
            enemySortFactionReversed = enemySorter.sortByFactionReversed
            enemySortName = enemySorter.sortByName
            enemySortDistance = enemySorter.sortByDistance
            surrenderRangeEnabled =
                maxSurrenderDistance?.also { surrenderRange = it.toInt() } != null
            showEnemyIntel = this@AgentViewModel.showEnemyIntel
            showTauntStatuses = this@AgentViewModel.showTauntStatuses
            disableIneffectiveTaunts = this@AgentViewModel.disableIneffectiveTaunts

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

            blackHoleClearance = this@AgentViewModel.blackHoleClearance.toInt()
            mineClearance = this@AgentViewModel.mineClearance.toInt()
            typhonClearance = this@AgentViewModel.typhonClearance.toInt()

            threeDigitDirections = this@AgentViewModel.threeDigitDirections
            soundVolume = (volume * VOLUME_SCALE).toInt()
            themeValue = ALL_THEMES.indexOf(themeRes)
            showNetworkInfo = showingNetworkInfo
            alwaysScanPublic = alwaysScanPublicBroadcasts
        }

    companion object {
        private const val DEFAULT_PORT = 2010
        private const val DEFAULT_SCAN_TIMEOUT = 5
        private const val DEFAULT_CONNECT_TIMEOUT = 9
        private const val DEFAULT_HEARTBEAT_TIMEOUT = 15L
        private const val DEFAULT_FREEZE_TIME = 220
        private const val DEFAULT_COMPLETED_DISMISSAL = 3

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

        private val STATION_CALLSIGN = Regex("DS\\d+")
        private val ENEMY_STATION = Regex("^[A-Z][a-z]+ Base \\d+")
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
            )

        fun Int.formatString(): String = toString().format(Locale.getDefault())
    }
}
