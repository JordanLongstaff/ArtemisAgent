package artemis.agent.game.status

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withName
import com.lemonappdev.konsist.api.verify.assertFalse
import io.kotest.core.spec.style.DescribeSpec

class StatusInfoKonsistTest :
    DescribeSpec({
        val statusPackage = Konsist.scopeFromPackage("artemis.agent.game.status", "app", "main")
        val shieldClasses = statusPackage.classes().withName("Shield")

        it("StatusInfo.Shield cannot be a data class") {
            shieldClasses.assertFalse { cls -> cls.hasDataModifier }
        }
    })
