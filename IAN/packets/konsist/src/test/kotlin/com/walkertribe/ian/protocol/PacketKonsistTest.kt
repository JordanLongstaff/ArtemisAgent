package com.walkertribe.ian.protocol

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withAnnotationOf
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.ext.list.withParentOf
import com.lemonappdev.konsist.api.ext.list.withRepresentedTypeOf
import com.lemonappdev.konsist.api.ext.list.withTopLevel
import com.lemonappdev.konsist.api.ext.provider.hasAnnotationOf
import com.lemonappdev.konsist.api.ext.provider.hasParentOf
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import com.walkertribe.ian.protocol.core.HeartbeatPacket
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

        val packetClasses = module.classes()
            .withTopLevel()
            .withParentOf(Packet::class, indirectParents = true) +
                module.interfaces().withRepresentedTypeOf(HeartbeatPacket::class)

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
                    it.fullyQualifiedName?.let(classNameRegex::find)?.value?.substring(1) ?: it.name
                },
                classes.withParentOf(Packet.Server::class),
            ) { packetClass ->
                packetClass.assertTrue { it.hasAnnotationOf<PacketType>() }
            }
        }

        describe("Packet classes with @PacketType annotation extend Packet.Server") {
            withData(
                nameFn = {
                    it.fullyQualifiedName?.let(classNameRegex::find)?.value?.substring(1) ?: it.name
                },
                classes.withAnnotationOf(PacketType::class),
            ) { packetClass ->
                packetClass.assertTrue { it.hasParentOf<Packet.Server>() }
            }
        }

        describe("@PacketType annotations use type from CorePacketType object") {
            withData(
                nameFn = {
                    it.fullyQualifiedName?.let(classNameRegex::find)?.value?.substring(1) ?: it.name
                },
                classes.withAnnotationOf(PacketType::class),
            ) { packetClass ->
                packetClass.annotations.withRepresentedTypeOf(PacketType::class).assertTrue {
                    it.hasAllArguments { arg ->
                        when {
                            arg.name != "type" -> false
                            arg.value.toString().startsWith("CorePacketType.") -> true
                            else -> {
                                it.containingFile.hasImport { imp ->
                                    imp.name.endsWith("CorePacketType.${arg.value}")
                                }
                            }
                        }
                    }
                }
            }
        }

        describe("SimpleEventPacket subclasses have @PacketSubtype annotation") {
            withData(
                nameFn = {
                    it.fullyQualifiedName?.let(classNameRegex::find)?.value?.substring(1) ?: it.name
                },
                packetClasses.withParentOf(SimpleEventPacket::class),
            ) { packetClass ->
                packetClass.assertTrue { it.hasAnnotationOf<PacketSubtype>() }
            }
        }

        describe("Packet classes with @PacketSubtype annotation extend SimpleEventPacket") {
            withData(
                nameFn = {
                    it.fullyQualifiedName?.let(classNameRegex::find)?.value?.substring(1) ?: it.name
                },
                classes.withAnnotationOf(PacketSubtype::class),
            ) { packetClass ->
                packetClass.assertTrue {
                    it.hasParentOf(SimpleEventPacket::class, indirectParents = true)
                }
            }
        }

        describe("@PacketSubtype annotations use subtype from SimpleEventPacket.Subtype object") {
            withData(
                nameFn = {
                    it.fullyQualifiedName?.let(classNameRegex::find)?.value?.substring(1) ?: it.name
                },
                classes.withAnnotationOf(PacketSubtype::class),
            ) { packetClass ->
                packetClass.annotations.withRepresentedTypeOf(PacketSubtype::class).assertTrue {
                    it.hasAllArguments { arg ->
                        when {
                            arg.name != "subtype" -> false
                            arg.value.toString().startsWith("SimpleEventPacket.Subtype.") -> true
                            arg.value.toString().startsWith("Subtype.") -> {
                                it.containingFile.hasImport { imp ->
                                    imp.name.endsWith("SimpleEventPacket.Subtype")
                                }
                            }
                            it.containingFile.hasImport { imp ->
                                imp.name.endsWith("SimpleEventPacket.Subtype.${arg.value}")
                            } -> true
                            else -> false
                        }
                    }
                }
            }
        }

        describe("Client packet classes have neither @PacketType nor @PacketSubtype") {
            withData(
                nameFn = {
                    it.fullyQualifiedName?.let(classNameRegex::find)?.value?.substring(1) ?: it.name
                },
                classes.withParentOf(Packet.Client::class, indirectParents = true),
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
