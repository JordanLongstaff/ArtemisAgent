package com.walkertribe.ian.world

import com.walkertribe.ian.util.BoolState
import com.walkertribe.ian.util.isKnown
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual

fun Property.BoolProperty.shouldBeUnspecified() {
    hasValue.shouldBeFalse()
    value.isKnown.shouldBeFalse()
    value.booleanValue.shouldBeFalse()
}

fun Property.BoolProperty.shouldBeSpecified() {
    hasValue.shouldBeTrue()
    value.isKnown.shouldBeTrue()
}

fun Property.BoolProperty.shouldBeTrue() {
    shouldContainValue(BoolState.True)
}

fun Property.BoolProperty.shouldBeFalse() {
    shouldContainValue(BoolState.False)
}

infix fun Property.BoolProperty.shouldContainValue(expectedValue: BoolState) {
    hasValue shouldBeEqual expectedValue.isKnown
    shouldContainValue(expectedValue.booleanValue)
    value shouldBeEqual expectedValue
}

infix fun Property.BoolProperty.shouldContainValue(expectedValue: Boolean) {
    shouldBeSpecified()
    value.booleanValue shouldBeEqual expectedValue
}

infix fun Property.BoolProperty.shouldMatch(property: Property.BoolProperty) {
    hasValue shouldBeEqual property.hasValue
    shouldContainValue(property.value)
}
