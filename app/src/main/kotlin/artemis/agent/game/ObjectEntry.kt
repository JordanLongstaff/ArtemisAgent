package artemis.agent.game

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.game.allies.AllySortIndex
import artemis.agent.game.allies.AllyStatus
import artemis.agent.game.missions.SideMissionStatus
import artemis.agent.util.TimerText
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.world.ArtemisBase
import com.walkertribe.ian.world.ArtemisNpc
import com.walkertribe.ian.world.ArtemisShielded
import java.util.SortedMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

sealed class ObjectEntry<Obj : ArtemisShielded<Obj>>(
    val obj: Obj,
    vesselData: VesselData,
    @PluralsRes private val missionsTextRes: Int,
) {
    class Ally(npc: ArtemisNpc, vesselData: VesselData, private val isDeepStrikeShip: Boolean) :
        ObjectEntry<ArtemisNpc>(npc, vesselData, R.plurals.side_missions_for_ally) {
        val vesselName: String by lazy { npc.getVessel(vesselData)?.name ?: "" }
        var status: AllyStatus = AllyStatus.NORMAL
            set(value) {
                field = value
                isHailed = true
            }

        var hasEnergy: Boolean = false
        var destination: String? = null
        var isAttacking: Boolean = false
        var isMovingToStation: Boolean = false
        var direction: Int? = null

        private var isHailed: Boolean = false
        val isTrap: Boolean
            get() = status.sortIndex == AllySortIndex.TRAP

        val isNormal: Boolean
            get() = status.sortIndex == AllySortIndex.NORMAL

        val isDamaged: Boolean
            get() = obj.shieldsFront.isDamaged || obj.shieldsRear.isDamaged

        val isInstructable: Boolean
            get() = isHailed && (isNormal || status == AllyStatus.FLYING_BLIND)

        override val missionStatus: SideMissionStatus
            get() =
                when {
                    isDamaged -> SideMissionStatus.DAMAGED
                    status.sortIndex <= AllySortIndex.COMMANDEERED -> SideMissionStatus.OVERTAKEN
                    else -> SideMissionStatus.ALL_CLEAR
                }

        override fun getBackgroundColor(context: Context): Int =
            ContextCompat.getColor(
                context,
                if (isDeepStrikeShip && isDamaged) R.color.allyStatusBackgroundYellow
                else status.backgroundColor,
            )

        fun checkNebulaStatus() {
            val isInNebula = obj.isInNebula.value.booleanValue
            if (status == AllyStatus.COMMANDEERED && isInNebula) {
                status = AllyStatus.COMMANDEERED_NEBULA
            } else if (status == AllyStatus.COMMANDEERED_NEBULA && !isInNebula) {
                status = AllyStatus.COMMANDEERED
            }
        }
    }

    class Station(station: ArtemisBase, vesselData: VesselData) :
        ObjectEntry<ArtemisBase>(station, vesselData, R.plurals.side_missions) {
        var fighters: Int = 0
        var isDocking: Boolean = false
        var isDocked: Boolean = false
        var isStandingBy: Boolean = false
        var speedFactor: Int = 1

        private var startTime = 0L
        private var endTime = 0L
        private var firstMissile = false
        private var setMissile = false
        private var midBuild = false

        var isPaused: Boolean = false
            set(paused) {
                if (paused) {
                    field = true
                } else if (endTime >= System.currentTimeMillis()) {
                    field = false
                }
            }

        private val normalProductionCoefficient: Int =
            station.getVessel(vesselData)?.run { (productionCoefficient * 2).toInt() } ?: 2

        var builtOrdnanceType: OrdnanceType = OrdnanceType.TORPEDO
            set(type) {
                if (setMissile) return
                startTime = System.currentTimeMillis()
                field = type
                if (firstMissile && !midBuild) {
                    val buildTime =
                        (type.buildTime shl 1) / normalProductionCoefficient / speedFactor
                    endTime = startTime + buildTime
                }
                firstMissile = true
                setMissile = true
            }

        val ordnanceStock: SortedMap<OrdnanceType, Int> = sortedMapOf()

        override val missionStatus: SideMissionStatus
            get() =
                if (obj.shieldsFront.isDamaged) SideMissionStatus.DAMAGED
                else SideMissionStatus.ALL_CLEAR

        @get:StringRes
        val statusString: Int?
            get() =
                when {
                    isDocked -> R.string.docked
                    isDocking -> R.string.docking
                    isStandingBy -> R.string.standby
                    else -> null
                }

        override fun getBackgroundColor(context: Context): Int =
            getStationColorForShieldPercent(obj.shieldsFront.percentage, context)

        fun setBuildMinutes(minutes: Int) {
            if (firstMissile || setMissile) return
            endTime = System.currentTimeMillis() + minutes.minutes.inWholeMilliseconds
        }

        fun recalibrateSpeed(endOfBuild: Long) {
            if (isPaused) {
                isPaused = false
                return
            }
            val recalibrateTime = endOfBuild - startTime
            val buildTime = endTime - startTime
            speedFactor =
                ((speedFactor * buildTime + (recalibrateTime ushr 1)) / recalibrateTime)
                    .toInt()
                    .coerceAtLeast(1)
        }

        fun reconcileSpeed(minutes: Int) {
            if (midBuild) return
            midBuild = true

            val normalTime = (builtOrdnanceType.buildTime shl 1) / normalProductionCoefficient
            val predictedTime = normalTime / speedFactor
            val predictedMinutes = (predictedTime - 1).milliseconds.inWholeMinutes + 1
            if (predictedMinutes.toInt() == minutes) return

            val estimatedSpeed = ((predictedMinutes - 1) / minutes + 1).toInt()
            val expectedTime = normalTime / estimatedSpeed
            val expectedMinutes = (expectedTime - 1).milliseconds.inWholeMinutes + 1

            val actualTime: Long
            if (expectedMinutes < minutes) {
                speedFactor = estimatedSpeed - 1
                actualTime = minutes.minutes.inWholeMilliseconds
            } else {
                speedFactor = estimatedSpeed
                actualTime = expectedTime
            }

            endTime = actualTime + startTime
        }

        fun resetMissile() {
            setMissile = midBuild && setMissile
        }

        fun resetBuildProgress() {
            midBuild = false
        }

        fun getSpeedText(context: Context): String =
            context.getString(
                R.string.station_speed,
                speedFactor * normalProductionCoefficient * BASE_SPEED,
            )

        fun getFightersText(context: Context): String =
            context.resources.getQuantityString(R.plurals.replacement_fighters, fighters, fighters)

        fun getOrdnanceText(
            viewModel: AgentViewModel,
            context: Context,
            ordnanceType: OrdnanceType,
        ): String =
            if (ordnanceStock.containsKey(ordnanceType))
                context.getString(
                    R.string.stock_of_ordnance,
                    ordnanceStock[ordnanceType],
                    ordnanceType.getLabelFor(viewModel.version),
                )
            else ""

        fun getTimerText(context: Context): String =
            context.getString(R.string.build_timer, TimerText.getTimeUntil(endTime))

        companion object {
            private val CALLSIGN_REGEX = Regex("DS\\d+")
            private val ENEMY_REGEX = Regex("^[A-Z][a-z]+ Base \\d+")

            val FRIENDLY_COMPARATOR =
                buildStationNameComparator(CALLSIGN_REGEX) { it.substring(2).toInt() }

            val ENEMY_COMPARATOR =
                buildStationNameComparator(ENEMY_REGEX) { it.substringAfterLast(' ').toInt() }

            private fun buildStationNameComparator(
                regex: Regex,
                selector: (String) -> Comparable<*>,
            ) =
                Comparator<String> { first, second ->
                    val firstName = first.orEmpty()
                    val secondName = second.orEmpty()

                    if (regex matches firstName && regex matches secondName)
                        compareValuesBy(firstName, secondName, selector)
                    else compareValues(firstName, secondName)
                }
        }
    }

    var missions: Int = 0
    var heading: String = ""
    var range: Float = 0f

    val fullName: String by lazy { obj.getFullName(vesselData) ?: "" }

    abstract val missionStatus: SideMissionStatus

    fun getMissionsText(context: Context): String =
        context.resources.getQuantityString(missionsTextRes, missions, missions)

    override fun hashCode(): Int = obj.hashCode()

    override fun equals(other: Any?): Boolean = other is ObjectEntry<*> && other.obj == obj

    override fun toString(): String = obj.toString()

    @ColorInt abstract fun getBackgroundColor(context: Context): Int

    companion object {
        private const val BASE_SPEED = 0.5f

        private val GRADIENT =
            arrayOf(
                Pair(1.0f, R.color.stationShieldFull),
                Pair(0.7f, R.color.stationShieldDamaged),
                Pair(0.4f, R.color.stationShieldModerate),
                Pair(0.2f, R.color.stationShieldSevere),
                Pair(0.0f, R.color.stationShieldCritical),
            )

        fun getStationColorForShieldPercent(percent: Float, context: Context): Int =
            GRADIENT.find { percent >= it.first }
                ?.let { ContextCompat.getColor(context, it.second) } ?: Color.TRANSPARENT
    }
}
