package artemis.agent

import android.content.Intent
import android.os.Binder
import androidx.lifecycle.LifecycleService

/**
 * Notification service that starts whenever the app shifts to the background so that the
 * connection can stay afloat and notifications will be pushed. Also contains the logic of allowing
 * biomechs to be re-frozen when the appropriate button is pressed on a notification.
 */
class NotificationService : LifecycleService() {
    private var binder: LocalBinder? = LocalBinder(this)

    override fun onBind(intent: Intent): LocalBinder? {
        super.onBind(intent)
        return binder
    }

    /**
     * Receives possible intents to re-freeze Biomechs with the specified ID. Notification also
     * gets dismissed at the same time, if the Biomech was found.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getIntExtra(EXTRA_BIOMECH_ID, -1)?.also { id ->
            binder?.viewModel?.apply {
                scannedBiomechs.find { it.biomech.id == id }?.also {
                    it.freeze(this)
                    binder?.notificationManager?.dismissBiomechMessage(
                        getFullNameForShip(it.biomech)
                    )
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * When service is unbound. it stops and clears all notifications.
     */
    override fun onUnbind(intent: Intent?): Boolean {
        binder?.apply {
            notificationManager?.reset()
            notificationManager = null
            viewModel = null
            service = null
            binder = null
        }
        stopSelf()
        return super.onUnbind(intent)
    }

    /**
     * Binder class containing all important references to running data structures.
     */
    class LocalBinder(var service: NotificationService?) : Binder() {
        var viewModel: AgentViewModel? = null
        var notificationManager: NotificationManager? = null
    }

    companion object {
        const val EXTRA_BIOMECH_ID = "artemis.agent.EXTRA_BIOMECH_ID"
    }
}
