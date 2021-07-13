package artemis.agent.game.route

import artemis.agent.AgentViewModel
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.util.Version

sealed interface RouteObjective {
    object Tasks : RouteObjective {
        override fun hashCode(): Int = OrdnanceType.entries.size + 1

        override fun equals(other: Any?): Boolean = other is Tasks

        override fun getDataFrom(viewModel: AgentViewModel): String = ""
    }

    object ReplacementFighters : RouteObjective {
        override fun hashCode(): Int = OrdnanceType.entries.size

        override fun equals(other: Any?): Boolean = other is ReplacementFighters

        override fun getDataFrom(viewModel: AgentViewModel): String {
            val maxFighters = viewModel.playerShip?.getVessel(viewModel.vesselData)?.bayCount ?: 0
            val extraShuttle = if (viewModel.version >= SHUTTLE_VERSION) 1 else 0
            val totalFighters = viewModel.totalFighters.value
            return "$totalFighters/${maxFighters + extraShuttle}"
        }

        val REPORT_VERSION = Version(2, 4, 0)
        val SHUTTLE_VERSION = Version(2, 6, 0)
    }

    data class Ordnance(val ordnanceType: OrdnanceType) : RouteObjective {
        override fun hashCode(): Int = ordnanceType.ordinal

        override fun equals(other: Any?): Boolean =
            other is Ordnance && other.ordnanceType == ordnanceType

        override fun getDataFrom(viewModel: AgentViewModel): String {
            val playerShip = viewModel.playerShip ?: return ""
            val maxOrdnance = playerShip.getVessel(viewModel.vesselData)?.run {
                ordnanceStorage[ordnanceType]
            } ?: 0
            val currentOrdnance = playerShip.getTotalOrdnanceCount(ordnanceType)
            return "$currentOrdnance/$maxOrdnance"
        }
    }

    fun getDataFrom(viewModel: AgentViewModel): String
}
