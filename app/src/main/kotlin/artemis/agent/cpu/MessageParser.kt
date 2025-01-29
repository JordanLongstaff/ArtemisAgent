package artemis.agent.cpu

import artemis.agent.AgentViewModel
import artemis.agent.game.ObjectEntry
import artemis.agent.game.WarStatus
import artemis.agent.game.allies.AllyStatus
import artemis.agent.game.enemies.EnemyEntry
import artemis.agent.game.enemies.TauntStatus
import artemis.agent.game.missions.RewardType
import artemis.agent.game.missions.SideMissionEntry
import com.walkertribe.ian.enums.BaseMessage
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.protocol.core.comm.CommsIncomingPacket
import com.walkertribe.ian.protocol.core.comm.CommsOutgoingPacket
import com.walkertribe.ian.util.Version
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface MessageParser {
    data object TauntResponse : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val enemyIndex = viewModel.enemyNameIndex[packet.sender] ?: return false
            val enemy = viewModel.enemies[enemyIndex]

            val message = packet.message
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

        private fun setEnemyStatus(enemy: EnemyEntry?, status: TauntStatus) {
            val taunt = enemy?.lastTaunt ?: return
            enemy.tauntStatuses[taunt.ordinal - 1] = status
            enemy.lastTaunt = null
        }
    }

    data object BorderWar : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            if (packet.sender != TSNCOM) return false

            val message = packet.message
            val status = WarStatus.entries.last { message.startsWith(WAR_MESSAGES[it.ordinal]) }
            return if (status == WarStatus.TENSION) {
                false
            } else {
                viewModel.borderWarStatus.value = status
                viewModel.borderWarMessage.tryEmit(packet)
                true
            }
        }
    }

    data object Scrambled : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val message = packet.message
            if (!message.endsWith(SCRAMBLED)) return false

            val sender = packet.sender
            viewModel.scannedBiomechs.apply {
                find { it.biomech.name.value == sender }?.also { it.onFreezeResponse() }
            }
            return true
        }
    }

    data object Standby : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val message = packet.message
            if (!message.startsWith(STANDBY)) return false
            val sender = packet.sender

            val shipName = message.substring(STANDBY.length, message.length - 1)
            val indexOfShip = viewModel.selectableShips.value.indexOfFirst { it.name == shipName }
            return if (indexOfShip < 0) {
                false
            } else {
                if (indexOfShip == viewModel.shipIndex.value) {
                    viewModel.livingStationFullNameIndex[sender]
                        ?.let(viewModel.livingStations::get)
                        ?.isStandingBy = true
                }
                true
            }
        }
    }

    data object Production : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val sender = packet.sender
            val message = packet.message

            return when {
                message.startsWith(PRODUCED) -> {
                    val restOfMessage = message.substring(PRODUCED.length)
                    if (
                        !OrdnanceType.entries.any {
                            restOfMessage.startsWith(it.getLabelFor(viewModel.version))
                        }
                    ) {
                        return false
                    }

                    viewModel.stationProductionPacket.tryEmit(packet)
                    viewModel.livingStationNameIndex[sender]
                        ?.let(viewModel.livingStations::get)
                        ?.apply {
                            recalibrateSpeed(packet.timestamp)
                            resetBuildProgress()
                            resetMissile()
                            viewModel.sendToServer(
                                CommsOutgoingPacket(
                                    obj,
                                    BaseMessage.PleaseReportStatus,
                                    viewModel.vesselData,
                                )
                            )
                        }

                    true
                }

                message.contains(PRODUCING) -> {
                    viewModel.livingStationFullNameIndex[sender]
                        ?.let(viewModel.livingStations::get)
                        ?.apply {
                            resetBuildProgress()
                            resetMissile()
                            viewModel.sendToServer(
                                CommsOutgoingPacket(
                                    obj,
                                    BaseMessage.PleaseReportStatus,
                                    viewModel.vesselData,
                                )
                            )
                        }

                    true
                }

                else -> false
            }
        }
    }

    data object Fighters : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            if (!packet.message.endsWith(FIGHTER)) return false
            val sender = packet.sender

            viewModel.livingStationNameIndex[sender]?.let(viewModel.livingStations::get)?.apply {
                fighters--
            }
            return true
        }
    }

    data object Ordnance : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val inventory = packet.message.split('\n')
            if (!inventory.last().startsWith(ORDNANCE)) return false

            val sender = packet.sender
            viewModel.livingStationFullNameIndex[sender]
                ?.let(viewModel.livingStations::get)
                ?.also { station -> process(station, inventory, viewModel) }

            return true
        }

        private fun process(
            station: ObjectEntry.Station,
            inventory: List<String>,
            viewModel: AgentViewModel,
        ) {
            val allOrdnanceTypes = OrdnanceType.getAllForVersion(viewModel.version)
            for (i in allOrdnanceTypes.indices) {
                val ordnanceType = allOrdnanceTypes[i]
                val stock = inventory[i + 1]
                station.ordnanceStock[ordnanceType] = stock.substringBefore(" ").toInt()
            }

            val maybeFighters = inventory[inventory.size - 2].split(" ", limit = 3)
            if (maybeFighters[0] == "and") {
                station.fighters = maybeFighters[1].toInt()
            }

            val productionInfo = inventory.last().substringAfter(ORDNANCE).split(".", limit = 2)
            val builtOrdnanceLabel = productionInfo[0]
            allOrdnanceTypes
                .find { it.hasLabel(builtOrdnanceLabel) }
                ?.also {
                    val minutes =
                        BUILD_MINUTES.find(productionInfo[1])?.run {
                            value.substring(0, value.length - MINUTES_LENGTH).toInt()
                        } ?: DEFAULT_BUILD_MINUTES
                    station.setBuildMinutes(minutes)
                    station.builtOrdnanceType = it
                    if (viewModel.version >= Version.NEBULA_TYPES) {
                        station.resetMissile()
                        station.builtOrdnanceType = it
                        station.reconcileSpeed(minutes)
                    }
                }
        }
    }

    data object HeaveTo : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val message = packet.message
            if (!message.endsWith(HEAVE_TO)) return false

            viewModel.allyShipIndex[message.substring(STAND_DOWN_SEARCH_RANGE)]?.also {
                viewModel.allyShips[it]?.status = AllyStatus.NORMAL
            }
            return true
        }
    }

    data object TorpedoTransfer : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val message = packet.message
            if (!message.startsWith(TORPEDO_TRANS)) return false

            viewModel.torpedoesReady = false
            viewModel.torpedoFinishTime =
                System.currentTimeMillis() + DEEP_STRIKE_TORPEDO_BUILD_TIME

            return true
        }
    }

    data object EnergyTransfer : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val message = packet.message
            if (!message.startsWith(ENERGY_TRANS)) return false

            viewModel.allyShipIndex[packet.sender]?.let(viewModel.allyShips::get)?.also {
                it.hasEnergy = false
                if (viewModel.isDeepStrike) {
                    viewModel.cpu.launch {
                        delay(DEEP_STRIKE_TORPEDO_BUILD_TIME)
                        it.hasEnergy = true
                    }
                }
            }
            return true
        }
    }

    data object DeliveringReward : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val message = packet.message
            if (!message.endsWith(DROP_REWARD)) return false

            viewModel.allyShipIndex[packet.sender]?.also {
                viewModel.allyShips[it]?.status = AllyStatus.REWARD
            }

            return true
        }
    }

    data object NewMission : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val message = packet.message
            val srcIndex = NEW_MISSION.find(message)?.run { value.length } ?: return false

            return RewardType.entries
                .find { message.endsWith(it.parseKey) }
                ?.also { rewardType ->
                    process(
                        packet,
                        message.substring(srcIndex),
                        srcIndex <= SOURCE_DISCRIMINANT,
                        rewardType,
                        viewModel,
                    )
                } != null
        }

        private fun process(
            packet: CommsIncomingPacket,
            sourceName: String,
            isSourceStation: Boolean,
            rewardType: RewardType,
            viewModel: AgentViewModel,
        ) {
            viewModel.newMissionPacket.tryEmit(packet)

            val source =
                if (isSourceStation) {
                    viewModel.livingStations.values.find { station ->
                        station.obj.name.value?.let { stationName ->
                            sourceName.startsWith(stationName)
                        } == true
                    }
                } else {
                    viewModel.allyShips.values.find { ally ->
                        if (ally.isTrap) return@find false
                        val allyName = ally.obj.name.value ?: return@find false
                        sourceName.startsWith(allyName)
                    }
                } ?: return

            val destinationName = packet.sender
            val destination =
                viewModel.livingStationFullNameIndex[destinationName]?.let {
                    viewModel.livingStations[it]
                }
                    ?: viewModel.allyShips.values.find {
                        !it.isTrap && viewModel.getFullNameForShip(it.obj) == destinationName
                    }
                    ?: return

            val existingMission =
                viewModel.allMissions.find {
                    it.destination == destination && !it.isStarted && it.source == source
                }
            if (existingMission == null) {
                viewModel.allMissions.add(
                    SideMissionEntry(source, destination, rewardType, packet.timestamp)
                )
                viewModel.missionsExist = true
            } else {
                existingMission.rewards[rewardType.ordinal]++
            }
            if (viewModel.displayedRewards.contains(rewardType)) {
                source.missions++
                destination.missions++
            }

            viewModel.missionUpdate = true
        }
    }

    data object MissionProgress : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean =
            parseNormalProgress(packet, viewModel) ||
                parsePiratePickup(packet, viewModel) ||
                parsePirateCompletion(packet, viewModel)

        private fun parseNormalProgress(
            packet: CommsIncomingPacket,
            viewModel: AgentViewModel,
        ): Boolean {
            val (shipName, postfix) =
                packet.message.substringAfterStarting(PROGRESS)?.let {
                    getPlayerName(it, viewModel)?.let { playerName ->
                        playerName to it.substring(playerName.length)
                    }
                } ?: return false
            return parseNormalPickup(packet, shipName, postfix, viewModel) ||
                parseNormalCompletion(packet, shipName, postfix, viewModel)
        }

        private fun parseNormalPickup(
            packet: CommsIncomingPacket,
            shipName: String,
            postfix: String,
            viewModel: AgentViewModel,
        ): Boolean {
            val destination =
                postfix.substringAfterStarting(PROGRESS_1)?.let {
                    NEXT_DESTINATION.find(it)?.run { it.substring(0, range.first) }
                } ?: return false

            viewModel.missionProgressPacket.tryEmit(packet)
            processMissionProgress(packet.sender, destination, shipName, viewModel)
            return true
        }

        private fun parseNormalCompletion(
            packet: CommsIncomingPacket,
            shipName: String,
            postfix: String,
            viewModel: AgentViewModel,
        ): Boolean {
            if (postfix != PROGRESS_2) return false

            viewModel.missionCompletionPacket.tryEmit(packet)
            processMissionCompletion(packet.sender, shipName, viewModel)
            return true
        }

        private fun parsePiratePickup(
            packet: CommsIncomingPacket,
            viewModel: AgentViewModel,
        ): Boolean {
            val message = packet.message
            val (shipName, destination) =
                getPlayerName(message, viewModel)?.let { playerName ->
                    message
                        .substring(playerName.length)
                        .substringAfterStarting(PIRATE_PROGRESS_1)
                        ?.let {
                            PIRATE_NEXT_DESTINATION.find(it)?.run {
                                it.substring(range.last + 1, it.length - 1)
                            }
                        }
                        ?.let { playerName to it }
                } ?: return false

            viewModel.missionProgressPacket.tryEmit(packet)
            processMissionProgress(packet.sender, destination, shipName, viewModel)
            return true
        }

        private fun parsePirateCompletion(
            packet: CommsIncomingPacket,
            viewModel: AgentViewModel,
        ): Boolean {
            val shipName =
                packet.message
                    .takeIf { it.endsWith(PIRATE_PROGRESS_2) }
                    ?.let { message ->
                        PIRATE_COMPLETE.find(message)?.run { message.substring(value.length) }
                    }
                    ?.let { substring -> getPlayerName(substring, viewModel) } ?: return false

            viewModel.missionCompletionPacket.tryEmit(packet)
            processMissionCompletion(packet.sender, shipName, viewModel)
            return true
        }

        private fun getPlayerName(message: String, viewModel: AgentViewModel): String? =
            viewModel.players.values.mapNotNull { it.name.value }.find { message.startsWith(it) }

        private fun processMissionProgress(
            source: String,
            destination: String,
            shipName: String,
            viewModel: AgentViewModel,
        ) {
            viewModel.allMissions.forEach { mission ->
                if (
                    mission.isStarted ||
                        source != viewModel.getFullNameForShip(mission.source.obj) ||
                        destination != mission.destination.obj.name.value
                ) {
                    return@forEach
                }

                val totalRewards = viewModel.displayedRewards.sumOf { mission.rewards[it.ordinal] }
                mission.associatedShipName = shipName
                mission.source.missions -= totalRewards
                if (shipName != viewModel.playerName) {
                    mission.destination.missions -= totalRewards
                }
            }

            coalesceMissionRewards(shipName, viewModel)
        }

        private fun processMissionCompletion(
            destination: String,
            shipName: String,
            viewModel: AgentViewModel,
        ) {
            val timestamp = System.currentTimeMillis() + viewModel.completedDismissalTime
            viewModel.allMissions
                .filterNot { mission ->
                    mission.associatedShipName != shipName ||
                        mission.isCompleted ||
                        destination != viewModel.getFullNameForShip(mission.destination.obj)
                }
                .forEach { mission ->
                    mission.completionTimestamp = timestamp
                    viewModel.displayedRewards.forEach {
                        viewModel.payouts[it.ordinal] += mission.rewards[it.ordinal]
                    }

                    mission.destination.apply {
                        missions -= viewModel.displayedRewards.sumOf { mission.rewards[it.ordinal] }
                        if (this is ObjectEntry.Station) {
                            speedFactor += mission.rewards[RewardType.PRODUCTION.ordinal]
                        }
                    }
                }
            viewModel.updatePayouts()
        }

        private fun coalesceMissionRewards(shipName: String, viewModel: AgentViewModel) {
            val allRewards = RewardType.entries
            var i = 0
            while (i < viewModel.allMissions.size) {
                val mission = viewModel.allMissions[i++]
                if (mission.isCompleted || mission.associatedShipName != shipName) continue

                for (j in viewModel.allMissions.lastIndex downTo i) {
                    val otherMission = viewModel.allMissions[j]
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
                    viewModel.allMissions.removeAt(j)
                }
            }
        }
    }

    data object UnderAttack : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val message = packet.message
            if (UNDER_ATTACK.none(message::startsWith)) return false

            viewModel.stationAttackedPacket.tryEmit(packet)
            return true
        }
    }

    data object StationDestroyed : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val message = packet.message
            if (!message.startsWith(EXPLOSION)) return false

            viewModel.stationDestroyedPacket.tryEmit(packet)
            return true
        }
    }

    data object RewardDelivered : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val message = packet.message
            if (!message.startsWith(DELIVERED)) return false

            viewModel.allyShipIndex[packet.sender]?.let(viewModel.allyShips::get)?.apply {
                status = AllyStatus.NORMAL
                viewModel.livingStations.values
                    .minByOrNull { obj distanceSquaredTo it.obj }
                    ?.also {
                        viewModel.sendToServer(
                            CommsOutgoingPacket(
                                it.obj,
                                BaseMessage.PleaseReportStatus,
                                viewModel.vesselData,
                            )
                        )
                    }
            }

            return true
        }
    }

    data object RealCaptain : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val message = packet.message
            if (!message.startsWith(REAL_CAPTAIN)) return false

            viewModel.allyShipIndex[packet.sender]?.let(viewModel.allyShips::get)?.status =
                AllyStatus.NORMAL
            viewModel.payouts[RewardType.SHIELD.ordinal]++
            viewModel.updatePayouts()

            return true
        }
    }

    data object RescuedAmbassador : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val message = packet.message
            if (!AMBASS_PICKUP.containsMatchIn(message)) return false

            viewModel.allyShipIndex[packet.sender]?.let(viewModel.allyShips::get)?.status =
                AllyStatus.REPAIRING

            return true
        }
    }

    data object Directions : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val result = TURNING.find(packet.message) ?: return false
            val details = result.value.substring(TURNING_PREFIX.length)

            viewModel.allyShipIndex[packet.sender]?.let(viewModel.allyShips::get)?.apply {
                destination = null
                isAttacking = false
                isMovingToStation = false

                direction =
                    if (details.startsWith(TURNING_TO)) {
                        details.substring(TURNING_TO.length until details.length - 1).toInt()
                    } else {
                        val tailPosition = details.length - TURNING_DEGREES_OFFSET
                        val diff =
                            if (details.startsWith(TURNING_RIGHT)) {
                                details.substring(TURNING_RIGHT.length until tailPosition).toInt()
                            } else {
                                AgentViewModel.FULL_HEADING_RANGE -
                                    details
                                        .substring(TURNING_LEFT.length until tailPosition)
                                        .toInt()
                            }
                        ((direction ?: 0) + diff) % AgentViewModel.FULL_HEADING_RANGE
                    }
            }

            return true
        }
    }

    data object PlannedDestination : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val message = packet.message
            if (!message.endsWith(PLANNED)) return false

            viewModel.allyShipIndex[packet.sender]?.let(viewModel.allyShips::get)?.apply {
                direction = null
                destination = null
                isAttacking = false
                isMovingToStation = false
            }

            return true
        }
    }

    data object Attacking : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            packet.message.takeIf { message ->
                if (!message.startsWith(ATTACKING)) return@takeIf false
                val shipName = message.substring(ATTACKING.length, message.length - 1)
                viewModel.players.values.any { it.name.value == shipName }
            } ?: return false

            viewModel.allyShipIndex[packet.sender]?.let(viewModel.allyShips::get)?.apply {
                val nearestEnemy =
                    viewModel.enemies.values
                        .map { it.enemy }
                        .minByOrNull { it.horizontalDistanceSquaredTo(obj) }
                destination = nearestEnemy?.run { name.value }
                isAttacking = true
                isMovingToStation = false
            }

            return true
        }
    }

    data object HasDestination : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val message = packet.message
            val match = OK_GOING.find(message) ?: return false

            val destName = message.substring(match.value.length, message.length - 1)
            viewModel.allyShipIndex[packet.sender]?.let(viewModel.allyShips::get)?.apply {
                direction = null
                destination = destName
                isAttacking = false
                isMovingToStation = viewModel.livingStationNameIndex.containsKey(destName)
            }

            return true
        }
    }

    data object HailResponse : MessageParser {
        override fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean {
            val message = packet.message
            val responseMatch = OUR_SHIELDS.find(message)
            return when {
                responseMatch == null -> false
                responseMatch.value.length == message.length -> true
                else -> {
                    val response = message.substring(responseMatch.value.length + 1)
                    val sender = packet.sender
                    !sender.startsWith("DS") &&
                        HailResponseEffect.entries.any {
                            if (!it.appliesTo(response)) return@any false
                            val splitPoint = sender.lastIndexOf(" ")
                            val vesselName = sender.substring(0, splitPoint)
                            val name = sender.substring(splitPoint + 1)
                            viewModel.allyShipIndex[name]?.let(viewModel.allyShips::get)?.also {
                                ally ->
                                if (ally.vesselName == vesselName) {
                                    if (ally.status != AllyStatus.FLYING_BLIND)
                                        ally.status = it.getAllyStatus(response)
                                    ally.hasEnergy = response.endsWith(HAS_ENERGY)
                                    ally.checkNebulaStatus()
                                }
                            }
                            true
                        }
                }
            }
        }
    }

    fun parseResult(packet: CommsIncomingPacket, viewModel: AgentViewModel): Boolean

    private companion object {
        const val DEFAULT_BUILD_MINUTES = 5

        const val TSNCOM = "TSNCOM"
        val STAND_DOWN_SEARCH_RANGE = 25 until 28
        const val HAS_ENERGY = "some."
        const val SCRAMBLED = "iGH \nERROR% w23jr20ruj!!!"
        const val STANDBY = "Docking crew is ready, "
        const val PRODUCED = "We've produced another "
        const val PRODUCING = "Commencing production of "
        const val FIGHTER = "our ship.  You're welcome."
        const val ORDNANCE = "We're currently building another "
        const val HEAVE_TO = "son it and leave this sector."
        const val TORPEDO_TRANS = "Torpedo transfer complete."
        const val ENERGY_TRANS = "Here's the energy we prom"
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
        const val PLANNED = " our planned destination."
        const val ATTACKING = "Turning to attack, "
        const val WAR_WARNING = "This is an official WAR W"
        const val WAR_DECLARED = "ALERT!  War has been decl"
        const val TAUNTED = "Argh!  You terran scum!  "
        const val REUSED_TAUNT = "That won't work on me aga"
        const val RADIO_SILENCE = "You're a fool, Terran.  I"
        // ship names have max
        // length of 24 characters

        val UNDER_ATTACK = arrayOf(ATTACK_1, ATTACK_2, ATTACK_3, ATTACK_4)

        val WAR_MESSAGES = arrayOf("", WAR_WARNING, WAR_DECLARED)

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
        const val SOURCE_DISCRIMINANT = 35
        const val MINUTES_LENGTH = 9

        fun String.substringAfterStarting(prefix: String): String? =
            if (startsWith(prefix)) substring(prefix.length) else null
    }
}
