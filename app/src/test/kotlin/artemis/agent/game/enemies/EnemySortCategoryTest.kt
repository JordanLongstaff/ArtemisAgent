package artemis.agent.game.enemies

import android.content.Context
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.string
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk

class EnemySortCategoryTest :
    DescribeSpec({
        describe("EnemySortCategory") {
            val stringCount = 10000
            val strings = Arb.list(Arb.string(), stringCount..stringCount).next()
            val arbScrollIndex = Arb.int()

            val context =
                mockk<Context> { every { getString(any()) } answers { strings[firstArg()] } }

            afterSpec { clearMocks(context) }

            fun testCategoryType(create: (String, Int, Int) -> EnemySortCategory) {
                strings.forEachIndexed { index, string ->
                    val scrollIndex = arbScrollIndex.next()
                    val sortCategory = create(string, index, scrollIndex)
                    sortCategory.getString(context) shouldBeEqual string
                    sortCategory.scrollIndex shouldBeEqual scrollIndex
                }
            }

            it("Res") {
                testCategoryType { _, index, scrollIndex ->
                    EnemySortCategory.Res(index, scrollIndex)
                }
            }

            it("Text") {
                testCategoryType { text, _, scrollIndex ->
                    EnemySortCategory.Text(text, scrollIndex)
                }
            }
        }
    })
