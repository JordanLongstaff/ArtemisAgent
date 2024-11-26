package com.walkertribe.ian.enums

import com.lemonappdev.konsist.api.KoModifier
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.ext.list.modifierprovider.withModifier
import com.lemonappdev.konsist.api.ext.list.modifierprovider.withoutModifier
import com.lemonappdev.konsist.api.ext.list.withParentInterface
import com.lemonappdev.konsist.api.ext.list.withTopLevel
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData

class EnumsKonsistTest : DescribeSpec({
    val enums = "com.walkertribe.ian.enums"
    val enumsScope = Konsist.scopeFromPackage(enums, "IAN/enums", "main")
    val classes = enumsScope.classes().withTopLevel()
    val enumClasses = classes.withModifier(KoModifier.ENUM)
    val sealedClasses = classes.withModifier(KoModifier.SEALED)
    val valueClasses = classes.withModifier(KoModifier.VALUE)
    val interfaces = enumsScope.interfaces().withTopLevel()

    val files = enumsScope.files

    describe("All files in enums package contain only one top-level member declaration") {
        withData(nameFn = { it.name }, files) { file ->
            file.assertTrue {
                val members = it.classes() + it.interfaces() + it.objects()
                members.withTopLevel().size == 1
            }
        }
    }

    describe("All members of enums package are enums, interfaces, sealed or value classes") {
        arrayOf(
            "Enum classes" to enumClasses,
            "Interfaces" to interfaces,
            "Sealed classes" to sealedClasses,
            "Value classes" to valueClasses,
        ).forEach { (specName, classSet) ->
            describe(specName) {
                withData(nameFn = { it.name }, classSet) { cls ->
                    cls.assertTrue {
                        when (it) {
                            is KoInterfaceDeclaration -> true
                            is KoClassDeclaration ->
                                it.hasEnumModifier || it.hasSealedModifier || it.hasValueModifier
                            else -> false
                        }
                    }
                }
            }
        }

        describe("Nothing else") {
            val otherClasses = enumsScope.classes().withoutModifier(
                KoModifier.ENUM,
                KoModifier.SEALED,
                KoModifier.VALUE,
            ).withTopLevel()
            val objects = enumsScope.objects(includeNested = false).withTopLevel()
            arrayOf(otherClasses, objects).forEach { set ->
                withData(nameFn = { it.name }, set) {
                    it.assertTrue { false }
                }
            }
        }
    }

    describe("All members of enums package share name with containing file") {
        withData(
            nameFn = { it.name },
            enumClasses + sealedClasses + valueClasses + interfaces,
        ) { mem ->
            mem.assertTrue { it.containingFile.name == it.name }
        }
    }

    describe("All inheritors of interfaces are in enums package") {
        withData(nameFn = { it.name }, interfaces) { int ->
            val members = enumsScope.classes() + enumsScope.interfaces() + enumsScope.objects()
            val inheritors = members.withParentInterface {
                it.fullyQualifiedName == "$enums.${int.name}"
            }
            withData(nameFn = { it.name }, inheritors) { cls ->
                cls.assertTrue { it.resideInPackage(enums) }
            }
        }
    }
})
