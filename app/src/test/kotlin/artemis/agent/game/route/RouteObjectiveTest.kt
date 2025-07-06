package artemis.agent.game.route

import com.walkertribe.ian.enums.OrdnanceType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual

class RouteObjectiveTest :
    DescribeSpec({
        describe("RouteObjective") {
            describe("Ordnance") {
                OrdnanceType.entries.forEachIndexed { index, ordnanceType ->
                    describe(ordnanceType.name) {
                        it("Hash code") {
                            RouteObjective.Ordnance(ordnanceType).hashCode() shouldBeEqual index
                        }
                    }
                }
            }

            describe("ReplacementFighters") {
                it("Hash code") { RouteObjective.ReplacementFighters.hashCode() shouldBeEqual 8 }
            }

            describe("Tasks") {
                it("Hash code") { RouteObjective.Tasks.hashCode() shouldBeEqual 9 }
            }
        }
    })
