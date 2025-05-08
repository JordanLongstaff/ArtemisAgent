package artemis.agent.generic

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll

class GenericDataEntryTest :
    DescribeSpec({
        describe("GenericDataEntry") {
            describe("Constructor") {
                it("Without data") {
                    checkAll<String> { name ->
                        val entry = GenericDataEntry(name)
                        entry.name shouldBe name
                        entry.data.shouldBeNull()
                    }

                    checkAll<String, String?> { name, data ->
                        val entry = GenericDataEntry(name, data)
                        entry.name shouldBe name
                        entry.data shouldBe data
                    }
                }
            }
        }
    })
