package artemis.agent

import android.content.Context
import androidx.appcompat.app.AlertDialog
import io.noties.markwon.Markwon
import java.io.FileNotFoundException

enum class UpdateCheck {
    STARTUP {
        private val appVersionFile = "app_version.dat"

        override fun showAlert(context: Context) {
            val currentAppVersion = BuildConfig.VERSION_NAME
            val previousAppVersion =
                try {
                    context.openFileInput(appVersionFile).use { it.readBytes().decodeToString() }
                } catch (_: FileNotFoundException) {
                    ""
                }

            if (currentAppVersion != previousAppVersion) {
                context.openFileOutput(appVersionFile, Context.MODE_PRIVATE).use {
                    it.write(currentAppVersion.encodeToByteArray())
                }

                val changelog =
                    Markwon.create(context).toMarkdown(context.getString(R.string.changelog))

                AlertDialog.Builder(context)
                    .setTitle(R.string.app_version)
                    .setMessage(changelog)
                    .setCancelable(true)
                    .show()
            }
        }
    },
    MANUAL {
        override fun showAlert(context: Context) {
            AlertDialog.Builder(context)
                .setTitle(R.string.app_version)
                .setMessage(R.string.no_updates)
                .setCancelable(true)
                .show()
        }
    },
    GAME_END {
        override fun showAlert(context: Context) {
            // Do nothing
        }
    };

    abstract fun showAlert(context: Context)
}
