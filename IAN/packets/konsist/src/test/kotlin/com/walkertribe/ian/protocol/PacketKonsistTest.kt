package com.walkertribe.ian.protocol

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withAnnotationOf
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.ext.list.withParent
import com.lemonappdev.konsist.api.ext.list.withParentNamed
import com.lemonappdev.konsist.api.ext.list.withParentOf
import com.lemonappdev.konsist.api.ext.list.withRepresentedTypeOf
import com.lemonappdev.konsist.api.ext.list.withTopLevel
import com.lemonappdev.konsist.api.ext.provider.hasAnnotationOf
import com.lemonappdev.konsist.api.ext.provider.hasParentOf
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import com.walkertribe.ian.protocol.core.SimpleEventPacket
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.string.shouldEndWith

class PacketKonsistTest : DescribeSpec({
    describe("Packet") {
        val module = Konsist.scopeFromProject("IAN/packets", "main")
        val classes = module.classes() + module.objects()
        val protocol = "com.walkertribe.ian.protocol.."
        val classNameRegex = Regex("\\.[A-Z].+")
        val parentNameRegex = Regex("Packet\\..+")

        val packetClasses = module.classes().withTopLevel().withParent(indirectParents = true) {
            it.hasNameMatching(parentNameRegex)
        }

        describe("Top-level packet class names end with Packet") {
            withData(packetClasses.map { it.name }) { it shouldEndWith "Packet" }
        }

        describe("Top-level packet classes share name with containing file") {
            withData(nameFn = { it.name }, packetClasses) { packetClass ->
                packetClass.assertTrue { it.containingFile.name == it.name }
            }
        }

        describe("Packet.Server subclasses have @PacketType annotation") {
            withData(
                nameFn = {
                    classNameRegex.find(it.fullyQualifiedName)?.value?.substring(1) ?: it.name
                },
                classes.withParentNamed("Packet.Server")
            ) { packetClass ->
                packetClass.assertTrue { it.hasAnnotationOf<PacketType>() }
            }
        }

        describe("Packet classes with @PacketType annotation extend Packet.Server") {
            withData(
                nameFn = {
                    classNameRegex.find(it.fullyQualifiedName)?.value?.substring(1) ?: it.name
                },
                classes.withAnnotationOf(PacketType::class)
            ) { packetClass ->
                packetClass.assertTrue { it.hasParentOf<Packet.Server>() }
            }
        }

        describe("SimpleEventPacket subclasses have @PacketSubtype annotation") {
            withData(
                nameFn = {
                    classNameRegex.find(it.fullyQualifiedName)?.value?.substring(1) ?: it.name
                },
                packetClasses.withParentOf(SimpleEventPacket::class)
            ) { packetClass ->
                packetClass.assertTrue { it.hasAnnotationOf<PacketSubtype>() }
            }
        }

        describe("Packet classes with @PacketSubtype annotation extend SimpleEventPacket") {
            withData(
                nameFn = {
                    classNameRegex.find(it.fullyQualifiedName)?.value?.substring(1) ?: it.name
                },
                classes.withAnnotationOf(PacketSubtype::class),
            ) { packetClass ->
                packetClass.assertTrue {
                    it.hasParentOf(SimpleEventPacket::class, indirectParents = true)
                }
            }
        }

        describe("Client packets classes have neither @PacketType nor @PacketSubtype") {
            withData(
                nameFn = {
                    classNameRegex.find(it.fullyQualifiedName)?.value?.substring(1) ?: it.name
                },
                classes.withParentNamed("Packet.Client", indirectParents = true),
            ) { packetClass ->
                packetClass.assertFalse {
                    it.hasAnnotationOf(PacketType::class, PacketSubtype::class)
                }
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
                .withRepresentedTypeOf(Packet.Companion::class)
                .assertTrue {
                    it.hasProperty { header ->
                        header.name == "HEADER" && header.text.contains("deadbeef", true)
                    }
                }
        }
    }
})
