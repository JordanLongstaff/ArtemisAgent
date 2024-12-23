package com.walkertribe.ian.world

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.WithDataTestName
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.floats.shouldBeWithinPercentageOf
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.triple
import io.kotest.property.checkAll
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class PositionTest : DescribeSpec({
    val obj1 = ArtemisMine(0, 0L)
    val obj2 = ArtemisMine(1, 1L)

    val coordinateBounds = 100000f
    val arbCoordinate = Arb.numericFloat(min = 0f, max = coordinateBounds)
    val arbYCoordinate = Arb.numericFloat(min = 0f, max = coordinateBounds * 2f)

    describe("Object has position") {
        it("If X-coordinate is missing: false") {
            obj1.hasPosition.shouldBeFalse()
            obj2.hasPosition.shouldBeFalse()
        }

        it("If Z-coordinate is missing: false") {
            obj1.x.value = 0f
            obj2.x.value = 0f

            obj1.hasPosition.shouldBeFalse()
            obj2.hasPosition.shouldBeFalse()
        }

        it("If both are present: true") {
            obj1.z.value = 0f
            obj2.z.value = 0f

            obj1.hasPosition.shouldBeTrue()
            obj2.hasPosition.shouldBeTrue()
        }
    }

    describe("Distance") {
        obj1.x.value = 0f
        obj1.y.value = 0f
        obj1.z.value = 0f

        it("Computes with Pythagorean quadruples") {
            for (xDiff in 3 until 30 step 2) {
                val xDiffSquared = xDiff * xDiff
                val zDiff = xDiffSquared / 2
                val horizontalDiffSquared = (zDiff + 1) * (zDiff + 1)
                val yDiff = horizontalDiffSquared / 2
                val expectedDistance = yDiff + 1

                obj2.x.value = xDiff.toFloat()
                obj2.y.value = yDiff.toFloat()
                obj2.z.value = zDiff.toFloat()

                obj1.distanceTo(obj2).roundToInt() shouldBeEqual expectedDistance
                obj2.distanceTo(obj1).roundToInt() shouldBeEqual expectedDistance
            }

            for (xDiff in 1..315) {
                val zDiff = xDiff + 1
                val yDiff = xDiff * zDiff
                val expectedDistance = yDiff + 1

                obj2.x.value = xDiff.toFloat()
                obj2.y.value = yDiff.toFloat()
                obj2.z.value = zDiff.toFloat()

                obj1.distanceTo(obj2).roundToInt() shouldBeEqual expectedDistance
                obj2.distanceTo(obj1).roundToInt() shouldBeEqual expectedDistance
            }

            for (xDiff in 4 until 447 step 2) {
                val yDiff = xDiff * xDiff / 2 - 1
                val expectedDistance = yDiff + 2

                obj2.x.value = xDiff.toFloat()
                obj2.y.value = yDiff.toFloat()
                obj2.z.value = xDiff.toFloat()

                obj1.distanceTo(obj2).roundToInt() shouldBeEqual expectedDistance
                obj2.distanceTo(obj1).roundToInt() shouldBeEqual expectedDistance
            }
        }

        it("Computes with arbitrary coordinates") {
            val sqrt3 = sqrt(3f)

            checkAll(arbCoordinate, arbYCoordinate, arbCoordinate) { xDiff, yDiff, zDiff ->
                obj2.x.value = xDiff
                obj2.y.value = yDiff
                obj2.z.value = zDiff

                val distance1 = obj1 distanceTo obj2
                val distance2 = obj2 distanceTo obj1

                val diffs = arrayOf(xDiff, yDiff, zDiff)
                val maxDiff = diffs.max()
                val maxDiffBound = min(maxDiff * sqrt3, diffs.sum())

                distance1.shouldBeWithinPercentageOf(distance2, EPSILON)
                distance1 shouldBeIn maxDiff..maxDiffBound
            }
        }

        describe("Optimized with zeroes") {
            withData(
                nameFn = { "Returns ${it.first} coordinate" },
                "X" to Arb.triple(
                    arbCoordinate,
                    Arb.of(0f),
                    Arb.of(0f),
                ),
                "Y" to Arb.triple(
                    Arb.of(0f),
                    arbYCoordinate,
                    Arb.of(0f),
                ),
                "Z" to Arb.triple(
                    Arb.of(0f),
                    Arb.of(0f),
                    arbCoordinate,
                ),
            ) { (_, arbCoordinates) ->
                arbCoordinates.checkAll { (xDiff, yDiff, zDiff) ->
                    obj2.x.value = xDiff
                    obj2.y.value = yDiff
                    obj2.z.value = zDiff

                    val distance1 = obj1 distanceTo obj2
                    val distance2 = obj2 distanceTo obj1

                    val expectedDiff = abs(xDiff) + abs(yDiff) + abs(zDiff)

                    distance1.shouldBeWithinPercentageOf(expectedDiff, EPSILON)
                    distance2.shouldBeWithinPercentageOf(expectedDiff, EPSILON)
                }
            }
        }

        it("Computes distance squared") {
            checkAll(arbCoordinate, arbYCoordinate, arbCoordinate) { xDiff, yDiff, zDiff ->
                obj2.x.value = xDiff
                obj2.y.value = yDiff
                obj2.z.value = zDiff

                val distance1 = obj1 distanceTo obj2
                val distance2 = obj2 distanceTo obj1

                val distSquared1 = obj1 distanceSquaredTo obj2
                val distSquared2 = obj2 distanceSquaredTo obj1

                distSquared1.shouldBeWithinPercentageOf(distance1 * distance1, sqrt(EPSILON))
                distSquared2.shouldBeWithinPercentageOf(distance2 * distance2, sqrt(EPSILON))
            }
        }

        it("Fails when either object lacks a coordinate") {
            obj1.x.value = Float.NaN
            shouldThrow<IllegalStateException> { obj1 distanceTo obj2 }
            shouldThrow<IllegalStateException> { obj2 distanceTo obj1 }
            shouldThrow<IllegalStateException> { obj1 distanceSquaredTo obj2 }
            shouldThrow<IllegalStateException> { obj2 distanceSquaredTo obj1 }

            obj2.x.value = Float.NaN
            shouldThrow<IllegalStateException> { obj1 distanceTo obj2 }
            shouldThrow<IllegalStateException> { obj1 distanceSquaredTo obj2 }
        }
    }

    describe("Horizontal distance") {
        obj1.x.value = 0f
        obj1.z.value = 0f

        it("Computes with Pythagorean triples") {
            for (xDiff in 3 until 450 step 2) {
                val xDiffSquared = xDiff * xDiff
                val zDiff = xDiffSquared / 2
                val expectedDistance = zDiff + 1

                obj2.x.value = xDiff.toFloat()
                obj2.z.value = zDiff.toFloat()

                obj1.horizontalDistanceTo(obj2).roundToInt() shouldBeEqual expectedDistance
                obj2.horizontalDistanceTo(obj1).roundToInt() shouldBeEqual expectedDistance
            }

            Arb.int(2..25000).checkAll { seed ->
                val xDiff = seed * 3
                val zDiff = seed * 4
                val expectedDistance = seed * 5

                obj2.x.value = xDiff.toFloat()
                obj2.z.value = zDiff.toFloat()

                obj1.horizontalDistanceTo(obj2).roundToInt() shouldBeEqual expectedDistance
                obj2.horizontalDistanceTo(obj1).roundToInt() shouldBeEqual expectedDistance
            }
        }

        it("Computes with arbitrary coordinates") {
            val sqrt2 = sqrt(2f)

            checkAll(arbCoordinate, arbCoordinate) { xDiff, zDiff ->
                obj2.x.value = xDiff
                obj2.z.value = zDiff

                val distance1 = obj1 horizontalDistanceTo obj2
                val distance2 = obj2 horizontalDistanceTo obj1

                val diffs = arrayOf(xDiff, zDiff)
                val maxDiff = diffs.max()
                val maxDiffBound = min(maxDiff * sqrt2, diffs.sum())

                distance1.shouldBeWithinPercentageOf(distance2, EPSILON)
                distance1 shouldBeIn maxDiff..maxDiffBound
            }
        }

        describe("Optimized with zeroes") {
            withData(
                nameFn = { "Returns ${it.first} coordinate" },
                Triple("X", arbCoordinate, Arb.of(0f)),
                Triple("Z", Arb.of(0f), arbCoordinate),
            ) { (_, arbX, arbZ) ->
                checkAll(arbX, arbZ) { xDiff, zDiff ->
                    obj2.x.value = xDiff
                    obj2.z.value = zDiff

                    val distance1 = obj1 horizontalDistanceTo obj2
                    val distance2 = obj2 horizontalDistanceTo obj1

                    val expectedDiff = abs(xDiff) + abs(zDiff)

                    distance1.shouldBeWithinPercentageOf(expectedDiff, EPSILON)
                    distance2.shouldBeWithinPercentageOf(expectedDiff, EPSILON)
                }
            }
        }

        it("Computes distance squared") {
            checkAll(arbCoordinate, arbCoordinate) { xDiff, zDiff ->
                obj2.x.value = xDiff
                obj2.z.value = zDiff

                val distance1 = obj1 horizontalDistanceTo obj2
                val distance2 = obj2 horizontalDistanceTo obj1

                val distSquared1 = obj1 horizontalDistanceSquaredTo obj2
                val distSquared2 = obj2 horizontalDistanceSquaredTo obj1

                distSquared1.shouldBeWithinPercentageOf(distance1 * distance1, sqrt(EPSILON))
                distSquared2.shouldBeWithinPercentageOf(distance2 * distance2, sqrt(EPSILON))
            }
        }

        it("Fails when either object lacks a coordinate") {
            obj1.x.value = Float.NaN
            shouldThrow<IllegalStateException> { obj1 horizontalDistanceTo obj2 }
            shouldThrow<IllegalStateException> { obj2 horizontalDistanceTo obj1 }
            shouldThrow<IllegalStateException> { obj1 horizontalDistanceSquaredTo obj2 }
            shouldThrow<IllegalStateException> { obj2 horizontalDistanceSquaredTo obj1 }

            obj2.x.value = Float.NaN
            shouldThrow<IllegalStateException> { obj1 horizontalDistanceTo obj2 }
            shouldThrow<IllegalStateException> { obj1 horizontalDistanceSquaredTo obj2 }
        }
    }

    describe("Headings") {
        obj1.x.value = 0f
        obj1.z.value = 0f

        withData(HeadingTestCase.ALL) {
            obj2.x.value = it.xDiff
            obj2.z.value = it.zDiff

            val headingToInt = (obj1.headingTo(obj2) * 2f).roundToInt()
            val expectedHeadingToInt = (it.expectedHeading * 2f).roundToInt()
            headingToInt shouldBeEqual expectedHeadingToInt

            val expectedReverseHeading = expectedHeadingToInt.let { heading ->
                heading + 360 * if (heading >= 360) -1 else 1
            }
            val reverseHeadingToInt = (obj2.headingTo(obj1) * 2f).roundToInt()
            reverseHeadingToInt shouldBeEqual expectedReverseHeading
        }

        it("Fails when either object lacks a coordinate") {
            obj1.x.value = Float.NaN
            shouldThrow<IllegalStateException> { obj1 headingTo obj2 }
            shouldThrow<IllegalStateException> { obj2 headingTo obj1 }

            obj2.x.value = Float.NaN
            shouldThrow<IllegalStateException> { obj1 headingTo obj2 }
        }
    }
})

data class HeadingTestCase(
    val expectedHeading: Float,
    val xDiff: Float,
    val zDiff: Float,
) : WithDataTestName, Comparable<HeadingTestCase> {
    fun rotate(): HeadingTestCase = HeadingTestCase(ROTATE_AMOUNT + expectedHeading, zDiff, -xDiff)
    fun reflect(): HeadingTestCase = HeadingTestCase(-expectedHeading, -xDiff, zDiff)

    override fun dataTestName(): String = "$expectedHeading degrees"

    override fun compareTo(other: HeadingTestCase): Int =
        expectedHeading.compareTo(other.expectedHeading)

    companion object {
        private const val ROTATE_AMOUNT = 90f
        private val SQRT_2 = sqrt(2f)
        private val SQRT_3 = sqrt(3f)

        private val ZERO =
            HeadingTestCase(0f, -0f, -1f)
        private val SEVEN_POINT_FIVE =
            HeadingTestCase(7.5f, -(SQRT_2 - 1f) * (SQRT_3 - SQRT_2), -1f)
        private val FIFTEEN =
            HeadingTestCase(15f, SQRT_3 - 2f, -1f)
        private val TWENTY_TWO_POINT_FIVE =
            HeadingTestCase(22.5f, 1f - SQRT_2, -1f)
        private val THIRTY =
            HeadingTestCase(30f, -SQRT_3 / 3f, -1f)
        private val THIRTY_SEVEN_POINT_FIVE =
            HeadingTestCase(37.5f, -(SQRT_2 + 1f) * (SQRT_3 - SQRT_2), -1f)
        private val FORTY_FIVE =
            HeadingTestCase(45f, -1f, -1f)

        private val SIMPLE = listOf(ZERO, FORTY_FIVE)
        private val INTERMEDIATE = listOf(
            SEVEN_POINT_FIVE,
            FIFTEEN,
            TWENTY_TWO_POINT_FIVE,
            THIRTY,
            THIRTY_SEVEN_POINT_FIVE,
        ).flatMap { listOf(it, it.reflect().rotate()) }

        val ALL = (SIMPLE + INTERMEDIATE).flatMap {
            val rotations = mutableListOf(it)
            repeat(3) { rotations.add(rotations.last().rotate()) }
            rotations
        }.toSortedSet()
    }
}
