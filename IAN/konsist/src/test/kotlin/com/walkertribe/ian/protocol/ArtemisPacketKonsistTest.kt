package com.walkertribe.ian.protocol

import com.lemonappdev.konsist.api.KoModifier
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.modifierprovider.withModifier
import com.lemonappdev.konsist.api.ext.list.modifierprovider.withoutModifier
import com.lemonappdev.konsist.api.ext.list.withAnnotation
import com.lemonappdev.konsist.api.ext.list.withAnnotationOf
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.ext.list.withParentOf
import com.lemonappdev.konsist.api.ext.list.withRepresentedTypeOf
import com.lemonappdev.konsist.api.ext.list.withTopLevel
import com.lemonappdev.konsist.api.ext.list.withoutName
import com.lemonappdev.konsist.api.ext.provider.hasAnnotationOf
import com.lemonappdev.konsist.api.ext.provider.representsTypeOf
import com.lemonappdev.konsist.api.verify.assertTrue
import com.walkertribe.ian.protocol.core.SimpleEventPacket
import com.walkertribe.ian.protocol.core.ValueIntPacket
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.string.shouldEndWith

class ArtemisPacketKonsistTest : DescribeSpec({
    describe("ArtemisPacket") {
        val module = Konsist.scopeFromModule("IAN")
        val classes = module.classes() + module.objects()
        val protocol = "com.walkertribe.ian.protocol.."
        val classNameRegex = Regex("\\.[A-Z].+")

        val abstractClasses = classes.withNameEndingWith("Packet")
            .withModifier(KoModifier.ABSTRACT, KoModifier.OPEN, KoModifier.SEALED)
            .withoutName("RawPacket")
        val abstractNames = abstractClasses.map { it.name }.toSet()

        val finalClasses =
            classes.withoutModifier(KoModifier.ABSTRACT, KoModifier.OPEN, KoModifier.SEALED)
        val packetClasses = module.classes().withTopLevel()
            .withParentOf(ArtemisPacket::class, indirectParents = true)

        describe("Final BaseArtemisPacket subclasses have @Packet annotation") {
            withData(
                nameFn = {
                    classNameRegex.find(it.fullyQualifiedName)?.value?.substring(1) ?: it.name
                },
                finalClasses.withParentOf(BaseArtemisPacket::class, indirectParents = true)
            ) { packetClass ->
                packetClass.assertTrue { it.hasAnnotationOf<Packet>() }
            }
        }

        describe("Top-level packet class names end with Packet") {
            withData(packetClasses.map { it.name }) { it shouldEndWith "Packet" }
        }

        describe("Top-level packet classes share name with containing file") {
            withData(nameFn = { it.name }, packetClasses) { packetClass ->
                packetClass.assertTrue { it.containingFile.name == it.name }
            }
        }

        describe("Packet classes with @Packet annotation extend BaseArtemisPacket") {
            withData(
                nameFn = {
                    classNameRegex.find(it.fullyQualifiedName)?.value?.substring(1) ?: it.name
                },
                classes.withAnnotationOf(Packet::class)
            ) { packetClass ->
                packetClass.assertTrue {
                    it.hasParent { parent -> abstractNames.contains(parent.name) }
                }
            }
        }

        it("@Packet annotation usages specify either type or hash, but not both") {
            classes.withAnnotationOf(Packet::class).flatMap {
                it.annotations.withRepresentedTypeOf(Packet::class)
            }.assertTrue {
                it.hasArgumentWithName("type") != it.hasArgumentWithName("hash")
            }
        }

        val packetsWithSubtypes = mapOf(
            "SIMPLE_EVENT" to SimpleEventPacket::class,
            "VALUE_INT" to ValueIntPacket::class,
        )

        packetsWithSubtypes.forEach { (packetType, parentClass) ->
            val parentClassName = parentClass.java.simpleName

            describe("@Packet on $parentClassName subclass has type $packetType and a subtype") {
                withData(
                    nameFn = {
                        classNameRegex.find(it.fullyQualifiedName)?.value?.substring(1) ?: it.name
                    },
                    finalClasses.withParentOf(parentClass, indirectParents = true)
                ) { packetClass ->
                    packetClass.assertTrue {
                        it.hasAnnotation { annotation ->
                            annotation.representsTypeOf<Packet>() && annotation.hasArgument { arg ->
                                arg.name == "type" && arg.value == "CorePacketType.$packetType"
                            } && annotation.hasArgument { arg ->
                                arg.name == "subtype" && arg.value != "null"
                            }
                        }
                    }
                }
            }

            describe("@Packet with type $packetType is descendant of $parentClassName") {
                withData(
                    nameFn = {
                        classNameRegex.find(it.fullyQualifiedName)?.value?.substring(1) ?: it.name
                    },
                    finalClasses.withAnnotation { annotation ->
                        annotation.representsTypeOf<Packet>() && annotation.hasArgument { arg ->
                            arg.name == "type" && arg.value == "CorePacketType.$packetType"
                        }
                    }
                ) { packetClass ->
                    packetClass.assertTrue { it.hasParentOf(parentClass, indirectParents = true) }
                }
            }
        }

        describe("@Packet with type other than SIMPLE_EVENT or VALUE_INT has no subtype") {
            val withSubtypes = packetsWithSubtypes.keys
            withData(
                nameFn = { annotation ->
                    annotation.arguments.first { it.name == "type" }.value.toString()
                },
                classes.flatMap { it.annotations }.withRepresentedTypeOf(Packet::class)
                    .filter { annotation ->
                        annotation.hasArgument { arg ->
                            arg.name == "type" && arg.value?.let { value ->
                                withSubtypes.any(value::endsWith)
                            } == false
                        }
                    }
            ) { annotation ->
                annotation.assertTrue { !it.hasArgumentWithName("subtype") }
            }
        }

        describe("Packet classes reside in package $protocol") {
            withData(
                nameFn = { it.name },
                (module.interfaces() + classes).withNameEndingWith("Packet"),
            ) { packetClass ->
                packetClass.assertTrue { it.resideInPackage(protocol) }
            }
        }

        it("HEADER constant defined as DEADBEEF") {
            module.objects()
                .withRepresentedTypeOf(ArtemisPacket.Companion::class)
                .assertTrue {
                    it.hasProperty { header ->
                        header.name == "HEADER" && header.text.contains("deadbeef", true)
                    }
                }
        }
    }
})
