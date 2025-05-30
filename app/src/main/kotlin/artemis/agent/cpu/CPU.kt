package artemis.agent.cpu

import artemis.agent.AgentViewModel
import artemis.agent.game.GameFragment
import artemis.agent.game.ObjectEntry
import artemis.agent.game.biomechs.BiomechEntry
import artemis.agent.game.enemies.EnemyEntry
import com.walkertribe.ian.enums.BaseMessage
import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.enums.OtherMessage
import com.walkertribe.ian.iface.Listener
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket
import com.walkertribe.ian.protocol.core.comm.CommsOutgoingPacket
import com.walkertribe.ian.protocol.core.world.DeleteObjectPacket
import com.walkertribe.ian.util.isKnown
import com.walkertribe.ian.vesseldata.Faction
import com.walkertribe.ian.world.ArtemisBase
import com.walkertribe.ian.world.ArtemisBlackHole
import com.walkertribe.ian.world.ArtemisCreature
import com.walkertribe.ian.world.ArtemisMine
import com.walkertribe.ian.world.ArtemisNpc
import com.walkertribe.ian.world.ArtemisObject
import com.walkertribe.ian.world.ArtemisPlayer
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext

class CPU(private val viewModel: AgentViewModel) : CoroutineScope {
    @OptIn(DelicateCoroutinesApi::class)
    override val coroutineContext = newFixedThreadPoolContext(NUM_THREADS, "CPU")

    private val pendingNPCs = ConcurrentHashMap<Int, ArtemisNpc>()
    private val pendingStations = ConcurrentHashMap<Int, ArtemisBase>()
    private val pendingCreatures = ConcurrentHashMap<Int, ArtemisCreature>()

    private val npcUpdateFunctions =
        arrayOf(
            this::updateAllyShip,
            this::updateUnscannedBiomech,
            this::updateScannedBiomech,
            this::updateEnemy,
        )

    private val npcDeleteFunctions =
        arrayOf(this::deleteBiomech, this::deleteEnemy, this::deleteAlly)

    private val messageParsers =
        arrayOf(
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

    private fun onStationDelete(id: Int) {
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
                                val replacementName =
                                    livingStationNameIndex.run { higherKey(name) ?: lowerKey(name) }

                                val destroyedStationList = destroyedStations.value.toMutableList()
                                destroyedStationList.add(fullName)
                                destroyedStations.value = destroyedStationList

                                if (replacementName == null) {
                                    stationsRemain.value = false
                                } else if (stationName.value == name) {
                                    stationName.value = replacementName
                                }

                                allyShips.values
                                    .filter { it.destination == name }
                                    .forEach {
                                        it.destination = null
                                        it.isMovingToStation = false
                                    }
                                livingStationFullNameIndex.remove(fullName)
                                livingStationNameIndex.remove(name)
                            }
                            missionManager.purgeMissions(obj)
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
                val createdStation = pendingStations.remove(id)?.also(station::updates) ?: station

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
                CommsOutgoingPacket(station, BaseMessage.PleaseReportStatus, viewModel.vesselData)
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
                val entry = ObjectEntry.Station(station, vesselData)
                livingStations[id] = entry
                livingStationNameIndex[name] = id
                livingStationFullNameIndex[entry.fullName] = id

                if (firstStation) {
                    stationName.value = livingStationNameIndex.firstKey()
                    stationsExist.value = true
                    stationsRemain.value = true
                }
            }
        }
    }

    private fun onPlayerDelete(id: Int) {
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
        val existingPlayer =
            viewModel.players[id]?.also { player ->
                if (player == viewModel.playerShip) {
                    val dockingBase =
                        player.dockingBase.value
                            .takeIf { it > 0 }
                            ?.let(viewModel.livingStations::get)

                    if (
                        dockingBase != null && (update.impulse.value > 0 || update.warp.value > 0)
                    ) {
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
        val player = viewModel.playerShip

        if (update.capitalShipID.value == player?.id) {
            viewModel.fighterIDs.add(update.id)
        }

        if (player != update) return

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

        val secondsLeft = update.doubleAgentSecondsLeft.value
        if (secondsLeft > 0 || (secondsLeft == 0 && active)) {
            viewModel.doubleAgentSecondsLeft = secondsLeft
        }

        update.alertStatus.value?.also { viewModel.alertStatus.value = it }

        if (agentUpdate) {
            viewModel.doubleAgentEnabled.value = count > 0 && !active
        }
    }

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
        val biomechManager = viewModel.biomechManager
        val biomech = biomechManager.unscanned[update.id] ?: return false
        update updates biomech

        if (viewModel.playerShip?.let { update.hasBeenScannedBy(it).booleanValue } == true) {
            biomechManager.unscanned.remove(biomech.id)
            biomechManager.scanned.add(BiomechEntry(biomech))
        }

        return true
    }

    private fun updateScannedBiomech(update: ArtemisNpc): Boolean =
        viewModel.biomechManager.scanned
            .find { it.biomech == update }
            ?.also { biomechEntry ->
                update updates biomechEntry.biomech

                if (update.x.hasValue || update.y.hasValue || update.z.hasValue) {
                    biomechEntry.onFreezeEnd()
                }
            } != null

    private fun updateEnemy(update: ArtemisNpc): Boolean {
        val enemiesManager = viewModel.enemiesManager
        val entry = enemiesManager.allEnemies[update.id] ?: return false
        val enemy = entry.enemy

        val wasSurrendered = enemy.isSurrendered.value.booleanValue
        val isSurrendered = update.isSurrendered.value.booleanValue
        update updates enemy

        if (isSurrendered && enemiesManager.selection.value?.enemy == enemy) {
            enemiesManager.selection.value = null
        } else if (update.isSurrendered.hasValue && !isSurrendered && wasSurrendered) {
            enemiesManager.perfidy.tryEmit(entry)
            enemiesManager.hasUpdate = true
        }

        return true
    }

    private fun onNpcDelete(id: Int) {
        npcDeleteFunctions.firstNotNullOfOrNull { it(id) }
    }

    private fun deleteBiomech(id: Int): ArtemisNpc? {
        val biomechManager = viewModel.biomechManager
        val unscannedBiomech = biomechManager.unscanned.remove(id)
        if (unscannedBiomech != null) return unscannedBiomech

        val biomechIndex = biomechManager.scanned.indexOfFirst { it.biomech.id == id }
        return if (biomechIndex >= 0) {
            val scannedBiomech = biomechManager.scanned.removeAt(biomechIndex)
            biomechManager.destroyedBiomechName.tryEmit(scannedBiomech.getFullName(viewModel))

            if (biomechManager.scanned.isEmpty()) {
                biomechManager.resetUpdate()
            }

            scannedBiomech.biomech
        } else {
            null
        }
    }

    private fun deleteEnemy(id: Int): ArtemisNpc? =
        viewModel.enemiesManager.let { enemiesManager ->
            enemiesManager.allEnemies.remove(id)?.let { entry ->
                val enemy = entry.enemy
                enemiesManager.destroyedEnemyName.tryEmit(entry.fullName)

                val name = enemy.name.value
                viewModel.allyShips.values
                    .filter { it.isAttacking && it.destination == name }
                    .forEach {
                        it.isAttacking = false
                        it.destination = null
                    }
                name?.also(enemiesManager.nameIndex::remove)

                if (enemiesManager.selection.value == entry) {
                    enemiesManager.selection.value = null
                }

                enemy
            }
        }

    private fun deleteAlly(id: Int): ArtemisNpc? =
        viewModel.allyShips.remove(id)?.let { ally ->
            val npc = ally.obj
            npc.name.value?.also(viewModel.allyShipIndex::remove)

            val destroyedAllyList = viewModel.destroyedAllies.value.toMutableList()
            destroyedAllyList.add(ally.fullName)
            viewModel.destroyedAllies.value = destroyedAllyList

            if (viewModel.focusedAlly.value == ally) {
                viewModel.focusedAlly.value = null
            }
            viewModel.missionManager.purgeMissions(npc)

            npc
        }

    private fun onNpcCreate(npc: ArtemisNpc): Boolean =
        viewModel.run {
            val vessel = npc.getVessel(vesselData) ?: return@run false
            vessel.getFaction(vesselData)?.also { faction ->
                when {
                    faction[Faction.FRIENDLY] ->
                        npc.name.value?.also {
                            if (allyShips.isEmpty()) {
                                alliesExist = true
                            }
                            allyShipIndex[it] = npc.id
                            allyShips[npc.id] = ObjectEntry.Ally(npc, vesselData, isDeepStrike)
                            sendToServer(CommsOutgoingPacket(npc, OtherMessage.Hail, vesselData))
                        }
                    faction[Faction.BIOMECH] -> {
                        biomechManager.confirmed = true
                        if (playerShip?.let { npc.hasBeenScannedBy(it).booleanValue } == true) {
                            biomechManager.scanned.add(BiomechEntry(npc))
                        } else {
                            biomechManager.unscanned[npc.id] = npc
                        }
                    }
                    faction[Faction.ENEMY] ->
                        npc.name.value?.also { name ->
                            enemiesManager.addEnemy(
                                EnemyEntry(npc, vessel, faction, vesselData),
                                name,
                            )
                        }
                }
            } != null
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
                    isNotTyphon.booleanValue -> {}
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
        obj updates map.getOrPut(obj.id) { obj }
    }

    @Listener
    fun onPacket(packet: DeleteObjectPacket) {
        val id = packet.target

        when (packet.targetType) {
            ObjectType.NPC_SHIP -> onNpcDelete(id)
            ObjectType.BASE -> onStationDelete(id)
            ObjectType.PLAYER_SHIP -> onPlayerDelete(id)
            ObjectType.MINE -> launch { viewModel.onDeleteObstacle(id, viewModel.mines) }
            ObjectType.BLACK_HOLE -> launch { viewModel.onDeleteObstacle(id, viewModel.blackHoles) }
            ObjectType.CREATURE -> launch { viewModel.onDeleteObstacle(id, viewModel.typhons) }
            else -> {}
        }
    }

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
    }
}
