package artemis.agent.cpu

import artemis.agent.AgentViewModel
import artemis.agent.game.GameFragment
import artemis.agent.game.ObjectEntry
import artemis.agent.game.WarStatus
import artemis.agent.game.allies.AllyStatus
import artemis.agent.game.biomechs.BiomechEntry
import artemis.agent.game.enemies.EnemyCaptainStatus
import artemis.agent.game.enemies.EnemyEntry
import artemis.agent.game.enemies.TauntStatus
import artemis.agent.game.missions.RewardType
import artemis.agent.game.missions.SideMissionEntry
import com.walkertribe.ian.enums.BaseMessage
import com.walkertribe.ian.enums.IntelType
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.enums.OtherMessage
import com.walkertribe.ian.iface.Listener
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket
import com.walkertribe.ian.protocol.core.comm.CommsOutgoingPacket
import com.walkertribe.ian.protocol.core.world.IntelPacket
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.isKnown
import com.walkertribe.ian.vesseldata.Faction
import com.walkertribe.ian.world.Artemis
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
                    enemyStation.obj.nameString?.also {
                        enemyStationNameIndex.remove(it)

                        val destroyedStationList = destroyedStations.value.toMutableList()
                        destroyedStationList.add(it)
                        destroyedStations.value = destroyedStationList
                    }
                } else {
                    livingStations.also { stations ->
                        stations.remove(id)?.apply {
                            obj.nameString?.also { name ->
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
        station.nameString?.also { name ->
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
        station.nameString?.also { name ->
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
        with(viewModel) {
            checkGameStart()

            ordnanceUpdated.value = update.hasWeaponsData

            val id = update.id
            var existingPlayer = players[id]

            if (existingPlayer != null) {
                if (existingPlayer == playerShip) {
                    val dockingBase = existingPlayer.dockingBase.value
                    if (dockingBase > 0 && (update.impulse.value > 0 || update.warp.value > 0)) {
                        livingStations[dockingBase]?.apply {
                            sendToServer(
                                CommsOutgoingPacket(
                                    obj,
                                    BaseMessage.PleaseReportStatus,
                                    vesselData
                                )
                            )
                            isStandingBy = false
                        }
                    }
                }
                update updates existingPlayer
            } else {
                existingPlayer = update
                players[id] = existingPlayer
            }

            val index = existingPlayer.shipIndex.value.toInt()
            if (index in playerIndex.indices) {
                playerIndex[index] = id
            }

            playerShip?.also { player ->
                val selectedPlayerUpdated = update == player
                if (selectedPlayerUpdated) {
                    var count = player.doubleAgentCount.value
                    var active = player.doubleAgentActive.value.booleanValue
                    var agentUpdate = false

                    update.doubleAgentCount.value.also {
                        if (it >= 0) {
                            agentUpdate = true
                            count = it
                        }
                    }
                    update.doubleAgentActive.value.also {
                        if (it.isKnown) {
                            agentUpdate = true
                            active = it.booleanValue
                            doubleAgentActive.value = active
                            if (!active) {
                                doubleAgentSecondsLeft = -1
                            }
                        }
                    }
                    update.doubleAgentSecondsLeft.value.also {
                        if (it > 0 || (it == 0 && active)) {
                            doubleAgentSecondsLeft = it
                        }
                    }
                    update.alertStatus.value?.also {
                        alertStatus.value = it
                    }

                    if (agentUpdate) {
                        doubleAgentEnabled.value = count > 0 && !active
                    }
                }

                if (update.capitalShipID.value == player.id) {
                    fighterIDs.add(id)
                }
            }
        }
    }

    @Listener
    fun onNpcUpdate(update: ArtemisNpc) {
        viewModel.checkGameStart()

        if (updateAllyShip(update)) return
        if (updateUnscannedBiomech(update)) return
        if (updateScannedBiomech(update)) return
        if (updateEnemy(update)) return

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
        } else if (!isSurrendered && wasSurrendered) {
            viewModel.enemiesUpdate = true
        }

        return true
    }

    @Listener
    fun onIntel(packet: IntelPacket) {
        if (packet.intelType != IntelType.LEVEL_2_SCAN) return

        val enemy = viewModel.enemies[packet.id] ?: return
        val taunts = enemy.faction.taunts

        val intel = packet.intel.toString()
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

            enemies.remove(id)?.also { enemy ->
                val name = enemy.enemy.nameString
                allyShips.values.filter {
                    it.isAttacking && it.destination == name
                }.forEach {
                    it.isAttacking = false
                    it.destination = null
                }
                name?.also(enemyNameIndex::remove)

                if (viewModel.selectedEnemy.value == enemy) {
                    viewModel.selectedEnemy.value = null
                }

                return@apply
            }

            allyShips.remove(id)?.also { ally ->
                ally.obj.also {
                    it.nameString?.also(allyShipIndex::remove)

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

    private fun purgeMissions(obj: BaseArtemisShielded) {
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
            faction[Faction.FRIENDLY] -> npc.nameString?.also {
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
            faction[Faction.ENEMY] -> npc.nameString?.also {
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

    private fun <Obj : ArtemisObject> onObjectUpdate(obj: Obj, map: ConcurrentHashMap<Int, Obj>) {
        val existingObj = map[obj.id]?.also(obj::updates)
        if (existingObj == null) {
            map[obj.id] = obj
        }
    }

    private val parseFunctions = arrayOf(
        this::parseTauntResult,
        this::parseWarStatus,
        this::parseScrambled,
        this::parseMalfunction,
        this::parseStandby,
        this::parseProduction,
        this::parseFighter,
        this::parseOrdnance,
        this::parseHostage,
        this::parseCommandeered,
        this::parseFlyingBlind,
        this::parseAmbassador,
        this::parseContraband,
        this::parseHeaveTo,
        this::parseSecureData,
        this::parseNeedsDamcon,
        this::parseNeedsEnergy,
        this::parseTorpedoTransfer,
        this::parseEnergyTransfer,
        this::parseDeliveringReward,
        this::parseTrap,
        this::parseNewMission,
        this::parseMissionProgress,
        this::parseUnderAttack,
        this::parseDestroyed,
        this::parseRewardDelivered,
        this::parseRealCaptain,
        this::parseRescued,
        this::parseDirections,
        this::parsePlannedDestination,
        this::parseAttacking,
        this::parseHasDestination,
        this::parseOther,
    )

    @Listener
    fun onCommsPacket(packet: CommsIncomingPacket) {
        for (fn in parseFunctions) {
            if (fn(packet)) break
        }
    }

    private fun checkForHailResponse(packet: CommsIncomingPacket): String? {
        val message = packet.message
        return OUR_SHIELDS.find(message)?.run {
            message.substring(value.length + 1)
        }
    }

    private fun setEnemyStatus(enemy: EnemyEntry?, status: TauntStatus) {
        val taunt = enemy?.lastTaunt ?: return
        enemy.tauntStatuses[taunt.ordinal - 1] = status
        enemy.lastTaunt = null
    }

    private fun parseTauntResult(packet: CommsIncomingPacket): Boolean {
        val enemyIndex = viewModel.enemyNameIndex[packet.sender.toString()] ?: return false
        val enemy = viewModel.enemies[enemyIndex]

        val message = packet.message.toString()
        return when {
            message.startsWith(TAUNTED) -> {
                setEnemyStatus(enemy, TauntStatus.SUCCESSFUL)
                enemy?.apply { tauntCount++ }
                true
            }
            message.startsWith(REUSED_TAUNT) -> {
                setEnemyStatus(enemy, TauntStatus.INEFFECTIVE)
                true
            }
            message.startsWith(RADIO_SILENCE) -> {
                if (enemy != null) {
                    enemy.tauntStatuses.fill(TauntStatus.INEFFECTIVE)
                    if (viewModel.selectedEnemy.value == enemy) {
                        viewModel.selectedEnemy.value = null
                    }
                }
                true
            }
            else -> false
        }
    }

    private fun parseWarStatus(packet: CommsIncomingPacket): Boolean {
        val message = packet.message
        return when {
            packet.sender.toString() != TSNCOM -> false
            message.startsWith(WAR_WARNING) -> {
                viewModel.borderWarStatus.value = WarStatus.WARNING
                true
            }
            message.startsWith(WAR_DECLARED) -> {
                viewModel.borderWarStatus.value = WarStatus.DECLARED
                true
            }
            else -> false
        }
    }

    private fun parseScrambled(packet: CommsIncomingPacket): Boolean {
        val message = packet.message
        if (!message.endsWith(SCRAMBLED)) return false

        val sender = packet.sender.toString()
        viewModel.scannedBiomechs.apply {
            find { it.biomech.nameString == sender }?.also {
                it.onFreezeResponse()
            }
        }
        return true
    }

    private fun parseMalfunction(packet: CommsIncomingPacket): Boolean {
        val message = checkForHailResponse(packet)?.takeIf {
            it.startsWith(MALFUNCTION)
        } ?: return false

        setAllyStatus(packet.sender.toString(), message, AllyStatus.MALFUNCTION)
        return true
    }

    private fun parseStandby(packet: CommsIncomingPacket): Boolean {
        val playerShips = viewModel.selectableShips.value
        val message = packet.message
        if (!message.startsWith(STANDBY)) return false
        val sender = packet.sender.toString()

        val shipName = message.substring(STANDBY.length, message.length - 1)
        for (i in 0 until Artemis.SHIP_COUNT) {
            if (playerShips[i].name.toString() != shipName) continue

            if (i == viewModel.shipIndex.value) {
                viewModel.livingStationFullNameIndex[sender]?.also {
                    viewModel.livingStations[it]?.isStandingBy = true
                }
            }
            return true
        }

        return false
    }

    private fun parseProduction(packet: CommsIncomingPacket): Boolean {
        val sender = packet.sender.toString()
        val message = packet.message
        if (message.startsWith(PRODUCED)) {
            val restOfMessage = message.substring(PRODUCED.length)
            if (
                !OrdnanceType.entries.any {
                    restOfMessage.startsWith(it.toString()) || it.alternateLabel.run {
                        isNotEmpty() && restOfMessage.startsWith(this)
                    }
                }
            ) {
                return false
            }

            viewModel.stationProductionPacket.tryEmit(packet)
            viewModel.livingStationNameIndex[sender]?.let(viewModel.livingStations::get)?.apply {
                recalibrateSpeed(packet.timestamp)
                resetBuildProgress()
                resetMissile()
                viewModel.sendToServer(
                    CommsOutgoingPacket(
                        obj,
                        BaseMessage.PleaseReportStatus,
                        viewModel.vesselData
                    )
                )
            }
            return true
        }

        if (message.contains(PRODUCING)) {
            viewModel.livingStationFullNameIndex[sender]?.let(viewModel.livingStations::get)?.apply {
                resetBuildProgress()
                resetMissile()
                viewModel.sendToServer(
                    CommsOutgoingPacket(
                        obj,
                        BaseMessage.PleaseReportStatus,
                        viewModel.vesselData
                    )
                )
            }
            return true
        }

        return false
    }

    private fun parseFighter(packet: CommsIncomingPacket): Boolean {
        if (!packet.message.endsWith(FIGHTER)) return false
        val sender = packet.sender.toString()

        with(viewModel) {
            livingStationNameIndex[sender]?.let(livingStations::get)?.apply { fighters-- }
        }
        return true
    }

    private fun parseOrdnance(packet: CommsIncomingPacket): Boolean {
        val list = packet.message.split('\n')
        val productionLine = list[list.size - 1]
        if (!productionLine.startsWith(ORDNANCE)) return false
        val sender = packet.sender.toString()

        val station = viewModel.livingStationFullNameIndex[sender]?.let(
            viewModel.livingStations::get
        ) ?: return true

        val allOrdnanceTypes = OrdnanceType.getAllForVersion(viewModel.version)
        for (i in allOrdnanceTypes.indices) {
            val ordnanceType = allOrdnanceTypes[i]
            val stock = list[i + 1]
            station.ordnanceStock[ordnanceType] = stock.let {
                it.substring(0, it.indexOf(" ")).toInt()
            }
        }

        val maybeFighters = list[list.size - 2].split(" ", limit = 3)
        if (maybeFighters[0] == "and") {
            station.fighters = maybeFighters[1].toInt()
        }

        val productionInfo = productionLine.substring(ORDNANCE.length).split(".", limit = 2)
        val builtOrdnanceLabel = productionInfo[0]
        allOrdnanceTypes.find {
            it.toString() == builtOrdnanceLabel || it.alternateLabel == builtOrdnanceLabel
        }?.also {
            val minutes = BUILD_MINUTES.find(productionInfo[1])?.value?.run {
                substring(0, length - MINUTES_LENGTH).toInt()
            } ?: DEFAULT_BUILD_MINUTES
            station.setBuildMinutes(minutes)
            station.builtOrdnanceType = it
            if (viewModel.version >= Version.NEBULA_TYPES) {
                station.resetMissile()
                station.builtOrdnanceType = it
                station.reconcileSpeed(minutes)
            }
        }
        return true
    }

    private fun parseHostage(packet: CommsIncomingPacket): Boolean {
        val message = checkForHailResponse(packet)?.takeIf {
            it.startsWith(HOSTAGE)
        } ?: return false

        setAllyStatus(packet.sender.toString(), message, AllyStatus.HOSTAGE)
        return true
    }

    private fun parseCommandeered(packet: CommsIncomingPacket): Boolean {
        val message = checkForHailResponse(packet)?.takeIf {
            it.startsWith(COMMANDEERED)
        } ?: return false

        setAllyStatus(packet.sender.toString(), message, AllyStatus.COMMANDEERED)
        return true
    }

    private fun parseFlyingBlind(packet: CommsIncomingPacket): Boolean {
        val message = checkForHailResponse(packet)?.takeIf {
            it.startsWith(BLIND)
        } ?: return false

        setAllyStatus(packet.sender.toString(), message, AllyStatus.FLYING_BLIND)
        return true
    }

    private fun parseAmbassador(packet: CommsIncomingPacket): Boolean {
        val message = checkForHailResponse(packet)?.takeIf {
            it.startsWith(AMBASSADOR)
        } ?: return false

        setAllyStatus(
            packet.sender.toString(),
            message,
            if (message.substring(AMBASSADOR_SEARCH_INDEX).startsWith(PIRATE_BOSS)) {
                AllyStatus.PIRATE_BOSS
            } else {
                AllyStatus.AMBASSADOR
            }
        )
        return true
    }

    private fun parseContraband(packet: CommsIncomingPacket): Boolean {
        val message = checkForHailResponse(packet) ?: return false
        val pirateAware = message.startsWith(PRIVATEER)
        if (!pirateAware && !message.startsWith(CONTRABAND)) return false

        setAllyStatus(
            packet.sender.toString(),
            message,
            if (pirateAware) AllyStatus.PIRATE_SUPPLIES else AllyStatus.CONTRABAND
        )
        return true
    }

    private fun parseHeaveTo(packet: CommsIncomingPacket): Boolean {
        val message = packet.message
        if (!message.endsWith(HEAVE_TO)) return false

        with(viewModel) {
            allyShipIndex[message.substring(STAND_DOWN_SEARCH_RANGE)]?.also {
                allyShips[it]?.status = AllyStatus.NORMAL
            }
        }
        return true
    }

    private fun parseSecureData(packet: CommsIncomingPacket): Boolean {
        val message = checkForHailResponse(packet) ?: return false
        val pirateAware = message.startsWith(PIRATE_SCUM)
        if (!pirateAware && !message.startsWith(SECRET_DATA)) return false

        setAllyStatus(
            packet.sender.toString(),
            message,
            if (pirateAware) AllyStatus.PIRATE_DATA else AllyStatus.SECURE_DATA
        )
        return true
    }

    private fun parseNeedsDamcon(packet: CommsIncomingPacket): Boolean {
        val message = checkForHailResponse(packet)?.takeIf {
            it.startsWith(NEED_DAMCON)
        } ?: return false

        setAllyStatus(packet.sender.toString(), message, AllyStatus.NEED_DAMCON)
        return true
    }

    private fun parseNeedsEnergy(packet: CommsIncomingPacket): Boolean {
        val message = checkForHailResponse(packet)?.takeIf {
            it.startsWith(NEED_ENERGY)
        } ?: return false

        setAllyStatus(packet.sender.toString(), message, AllyStatus.NEED_ENERGY)
        return true
    }

    private fun parseTorpedoTransfer(packet: CommsIncomingPacket): Boolean {
        val message = packet.message
        if (!message.startsWith(TORPEDO_TRANS)) return false

        with(viewModel) {
            torpedoesReady = false
            torpedoFinishTime = System.currentTimeMillis() + DEEP_STRIKE_TORPEDO_BUILD_TIME
        }
        return true
    }

    private fun parseEnergyTransfer(packet: CommsIncomingPacket): Boolean {
        val message = packet.message
        if (!message.startsWith(ENERGY_TRANS)) return false

        with(viewModel) {
            allyShipIndex[packet.sender.toString()]?.let(allyShips::get)?.also {
                it.hasEnergy = false
                if (isDeepStrike) {
                    launch {
                        delay(DEEP_STRIKE_TORPEDO_BUILD_TIME)
                        it.hasEnergy = true
                    }
                }
            }
        }
        return true
    }

    private fun parseDeliveringReward(packet: CommsIncomingPacket): Boolean {
        val message = packet.message
        if (!message.endsWith(DROP_REWARD)) return false

        with(viewModel) {
            allyShipIndex[packet.sender.toString()]?.also {
                allyShips[it]?.status = AllyStatus.REWARD
            }
        }
        return true
    }

    private fun parseTrap(packet: CommsIncomingPacket): Boolean {
        val message = checkForHailResponse(packet) ?: return false
        val mineTrap = message.startsWith(MINE_TRAP)
        if (!mineTrap && !message.startsWith(FIGHTER_TRAP)) return false

        setAllyStatus(
            packet.sender.toString(),
            message,
            if (mineTrap) AllyStatus.MINE_TRAP else AllyStatus.FIGHTER_TRAP
        )
        return true
    }

    private fun parseNewMission(packet: CommsIncomingPacket): Boolean {
        val message = packet.message
        val srcIndex = NEW_MISSION.find(message)?.value?.length ?: return false
        val rewardType = RewardType.entries.find { message.endsWith(it.parseKey) } ?: return false
        viewModel.newMissionPacket.tryEmit(packet)

        with(viewModel) {
            val srcName = message.substring(srcIndex)
            val source = if (srcIndex > SOURCE_DISCRIMINANT) {
                allyShips.values.find {
                    it.obj.nameString?.run { srcName.startsWith(this) && !it.isTrap } == true
                } ?: return true
            } else {
                livingStations.values.find {
                    it.obj.nameString?.let(srcName::startsWith) == true
                } ?: return true
            }

            val destName = packet.sender.toString()
            val destination = livingStationFullNameIndex[destName]?.let {
                livingStations[it]
            } ?: allyShips.values.find {
                getFullNameForShip(it.obj) == destName && !it.isTrap
            } ?: return true

            val existingMission = allMissions.find {
                it.destination == destination && !it.isStarted && it.source == source
            }
            if (existingMission == null) {
                allMissions.add(
                    SideMissionEntry(
                        source,
                        destination,
                        rewardType,
                        packet.timestamp
                    )
                )
                missionsExist = true
            } else {
                existingMission.rewards[rewardType.ordinal]++
            }
            if (displayedRewards.contains(rewardType)) {
                source.missions++
                destination.missions++
            }
        }

        viewModel.missionUpdate = true
        return true
    }

    private fun parseMissionProgress(packet: CommsIncomingPacket): Boolean {
        val message = packet.message.toString()
        val sender = packet.sender.toString()
        return when {
            message.endsWith(PIRATE_PROGRESS_2) -> {
                val shipNameIndex = PIRATE_COMPLETE.find(message)?.value?.length ?: return false
                val skipToShipName = message.substring(shipNameIndex)
                val shipName = getPlayerName(skipToShipName) ?: return false
                viewModel.missionCompletionPacket.tryEmit(packet)
                processMissionCompletion(sender, shipName)
                true
            }
            message.startsWith(PROGRESS) -> {
                val skipToShipName = message.substring(PROGRESS.length)
                val shipName = getPlayerName(skipToShipName) ?: return false
                val restOfMessage = skipToShipName.substring(shipName.length)
                when {
                    restOfMessage.startsWith(PROGRESS_1) -> {
                        val destination = restOfMessage.substring(PROGRESS_1.length).let {
                            NEXT_DESTINATION.find(it)?.run { it.substring(0, range.first) }
                        } ?: return false
                        viewModel.missionProgressPacket.tryEmit(packet)
                        processMissionProgress(sender, destination, shipName)
                        true
                    }
                    restOfMessage == PROGRESS_2 -> {
                        viewModel.missionCompletionPacket.tryEmit(packet)
                        processMissionCompletion(sender, shipName)
                        true
                    }
                    else -> false
                }
            }
            else -> {
                val shipName = getPlayerName(message) ?: return false
                val restOfMessage = message.substring(shipName.length)
                if (!restOfMessage.startsWith(PIRATE_PROGRESS_1)) return false
                val destination = restOfMessage.substring(PIRATE_PROGRESS_1.length).let {
                    PIRATE_NEXT_DESTINATION.find(it)?.run {
                        it.substring(range.last + 1, it.length - 1)
                    }
                } ?: return false
                viewModel.missionProgressPacket.tryEmit(packet)
                processMissionProgress(sender, destination, shipName)
                true
            }
        }
    }

    private fun getPlayerName(message: String): String? = viewModel.players.values.map {
        it.nameString
    }.find { it?.let(message::startsWith) == true }

    private fun processMissionProgress(
        source: String,
        destination: String,
        shipName: String
    ) {
        with(viewModel) {
            allMissions.forEach { mission ->
                if (mission.isStarted) return@forEach
                if (source != getFullNameForShip(mission.source.obj)) return@forEach
                if (destination != mission.destination.obj.nameString) return@forEach

                mission.associatedShipName = shipName
                mission.source.missions -= displayedRewards.sumOf { mission.rewards[it.ordinal] }
                if (shipName != playerShip?.nameString) {
                    mission.destination.missions -= displayedRewards.sumOf { mission.rewards[it.ordinal] }
                }
            }

            val allRewards = RewardType.entries
            var i = 0
            while (i < allMissions.size) {
                val mission = allMissions[i++]
                if (mission.isCompleted || mission.associatedShipName != shipName) continue

                for (j in allMissions.size - 1 downTo i) {
                    val otherMission = allMissions[j]
                    if (
                        otherMission.isCompleted ||
                        otherMission.associatedShipName != shipName ||
                        mission.destination != otherMission.destination
                    ) {
                        continue
                    }

                    allRewards.forEach {
                        mission.rewards[it.ordinal] += otherMission.rewards[it.ordinal]
                    }
                    allMissions.removeAt(j)
                }
            }
        }
    }

    private fun processMissionCompletion(destination: String, shipName: String) {
        with(viewModel) {
            val timestamp = System.currentTimeMillis() + completedDismissalTime
            allMissions.forEach { mission ->
                if (mission.associatedShipName != shipName) return@forEach
                if (mission.isCompleted) return@forEach
                if (destination != getFullNameForShip(mission.destination.obj)) return@forEach

                mission.completionTimestamp = timestamp
                displayedRewards.forEach {
                    payouts[it.ordinal] += mission.rewards[it.ordinal]
                }
                mission.destination.apply {
                    missions -= displayedRewards.sumOf { mission.rewards[it.ordinal] }
                    if (this is ObjectEntry.Station) {
                        speedFactor += mission.rewards[RewardType.PRODUCTION.ordinal]
                    }
                }
            }
            updatePayouts()
        }
    }

    private fun parseUnderAttack(packet: CommsIncomingPacket): Boolean {
        val message = packet.message
        if (UNDER_ATTACK.none(message::startsWith)) return false

        viewModel.stationAttackedPacket.tryEmit(packet)
        return true
    }

    private fun parseDestroyed(packet: CommsIncomingPacket): Boolean {
        val message = packet.message
        if (!message.startsWith(EXPLOSION)) return false

        viewModel.stationDestroyedPacket.tryEmit(packet)
        return true
    }

    private fun parseRewardDelivered(packet: CommsIncomingPacket): Boolean {
        val message = packet.message
        if (!message.startsWith(DELIVERED)) return false

        with(viewModel) {
            allyShipIndex[packet.sender.toString()]?.let(allyShips::get)?.apply {
                status = AllyStatus.NORMAL
                livingStations.values.minByOrNull {
                    obj distanceSquaredTo it.obj
                }?.also {
                    sendToServer(
                        CommsOutgoingPacket(
                            it.obj,
                            BaseMessage.PleaseReportStatus,
                            vesselData
                        )
                    )
                }
            }
        }
        return true
    }

    private fun parseRealCaptain(packet: CommsIncomingPacket): Boolean {
        val message = packet.message
        if (!message.startsWith(REAL_CAPTAIN)) return false

        with(viewModel) {
            allyShipIndex[packet.sender.toString()]?.let(allyShips::get)?.status =
                AllyStatus.NORMAL
            payouts[RewardType.SHIELD.ordinal]++
            updatePayouts()
        }
        return true
    }

    private fun parseRescued(packet: CommsIncomingPacket): Boolean {
        val message = packet.message
        if (!AMBASS_PICKUP.containsMatchIn(message)) return false

        with(viewModel) {
            allyShipIndex[packet.sender.toString()]?.let(allyShips::get)?.status =
                AllyStatus.REPAIRING
        }
        return true
    }

    private fun parseDirections(packet: CommsIncomingPacket): Boolean =
        TURNING.find(packet.message)?.run {
            val details = value.substring(TURNING_PREFIX.length)
            with(viewModel) {
                allyShipIndex[packet.sender.toString()]?.let(allyShips::get)?.apply {
                    destination = null
                    isAttacking = false
                    isMovingToStation = false
                    direction = details.run {
                        when {
                            startsWith(TURNING_TO) ->
                                substring(TURNING_TO.length until length - 1).toInt()
                            startsWith(TURNING_LEFT) ->
                                (
                                    (direction ?: 0) +
                                        AgentViewModel.FULL_HEADING_RANGE -
                                        substring(
                                            TURNING_LEFT.length until length - TURNING_DEGREES_OFFSET
                                        ).toInt()
                                    ) % AgentViewModel.FULL_HEADING_RANGE
                            else ->
                                (
                                    (direction ?: 0) +
                                        substring(
                                            TURNING_RIGHT.length until length - TURNING_DEGREES_OFFSET
                                        ).toInt()
                                    ) % AgentViewModel.FULL_HEADING_RANGE
                        }
                    }
                }
            }
            true
        } == true

    private fun parsePlannedDestination(packet: CommsIncomingPacket): Boolean {
        val message = packet.message
        if (!message.endsWith(PLANNED)) return false

        with(viewModel) {
            allyShipIndex[packet.sender.toString()]?.let(allyShips::get)?.apply {
                direction = null
                destination = null
                isAttacking = false
                isMovingToStation = false
            }
        }
        return true
    }

    private fun parseAttacking(packet: CommsIncomingPacket): Boolean = viewModel.run {
        val message = packet.message
        if (!message.startsWith(ATTACKING)) return@run false

        val shipName = message.substring(ATTACKING.length, message.length - 1)
        if (!players.values.any { it.nameString == shipName }) return@run false

        allyShipIndex[packet.sender.toString()]?.let(allyShips::get)?.apply {
            val nearestEnemy = enemies.values.map { it.enemy }.minByOrNull {
                it.horizontalDistanceSquaredTo(obj)
            }
            destination = nearestEnemy?.nameString
            isAttacking = true
            isMovingToStation = false
        }

        true
    }

    private fun parseHasDestination(packet: CommsIncomingPacket): Boolean {
        val message = packet.message
        return OK_GOING.find(message)?.run {
            val destName = message.substring(value.length, message.length - 1)
            with(viewModel) {
                allyShipIndex[packet.sender.toString()]?.let(allyShips::get)?.apply {
                    direction = null
                    destination = destName
                    isAttacking = false
                    isMovingToStation = livingStationNameIndex.containsKey(destName)
                }
            }
            true
        } == true
    }

    private fun parseOther(packet: CommsIncomingPacket): Boolean {
        val message = checkForHailResponse(packet)?.takeUnless {
            it.startsWith(TO_STATION) || it.startsWith(FIX_ENGINES)
        } ?: return false
        val sender = packet.sender.toString()
        if (sender.startsWith("DS")) return false

        setAllyStatus(sender, message, AllyStatus.NORMAL)
        return true
    }

    private fun setAllyStatus(sender: String, message: String, status: AllyStatus) {
        val splitPoint = sender.lastIndexOf(" ")
        val vesselName = sender.substring(0, splitPoint)
        val name = sender.substring(splitPoint + 1)
        with(viewModel) {
            allyShipIndex[name]?.let(allyShips::get)?.also {
                if (it.vesselName == vesselName) {
                    it.status = status
                    it.hasEnergy = message.endsWith(HAS_ENERGY)
                    it.checkNebulaStatus()
                }
            }
        }
    }

    internal fun clear() {
        pendingNPCs.clear()
        pendingStations.clear()
        pendingCreatures.clear()
    }

    private companion object {
        const val DEFAULT_BUILD_MINUTES = 5
        const val NUM_THREADS = 20

        const val TSNCOM = "TSNCOM"
        const val AMBASSADOR_SEARCH_INDEX = 58
        val STAND_DOWN_SEARCH_RANGE = 25 until 28
        const val HAS_ENERGY = "some."
        const val PIRATE_BOSS = "the big boss"
        const val SCRAMBLED = "iGH \nERROR% w23jr20ruj!!!"
        const val MALFUNCTION = "Our shipboard computer ha"
        const val STANDBY = "Docking crew is ready, "
        const val PRODUCED = "We've produced another "
        const val PRODUCING = "Commencing production of "
        const val FIGHTER = "our ship.  You're welcome."
        const val ORDNANCE = "We're currently building another "
        const val HOSTAGE = "We are holding this ship h"
        const val COMMANDEERED = "We have commandeered this"
        const val BLIND = "Our sensors are all down!"
        const val AMBASSADOR = "We're dead in space, our d"
        const val CONTRABAND = "We are carrying needed su"
        const val PRIVATEER = "Hail, Bold Privateer!  We"
        const val SECRET_DATA = "We are carrying secret, s"
        const val PIRATE_SCUM = "Pirate scum!  We're carry"
        const val HEAVE_TO = "son it and leave this sector."
        const val NEED_DAMCON = "Our engines are damaged a"
        const val NEED_ENERGY = "We're out of energy!  Cou"
        const val TORPEDO_TRANS = "Torpedo transfer complete."
        const val ENERGY_TRANS = "Here's the energy we prom"
        const val MINE_TRAP = "We're just moving cargo b"
        const val FIGHTER_TRAP = "We're broken down!  Out o"
        const val DROP_REWARD = "reward when we get there."
        const val PROGRESS = "Transfer complete, "
        const val PROGRESS_1 = ". Please proceed to "
        const val PROGRESS_2 = ".  Thanks for your help!"
        const val PIRATE_PROGRESS_1 = ", you pirate scum!"
        const val PIRATE_PROGRESS_2 = " deal with you this time!"
        const val ATTACK_1 = "We're under direct attack"
        const val ATTACK_2 = "Our shields are down to 7"
        const val ATTACK_3 = "Shields have dropped to 4"
        const val ATTACK_4 = "Shields are down to 20%! "
        const val EXPLOSION = "We've detected an explosi"
        const val DELIVERED = "Thanks for the assist!  W"
        const val REAL_CAPTAIN = "This is the captain, the "
        const val TO_STATION = "We're heading to the stat"
        const val PLANNED = " our planned destination."
        const val ATTACKING = "Turning to attack, "
        const val FIX_ENGINES = "We appreciate your help. "
        const val WAR_WARNING = "This is an official WAR W"
        const val WAR_DECLARED = "ALERT!  War has been decl"
        const val TAUNTED = "Argh!  You terran scum!  "
        const val REUSED_TAUNT = "That won't work on me aga"
        const val RADIO_SILENCE = "You're a fool, Terran.  I"
        // ship names have max
        // length of 24 characters

        val UNDER_ATTACK = arrayOf(
            ATTACK_1,
            ATTACK_2,
            ATTACK_3,
            ATTACK_4,
        )

        const val DEEP_STRIKE_TORPEDO_BUILD_TIME = 300_000L

        const val TURNING_PREFIX = ", we are turning "
        const val TURNING_LEFT = "left "
        const val TURNING_RIGHT = "right "
        const val TURNING_TO = "to "
        const val TURNING_DEGREES_OFFSET = 9
        val TURNING = Regex("$TURNING_PREFIX(to \\d+|(lef|righ)t \\d+ degrees)\\.$")
        val OK_GOING = Regex("^Okay, going to (defend|rendezvous with) ")
        val OUR_SHIELDS = Regex("^Our shields are at \\d+ \\(\\d+%\\), \\d+ \\(\\d+%\\)\\.")
        val AMBASS_PICKUP = Regex("^Thanks for (rescuing our a|picking up the)")
        val BUILD_MINUTES = Regex("\\d+ minutes?\\.")
        val NEW_MISSION = Regex("^Help us help you\\.\\nFirst, (dock|rendezvous) with ")
        val NEXT_DESTINATION = Regex(" to deliver the (data|supplies)\\.$")
        val PIRATE_NEXT_DESTINATION = Regex("belongs? to ")
        val PIRATE_COMPLETE = Regex("^You can't (steal|just take) what's ours, ")
        const val INTEL_PREFIX_LENGTH = 19
        const val CAPTAIN_STATUS_PREFIX = ", and is "
        const val SOURCE_DISCRIMINANT = 35
        const val MINUTES_LENGTH = 9
    }
}
