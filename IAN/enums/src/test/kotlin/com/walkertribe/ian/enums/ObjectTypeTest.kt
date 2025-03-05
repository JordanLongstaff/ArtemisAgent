package com.walkertribe.ian.enums

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.negativeInt
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.of

class ObjectTypeTest :
    DescribeSpec({
        val expectedIDs = (1..16).toSortedSet() - setOf(9)

        withData(
            nameFn = { "Object type #${it.second}: ${it.first}" },
            ObjectType.entries.zip(expectedIDs),
        ) { (objectType, id) ->
            objectType.id shouldBeEqual id.toByte()
            ObjectType[id].shouldNotBeNull() shouldBeEqual objectType
        }

        it("Object type 0 returns null") { ObjectType[0].shouldBeNull() }

        val highestObjectID = ObjectType.entries.maxOf { it.id.toInt() }
        describe("Invalid object type throws") {
            withData<Pair<String, Gen<Int>>>(
                nameFn = { it.first },
                "9" to Exhaustive.of(9),
                "Negative number" to Arb.negativeInt(),
                "Too high" to Arb.int(min = highestObjectID + 1),
            ) { (_, testGen) ->
                testGen.checkAll { shouldThrow<IllegalArgumentException> { ObjectType[it] } }
            }
        }
    })
