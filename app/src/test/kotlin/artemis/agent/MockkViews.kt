package artemis.agent

import android.view.View
import androidx.annotation.IdRes
import androidx.viewbinding.ViewBindings
import io.mockk.every
import io.mockk.mockk

inline fun <reified V : View> mockkView(mockkId: Int): V = mockk { every { id } returns mockkId }

inline fun <reified V : View> mockkViewBinding(@IdRes id: Int, mockkId: Int = View.NO_ID) {
    every { ViewBindings.findChildViewById<V>(any(), id) } returns mockkView(mockkId)
}
