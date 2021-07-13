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

class ObjectTypeTest : DescribeSpec({
    withData(
        nameFn = { "Object type #${it.id}: ${it.name}" },
        ObjectType.entries.toList()
    ) {
        val objectTypeResult = ObjectType[it.id.toInt()]
        objectTypeResult.shouldNotBeNull()
        objectTypeResult shouldBeEqual it
    }

    it("Object type 0 returns null") {
        ObjectType[0].shouldBeNull()
    }

    val highestObjectID = ObjectType.entries.maxOf { it.id.toInt() }
    describe("Invalid object type throws") {
        withData<Pair<String, Gen<Int>>>(
            nameFn = { it.first },
            "9" to Exhaustive.of(9),
            "Negative number" to Arb.negativeInt(),
            "Too high" to Arb.int(min = highestObjectID + 1),
        ) { (_, testGen) ->
            testGen.checkAll {
                shouldThrow<IllegalArgumentException> { ObjectType[it] }
            }
        }
    }

    describe("Dsl") {
        val typesWithDsl = listOf(
            ObjectType.PLAYER_SHIP,
            ObjectType.WEAPONS_CONSOLE,
            ObjectType.UPGRADES,
            ObjectType.NPC_SHIP,
            ObjectType.BASE,
            ObjectType.BLACK_HOLE,
            ObjectType.CREATURE,
            ObjectType.MINE,
        )

        val typesWithoutDsl = listOf(
            ObjectType.ENGINEERING_CONSOLE,
            ObjectType.ANOMALY,
            ObjectType.NEBULA,
            ObjectType.TORPEDO,
            ObjectType.ASTEROID,
            ObjectType.GENERIC_MESH,
            ObjectType.DRONE,
        )

        typesWithDsl.size + typesWithoutDsl.size shouldBeEqual ObjectType.entries.size

        describe("Exists for") {
            withData(typesWithDsl) { it.dsl.shouldNotBeNull() }
        }

        describe("Does not exist for") {
            withData(typesWithoutDsl) { it.dsl.shouldBeNull() }
        }
    }
})
