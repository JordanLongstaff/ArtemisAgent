package artemis.agent.game.enemies

import android.content.Context
import androidx.annotation.StringRes

sealed class EnemySortCategory(val scrollIndex: Int) {
    class Res(@StringRes val resId: Int, scrollIndex: Int) : EnemySortCategory(scrollIndex) {
        override fun getString(context: Context): String = context.getString(resId)
    }

    class Text(val text: String, scrollIndex: Int) : EnemySortCategory(scrollIndex) {
        override fun getString(context: Context): String = text
    }

    abstract fun getString(context: Context): String
}
