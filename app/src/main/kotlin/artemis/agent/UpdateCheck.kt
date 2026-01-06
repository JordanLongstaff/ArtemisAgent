package artemis.agent

import android.content.Context
import androidx.appcompat.app.AlertDialog
import io.noties.markwon.Markwon
import java.io.FileNotFoundException

enum class UpdateCheck {
    STARTUP {
        private val appVersionFile = "app_version.dat"

        override fun createAlert(context: Context): AlertDialog.Builder? {
            val currentAppVersion = BuildConfig.VERSION_NAME
            val previousAppVersion =
                try {
                    context.openFileInput(appVersionFile).use { it.readBytes().decodeToString() }
                } catch (_: FileNotFoundException) {
                    ""
                }

            return if (currentAppVersion != previousAppVersion) {
                context.openFileOutput(appVersionFile, Context.MODE_PRIVATE).use {
                    it.write(currentAppVersion.encodeToByteArray())
                }

                val changelog =
                    Markwon.create(context).toMarkdown(context.getString(R.string.changelog))

                AlertDialog.Builder(context)
                    .setTitle(R.string.app_version)
                    .setMessage(changelog)
                    .setCancelable(true)
            } else {
                null
            }
        }
    },
    MANUAL {
        override fun createAlert(context: Context): AlertDialog.Builder? =
            AlertDialog.Builder(context)
                .setTitle(R.string.app_version)
                .setMessage(R.string.no_updates)
                .setCancelable(true)
    },
    GAME_END {
        // No dialog to show on game end
        override fun createAlert(context: Context): AlertDialog.Builder? = null
    };

    abstract fun createAlert(context: Context): AlertDialog.Builder?
}
