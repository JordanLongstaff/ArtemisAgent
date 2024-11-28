package com.walkertribe.ian.protocol.udp

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoEnumConstantDeclaration
import com.lemonappdev.konsist.api.ext.list.enumConstants
import com.lemonappdev.konsist.api.ext.list.withRepresentedTypeOf
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData

class PrivateNetworkTypeKonsistTest : DescribeSpec({
    describe("PrivateNetworkType") {
        val types = Konsist.scopeFromModule("IAN/udp")
            .classes()
            .withRepresentedTypeOf(PrivateNetworkType::class)
            .enumConstants

        withData(nameFn = { it.name }, types) { type ->
            withData(nameFn = { it.testName }, NamingConventionTest.entries) { convention ->
                convention.test(type)
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
