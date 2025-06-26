package artemis.agent

import android.content.Context
import androidx.annotation.StringRes
import artemis.agent.util.VersionString
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.walkertribe.ian.util.Version

sealed class UpdateAlert(@all:StringRes val title: Int, @all:StringRes val message: Int) {
    sealed class ArtemisVersion(@StringRes message: Int, internal val newVersion: String) :
        UpdateAlert(R.string.new_version_title, message) {
        class Restart(newVersion: String) :
            ArtemisVersion(R.string.new_version_restart_message, newVersion)

        class Update(newVersion: String) :
            ArtemisVersion(R.string.new_version_update_message, newVersion)

        final override fun getTitle(context: Context): String = context.getString(title, newVersion)

        final override fun getMessage(context: Context): String =
            context.getString(message, newVersion)
    }

    data object Immediate :
        UpdateAlert(R.string.update_required_title, R.string.update_required_message)

    data object Flexible :
        UpdateAlert(R.string.update_available_title, R.string.update_available_message)

    open fun getTitle(context: Context): String = context.getString(title)

    open fun getMessage(context: Context): String = context.getString(message)

    companion object {
        fun check(maxVersion: Version, latestVersionCode: Int): UpdateAlert? {
            val remoteConfig = Firebase.remoteConfig
            val nextVersion = BuildConfig.VERSION_CODE + 1
            val updateRange = nextVersion..latestVersionCode

            val securityUpdate =
                remoteConfig.getLong(RemoteConfigKey.RequiredVersion.SECURITY).toInt()
            val artemisUpdate =
                remoteConfig.getLong(RemoteConfigKey.RequiredVersion.ARTEMIS).toInt()
            val artemisVersion =
                VersionString(remoteConfig.getString(RemoteConfigKey.ARTEMIS_LATEST_VERSION))

            return when {
                securityUpdate in updateRange -> Immediate
                artemisUpdate in updateRange -> ArtemisVersion.Update(artemisVersion.toString())
                !updateRange.isEmpty() -> Flexible
                artemisVersion.toVersion() > maxVersion ->
                    ArtemisVersion.Restart(artemisVersion.toString())
                else -> null
            }
        }
    }
}
