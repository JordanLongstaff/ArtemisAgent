package com.walkertribe.ian.world

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.floats.shouldBeNaN
import io.kotest.matchers.floats.shouldBeWithinPercentageOf
import io.kotest.matchers.floats.shouldBeZero
import io.kotest.matchers.floats.shouldNotBeNaN

const val EPSILON = 0.00000001

fun Property.FloatProperty.shouldBeUnspecified() {
    hasValue.shouldBeFalse()
    value.shouldBeNaN()
    valueOrZero.shouldBeZero()
}

fun Property.FloatProperty.shouldBeSpecified() {
    hasValue.shouldBeTrue()
    value.shouldNotBeNaN()
}

infix fun Property.FloatProperty.shouldContainValue(expectedValue: Float) {
    shouldBeSpecified()
    value.shouldBeWithinPercentageOf(expectedValue, EPSILON)
}

infix fun Property.FloatProperty.shouldMatch(property: Property.FloatProperty) {
    hasValue shouldBeEqual property.hasValue
    shouldContainValue(property.value)
}
