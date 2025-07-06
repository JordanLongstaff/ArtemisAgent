package artemis.agent.game.route

import android.content.Context
import artemis.agent.AgentViewModel
import artemis.agent.R
import artemis.agent.game.ObjectEntry

data class RouteEntry(val objEntry: ObjectEntry<*>) {
    var pathKey: Int = objEntry.obj.id

    fun getReasonText(
        objective: RouteObjective,
        context: Context,
        viewModel: AgentViewModel,
    ): String =
        when (objective) {
            is RouteObjective.ReplacementFighters ->
                if (objEntry is ObjectEntry.Station) objEntry.getFightersText(context) else ""
            is RouteObjective.Ordnance ->
                if (objEntry is ObjectEntry.Station)
                    objEntry.getOrdnanceText(viewModel, context, objective.ordnanceType)
                else ""
            is RouteObjective.Tasks -> getReasonTextForTasks(viewModel, context)
        }

    private fun getReasonTextForTasks(viewModel: AgentViewModel, context: Context): String =
        buildList {
                if (objEntry is ObjectEntry.Ally) {
                    addAll(
                        viewModel.routeIncentives
                            .filter { it.matches(objEntry) }
                            .map { context.getString(it.getTextFor(objEntry)) }
                    )
                }

                if (viewModel.routeIncludesMissions) {
                    objEntry.missions
                        .takeIf { it > 0 }
                        ?.also { missions ->
                            add(
                                context.resources.getQuantityString(
                                    R.plurals.side_missions,
                                    missions,
                                    missions,
                                )
                            )
                        }
                }
            }
            .joinToString()
            .let { if (it.isEmpty()) it else it[0].uppercase() + it.substring(1) }

    fun getBuildTimeText(objective: RouteObjective, context: Context): String =
        when {
            objEntry !is ObjectEntry.Station -> ""
            objective !is RouteObjective.Ordnance -> ""
            objEntry.builtOrdnanceType != objective.ordnanceType -> ""
            else -> objEntry.getTimerText(context)
        }

    override fun hashCode(): Int = pathKey

    override fun equals(other: Any?): Boolean = other is RouteEntry && other.pathKey == pathKey

    override fun toString(): String = objEntry.toString()
}
