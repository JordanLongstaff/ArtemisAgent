package artemis.agent.util

import com.walkertribe.ian.util.Version
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.property.Arb
import io.kotest.property.arbitrary.positiveInt
import io.kotest.property.checkAll

class VersionStringTest :
    DescribeSpec({
        describe("VersionString") {
            it("Constructor") { checkAll<String> { VersionString(it).toString() shouldBeEqual it } }

            it("Convert to Version") {
                checkAll(Arb.positiveInt(), Arb.positiveInt(), Arb.positiveInt()) {
                    major,
                    minor,
                    patch ->
                    val versionString = "$major.$minor.$patch"
                    val expectedVersion = Version(major, minor, patch)
                    VersionString(versionString).toVersion() shouldBeEqual expectedVersion
                }
            }
        }
    })
