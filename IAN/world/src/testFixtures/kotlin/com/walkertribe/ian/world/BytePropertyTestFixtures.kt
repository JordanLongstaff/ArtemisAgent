package com.walkertribe.ian.world

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual

fun Property.ByteProperty.shouldBeUnspecified(unknownValue: Byte = -1) {
    hasValue.shouldBeFalse()
    value shouldBeEqual unknownValue
}

fun Property.ByteProperty.shouldBeSpecified(unknownValue: Byte = -1) {
    hasValue.shouldBeTrue()
    value shouldNotBeEqual unknownValue
}

infix fun Property.ByteProperty.shouldContainValue(expectedValue: Byte) {
    hasValue.shouldBeTrue()
    value shouldBeEqual expectedValue
}

infix fun Property.ByteProperty.shouldMatch(property: Property.ByteProperty) {
    hasValue shouldBeEqual property.hasValue
    shouldContainValue(property.value)
}
