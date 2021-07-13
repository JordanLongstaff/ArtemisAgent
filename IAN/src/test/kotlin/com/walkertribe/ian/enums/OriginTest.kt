package com.walkertribe.ian.enums

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

class OriginTest : DescribeSpec({
    describe("Origin") {
        data class FromConnectionTypeTestCase(val connectionType: Int, val expectedOrigin: Origin)

        val allTestCases = listOf(
            FromConnectionTypeTestCase(1, Origin.SERVER),
            FromConnectionTypeTestCase(2, Origin.CLIENT),
        )
        allTestCases.map { it.expectedOrigin } shouldContainExactlyInAnyOrder Origin.entries

        withData(
            nameFn = { "Connection type ${it.connectionType}: ${it.expectedOrigin}" },
            allTestCases.toList()
        ) {
            val actualOrigin = Origin[it.connectionType]
            actualOrigin.shouldNotBeNull()
            actualOrigin shouldBeEqual it.expectedOrigin
        }

        it("Invalid connection type") {
            Arb.int().filter { it !in 1..2 }.checkAll {
                collect(if (it > 2) "POSITIVE" else "NON-POSITIVE")
                Origin[it].shouldBeNull()
            }
        }
    }
})
