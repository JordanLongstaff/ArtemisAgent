package com.walkertribe.ian.enums

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual

class BaseMessageTest :
    DescribeSpec({
        val allBuildMessages =
            listOf(BaseMessage.StandByForDockingOrCeaseOperation, BaseMessage.PleaseReportStatus) +
                OrdnanceType.entries.map(BaseMessage.Build.Companion::invoke)

        describe("Base message") {
            withData(nameFn = { it.toString() }, allBuildMessages) {
                it.recipientType shouldBeEqual CommsRecipientType.BASE
            }
        }

        describe("ID") {
            allBuildMessages.forEachIndexed { index, message ->
                it("$message = $index") { message.id shouldBeEqual index }
            }
        }

        describe("Build ordnance message") {
            withData(OrdnanceType.entries.toList()) {
                val buildMessage = BaseMessage.Build(it)
                buildMessage.ordnanceType shouldBeEqual it
            }
        }
    })
