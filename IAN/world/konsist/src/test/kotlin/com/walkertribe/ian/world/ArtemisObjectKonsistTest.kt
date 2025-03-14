package com.walkertribe.ian.world

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.classes
import com.lemonappdev.konsist.api.ext.list.classesAndInterfaces
import com.lemonappdev.konsist.api.ext.list.modifierprovider.withSealedModifier
import com.lemonappdev.konsist.api.ext.list.modifierprovider.withoutAbstractModifier
import com.lemonappdev.konsist.api.ext.list.objects
import com.lemonappdev.konsist.api.ext.list.withAllConstructors
import com.lemonappdev.konsist.api.ext.list.withName
import com.lemonappdev.konsist.api.ext.list.withParent
import com.lemonappdev.konsist.api.ext.list.withParentOf
import com.lemonappdev.konsist.api.ext.list.withRepresentedTypeOf
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty

class ArtemisObjectKonsistTest :
    DescribeSpec({
      val module = Konsist.scopeFromProject("IAN/world", "main")
      val classes = module.classes()
      val objectClasses = classes.withParentOf(ArtemisObject::class, indirectParents = true)
      val playerClasses = objectClasses.withRepresentedTypeOf(ArtemisPlayer::class)
      val objectInterfaces =
          module.interfaces().withName("ArtemisObject") +
              module.interfaces().withParentOf(ArtemisObject::class, indirectParents = true)
      val world = "com.walkertribe.ian.world"

      describe("Object classes have Dsl object, except ArtemisPlayer") {
        withData(
            nameFn = { it.name },
            objectClasses.withoutAbstractModifier().subtract(playerClasses.toSet()),
        ) {
          it.assertTrue { objectClass ->
            objectClass.hasObject { inner ->
              inner.name == "Dsl" &&
                  inner.hasParent { parent -> parent.hasNameEndingWith("Dsl<${objectClass.name}>") }
            }
          }
        }
      }

      describe("Object classes reside in package $world") {
        withData(nameFn = { it.name }, objectInterfaces + objectClasses) { int ->
          int.assertTrue { it.resideInPackage(world) }
        }
      }

      describe("Object classes are top-level") {
        withData(nameFn = { it.name }, objectInterfaces + objectClasses) { int ->
          int.assertTrue { it.isTopLevel }
        }
      }

      describe("Object classes share name with containing file") {
        withData(nameFn = { it.name }, objectInterfaces + objectClasses) { int ->
          int.assertTrue { it.containingFile.name == it.name }
        }
      }

      describe("Object classes have one primary constructor accepting ID and timestamp") {
        val requiredParameters = listOf("id" to "Int", "timestamp" to "Long")
        withData(nameFn = { it.name }, objectClasses) { cls ->
          cls.assertTrue {
            it.primaryConstructor?.takeIf { constr ->
              constr.parameters.map { param -> param.name to param.type.name } == requiredParameters
            } != null
          }
        }
      }

      describe("ArtemisPlayer") {
        val dslClasses =
            playerClasses
                .classes()
                .withName("Dsl")
                .withSealedModifier()
                .withAllConstructors { it.hasPrivateModifier }
                .withParent { it.hasNameEndingWith("Dsl<${playerClasses.first().name}>") }

        it("Dsl inner class is sealed with private constructor") { dslClasses.shouldNotBeEmpty() }

        describe("Dsl subclasses are all data objects") {
          dslClasses.classesAndInterfaces().shouldBeEmpty()

          withData(nameFn = { it.name }, dslClasses.objects()) { dslObject ->
            dslObject.assertTrue { dsl -> dsl.hasDataModifier && dsl.hasParentWithName("Dsl") }
          }
        }
      }
    })
