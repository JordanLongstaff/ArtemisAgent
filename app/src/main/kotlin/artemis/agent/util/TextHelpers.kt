package artemis.agent.util

import android.content.Context
import androidx.annotation.StringRes
import artemis.agent.R
import com.walkertribe.ian.world.Shields

private const val PERCENT = 100

fun getShieldText(
    context: Context,
    @StringRes shieldString: Int,
    shields: Shields,
    includePercentage: Boolean,
): String {
    val shieldText =
        context.getString(
            shieldString,
            shields.strength.value.coerceAtLeast(0f),
            shields.maxStrength.value,
        )
    val percentageText =
        if (includePercentage)
            " " + context.getString(R.string.percentage_paren, shields.percentage * PERCENT)
        else ""

    return "$shieldText$percentageText"
}

fun getDamageReportText(
    context: Context,
    systemLabel: String,
    nodeCount: Int,
    damageCount: Int,
    damageValue: Double,
): String {
    val reportLine = context.getString(R.string.node_count, systemLabel, damageCount, nodeCount)
    val percentageText = context.getString(R.string.percentage_paren, damageValue * PERCENT)

    return "$reportLine $percentageText"
}
