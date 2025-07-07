package artemis.agent.generic

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.checkAll

class GenericDataDiffUtilCallbackTest :
    DescribeSpec({
        describe("GenericDataDiffUtilCallback") {
            val firstEntry = GenericDataEntry("A")
            val secondEntry = GenericDataEntry("B", "C")
            val thirdEntry = GenericDataEntry("D", "E")

            val oldList = listOf(firstEntry, secondEntry)
            val newList = listOf(secondEntry, firstEntry, thirdEntry)

            val callback = GenericDataDiffUtilCallback(oldList, newList)

            describe("List sizes") {
                it("Old") { callback.oldListSize shouldBeEqual oldList.size }
                it("New") { callback.newListSize shouldBeEqual newList.size }
            }

            it("Contents are always the same") {
                checkAll<Int, Int> { old, new ->
                    callback.areContentsTheSame(old, new).shouldBeTrue()
                }
            }

            it("Check if items are the same") {
                repeat(oldList.size) { oldIndex ->
                    repeat(newList.size) { newIndex ->
                        val same = newIndex == 1 - oldIndex
                        callback.areItemsTheSame(oldIndex, newIndex) shouldBeEqual same
                    }
                }
            }
        }
    })
