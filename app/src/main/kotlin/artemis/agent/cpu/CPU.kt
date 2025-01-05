package artemis.agent.cpu

import artemis.agent.AgentViewModel
import artemis.agent.game.GameFragment
import artemis.agent.game.ObjectEntry
import artemis.agent.game.biomechs.BiomechEntry
import artemis.agent.game.enemies.EnemyCaptainStatus
import artemis.agent.game.enemies.EnemyEntry
import artemis.agent.game.enemies.TauntStatus
import com.walkertribe.ian.enums.BaseMessage
import com.walkertribe.ian.enums.IntelType
import com.walkertribe.ian.enums.OtherMessage
import com.walkertribe.ian.iface.Listener
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket
import com.walkertribe.ian.protocol.core.comm.CommsOutgoingPacket
import com.walkertribe.ian.protocol.core.world.IntelPacket
import com.walkertribe.ian.util.isKnown
import com.walkertribe.ian.vesseldata.Faction
import com.walkertribe.ian.world.ArtemisBase
import com.walkertribe.ian.world.ArtemisBlackHole
import com.walkertribe.ian.world.ArtemisCreature
import com.walkertribe.ian.world.ArtemisMine
import com.walkertribe.ian.world.ArtemisNpc
import com.walkertribe.ian.world.ArtemisObject
import com.walkertribe.ian.world.ArtemisPlayer
import com.walkertribe.ian.world.BaseArtemisShielded
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

class CPU(private val viewModel: AgentViewModel) : CoroutineScope {
    @OptIn(DelicateCoroutinesApi::class)
    override val coroutineContext = newFixedThreadPoolContext(NUM_THREADS, "CPU")

    private val pendingNPCs = ConcurrentHashMap<Int, ArtemisNpc>()
    private val pendingStations = ConcurrentHashMap<Int, ArtemisBase>()
    private val pendingCreatures = ConcurrentHashMap<Int, ArtemisCreature>()

    fun onStationDelete(id: Int) {
        with(viewModel) {
            livingEnemyStations.also { enemyStations ->
                val enemyStation = enemyStations.remove(id)
                if (enemyStation != null) {
                    enemyStation.obj.name.value?.also {
                        enemyStationNameIndex.remove(it)

                        val destroyedStationList = destroyedStations.value.toMutableList()
                        destroyedStationList.add(it)
                        destroyedStations.value = destroyedStationList
                    }
                } else {
                    livingStations.also { stations ->
                        stations.remove(id)?.apply {
                            obj.name.value?.also { name ->
                                getFullNameForShip(obj).also { fullName ->
                                    val replacementName = livingStationNameIndex.run {
                                        higherKey(name) ?: lowerKey(name)
                                    }

                                    val destroyedStationList =
                                        destroyedStations.value.toMutableList()
                                    destroyedStationList.add(fullName)
                                    destroyedStations.value = destroyedStationList

                                    if (replacementName == null) {
                                        stationsRemain.value = false
                                    } else if (stationName.value == name) {
                                        stationName.value = replacementName
                                    }

                                    allyShips.values.filter { it.destination == name }.forEach {
                                        it.destination = null
                                        it.isMovingToStation = false
                                    }
                                    livingStationFullNameIndex.remove(fullName)
                                }
                                livingStationNameIndex.remove(name)
                            }
                            purgeMissions(obj)
                        }
                    }
                }
            }
        }
    }

    @Listener
    fun onStationUpdate(station: ArtemisBase) {
        with(viewModel) {
            checkGameStart()

            val id = station.id
            val existingStation = livingStations[id] ?: livingEnemyStations[id]

            if (existingStation == null) {
                val createdStation =
                    pendingStations.remove(id)?.also(station::updates) ?: station

                if (!onStationCreate(createdStation)) {
                    pendingStations[id] = createdStation
                }
            } else {
                station updates existingStation.obj
            }
        }
    }

    private fun onStationCreate(station: ArtemisBase): Boolean {
        val vessel = station.getVessel(viewModel.vesselData) ?: return false
        if (vessel.side > 1) {
            addEnemyStation(station)
        } else {
            addLivingStation(station)
            viewModel.sendToServer(
                CommsOutgoingPacket(
                    station,
                    BaseMessage.PleaseReportStatus,
                    viewModel.vesselData
                )
            )
        }
        return true
    }

    private fun addEnemyStation(station: ArtemisBase) {
        station.name.value?.also { name ->
            with(viewModel) {
                val firstStation = livingEnemyStations.isEmpty()
                val id = station.id
                livingEnemyStations[id] = ObjectEntry.Station(station, vesselData)
                enemyStationNameIndex[name] = id

                if (firstStation) {
                    enemyStationsExist.value = true
                    if (isDeepStrike) {
                        currentGamePage.value = GameFragment.Page.ALLIES
                    }
                }
            }
        }
    }

    private fun addLivingStation(station: ArtemisBase) {
        station.name.value?.also { name ->
            with(viewModel) {
                val firstStation = livingStations.isEmpty()
                val id = station.id
                livingStations[id] = ObjectEntry.Station(station, vesselData)
                livingStationNameIndex[name] = id
                livingStationFullNameIndex[getFullNameForShip(station)] = id

                if (firstStation) {
                    stationName.value = livingStationNameIndex.firstKey()
                    stationsExist.value = true
                    stationsRemain.value = true
                }
            }
        }
    }

    fun onPlayerDelete(id: Int) {
        with(viewModel) {
            players.remove(id)?.also {
                val index = it.shipIndex.value.toInt()
                if (index in playerIndex.indices) {
                    playerIndex[index] = -1
                }
            }
            fighterIDs.remove(id)
            onPlayerShipDisposed()
        }
    }

    @Listener
    fun onPlayerUpdate(update: ArtemisPlayer) {
        viewModel.checkGameStart()

        viewModel.ordnanceUpdated.value = update.hasWeaponsData

        val id = update.id
        val existingPlayer = viewModel.players[id]?.also { player ->
            if (player == viewModel.playerShip) {
                val dockingBase = player.dockingBase.value.takeIf {
                    it > 0
                }?.let(viewModel.livingStations::get)

                if (dockingBase != null && (update.impulse.value > 0 || update.warp.value > 0)) {
                    viewModel.sendToServer(
                        CommsOutgoingPacket(
                            dockingBase.obj,
                            BaseMessage.PleaseReportStatus,
                            viewModel.vesselData,
                        )
                    )
                    dockingBase.isStandingBy = false
                }
            }

            update updates player
        } ?: update.also { viewModel.players[id] = it }

        val index = existingPlayer.shipIndex.value.toInt()
        if (index in viewModel.playerIndex.indices) {
            viewModel.playerIndex[index] = id
        }

        onSelectedPlayerUpdate(update)
    }

    private fun onSelectedPlayerUpdate(update: ArtemisPlayer) {
        val player = viewModel.playerShip ?: return

        if (update == player) {
            var count = player.doubleAgentCount.value
            var active = player.doubleAgentActive.value.booleanValue
            var agentUpdate = false

            val doubleAgentCount = update.doubleAgentCount.value
            if (doubleAgentCount >= 0) {
                agentUpdate = true
                count = doubleAgentCount
            }

            val doubleAgentActive = update.doubleAgentActive.value
            if (doubleAgentActive.isKnown) {
                agentUpdate = true
                active = doubleAgentActive.booleanValue
                viewModel.doubleAgentActive.value = active
                if (!active) {
                    viewModel.doubleAgentSecondsLeft = -1
                }
            }

            update.doubleAgentSecondsLeft.value.also {
                if (it > 0 || (it == 0 && active)) {
                    viewModel.doubleAgentSecondsLeft = it
                }
            }

            update.alertStatus.value?.also {
                viewModel.alertStatus.value = it
            }

            if (agentUpdate) {
                viewModel.doubleAgentEnabled.value = count > 0 && !active
            }
        }

        if (update.capitalShipID.value == player.id) {
            viewModel.fighterIDs.add(update.id)
        }
    }

    private val npcUpdateFunctions = arrayOf(
        this::updateAllyShip,
        this::updateUnscannedBiomech,
        this::updateScannedBiomech,
        this::updateEnemy,
    )

    @Listener
    fun onNpcUpdate(update: ArtemisNpc) {
        viewModel.checkGameStart()

        if (npcUpdateFunctions.any { it(update) }) return

        val createdNpc = pendingNPCs.remove(update.id)?.also(update::updates) ?: update
        if (!onNpcCreate(createdNpc)) {
            pendingNPCs[createdNpc.id] = createdNpc
        }
    }

    private fun updateAllyShip(npc: ArtemisNpc): Boolean {
        val allyEntry = viewModel.allyShips[npc.id] ?: return false
        npc updates allyEntry.obj
        allyEntry.checkNebulaStatus()
        return true
    }

    private fun updateUnscannedBiomech(update: ArtemisNpc): Boolean {
        val biomech = viewModel.unscannedBiomechs[update.id] ?: return false
        update updates biomech

        if (viewModel.playerShip?.let { update.hasBeenScannedBy(it).booleanValue } == true) {
            viewModel.unscannedBiomechs.remove(biomech.id)
            viewModel.scannedBiomechs.add(BiomechEntry(biomech))
        }

        return true
    }

    private fun updateScannedBiomech(update: ArtemisNpc): Boolean {
        val biomechEntry = viewModel.scannedBiomechs.find { it.biomech == update } ?: return false
        update updates biomechEntry.biomech

        if (update.x.hasValue || update.y.hasValue || update.z.hasValue) {
            biomechEntry.onFreezeEnd()
        }

        return true
    }

    private fun updateEnemy(update: ArtemisNpc): Boolean {
        val entry = viewModel.enemies[update.id] ?: return false
        val enemy = entry.enemy

        val wasSurrendered = enemy.isSurrendered.value.booleanValue
        val isSurrendered = update.isSurrendered.value.booleanValue
        update updates enemy

        if (isSurrendered && viewModel.selectedEnemy.value?.enemy == enemy) {
            viewModel.selectedEnemy.value = null
        } else if (update.isSurrendered.hasValue && !isSurrendered && wasSurrendered) {
            viewModel.perfidiousEnemy.tryEmit(entry)
            viewModel.enemiesUpdate = true
        }

        return true
    }

    @Listener
    fun onIntel(packet: IntelPacket) {
        if (packet.intelType != IntelType.LEVEL_2_SCAN) return

        val enemy = viewModel.enemies[packet.id] ?: return
        val taunts = enemy.faction.taunts

        val intel = packet.intel
        enemy.intel = intel

        val description = intel.substring(INTEL_PREFIX_LENGTH)
        val tauntIndex = taunts.indexOfFirst { taunt ->
            description.startsWith(taunt.immunity)
        }
        val immunityEnd = if (tauntIndex < 0) {
            description.indexOf(',')
        } else {
            taunts[tauntIndex].immunity.length
        }

        val rest = description.substring(immunityEnd)
        val captainStatus = if (rest.startsWith(CAPTAIN_STATUS_PREFIX)) {
            val status = rest.substring(CAPTAIN_STATUS_PREFIX.length)
            EnemyCaptainStatus.entries.find {
                status.startsWith(it.name.lowercase().replace('_', ' '))
            }
        } else {
            null
        }
        enemy.captainStatus = captainStatus ?: EnemyCaptainStatus.NORMAL
        if (tauntIndex >= 0 && captainStatus != EnemyCaptainStatus.EASILY_OFFENDED) {
            enemy.tauntStatuses[tauntIndex] = TauntStatus.INEFFECTIVE
        }
    }

    fun onNpcDelete(id: Int) {
        viewModel.apply {
            val biomech = unscannedBiomechs.remove(id)
            if (biomech != null) return@apply

            val biomechIndex = scannedBiomechs.indexOfFirst { it.biomech.id == id }
            if (biomechIndex >= 0) {
                scannedBiomechs.removeAt(biomechIndex).biomech.also {
                    destroyedBiomechName.tryEmit(getFullNameForShip(it))
                }
                if (scannedBiomechs.isEmpty()) {
                    biomechUpdate = false
                }
                return@apply
            }

            enemies.remove(id)?.also { entry ->
                val enemy = entry.enemy
                destroyedEnemyName.tryEmit(getFullNameForShip(enemy))

                val name = enemy.name.value
                allyShips.values.filter {
                    it.isAttacking && it.destination == name
                }.forEach {
                    it.isAttacking = false
                    it.destination = null
                }
                name?.also(enemyNameIndex::remove)

                if (selectedEnemy.value == entry) {
                    selectedEnemy.value = null
                }

                return@apply
            }

            allyShips.remove(id)?.also { ally ->
                ally.obj.also {
                    it.name.value?.also(allyShipIndex::remove)

                    val destroyedAllyList = destroyedAllies.value.toMutableList()
                    destroyedAllyList.add(getFullNameForShip(it))
                    destroyedAllies.value = destroyedAllyList

                    if (focusedAlly.value == ally) {
                        focusedAlly.value = null
                    }
                    purgeMissions(it)
                }
            }
        }
    }

    private fun purgeMissions(obj: BaseArtemisShielded<*>) {
        with(viewModel) {
            allMissions.removeAll(
                allMissions.filter {
                    !it.isCompleted && (
                        it.destination.obj == obj || (
                            !it.isStarted && it.source.obj == obj
                            )
                        )
                }.toSet()
            )
        }
    }

    private fun onNpcCreate(npc: ArtemisNpc): Boolean = viewModel.run {
        val vessel = npc.getVessel(vesselData) ?: return@run false
        val faction = vessel.getFaction(vesselData) ?: return@run false
        when {
            faction[Faction.FRIENDLY] -> npc.name.value?.also {
                if (allyShips.isEmpty()) {
                    alliesExist = true
                }
                allyShipIndex[it] = npc.id
                allyShips[npc.id] = ObjectEntry.Ally(npc, vessel.name, isDeepStrike)
                sendToServer(
                    CommsOutgoingPacket(npc, OtherMessage.Hail, vesselData)
                )
            }
            faction[Faction.BIOMECH] -> {
                biomechsExist = true
                if (playerShip?.let { npc.hasBeenScannedBy(it).booleanValue } == true) {
                    scannedBiomechs.add(BiomechEntry(npc))
                } else {
                    unscannedBiomechs[npc.id] = npc
                }
            }
            faction[Faction.ENEMY] -> npc.name.value?.also {
                enemies[npc.id] = EnemyEntry(npc, vessel, faction)
                enemyNameIndex[it] = npc.id
            }
        }
        true
    }

    @Listener
    fun onMineUpdate(mine: ArtemisMine) {
        onObjectUpdate(mine, viewModel.mines)
    }

    @Listener
    fun onBlackHoleUpdate(blackHole: ArtemisBlackHole) {
        onObjectUpdate(blackHole, viewModel.blackHoles)
    }

    @Listener
    fun onCreatureUpdate(creature: ArtemisCreature) {
        with(viewModel) {
            val id = creature.id
            val addedCreature = pendingCreatures.remove(id)?.also(creature::updates) ?: creature
            val existingCreature = typhons[id]
            if (existingCreature == null) {
                val isNotTyphon = addedCreature.isNotTyphon.value
                when {
                    isNotTyphon.booleanValue -> { }
                    isNotTyphon.isKnown -> typhons[id] = addedCreature
                    else -> pendingCreatures[id] = addedCreature
                }
            } else {
                creature updates existingCreature
            }
        }
    }

    private fun <Obj : ArtemisObject<Obj>> onObjectUpdate(
        obj: Obj,
        map: ConcurrentHashMap<Int, Obj>,
    ) {
        val existingObj = map[obj.id]?.also(obj::updates)
        if (existingObj == null) {
            map[obj.id] = obj
        }
    }

    private val messageParsers = arrayOf(
        MessageParser.TauntResponse,
        MessageParser.BorderWar,
        MessageParser.Scrambled,
        MessageParser.Standby,
        MessageParser.Production,
        MessageParser.Fighters,
        MessageParser.Ordnance,
        MessageParser.HeaveTo,
        MessageParser.TorpedoTransfer,
        MessageParser.EnergyTransfer,
        MessageParser.DeliveringReward,
        MessageParser.NewMission,
        MessageParser.MissionProgress,
        MessageParser.UnderAttack,
        MessageParser.StationDestroyed,
        MessageParser.RewardDelivered,
        MessageParser.RealCaptain,
        MessageParser.RescuedAmbassador,
        MessageParser.Directions,
        MessageParser.PlannedDestination,
        MessageParser.Attacking,
        MessageParser.HasDestination,
        MessageParser.HailResponse,
    )

    @Listener
    fun onCommsPacket(packet: CommsIncomingPacket) {
        for (parser in messageParsers) {
            if (parser.parseResult(packet, viewModel)) break
        }
    }

    internal fun clear() {
        pendingNPCs.clear()
        pendingStations.clear()
        pendingCreatures.clear()
    }

    private companion object {
        const val NUM_THREADS = 20

        const val INTEL_PREFIX_LENGTH = 19
        const val CAPTAIN_STATUS_PREFIX = ", and is "
    }
}
