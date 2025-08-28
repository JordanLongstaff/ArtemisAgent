package com.walkertribe.ian.protocol.udp

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoEnumConstantDeclaration
import com.lemonappdev.konsist.api.declaration.KoObjectDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
import com.lemonappdev.konsist.api.ext.list.enumConstants
import com.lemonappdev.konsist.api.ext.list.objects
import com.lemonappdev.konsist.api.ext.list.withRepresentedTypeOf
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import withCompanionModifier

class PrivateNetworkTypeKonsistTest :
    DescribeSpec({
        describe("PrivateNetworkType") {
            val matchingClasses =
                Konsist.scopeFromModule("IAN/udp")
                    .classes()
                    .withRepresentedTypeOf(PrivateNetworkType::class)
            val types = matchingClasses.enumConstants
            val companionObjects = matchingClasses.objects().withCompanionModifier()

            types.forEachIndexed { index, type ->
                describe(type.name) {
                    withData(nameFn = { it.testName }, NamingConventionTest.entries) { convention ->
                        convention.test(type)
                    }

                    it("Has comment explaining accepted addresses") {
                        type.assertTrue { it.text.contains(Regex("// \\d+\\.(x|\\d+)\\.x\\.x")) }
                    }

                    withData(nameFn = { it.testName(type) }, CompanionTest.entries) { companionTest
                        ->
                        companionTest.test(companionObjects, type, index)
                    }
                }
            }
        }
    })

private enum class NamingConventionTest(val testName: String) {
    CONSTANT_NAME("Name ends in BIT_BLOCK") {
        override fun test(type: KoEnumConstantDeclaration) {
            type.assertTrue { it.hasNameEndingWith("BIT_BLOCK") }
        }
    },
    BROADCAST_ADDRESS("Broadcast address property follows naming patterns") {
        override fun test(type: KoEnumConstantDeclaration) {
            val prefix = type.name.substringBeforeLast("BLOCK")
            type.assertTrue {
                it.hasVariable { prop ->
                    prop.name == "broadcastAddress" && prop.text.contains("${prefix}BROADCAST")
                }
            }
        }
    },
    CONSTRAINTS("Constraints property follows naming patterns") {
        override fun test(type: KoEnumConstantDeclaration) {
            val prefix = type.name.substringBeforeLast("BLOCK")
            type.assertTrue {
                it.hasVariable { prop ->
                    prop.name == "constraints" && prop.text.contains("${prefix}CONSTRAINTS")
                }
            }
        }
    };

    abstract fun test(type: KoEnumConstantDeclaration)
}

private enum class CompanionTest {
    BROADCAST {
        override fun testProperty(property: KoPropertyDeclaration, index: Int): Boolean =
            property.hasConstModifier && property.hasValue(EXPECTED_BROADCAST[index])
    },
    CONSTRAINTS {
        override fun testProperty(property: KoPropertyDeclaration, index: Int): Boolean = true
    };

    fun test(objects: List<KoObjectDeclaration>, type: KoEnumConstantDeclaration, index: Int) {
        objects.assertTrue { companion ->
            companion.hasProperty { prop ->
                prop.name == testName(type) && testProperty(prop, index)
            }
        }
    }

    fun testName(type: KoEnumConstantDeclaration): String =
        "${type.name.substringBeforeLast("BLOCK")}$name"

    abstract fun testProperty(property: KoPropertyDeclaration, index: Int): Boolean

    private companion object {
        val EXPECTED_BROADCAST = arrayOf("10.255.255.255", "172.31.255.255", "192.168.255.255")
    }
}
