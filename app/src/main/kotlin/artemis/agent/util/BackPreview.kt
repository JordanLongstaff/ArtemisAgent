package artemis.agent.util

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback

abstract class BackPreview(enabled: Boolean) : OnBackPressedCallback(enabled) {
    abstract fun preview()

    abstract fun revert()

    protected open fun beforePreview() {}

    protected open fun close() {}

    fun onBackStarted() {
        beforePreview()
        preview()
    }

    final override fun handleOnBackStarted(backEvent: BackEventCompat) {
        onBackStarted()
    }

    final override fun handleOnBackProgressed(backEvent: BackEventCompat) {
        if (backEvent.progress > 0f) {
            preview()
        } else {
            revert()
        }
    }

    final override fun handleOnBackCancelled() {
        revert()
        close()
    }

    final override fun handleOnBackPressed() {
        isEnabled = false
        preview()
        close()
    }
}
