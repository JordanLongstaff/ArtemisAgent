package com.walkertribe.ian.enums

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldNotBeEmpty

class EnumsTest :
    DescribeSpec({
        withData(
            nameFn = { it.first },
            "AlertStatus" to AlertStatus.entries,
            "AudioCommand" to AudioCommand.entries,
            "CommsRecipientType" to CommsRecipientType.entries,
            "DriveType" to DriveType.entries,
            "GameType" to GameType.entries,
            "IntelType" to IntelType.entries,
            "TubeState" to TubeState.entries,
        ) {
            it.second.shouldNotBeEmpty()
        }
    })
