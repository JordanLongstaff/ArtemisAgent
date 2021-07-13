package artemis.agent

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withParent
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual

class ViewModelKonsistTest : DescribeSpec({
    val viewModelClasses = Konsist.scopeFromModule("app")
        .classes()
        .withParent { it.hasNameEndingWith("ViewModel") }

    it("Only one ViewModel class exists") {
        viewModelClasses.size shouldBeEqual 1
    }

    it("ViewModel class names end with ViewModel") {
        viewModelClasses.assertTrue { it.hasNameEndingWith("ViewModel") }
    }

    it("ViewModel classes are top-level") {
        viewModelClasses.assertTrue { it.isTopLevel }
    }

    it("ViewModel classes share name with containing file") {
        viewModelClasses.assertTrue { it.containingFile.name == it.name }
    }
})
