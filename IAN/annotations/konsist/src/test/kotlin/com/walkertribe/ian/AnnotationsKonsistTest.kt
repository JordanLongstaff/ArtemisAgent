package com.walkertribe.ian

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty

class AnnotationsKonsistTest :
    DescribeSpec({
      val annotationsScope = Konsist.scopeFromModule("IAN/annotations")

      describe("Annotations module only contains annotation classes") {
        annotationsScope.interfacesAndObjects().shouldBeEmpty()

        withData(nameFn = { it.fullyQualifiedName.toString() }, annotationsScope.classes()) { cls ->
          cls.assertTrue { it.hasAnnotationModifier }
        }
      }
    })
