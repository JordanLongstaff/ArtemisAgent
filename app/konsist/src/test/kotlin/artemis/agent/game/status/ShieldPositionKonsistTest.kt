package artemis.agent.game.status

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withName
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData

class ShieldPositionKonsistTest :
    DescribeSpec({
        val statusPackage = Konsist.scopeFromPackage("artemis.agent.game.status", "app")
        val shieldPositionClasses = statusPackage.classes().withName("ShieldPosition")

        it("ShieldPosition is an enum") { shieldPositionClasses.assertTrue { it.hasEnumModifier } }

        it("ShieldPosition members are FRONT and REAR") {
            shieldPositionClasses.assertTrue { enumCls ->
                enumCls.enumConstants.joinToString { it.name } == "FRONT, REAR"
            }
        }

        describe("ShieldPosition members have string ID matching name") {
            shieldPositionClasses.forEach { enumCls ->
                withData(nameFn = { it.name }, enumCls.enumConstants) { enumConst ->
                    enumConst.assertTrue {
                        it.arguments.joinToString { arg -> arg.text } ==
                            "R.string.${it.name.lowercase()}_shield"
                    }
                }
            }
        }
    })
