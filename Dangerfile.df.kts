@file:DependsOn("io.github.ackeecz:danger-kotlin-detekt:0.1.4")
@file:ImportDirectory("danger")

import io.github.ackeecz.danger.detekt.DetektPlugin
import java.io.File
import systems.danger.kotlin.Danger
import systems.danger.kotlin.danger
import systems.danger.kotlin.warn

Danger register DetektPlugin

danger(args) {
    applyRules()

    val detektReport = File("detekt.xml")
    if (detektReport.exists()) {
        DetektPlugin.parseAndReport(detektReport)
    } else {
        warn(":see_no_evil: No detekt report found")
    }
}
