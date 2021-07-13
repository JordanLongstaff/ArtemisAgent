package artemis.agent.databinding

import android.view.View
import androidx.viewbinding.ViewBinding

fun interface ViewBinder<T : ViewBinding> {
    fun bind(view: View): T

    companion object {
        inline fun <reified VB : ViewBinding> defaultBinder(): ViewBinder<VB> {
            return ViewBinder {
                VB::class.java.getMethod("bind", View::class.java).invoke(null, it) as VB
            }
        }
    }
}
