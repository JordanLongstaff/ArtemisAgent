package artemis.agent

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withParentOf
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeSingleton

class ViewModelKonsistTest :
    DescribeSpec({
        val viewModelClasses =
            Konsist.scopeFromModule("app")
                .classes()
                .withParentOf(ViewModel::class, AndroidViewModel::class)

        it("Only one ViewModel class exists") { viewModelClasses.shouldBeSingleton() }

        it("ViewModel class names end with ViewModel") {
            viewModelClasses.assertTrue { it.hasNameEndingWith("ViewModel") }
        }

        it("ViewModel classes are top-level") { viewModelClasses.assertTrue { it.isTopLevel } }

        it("ViewModel classes share name with containing file") {
            viewModelClasses.assertTrue { it.containingFile.name == it.name }
        }
    })
