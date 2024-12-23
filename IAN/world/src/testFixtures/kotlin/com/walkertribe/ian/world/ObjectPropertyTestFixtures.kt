package com.walkertribe.ian.world

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

fun Property.ObjectProperty<*>.shouldBeUnspecified() {
    hasValue.shouldBeFalse()
    value.shouldBeNull()
}

fun Property.ObjectProperty<*>.shouldBeSpecified() {
    hasValue.shouldBeTrue()
    value.shouldNotBeNull()
}

infix fun <V : Any> Property.ObjectProperty<V>.shouldContainValue(expectedValue: V) {
    hasValue.shouldBeTrue()

    val value = this.value
    value.shouldNotBeNull()
    value shouldBeEqual expectedValue
}

infix fun <V : Any> Property.ObjectProperty<V>.shouldMatch(
    property: Property.ObjectProperty<V>
) {
    hasValue shouldBeEqual property.hasValue
    value shouldBe property.value
}
