package com.walkertribe.ian.world

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual

fun Property.IntProperty.shouldBeUnspecified(unknownValue: Int = -1) {
    hasValue.shouldBeFalse()
    value shouldBeEqual unknownValue
}

fun Property.IntProperty.shouldBeSpecified(unknownValue: Int = -1) {
    hasValue.shouldBeTrue()
    value shouldNotBeEqual unknownValue
}

infix fun Property.IntProperty.shouldContainValue(expectedValue: Int) {
    hasValue.shouldBeTrue()
    value shouldBeEqual expectedValue
}

infix fun Property.IntProperty.shouldMatch(property: Property.IntProperty) {
    hasValue shouldBeEqual property.hasValue
    shouldContainValue(property.value)
}
