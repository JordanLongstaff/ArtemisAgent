package artemis.agent.game.status

import android.content.Context
import androidx.annotation.StringRes
import artemis.agent.R
import artemis.agent.util.getDamageReportText
import artemis.agent.util.getShieldText
import com.walkertribe.ian.enums.OrdnanceType
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.world.Shields

sealed interface StatusInfo {
    data object Empty : StatusInfo {
        override fun getString(context: Context): String = ""

        override fun itemEquals(other: StatusInfo): Boolean = other is Empty
    }

    data class Header(@all:StringRes val headerString: Int) : StatusInfo {
        override fun getString(context: Context): String = context.getString(headerString)

        override fun itemEquals(other: StatusInfo): Boolean = this == other
    }

    data class Energy(val energy: Float) : StatusInfo {
        override fun getString(context: Context): String =
            context.getString(R.string.energy_reserves, energy)

        override fun itemEquals(other: StatusInfo): Boolean = other is Energy
    }

    class Shield(val position: ShieldPosition, val shield: Shields) : StatusInfo {
        override fun getString(context: Context): String =
            getShieldText(context, position.stringId, shield)

        override fun itemEquals(other: StatusInfo): Boolean =
            other is Shield && position == other.position
    }

    data class OrdnanceCount(
        val ordnance: OrdnanceType,
        val version: Version,
        val count: Int,
        val max: Int,
    ) : StatusInfo {
        override fun getString(context: Context): String =
            context.getString(R.string.ordnance_stock, ordnance.getLabelFor(version), count, max)

        override fun itemEquals(other: StatusInfo): Boolean =
            other is OrdnanceCount && ordnance == other.ordnance
    }

    data class Singleseat(@all:StringRes val fighterLabel: Int, val count: Int) : StatusInfo {
        override fun getString(context: Context): String = context.getString(fighterLabel, count)

        override fun itemEquals(other: StatusInfo): Boolean =
            other is Singleseat && fighterLabel == other.fighterLabel
    }

    data class DamageReport(
        val systemLabel: String,
        val nodeCount: Int,
        val damageCount: Int,
        val damageValue: Double,
    ) : StatusInfo {
        override fun getString(context: Context): String =
            getDamageReportText(context, systemLabel, nodeCount, damageCount, damageValue)

        override fun itemEquals(other: StatusInfo): Boolean =
            other is DamageReport && systemLabel == other.systemLabel
    }

    fun getString(context: Context): String

    fun itemEquals(other: StatusInfo): Boolean
}
