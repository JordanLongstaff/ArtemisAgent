package com.walkertribe.ian.world

import com.walkertribe.ian.enums.AlertStatus
import com.walkertribe.ian.util.BoolState
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.scopes.ContainerScope
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.floats.shouldBeWithinPercentageOf
import io.kotest.matchers.floats.shouldBeZero
import io.kotest.matchers.floats.shouldNotBeWithinPercentageOf
import io.kotest.matchers.floats.shouldNotBeZero
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.Gen
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
import kotlin.math.sign
import kotlinx.coroutines.launch

class PropertyTest :
    DescribeSpec({
        describe("Property") {
            PropertyTestCase.entries.forEach {
                describe(it.toString()) { it.describeTestCases(this) }
            }
        }
    })

enum class PropertyTestCase {
    FLOAT {
        override suspend fun testInitial() {
            val prop = Property.FloatProperty(0L)
            prop.shouldBeUnspecified()

            Arb.float()
                .filter { !it.isNaN() }
                .checkAll { value ->
                    prop.value = value
                    prop.shouldBeSpecified()
                }
        }

        override suspend fun testUpdates(
            scenario: PropertyUpdateScenario,
            onUpdate: () -> Unit,
            onSkip: () -> Unit,
        ) {
            Arb.pair(
                    Arb.long(max = Long.MAX_VALUE - 1),
                    Arb.numericFloat().filter { it.sign.toInt() != 0 },
                )
                .flatMap { (oldTime, newValue) ->
                    Arb.long(min = oldTime + 1).map { newTime ->
                        Triple(oldTime, newTime, newValue)
                    }
                }
                .checkAll { (oldTime, newTime, newValue) ->
                    val oldProp = Property.FloatProperty(oldTime)
                    val newProp = Property.FloatProperty(newTime)
                    val (senderProp, receiverProp) = scenario.organize(oldProp, newProp)

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
            scope.describeComparisonTests(
                strictArbPair =
                    Arb.numericFloat().flatMap { a ->
                        Arb.numericFloat().filter { it != a }.map { Pair(a, it) }
                    },
                softArbPair = Arb.pair(Arb.numericFloat(), Arb.numericFloat()),
                singleArb = Arb.numericFloat(),
                emptyPropertyGenerator = Exhaustive.of(Property.FloatProperty(0L)),
            ) {
                Property.FloatProperty(it)
            }
        }
    },
    BYTE {
        override suspend fun testInitial() {
            val prop = Property.ByteProperty(0L)
            prop.shouldBeUnspecified()

            Exhaustive.bytes()
                .filter { it.toInt() != -1 }
                .checkAll { value ->
                    prop.value = value
                    prop.shouldBeSpecified()
                }
        }

        override suspend fun testUpdates(
            scenario: PropertyUpdateScenario,
            onUpdate: () -> Unit,
            onSkip: () -> Unit,
        ) {
            Arb.pair(Arb.long(max = Long.MAX_VALUE - 1), Arb.byte().filter { it.toInt() != -1 })
                .flatMap { oldData ->
                    Arb.pair(
                            Arb.long(min = oldData.first + 1),
                            Arb.byte().filter { it.toInt() != -1 && it != oldData.second },
                        )
                        .map { newData -> Pair(oldData, newData) }
                }
                .checkAll { (oldData, newData) ->
                    val (oldTime, oldValue) = oldData
                    val (newTime, newValue) = newData

                    val oldProp = Property.ByteProperty(oldTime)
                    val newProp = Property.ByteProperty(newTime)
                    val (senderProp, receiverProp) = scenario.organize(oldProp, newProp)

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

                    scenario.testSpecifiedMatch(receiverProp, senderProp)
                    scenario.testValueMatch(receiverProp, senderProp)
                }
        }

        override suspend fun describeMore(scope: DescribeSpecContainerScope) {
            super.describeMore(scope)

            scope.describe("Custom initial value") {
                it(INITIAL_TEST_NAME) {
                    Arb.byte()
                        .filter { it.toInt() != -1 }
                        .flatMap { initialValue ->
                            Arb.byte()
                                .filter { it != initialValue }
                                .map { newValue -> Pair(initialValue, newValue) }
                        }
                        .checkAll { (initialValue, newValue) ->
                            val prop = Property.ByteProperty(0L, initialValue)
                            prop.shouldBeUnspecified(initialValue)

                            prop.value = newValue
                            prop.shouldBeSpecified(initialValue)
                        }
                }

                it("Throws on initial value mismatch") {
                    checkAll(
                        Arb.pair(Arb.long(), Arb.long()).filter { it.first != it.second },
                        Arb.pair(Arb.byte(), Arb.byte()).filter { it.first != it.second },
                    ) { (timestampA, timestampB), (initialA, initialB) ->
                        val defaultProp = Property.ByteProperty(timestampA, initialA)
                        val customProp = Property.ByteProperty(timestampB, initialB)

                        shouldThrow<IllegalArgumentException> { customProp updates defaultProp }
                        shouldThrow<IllegalArgumentException> { defaultProp updates customProp }
                    }
                }
            }

            scope.describeComparisonTests(
                strictArbPair =
                    Arb.byte()
                        .filter { it.toInt() != -1 }
                        .flatMap { a ->
                            Arb.byte().filter { it.toInt() != -1 && it != a }.map { Pair(a, it) }
                        },
                softArbPair =
                    Arb.pair(
                        Arb.byte().filter { it.toInt() != -1 },
                        Arb.byte().filter { it.toInt() != -1 },
                    ),
                singleArb = Arb.byte().filter { it.toInt() != -1 },
                emptyPropertyGenerator = Arb.byte().map { Property.ByteProperty(0L, it) },
            ) {
                Property.ByteProperty(it)
            }
        }
    },
    INTEGER {
        override suspend fun testInitial() {
            val prop = Property.IntProperty(0L)
            prop.shouldBeUnspecified()

            Arb.int()
                .filter { it != -1 }
                .checkAll { value ->
                    prop.value = value
                    prop.shouldBeSpecified()
                }
        }

        override suspend fun testUpdates(
            scenario: PropertyUpdateScenario,
            onUpdate: () -> Unit,
            onSkip: () -> Unit,
        ) {
            Arb.pair(Arb.long(max = Long.MAX_VALUE - 1), Arb.int().filter { it != -1 })
                .flatMap { oldData ->
                    Arb.pair(
                            Arb.long(min = oldData.first + 1),
                            Arb.int().filter { it != -1 && it != oldData.second },
                        )
                        .map { newData -> Pair(oldData, newData) }
                }
                .checkAll { (oldData, newData) ->
                    val (oldTime, oldValue) = oldData
                    val (newTime, newValue) = newData

                    val oldProp = Property.IntProperty(oldTime)
                    val newProp = Property.IntProperty(newTime)
                    val (senderProp, receiverProp) = scenario.organize(oldProp, newProp)

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

                    scenario.testSpecifiedMatch(receiverProp, senderProp)
                    scenario.testValueMatch(receiverProp, senderProp)
                }
        }

        override suspend fun describeMore(scope: DescribeSpecContainerScope) {
            super.describeMore(scope)

            scope.describe("Custom initial value") {
                it(INITIAL_TEST_NAME) {
                    Arb.int()
                        .filter { it != -1 }
                        .flatMap { initialValue ->
                            Arb.int()
                                .filter { it != initialValue }
                                .map { newValue -> Pair(initialValue, newValue) }
                        }
                        .checkAll { (initialValue, newValue) ->
                            val prop = Property.IntProperty(0L, initialValue)
                            prop.shouldBeUnspecified(initialValue)

                            prop.value = newValue
                            prop.shouldBeSpecified(initialValue)
                        }
                }

                it("Throws on initial value mismatch") {
                    checkAll(
                        Arb.pair(Arb.long(), Arb.long()).filter { it.first != it.second },
                        Arb.pair(Arb.int(), Arb.int()).filter { it.first != it.second },
                    ) { (timestampA, timestampB), (initialA, initialB) ->
                        val defaultProp = Property.IntProperty(timestampA, initialA)
                        val customProp = Property.IntProperty(timestampB, initialB)

                        shouldThrow<IllegalArgumentException> { customProp updates defaultProp }
                        shouldThrow<IllegalArgumentException> { defaultProp updates customProp }
                    }
                }
            }

            scope.describeComparisonTests(
                strictArbPair =
                    Arb.int()
                        .filter { it != -1 }
                        .flatMap { a ->
                            Arb.int().filter { it != -1 && it != a }.map { Pair(a, it) }
                        },
                softArbPair =
                    Arb.pair(Arb.int().filter { it != -1 }, Arb.int().filter { it != -1 }),
                singleArb = Arb.int().filter { it != -1 },
                emptyPropertyGenerator = Arb.int().map { Property.IntProperty(0L, it) },
            ) {
                Property.IntProperty(it)
            }
        }
    },
    BOOLEAN {
        override suspend fun testInitial() {
            val prop = Property.BoolProperty(0L)
            prop.shouldBeUnspecified()

            Exhaustive.of(BoolState.True, BoolState.False).checkAll { value ->
                prop.value = value
                prop.shouldBeSpecified()
            }
        }

        override suspend fun testUpdates(
            scenario: PropertyUpdateScenario,
            onUpdate: () -> Unit,
            onSkip: () -> Unit,
        ) {
            Arb.long(max = Long.MAX_VALUE - 1)
                .flatMap { oldTime ->
                    Arb.long(min = oldTime + 1).map { newTime -> Pair(oldTime, newTime) }
                }
                .checkAll { (oldTime, newTime) ->
                    val oldProp = Property.BoolProperty(oldTime)
                    val newProp = Property.BoolProperty(newTime)
                    val (senderProp, receiverProp) = scenario.organize(oldProp, newProp)

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

                    scenario.testSpecifiedMatch(receiverProp, senderProp)
                    scenario.testValueMatch(receiverProp, senderProp)
                }
        }
    },
    OBJECT {
        override suspend fun testInitial() {
            val prop = Property.ObjectProperty<AlertStatus>(0L)
            prop.shouldBeUnspecified()

            Exhaustive.enum<AlertStatus>().checkAll { value ->
                prop.value = value
                prop.shouldBeSpecified()
            }
        }

        override suspend fun testUpdates(
            scenario: PropertyUpdateScenario,
            onUpdate: () -> Unit,
            onSkip: () -> Unit,
        ) {
            Arb.pair(Arb.long(max = Long.MAX_VALUE - 1), Arb.shuffle(AlertStatus.entries.toList()))
                .flatMap { (oldTime, values) ->
                    Arb.long(min = oldTime + 1).map { newTime -> Triple(oldTime, newTime, values) }
                }
                .checkAll { (oldTime, newTime, values) ->
                    collect(values.joinToString(" -> "))

                    val oldProp = Property.ObjectProperty<AlertStatus>(oldTime)
                    val newProp = Property.ObjectProperty<AlertStatus>(newTime)
                    val (senderProp, receiverProp) = scenario.organize(oldProp, newProp)

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

                    scenario.testSpecifiedMatch(receiverProp, senderProp)
                    scenario.testValueMatch(receiverProp, senderProp)
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
        scope.it(INITIAL_TEST_NAME) { testInitial() }

        scope.withData(nameFn = { "$it property" }, NewOldIdentifier.entries.toList()) {
            it.describeScenarios(this, this@PropertyTestCase)
        }

        describeMore(scope)
    }

    open suspend fun describeMore(scope: DescribeSpecContainerScope) {}

    override fun toString(): String = "${name[0]}${name.substring(1).lowercase()} property"

    private companion object {
        const val INITIAL_TEST_NAME = "Is unknown when initialized"

        suspend fun <N, P> testComparisons(
            arbPair: Arb<Pair<N, N>>,
            propertyGenerator: (Long) -> P,
            comparisonTest: (P, P) -> Boolean,
        ) where N : Number, N : Comparable<N>, P : Property<N, P>, P : Comparable<P> {
            arbPair.forAll { (first, second) ->
                val firstProp = propertyGenerator(0L)
                val secondProp = propertyGenerator(0L)

                firstProp.value = first
                secondProp.value = second

                if (first < second) comparisonTest(firstProp, secondProp)
                else comparisonTest(secondProp, firstProp)
            }
        }

        suspend fun <N, P> testComparisonWithOneValue(
            arb: Arb<N>,
            propertyGenerator: (Long) -> P,
            comparisonTest: (P, P) -> Boolean,
        ) where N : Number, N : Comparable<N>, P : Property<N, P>, P : Comparable<P> {
            arb.forAll { value ->
                val propWithoutValue = propertyGenerator(0L)
                val propWithValue = propertyGenerator(0L)
                propWithValue.value = value

                comparisonTest(propWithValue, propWithoutValue)
            }
        }

        fun <N, P> DescribeSpecContainerScope.describeComparisonTests(
            strictArbPair: Arb<Pair<N, N>>,
            softArbPair: Arb<Pair<N, N>>,
            singleArb: Arb<N>,
            emptyPropertyGenerator: Gen<P>,
            basePropertyGenerator: (Long) -> P,
        ) where N : Number, N : Comparable<N>, P : Property<N, P>, P : Comparable<P> = launch {
            describe("Comparisons") {
                it("Less than") {
                    testComparisons(strictArbPair, basePropertyGenerator) { a, b -> a < b }
                }

                it("Less than or equal") {
                    testComparisons(softArbPair, basePropertyGenerator) { a, b -> a <= b }
                }

                it("Greater than") {
                    testComparisons(strictArbPair, basePropertyGenerator) { a, b -> b > a }
                }

                it("Greater than or equal") {
                    testComparisons(softArbPair, basePropertyGenerator) { a, b -> b >= a }
                }

                it("Less than if this property has no value") {
                    testComparisonWithOneValue(singleArb, basePropertyGenerator) { a, b -> b < a }
                }

                it("Greater than if this property has no value") {
                    testComparisonWithOneValue(singleArb, basePropertyGenerator) { a, b -> a > b }
                }

                it("Equal if neither has a value") {
                    checkAll(emptyPropertyGenerator, emptyPropertyGenerator) { a, b ->
                        a shouldBeEqualComparingTo b
                    }
                }
            }
        }
    }
}

enum class NewOldIdentifier(private val receiverIdName: String) {
    NEW("old") {
        override fun <V, P : Property<V, P>> organize(old: P, new: P): Pair<P, P> = new to old
    },
    OLD("new") {
        override fun <V, P : Property<V, P>> organize(old: P, new: P): Pair<P, P> = old to new
    };

    suspend fun describeScenarios(scope: ContainerScope, testCase: PropertyTestCase) {
        val specifiedIdentifiers = SpecifiedIdentifier.entries.toList()
        scope.withData(
            nameFn = { it.name.run { this[0] + substring(1).lowercase() } },
            specifiedIdentifiers,
        ) { sender ->
            withData(
                nameFn = { scenario ->
                    "${scenario.expectedBehaviourDescription} $receiverIdName, " +
                        "${scenario.receiverIsSpecifiedIdentifier.name.lowercase()} property"
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

    abstract fun <V, P : Property<V, P>> organize(old: P, new: P): Pair<P, P>

    override fun toString(): String = name[0] + name.substring(1).lowercase()
}

enum class SpecifiedIdentifier {
    SPECIFIED,
    UNSPECIFIED,
}

data class PropertyUpdateScenario(
    val senderNewOldIdentifier: NewOldIdentifier,
    val senderIsSpecifiedIdentifier: SpecifiedIdentifier,
    val receiverIsSpecifiedIdentifier: SpecifiedIdentifier,
) {
    val expectedToUpdate =
        senderNewOldIdentifier == NewOldIdentifier.NEW &&
            senderIsSpecifiedIdentifier == SpecifiedIdentifier.SPECIFIED

    val expectedToSkip =
        senderNewOldIdentifier == NewOldIdentifier.NEW &&
            senderIsSpecifiedIdentifier == SpecifiedIdentifier.UNSPECIFIED

    val expectReceiverSpecified =
        expectedToUpdate || receiverIsSpecifiedIdentifier == SpecifiedIdentifier.SPECIFIED

    val expectSpecifiedMatch =
        expectedToUpdate || senderIsSpecifiedIdentifier == receiverIsSpecifiedIdentifier

    val expectedBehaviourDescription = if (expectedToUpdate) "Updates" else "Skips"

    fun <V, P : Property<V, P>> organize(old: P, new: P): Pair<P, P> =
        senderNewOldIdentifier.organize(old, new)

    fun <V, P : Property<V, P>> testSpecifiedMatch(receiver: P, sender: P) {
        if (expectSpecifiedMatch) {
            receiver.hasValue shouldBeEqual sender.hasValue
        } else {
            receiver.hasValue shouldNotBeEqual sender.hasValue
        }
    }

    fun <V, P : Property<V, P>> testValueMatch(receiver: P, sender: P) {
        if (expectedToUpdate || (expectSpecifiedMatch && !expectReceiverSpecified)) {
            receiver.value shouldBe sender.value
        } else {
            receiver.value shouldNotBe sender.value
        }
    }
}
