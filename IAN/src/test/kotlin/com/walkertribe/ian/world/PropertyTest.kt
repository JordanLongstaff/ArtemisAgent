package com.walkertribe.ian.world

import com.walkertribe.ian.enums.AlertStatus
import com.walkertribe.ian.util.BoolState
import com.walkertribe.ian.util.isKnown
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.floats.shouldBeNaN
import io.kotest.matchers.floats.shouldBeWithinPercentageOf
import io.kotest.matchers.floats.shouldBeZero
import io.kotest.matchers.floats.shouldNotBeNaN
import io.kotest.matchers.floats.shouldNotBeWithinPercentageOf
import io.kotest.matchers.floats.shouldNotBeZero
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.flatMap
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.numericFloat
import io.kotest.property.arbitrary.pair
import io.kotest.property.arbitrary.shuffle
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.bytes
import io.kotest.property.exhaustive.enum
import io.kotest.property.exhaustive.filter
import io.kotest.property.exhaustive.of
import io.kotest.property.forAll
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

class PropertyTest : DescribeSpec({
    describe("Property") {
        PropertyTestCase.entries.forEach {
            describe(it.toString()) {
                it.describeTestCases(this)
            }
        }
    }
})

enum class PropertyTestCase {
    FLOAT {
        override suspend fun testInitial() {
            val prop = Property.FloatProperty(0L)
            prop.shouldBeUnspecified()

            Arb.float().filter { !it.isNaN() }.checkAll {
                prop.value = it
                prop.shouldBeSpecified()
            }
        }

        override suspend fun testUpdates(
            scenario: PropertyUpdateScenario,
            onUpdate: () -> Unit,
            onSkip: () -> Unit
        ) {
            Arb.pair(
                Arb.long(max = Long.MAX_VALUE - 1),
                Arb.numericFloat().filter { it.sign.toInt() != 0 },
            ).flatMap { (oldTime, newValue) ->
                Arb.long(min = oldTime + 1).map { newTime ->
                    Triple(oldTime, newTime, newValue)
                }
            }.checkAll { (oldTime, newTime, newValue) ->
                val oldProp = Property.FloatProperty(oldTime)
                val newProp = Property.FloatProperty(newTime)

                val senderProp: Property.FloatProperty
                val receiverProp: Property.FloatProperty
                if (scenario.senderNewOldIdentifier == NewOldIdentifier.NEW) {
                    senderProp = newProp
                    receiverProp = oldProp
                } else {
                    senderProp = oldProp
                    receiverProp = newProp
                }

                if (scenario.senderIsSpecifiedIdentifier == SpecifiedIdentifier.SPECIFIED) {
                    senderProp.value = newValue
                }

                if (scenario.receiverIsSpecifiedIdentifier == SpecifiedIdentifier.SPECIFIED) {
                    receiverProp.value = 0f
                    receiverProp.shouldBeSpecified()
                    receiverProp.valueOrZero.shouldBeZero()
                    receiverProp.value.shouldNotBeWithinPercentageOf(senderProp.value, EPSILON)
                } else {
                    receiverProp.shouldBeUnspecified()
                }

                receiverProp.addListener { onUpdate() }
                senderProp.updates(receiverProp, onSkip)

                if (scenario.expectedToUpdate) {
                    receiverProp.valueOrZero.shouldNotBeZero()
                    receiverProp.value.shouldBeWithinPercentageOf(newValue, EPSILON)
                    oldProp shouldMatch newProp
                    return@checkAll
                }

                if (scenario.expectReceiverSpecified) {
                    receiverProp.shouldBeSpecified()
                    receiverProp.valueOrZero.shouldBeZero()
                    receiverProp.value.shouldNotBeWithinPercentageOf(senderProp.value, EPSILON)
                } else {
                    receiverProp.shouldBeUnspecified()
                }

                if (scenario.expectSpecifiedMatch) {
                    receiverProp.hasValue shouldBeEqual senderProp.hasValue
                } else {
                    receiverProp.hasValue shouldNotBeEqual senderProp.hasValue
                }
            }
        }

        override suspend fun describeMore(scope: DescribeSpecContainerScope) {
            scope.describe("Comparisons") {
                it("Less than") {
                    Arb.numericFloat().flatMap { a ->
                        Arb.numericFloat().filter { it != a }.map { Pair(a, it) }
                    }.forAll {
                        val minProp = Property.FloatProperty(0L)
                        minProp.value = min(it.first, it.second)

                        val maxProp = Property.FloatProperty(0L)
                        maxProp.value = max(it.first, it.second)

                        minProp < maxProp
                    }
                }

                it("Less than or equal") {
                    forAll(
                        Arb.numericFloat(),
                        Arb.numericFloat(),
                    ) { a, b ->
                        val minProp = Property.FloatProperty(0L)
                        minProp.value = min(a, b)

                        val maxProp = Property.FloatProperty(0L)
                        maxProp.value = max(a, b)

                        minProp <= maxProp
                    }
                }

                it("Greater than") {
                    Arb.numericFloat().flatMap { a ->
                        Arb.numericFloat().filter { it != a }.map { Pair(a, it) }
                    }.forAll {
                        val minProp = Property.FloatProperty(0L)
                        minProp.value = min(it.first, it.second)

                        val maxProp = Property.FloatProperty(0L)
                        maxProp.value = max(it.first, it.second)

                        maxProp > minProp
                    }
                }

                it("Greater than or equal") {
                    forAll(
                        Arb.numericFloat(),
                        Arb.numericFloat(),
                    ) { a, b ->
                        val minProp = Property.FloatProperty(0L)
                        minProp.value = min(a, b)

                        val maxProp = Property.FloatProperty(0L)
                        maxProp.value = max(a, b)

                        maxProp >= minProp
                    }
                }

                it("Less than if this property has no value") {
                    Arb.numericFloat().forAll {
                        val propWithoutValue = Property.FloatProperty(0L)
                        val propWithValue = Property.FloatProperty(0L)
                        propWithValue.value = it

                        propWithoutValue < propWithValue
                    }
                }

                it("Greater than if this property has no value") {
                    Arb.numericFloat().forAll {
                        val propWithoutValue = Property.FloatProperty(0L)
                        val propWithValue = Property.FloatProperty(0L)
                        propWithValue.value = it

                        propWithValue > propWithoutValue
                    }
                }

                it("Equal if neither has a value") {
                    val propA = Property.FloatProperty(0L)
                    val propB = Property.FloatProperty(0L)
                    propA shouldBeEqualComparingTo propB
                }
            }
        }
    },
    BYTE {
        override suspend fun testInitial() {
            val prop = Property.ByteProperty(0L)
            prop.shouldBeUnspecified()

            Exhaustive.bytes().filter { it.toInt() != -1 }.checkAll {
                prop.value = it
                prop.shouldBeSpecified()
            }
        }

        override suspend fun testUpdates(
            scenario: PropertyUpdateScenario,
            onUpdate: () -> Unit,
            onSkip: () -> Unit
        ) {
            Arb.pair(
                Arb.long(max = Long.MAX_VALUE - 1),
                Arb.byte().filter { it.toInt() != -1 },
            ).flatMap { oldData ->
                Arb.pair(
                    Arb.long(min = oldData.first + 1),
                    Arb.byte().filter { it.toInt() != -1 && it != oldData.second },
                ).map { newData -> Pair(oldData, newData) }
            }.checkAll { (oldData, newData) ->
                val (oldTime, oldValue) = oldData
                val (newTime, newValue) = newData

                val oldProp = Property.ByteProperty(oldTime)
                val newProp = Property.ByteProperty(newTime)

                val senderProp: Property.ByteProperty
                val receiverProp: Property.ByteProperty
                if (scenario.senderNewOldIdentifier == NewOldIdentifier.NEW) {
                    senderProp = newProp
                    receiverProp = oldProp
                } else {
                    senderProp = oldProp
                    receiverProp = newProp
                }

                if (scenario.senderIsSpecifiedIdentifier == SpecifiedIdentifier.SPECIFIED) {
                    senderProp.value = newValue
                }

                if (scenario.receiverIsSpecifiedIdentifier == SpecifiedIdentifier.SPECIFIED) {
                    receiverProp.value = oldValue
                    receiverProp.shouldBeSpecified()
                    receiverProp.value shouldBeEqual oldValue
                    receiverProp.value shouldNotBeEqual senderProp.value
                } else {
                    receiverProp.shouldBeUnspecified()
                }

                receiverProp.addListener { onUpdate() }
                senderProp.updates(receiverProp, onSkip)

                if (scenario.expectedToUpdate) {
                    receiverProp.shouldBeSpecified()
                    receiverProp.value shouldBeEqual newValue
                    receiverProp shouldMatch senderProp
                    return@checkAll
                }

                if (scenario.expectReceiverSpecified) {
                    receiverProp shouldContainValue oldValue
                } else {
                    receiverProp.shouldBeUnspecified()
                }

                if (scenario.expectSpecifiedMatch) {
                    receiverProp.hasValue shouldBeEqual senderProp.hasValue
                } else {
                    receiverProp.hasValue shouldNotBeEqual senderProp.hasValue
                }

                if (scenario.expectValueMatch) {
                    receiverProp.value shouldBeEqual senderProp.value
                } else {
                    receiverProp.value shouldNotBeEqual senderProp.value
                }
            }
        }

        override suspend fun describeMore(scope: DescribeSpecContainerScope) {
            super.describeMore(scope)

            scope.describe("Custom initial value") {
                it(INITIAL_TEST_NAME) {
                    Arb.byte().filter { it.toInt() != -1 }.flatMap { initialValue ->
                        Arb.byte().filter { it != initialValue }.map { newValue ->
                            Pair(initialValue, newValue)
                        }
                    }.checkAll { (initialValue, newValue) ->
                        val prop = Property.ByteProperty(
                            0L,
                            initialValue,
                        )
                        prop.shouldBeUnspecified(initialValue)

                        prop.value = newValue
                        prop.shouldBeSpecified(initialValue)
                    }
                }

                it("Throws on initial value mismatch") {
                    checkAll(
                        Arb.pair(Arb.long(), Arb.long()).filter {
                            it.first != it.second
                        },
                        Arb.pair(Arb.byte(), Arb.byte()).filter {
                            it.first != it.second
                        },
                    ) { (timestampA, timestampB), (initialA, initialB) ->
                        val defaultProp = Property.ByteProperty(timestampA, initialA)
                        val customProp = Property.ByteProperty(timestampB, initialB)

                        shouldThrow<IllegalArgumentException> { customProp updates defaultProp }
                        shouldThrow<IllegalArgumentException> { defaultProp updates customProp }
                    }
                }
            }

            scope.describe("Comparisons") {
                it("Less than") {
                    Arb.byte().filter { it.toInt() != -1 }.flatMap { a ->
                        Arb.byte().filter { it.toInt() != -1 && it != a }.map {
                            Pair(a.toInt(), it.toInt())
                        }
                    }.forAll {
                        val minProp = Property.ByteProperty(0L)
                        minProp.value = min(it.first, it.second).toByte()

                        val maxProp = Property.ByteProperty(0L)
                        maxProp.value = max(it.first, it.second).toByte()

                        minProp < maxProp
                    }
                }

                it("Less than or equal") {
                    forAll(
                        Arb.byte().filter { it.toInt() != -1 },
                        Arb.byte().filter { it.toInt() != -1 },
                    ) { a, b ->
                        val minProp = Property.ByteProperty(0L)
                        minProp.value = min(a.toInt(), b.toInt()).toByte()

                        val maxProp = Property.ByteProperty(0L)
                        maxProp.value = max(a.toInt(), b.toInt()).toByte()

                        minProp <= maxProp
                    }
                }

                it("Greater than") {
                    Arb.byte().filter { it.toInt() != -1 }.flatMap { a ->
                        Arb.byte().filter { it.toInt() != -1 && it != a }.map {
                            Pair(a.toInt(), it.toInt())
                        }
                    }.forAll {
                        val minProp = Property.ByteProperty(0L)
                        minProp.value = min(it.first, it.second).toByte()

                        val maxProp = Property.ByteProperty(0L)
                        maxProp.value = max(it.first, it.second).toByte()

                        maxProp > minProp
                    }
                }

                it("Greater than or equal") {
                    forAll(
                        Arb.byte().filter { it.toInt() != -1 },
                        Arb.byte().filter { it.toInt() != -1 },
                    ) { a, b ->
                        val minProp = Property.ByteProperty(0L)
                        minProp.value = min(a.toInt(), b.toInt()).toByte()

                        val maxProp = Property.ByteProperty(0L)
                        maxProp.value = max(a.toInt(), b.toInt()).toByte()

                        maxProp >= minProp
                    }
                }

                it("Less than if this property has no value") {
                    Arb.byte().filter { it.toInt() != -1 }.forAll {
                        val propWithoutValue = Property.ByteProperty(0L)
                        val propWithValue = Property.ByteProperty(0L)
                        propWithValue.value = it

                        propWithoutValue < propWithValue
                    }
                }

                it("Greater than if this property has no value") {
                    Arb.byte().filter { it.toInt() != -1 }.forAll {
                        val propWithoutValue = Property.ByteProperty(0L)
                        val propWithValue = Property.ByteProperty(0L)
                        propWithValue.value = it

                        propWithValue > propWithoutValue
                    }
                }

                it("Equal if neither has a value") {
                    checkAll(Arb.byte(), Arb.byte()) { a, b ->
                        val propA = Property.ByteProperty(0L, a)
                        val propB = Property.ByteProperty(0L, b)
                        propA shouldBeEqualComparingTo propB
                    }
                }
            }
        }
    },
    INTEGER {
        override suspend fun testInitial() {
            val prop = Property.IntProperty(0L)
            prop.shouldBeUnspecified()

            Arb.int().filter { it != -1 }.checkAll {
                prop.value = it
                prop.shouldBeSpecified()
            }
        }

        override suspend fun testUpdates(
            scenario: PropertyUpdateScenario,
            onUpdate: () -> Unit,
            onSkip: () -> Unit
        ) {
            Arb.pair(
                Arb.long(max = Long.MAX_VALUE - 1),
                Arb.int().filter { it != -1 },
            ).flatMap { oldData ->
                Arb.pair(
                    Arb.long(min = oldData.first + 1),
                    Arb.int().filter { it != -1 && it != oldData.second },
                ).map { newData -> Pair(oldData, newData) }
            }.checkAll { (oldData, newData) ->
                val (oldTime, oldValue) = oldData
                val (newTime, newValue) = newData

                val oldProp = Property.IntProperty(oldTime)
                val newProp = Property.IntProperty(newTime)

                val senderProp: Property.IntProperty
                val receiverProp: Property.IntProperty
                if (scenario.senderNewOldIdentifier == NewOldIdentifier.NEW) {
                    senderProp = newProp
                    receiverProp = oldProp
                } else {
                    senderProp = oldProp
                    receiverProp = newProp
                }

                if (scenario.senderIsSpecifiedIdentifier == SpecifiedIdentifier.SPECIFIED) {
                    senderProp.value = newValue
                }

                if (scenario.receiverIsSpecifiedIdentifier == SpecifiedIdentifier.SPECIFIED) {
                    receiverProp.value = oldValue
                    receiverProp.shouldBeSpecified()
                    receiverProp.value shouldBeEqual oldValue
                    receiverProp.value shouldNotBeEqual senderProp.value
                } else {
                    receiverProp.shouldBeUnspecified()
                }

                receiverProp.addListener { onUpdate() }
                senderProp.updates(receiverProp, onSkip)

                if (scenario.expectedToUpdate) {
                    receiverProp.shouldBeSpecified()
                    receiverProp.value shouldBeEqual newValue
                    receiverProp shouldMatch senderProp
                    return@checkAll
                }

                if (scenario.expectReceiverSpecified) {
                    receiverProp shouldContainValue oldValue
                } else {
                    receiverProp.shouldBeUnspecified()
                }

                if (scenario.expectSpecifiedMatch) {
                    receiverProp.hasValue shouldBeEqual senderProp.hasValue
                } else {
                    receiverProp.hasValue shouldNotBeEqual senderProp.hasValue
                }

                if (scenario.expectValueMatch) {
                    receiverProp.value shouldBeEqual senderProp.value
                } else {
                    receiverProp.value shouldNotBeEqual senderProp.value
                }
            }
        }

        override suspend fun describeMore(scope: DescribeSpecContainerScope) {
            super.describeMore(scope)

            scope.describe("Custom initial value") {
                it(INITIAL_TEST_NAME) {
                    Arb.int().filter { it != -1 }.flatMap { initialValue ->
                        Arb.int().filter { it != initialValue }.map { newValue ->
                            Pair(initialValue, newValue)
                        }
                    }.checkAll { (initialValue, newValue) ->
                        val prop = Property.IntProperty(
                            0L,
                            initialValue,
                        )
                        prop.shouldBeUnspecified(initialValue)

                        prop.value = newValue
                        prop.shouldBeSpecified(initialValue)
                    }
                }

                it("Throws on initial value mismatch") {
                    checkAll(
                        Arb.pair(Arb.long(), Arb.long()).filter {
                            it.first != it.second
                        },
                        Arb.pair(Arb.int(), Arb.int()).filter {
                            it.first != it.second
                        },
                    ) { (timestampA, timestampB), (initialA, initialB) ->
                        val defaultProp = Property.IntProperty(timestampA, initialA)
                        val customProp = Property.IntProperty(timestampB, initialB)

                        shouldThrow<IllegalArgumentException> { customProp updates defaultProp }
                        shouldThrow<IllegalArgumentException> { defaultProp updates customProp }
                    }
                }
            }

            scope.describe("Comparisons") {
                it("Less than") {
                    Arb.int().filter { it != -1 }.flatMap { a ->
                        Arb.int().filter { it != -1 && it != a }.map { Pair(a, it) }
                    }.forAll {
                        val minProp = Property.IntProperty(0L)
                        minProp.value = min(it.first, it.second)

                        val maxProp = Property.IntProperty(0L)
                        maxProp.value = max(it.first, it.second)

                        minProp < maxProp
                    }
                }

                it("Less than or equal") {
                    forAll(Arb.int().filter { it != -1 }, Arb.int().filter { it != -1 }) { a, b ->
                        val minProp = Property.IntProperty(0L)
                        minProp.value = min(a, b)

                        val maxProp = Property.IntProperty(0L)
                        maxProp.value = max(a, b)

                        minProp <= maxProp
                    }
                }

                it("Greater than") {
                    Arb.int().filter { it != -1 }.flatMap { a ->
                        Arb.int().filter { it != -1 && it != a }.map { Pair(a, it) }
                    }.forAll {
                        val minProp = Property.IntProperty(0L)
                        minProp.value = min(it.first, it.second)

                        val maxProp = Property.IntProperty(0L)
                        maxProp.value = max(it.first, it.second)

                        maxProp > minProp
                    }
                }

                it("Greater than or equal") {
                    forAll(Arb.int().filter { it != -1 }, Arb.int().filter { it != -1 }) { a, b ->
                        val minProp = Property.IntProperty(0L)
                        minProp.value = min(a, b)

                        val maxProp = Property.IntProperty(0L)
                        maxProp.value = max(a, b)

                        maxProp >= minProp
                    }
                }

                it("Less than if this property has no value") {
                    Arb.int().filter { it != -1 }.forAll {
                        val propWithoutValue = Property.IntProperty(0L)
                        val propWithValue = Property.IntProperty(0L)
                        propWithValue.value = it

                        propWithoutValue < propWithValue
                    }
                }

                it("Greater than if this property has no value") {
                    Arb.int().filter { it != -1 }.forAll {
                        val propWithoutValue = Property.IntProperty(0L)
                        val propWithValue = Property.IntProperty(0L)
                        propWithValue.value = it

                        propWithValue > propWithoutValue
                    }
                }

                it("Equal if neither has a value") {
                    checkAll(Arb.int(), Arb.int()) { a, b ->
                        val propA = Property.IntProperty(0L, a)
                        val propB = Property.IntProperty(0L, b)
                        propA shouldBeEqualComparingTo propB
                    }
                }
            }
        }
    },
    BOOLEAN {
        override suspend fun testInitial() {
            val prop = Property.BoolProperty(0L)
            prop.shouldBeUnspecified()

            Exhaustive.of(BoolState.True, BoolState.False).checkAll {
                prop.value = it
                prop.shouldBeSpecified()
            }
        }

        override suspend fun testUpdates(
            scenario: PropertyUpdateScenario,
            onUpdate: () -> Unit,
            onSkip: () -> Unit
        ) {
            Arb.long(max = Long.MAX_VALUE - 1).flatMap { oldTime ->
                Arb.long(min = oldTime + 1).map { newTime ->
                    Pair(oldTime, newTime)
                }
            }.checkAll { (oldTime, newTime) ->
                val oldProp = Property.BoolProperty(oldTime)
                val newProp = Property.BoolProperty(newTime)

                val senderProp: Property.BoolProperty
                val receiverProp: Property.BoolProperty
                if (scenario.senderNewOldIdentifier == NewOldIdentifier.NEW) {
                    senderProp = newProp
                    receiverProp = oldProp
                } else {
                    senderProp = oldProp
                    receiverProp = newProp
                }

                if (scenario.senderIsSpecifiedIdentifier == SpecifiedIdentifier.SPECIFIED) {
                    senderProp.value = BoolState.True
                }

                if (scenario.receiverIsSpecifiedIdentifier == SpecifiedIdentifier.SPECIFIED) {
                    receiverProp.value = BoolState.False
                    receiverProp.shouldBeFalse()
                    receiverProp.value shouldNotBeEqual senderProp.value
                } else {
                    receiverProp.shouldBeUnspecified()
                }

                receiverProp.addListener { onUpdate() }
                senderProp.updates(receiverProp, onSkip)

                if (scenario.expectedToUpdate) {
                    receiverProp.shouldBeTrue()
                    receiverProp shouldMatch senderProp
                    return@checkAll
                }

                if (scenario.expectReceiverSpecified) {
                    receiverProp.shouldBeFalse()
                } else {
                    receiverProp.shouldBeUnspecified()
                }

                if (scenario.expectSpecifiedMatch) {
                    receiverProp.hasValue shouldBeEqual senderProp.hasValue
                } else {
                    receiverProp.hasValue shouldNotBeEqual senderProp.hasValue
                }

                if (scenario.expectValueMatch) {
                    receiverProp.value shouldBeEqual senderProp.value
                } else {
                    receiverProp.value shouldNotBeEqual senderProp.value
                }
            }
        }
    },
    OBJECT {
        override suspend fun testInitial() {
            val prop = Property.ObjectProperty<AlertStatus>(0L)
            prop.shouldBeUnspecified()

            Exhaustive.enum<AlertStatus>().checkAll {
                prop.value = it
                prop.shouldBeSpecified()
            }
        }

        override suspend fun testUpdates(
            scenario: PropertyUpdateScenario,
            onUpdate: () -> Unit,
            onSkip: () -> Unit
        ) {
            Arb.pair(
                Arb.long(max = Long.MAX_VALUE - 1),
                Arb.shuffle(AlertStatus.entries.toList()),
            ).flatMap { (oldTime, values) ->
                Arb.long(min = oldTime + 1).map { newTime ->
                    Triple(oldTime, newTime, values)
                }
            }.checkAll { (oldTime, newTime, values) ->
                collect(values.joinToString(" -> "))

                val oldProp = Property.ObjectProperty<AlertStatus>(oldTime)
                val newProp = Property.ObjectProperty<AlertStatus>(newTime)

                val senderProp: Property.ObjectProperty<AlertStatus>
                val receiverProp: Property.ObjectProperty<AlertStatus>
                if (scenario.senderNewOldIdentifier == NewOldIdentifier.NEW) {
                    senderProp = newProp
                    receiverProp = oldProp
                } else {
                    senderProp = oldProp
                    receiverProp = newProp
                }

                val senderValue = values[1]
                if (scenario.senderIsSpecifiedIdentifier == SpecifiedIdentifier.SPECIFIED) {
                    senderProp.value = senderValue
                }

                val receiverValue = values[0]
                if (scenario.receiverIsSpecifiedIdentifier == SpecifiedIdentifier.SPECIFIED) {
                    receiverProp.value = receiverValue
                    receiverProp shouldContainValue receiverValue
                    receiverProp.value shouldNotBe senderProp.value
                } else {
                    receiverProp.shouldBeUnspecified()
                }

                receiverProp.addListener { onUpdate() }
                senderProp.updates(receiverProp, onSkip)

                if (scenario.expectedToUpdate) {
                    receiverProp shouldContainValue senderValue
                    receiverProp shouldMatch senderProp
                    return@checkAll
                }

                if (scenario.expectReceiverSpecified) {
                    receiverProp shouldContainValue receiverValue
                } else {
                    receiverProp.shouldBeUnspecified()
                }

                if (scenario.expectSpecifiedMatch) {
                    receiverProp.hasValue shouldBeEqual senderProp.hasValue
                } else {
                    receiverProp.hasValue shouldNotBeEqual senderProp.hasValue
                }

                if (scenario.expectValueMatch) {
                    receiverProp.value shouldBe senderProp.value
                } else {
                    receiverProp.value shouldNotBe senderProp.value
                }
            }
        }
    };

    protected abstract suspend fun testInitial()
    abstract suspend fun testUpdates(
        scenario: PropertyUpdateScenario,
        onUpdate: () -> Unit,
        onSkip: () -> Unit,
    )

    suspend fun describeTestCases(scope: DescribeSpecContainerScope) {
        scope.it(INITIAL_TEST_NAME) {
            testInitial()
        }

        scope.withData(
            nameFn = { "$it property" },
            NewOldIdentifier.entries.toList(),
        ) {
            it.describeScenarios(this, this@PropertyTestCase)
        }

        describeMore(scope)
    }

    open suspend fun describeMore(scope: DescribeSpecContainerScope) { }

    override fun toString(): String = "${name[0]}${name.substring(1).lowercase()} property"

    private companion object {
        const val INITIAL_TEST_NAME = "Is unknown when initialized"
    }
}

enum class NewOldIdentifier(private val receiverIdName: String) {
    NEW("old"),
    OLD("new");

    suspend fun describeScenarios(scope: ContainerScope, testCase: PropertyTestCase) {
        val specifiedIdentifiers = SpecifiedIdentifier.entries.toList()
        scope.withData(
            nameFn = { it.name.run { this[0] + substring(1).lowercase() } },
            specifiedIdentifiers,
        ) { sender ->
            withData(
                nameFn = {
                    "${it.expectedBehaviourDescription} $receiverIdName, " +
                        "${it.receiverIsSpecifiedIdentifier.name.lowercase()} property"
                },
                specifiedIdentifiers.map { receiver ->
                    PropertyUpdateScenario(
                        senderNewOldIdentifier = this@NewOldIdentifier,
                        senderIsSpecifiedIdentifier = sender,
                        receiverIsSpecifiedIdentifier = receiver,
                    )
                },
            ) { scenario ->
                var updated = false
                var skipped = false

                assertSoftly {
                    testCase.testUpdates(scenario, { updated = true }, { skipped = true })

                    updated shouldBeEqual scenario.expectedToUpdate
                    skipped shouldBeEqual scenario.expectedToSkip
                }
            }
        }
    }

    override fun toString(): String = name[0] + name.substring(1).lowercase()
}

enum class SpecifiedIdentifier {
    SPECIFIED,
    UNSPECIFIED
}

data class PropertyUpdateScenario(
    val senderNewOldIdentifier: NewOldIdentifier,
    val senderIsSpecifiedIdentifier: SpecifiedIdentifier,
    val receiverIsSpecifiedIdentifier: SpecifiedIdentifier,
) {
    val expectedToUpdate = senderNewOldIdentifier == NewOldIdentifier.NEW &&
        senderIsSpecifiedIdentifier == SpecifiedIdentifier.SPECIFIED

    val expectedToSkip = senderNewOldIdentifier == NewOldIdentifier.NEW &&
        senderIsSpecifiedIdentifier == SpecifiedIdentifier.UNSPECIFIED

    val expectReceiverSpecified = expectedToUpdate ||
        receiverIsSpecifiedIdentifier == SpecifiedIdentifier.SPECIFIED

    val expectSpecifiedMatch = expectedToUpdate ||
        senderIsSpecifiedIdentifier == receiverIsSpecifiedIdentifier

    val expectValueMatch = expectSpecifiedMatch && (expectedToUpdate == expectReceiverSpecified)

    val expectedBehaviourDescription = if (expectedToUpdate) "Updates" else "Skips"
}

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
