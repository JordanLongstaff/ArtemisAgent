package artemis.agent

import androidx.fragment.app.Fragment
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.ext.list.withParentOf
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.string.shouldEndWith

class FragmentKonsistTest : DescribeSpec({
    val moduleClasses = Konsist.scopeFromModule("app").classes()
    val fragmentClasses = moduleClasses.withParentOf(Fragment::class)
    val fragmentNamedClasses = moduleClasses.withNameEndingWith("Fragment")

    val viewModelDelegate = "activityViewModels()"
    val bindingDelegate = "fragmentViewBinding()"

    describe("Classes with names ending in Fragment are subclasses of Fragment") {
        withData(nameFn = { it.name }, fragmentNamedClasses) { fragmentClass ->
            fragmentClass.assertTrue { it.hasParentOf(Fragment::class) }
        }
    }

    describe("Fragment class names end with Fragment") {
        withData(fragmentClasses.map { it.name }) { it shouldEndWith "Fragment" }
    }

    describe("Fragment classes are top-level") {
        withData(nameFn = { it.name }, fragmentClasses) { fragmentClass ->
            fragmentClass.assertTrue { it.isTopLevel }
        }
    }

    describe("Fragment classes share name with containing file") {
        withData(nameFn = { it.name }, fragmentClasses) { fragmentClass ->
            fragmentClass.assertTrue {
                it.containingFile.name == it.name
            }
        }
    }

    describe("Fragment classes reference AgentViewModel") {
        withData(nameFn = { it.name }, fragmentClasses) { fragmentClass ->
            fragmentClass.assertTrue {
                it.hasProperty { property ->
                    property.hasTypeOf(AgentViewModel::class) &&
                        property.delegateName == viewModelDelegate
                }
            }
        }
    }

    describe("Fragment classes reference view binding") {
        withData(nameFn = { it.name }, fragmentClasses) { fragmentClass ->
            fragmentClass.assertTrue {
                it.hasProperty { property ->
                    property.hasType { type ->
                        type.hasNameEndingWith("Binding")
                    } && property.delegateName == bindingDelegate
                }
            }
        }
    }

    describe("Fragment classes override onViewCreated") {
        withData(nameFn = { it.name }, fragmentClasses) { fragmentClass ->
            fragmentClass.assertTrue {
                it.hasFunction { function ->
                    function.name == "onViewCreated" && function.hasOverrideModifier
                }
            }
        }
    }
})
