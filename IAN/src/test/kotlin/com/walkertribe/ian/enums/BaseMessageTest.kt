package com.walkertribe.ian.enums

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual

class BaseMessageTest : DescribeSpec({
    describe("Base message") {
        withData(
            nameFn = { it.toString() },
            listOf(
                BaseMessage.StandByForDockingOrCeaseOperation,
                BaseMessage.PleaseReportStatus,
            ) + OrdnanceType.entries.map(BaseMessage.Build::invoke)
        ) {
            it.recipientType shouldBeEqual CommsRecipientType.BASE
        }
    }

    describe("Build ordnance message") {
        withData(OrdnanceType.entries.toList()) {
            val buildMessage = BaseMessage.Build(it)
            buildMessage.ordnanceType shouldBeEqual it
        }
    }
})
