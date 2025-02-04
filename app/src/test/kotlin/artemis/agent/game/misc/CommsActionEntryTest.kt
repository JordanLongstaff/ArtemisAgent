package artemis.agent.game.misc

import com.walkertribe.ian.util.JamCrc
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class CommsActionEntryTest :
    DescribeSpec({
        suspend fun testCommsActionEntry(test: (CommsActionEntry, String) -> Unit) {
            Arb.string().checkAll { label -> test(CommsActionEntry(label), label) }
        }

        describe("CommsActionEntry") {
            describe("Properties") {
                it("Label") {
                    testCommsActionEntry { entry, label -> entry.label shouldBeEqual label }
                }

                it("Hash code") {
                    testCommsActionEntry { entry, label ->
                        entry.hashCode() shouldBeEqual JamCrc.compute(label)
                    }
                }
            }
        }
    })
