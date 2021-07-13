package artemis.agent.databinding

import android.os.Looper
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class FragmentViewBindingDelegate<VB : ViewBinding>(
    private val viewBinder: ViewBinder<VB>
) : ReadOnlyProperty<Fragment, VB> {
    private var viewBinding: VB? = null
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        @MainThread
        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            owner.lifecycle.removeObserver(this)
            viewBinding = null
        }
    }

    @MainThread
    override fun getValue(thisRef: Fragment, property: KProperty<*>): VB {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "Must be called from main thread"
        }

        viewBinding?.let { return it }

        val view = thisRef.requireView()
        thisRef.viewLifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        return viewBinder.bind(view).also { viewBinding = it }
    }
}

inline fun <reified VB : ViewBinding> fragmentViewBinding(): FragmentViewBindingDelegate<VB> {
    return FragmentViewBindingDelegate(ViewBinder.defaultBinder())
}
