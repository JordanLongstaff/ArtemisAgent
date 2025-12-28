package artemis.agent.game.status

import com.walkertribe.ian.enums.OrdnanceType
import io.kotest.core.spec.style.scopes.DescribeSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.engine.names.WithDataTestName
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.property.checkAll

sealed class StatusInfoTestCategory<SI : StatusInfo, TC : StatusInfoTestCase<SI, TC>> :
    WithDataTestName {
    data object Empty : StatusInfoTestCategory<StatusInfo.Empty, StatusInfoTestCase.Empty>() {
        override val items: List<StatusInfoTestCase.Empty> = listOf(StatusInfoTestCase.Empty)

        override suspend fun describeItemNotEqualsTests(
            scope: DescribeSpecContainerScope,
            index: Int,
        ) {
            // No tests
        }
    }

    data object Header : StatusInfoTestCategory<StatusInfo.Header, StatusInfoTestCase.Header>() {
        override val items: List<StatusInfoTestCase.Header> =
            listOf(
                StatusInfoTestCase.Header.ShieldsActive,
                StatusInfoTestCase.Header.ShieldsInactive,
                StatusInfoTestCase.Header.Damages,
            )

        override suspend fun describeItemNotEqualsTests(
            scope: DescribeSpecContainerScope,
            index: Int,
        ) {
            scope.it("Does not equal different Header") {
                val item = items[index]
                items.forEachIndexed { index2, other ->
                    if (index == index2) return@forEachIndexed

                    item.statusInfoNotEqualsGen(other).checkAll { (a, b) ->
                        a.itemEquals(b).shouldBeFalse()
                    }
                }
            }
        }
    }

    data object Energy : StatusInfoTestCategory<StatusInfo.Energy, StatusInfoTestCase.Energy>() {
        override val items: List<StatusInfoTestCase.Energy> = listOf(StatusInfoTestCase.Energy)

        override fun equalsLabel(item: StatusInfoTestCase.Energy): String =
            "Energy regardless of value"

        override suspend fun describeItemNotEqualsTests(
            scope: DescribeSpecContainerScope,
            index: Int,
        ) {
            // No tests
        }
    }

    data object Shield : StatusInfoTestCategory<StatusInfo.Shield, StatusInfoTestCase.Shield>() {
        override val items: List<StatusInfoTestCase.Shield> =
            ShieldPosition.entries.map { StatusInfoTestCase.Shield(it) }

        override fun equalsLabel(item: StatusInfoTestCase.Shield): String =
            "Shield: ${item.shieldPosition}"

        override suspend fun describeItemNotEqualsTests(
            scope: DescribeSpecContainerScope,
            index: Int,
        ) {
            val otherShield = items[items.lastIndex - index]
            scope.it("Does not equal Shield: ${otherShield.shieldPosition}") {
                items[index].statusInfoNotEqualsGen(otherShield).checkAll { (a, b) ->
                    a.itemEquals(b).shouldBeFalse()
                }
            }
        }
    }

    data object OrdnanceCount :
        StatusInfoTestCategory<StatusInfo.OrdnanceCount, StatusInfoTestCase.OrdnanceCount>() {
        override val items: List<StatusInfoTestCase.OrdnanceCount> =
            OrdnanceType.entries.map { StatusInfoTestCase.OrdnanceCount(it) }

        override fun equalsLabel(item: StatusInfoTestCase.OrdnanceCount): String =
            "OrdnanceCount: ${item.ordnanceType.name}"

        override suspend fun describeItemNotEqualsTests(
            scope: DescribeSpecContainerScope,
            index: Int,
        ) {
            scope.describe("Does not equal different OrdnanceCount") {
                val item = items[index]
                items.forEachIndexed { index2, other ->
                    if (index == index2) return@forEachIndexed
                    it("${other.ordnanceType}") {
                        item.statusInfoNotEqualsGen(other).checkAll { (a, b) ->
                            a.itemEquals(b).shouldBeFalse()
                        }
                    }
                }
            }
        }
    }

    data object Singleseat :
        StatusInfoTestCategory<StatusInfo.Singleseat, StatusInfoTestCase.Singleseat>() {
        override val items: List<StatusInfoTestCase.Singleseat> =
            listOf(
                StatusInfoTestCase.Singleseat.Docked,
                StatusInfoTestCase.Singleseat.Launched,
                StatusInfoTestCase.Singleseat.Lost,
            )

        override fun equalsLabel(item: StatusInfoTestCase.Singleseat): String =
            "Singleseat: ${item.expectedText}"

        override suspend fun describeItemNotEqualsTests(
            scope: DescribeSpecContainerScope,
            index: Int,
        ) {
            scope.it("Does not equal different Singleseat") {
                val item = items[index]
                items.forEachIndexed { index2, other ->
                    if (index == index2) return@forEachIndexed

                    item.statusInfoNotEqualsGen(other).checkAll { (a, b) ->
                        a.itemEquals(b).shouldBeFalse()
                    }
                }
            }
        }
    }

    data object DamageReport :
        StatusInfoTestCategory<StatusInfo.DamageReport, StatusInfoTestCase.DamageReport>() {
        override val items: List<StatusInfoTestCase.DamageReport> =
            listOf(StatusInfoTestCase.DamageReport)

        override fun equalsLabel(item: StatusInfoTestCase.DamageReport): String =
            "DamageReport with same system label"

        override suspend fun describeItemNotEqualsTests(
            scope: DescribeSpecContainerScope,
            index: Int,
        ) {
            scope.it("Does not equal DamageReport with different label") {
                val item = items[index]
                item.statusInfoNotEqualsGen(item).checkAll { (a, b) ->
                    a.itemEquals(b).shouldBeFalse()
                }
            }
        }
    }

    abstract val items: List<TC>

    open fun equalsLabel(item: TC): String = toString()

    suspend fun describeTextTests(scope: DescribeSpecContainerScope) {
        if (items.size <= 1) {
            items.forEach { item -> item.testText() }
        } else {
            scope.withData(items) { item -> item.testText() }
        }
    }

    suspend fun describeItemEqualsTests(scope: DescribeSpecContainerScope) {
        if (items.size <= 1) {
            describeItemEqualsTests(scope, 0)
        } else {
            items.forEachIndexed { index, item ->
                scope.describe(item.dataTestName()) { describeItemEqualsTests(this, index) }
            }
        }
    }

    private suspend fun describeItemEqualsTests(scope: DescribeSpecContainerScope, index: Int) {
        val item = items[index]

        ALL.forEach { other ->
            if (other == this) return@forEach

            scope.it("Does not equal $other") {
                other.items.forEach { otherItem ->
                    checkAll(item.statusInfoGen, otherItem.statusInfoGen) { first, second ->
                        first.itemEquals(second).shouldBeFalse()
                    }
                }
            }
        }

        scope.it("Equals ${equalsLabel(item)}") {
            item.statusInfoEqualsGen.checkAll { (a, b) -> a.itemEquals(b).shouldBeTrue() }
        }

        describeItemNotEqualsTests(scope, index)
    }

    abstract suspend fun describeItemNotEqualsTests(scope: DescribeSpecContainerScope, index: Int)

    override fun dataTestName(): String = toString()

    companion object {
        val ALL = listOf(Empty, Header, Energy, Shield, OrdnanceCount, Singleseat, DamageReport)
    }
}
