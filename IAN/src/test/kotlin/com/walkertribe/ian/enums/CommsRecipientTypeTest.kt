package com.walkertribe.ian.enums

import com.walkertribe.ian.util.BoolState
import com.walkertribe.ian.vesseldata.VesselData
import com.walkertribe.ian.world.ArtemisBase
import com.walkertribe.ian.world.ArtemisBlackHole
import com.walkertribe.ian.world.ArtemisCreature
import com.walkertribe.ian.world.ArtemisMine
import com.walkertribe.ian.world.ArtemisNpc
import com.walkertribe.ian.world.ArtemisObject
import com.walkertribe.ian.world.ArtemisPlayer
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.unmockkAll

class CommsRecipientTypeTest : DescribeSpec({
    val vesselData = mockk<VesselData>(relaxed = true)

    afterSpec {
        clearAllMocks()
        unmockkAll()
    }

    describe("Valid recipients") {
        data class FromObjectTestCase(
            val recipient: ArtemisObject,
            val expectedRecipientType: CommsRecipientType,
        )

        val allTestCases = listOf(
            FromObjectTestCase(ArtemisPlayer(0, 0L), CommsRecipientType.PLAYER),
            FromObjectTestCase(ArtemisBase(0, 0L), CommsRecipientType.BASE),
            FromObjectTestCase(
                ArtemisNpc(0, 0L).apply { isEnemy.value = BoolState.True },
                CommsRecipientType.ENEMY,
            ),
            FromObjectTestCase(ArtemisNpc(0, 0L), CommsRecipientType.OTHER),
        )
        allTestCases.map { it.expectedRecipientType } shouldContainExactlyInAnyOrder
            CommsRecipientType.entries

        withData(nameFn = { it.expectedRecipientType.name }, allTestCases) {
            val actualRecipientType = CommsRecipientType(it.recipient, vesselData)
            actualRecipientType.shouldNotBeNull()
            actualRecipientType shouldBeEqual it.expectedRecipientType
        }
    }

    describe("Invalid recipients") {
        withData(
            nameFn = { it.javaClass.simpleName },
            ArtemisBlackHole(0, 0L),
            ArtemisCreature(0, 0L),
            ArtemisMine(0, 0L),
        ) {
            CommsRecipientType(it, vesselData).shouldBeNull()
        }
    }
})
