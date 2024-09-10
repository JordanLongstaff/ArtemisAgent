package com.walkertribe.ian

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty

class AnnotationsKonsistTest : DescribeSpec({
    val annotationsScope = Konsist.scopeFromModule("IAN/annotations")
    val classes = annotationsScope.classes()
    val interfaces = annotationsScope.interfaces()
    val objects = annotationsScope.objects()

    describe("Annotations module only contains annotation classes") {
        interfaces.shouldBeEmpty()
        objects.shouldBeEmpty()

        withData(nameFn = { it.fullyQualifiedName.toString() }, classes) { cls ->
            cls.assertTrue { it.hasAnnotationModifier }
        }
    }
})
