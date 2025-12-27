package artemis.agent.game.status

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.mockk.clearAllMocks
import io.mockk.unmockkAll

class StatusInfoTest :
    DescribeSpec({
        describe("StatusInfo") {
            afterSpec {
                clearAllMocks()
                unmockkAll()
            }

            describe("Text") {
                withData(StatusInfoTestCategory.ALL) { category ->
                    category.describeTextTests(this)
                }
            }

            describe("Item equals") {
                withData(StatusInfoTestCategory.ALL) { category ->
                    category.describeItemEqualsTests(this)
                }
            }
        }
    })
