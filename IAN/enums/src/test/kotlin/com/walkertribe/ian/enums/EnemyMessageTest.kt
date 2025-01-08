package com.walkertribe.ian.enums

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual

class EnemyMessageTest :
    DescribeSpec({
        describe("Enemy message") {
            withData(EnemyMessage.entries.toList()) {
                it.recipientType shouldBeEqual CommsRecipientType.ENEMY
            }
        }

        describe("ID") {
            withData(nameFn = { "$it = ${it.ordinal}" }, EnemyMessage.entries.toList()) {
                it.id shouldBeEqual it.ordinal
            }
        }
    })
