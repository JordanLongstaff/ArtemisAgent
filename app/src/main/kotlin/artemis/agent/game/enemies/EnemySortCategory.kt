package artemis.agent.game.enemies

import android.content.Context
import androidx.annotation.StringRes

sealed interface EnemySortCategory {
    data class Res(@all:StringRes val resId: Int, override val scrollIndex: Int) :
        EnemySortCategory {
        override fun getString(context: Context): String = context.getString(resId)

        override fun hashCode(): Int = resId

        override fun equals(other: Any?): Boolean = other is Res && resId == other.resId
    }

    data class Text(val text: String, override val scrollIndex: Int) : EnemySortCategory {
        override fun getString(context: Context): String = text

        override fun hashCode(): Int = text.hashCode()

        override fun equals(other: Any?): Boolean = other is Text && text == other.text
    }

    val scrollIndex: Int

    fun getString(context: Context): String
}
