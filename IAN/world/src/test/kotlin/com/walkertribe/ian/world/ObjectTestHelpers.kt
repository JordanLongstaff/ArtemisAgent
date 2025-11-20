package com.walkertribe.ian.world

import com.walkertribe.ian.enums.ObjectType
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.floats.shouldBeNaN
import io.kotest.matchers.nulls.shouldBeNull

data class ShieldStrength(val strength: Float, val maxStrength: Float)

data class BaseArtemisObjectData(val x: Float, val y: Float, val z: Float)

data class BaseArtemisShieldedData(
    val name: String,
    val hullId: Int,
    val shieldsFront: ShieldStrength,
)

data class BaseArtemisShipData(val shieldsRear: ShieldStrength, val impulse: Float, val side: Byte)

internal fun BaseArtemisObject<*>.shouldBeUnknownObject(id: Int, type: ObjectType) {
    this.id shouldBeEqual id
    this.type shouldBeEqual type
    this.x.shouldBeUnspecified()
    this.y.shouldBeUnspecified()
    this.z.shouldBeUnspecified()
    this.hasPosition.shouldBeFalse()
    this.hasData.shouldBeFalse()
}

internal fun BaseArtemisObject<*>.shouldBeKnownObject(
    id: Int,
    type: ObjectType,
    baseData: BaseArtemisObjectData,
) {
    this.id shouldBeEqual id
    this.type shouldBeEqual type
    this.x shouldContainValue baseData.x
    this.y shouldContainValue baseData.y
    this.z shouldContainValue baseData.z
    this.hasPosition.shouldBeTrue()
    this.hasData.shouldBeTrue()
}

internal fun BaseArtemisObject.Dsl<*>.shouldBeReset() {
    this.x.shouldBeNaN()
    this.y.shouldBeNaN()
    this.z.shouldBeNaN()
}

internal fun BaseArtemisShielded<*>.shouldBeUnknownObject(id: Int, type: ObjectType) {
    (this as BaseArtemisObject<*>).shouldBeUnknownObject(id, type)

    this.name.shouldBeUnspecified()
    this.hullId.shouldBeUnspecified()
    this.shieldsFront.strength.shouldBeUnspecified()
    this.shieldsFront.maxStrength.shouldBeUnspecified()
}

internal fun BaseArtemisShielded<*>.shouldBeKnownObject(
    id: Int,
    type: ObjectType,
    baseData: BaseArtemisObjectData,
    shieldedData: BaseArtemisShieldedData,
) {
    shouldBeKnownObject(id, type, baseData)

    this.name shouldContainValue shieldedData.name
    this.hullId shouldContainValue shieldedData.hullId
    this.shieldsFront.strength shouldContainValue shieldedData.shieldsFront.strength
    this.shieldsFront.maxStrength shouldContainValue shieldedData.shieldsFront.maxStrength
}

internal fun BaseArtemisShielded.Dsl<*>.shouldBeReset() {
    (this as BaseArtemisObject.Dsl<*>).shouldBeReset()

    this.name.shouldBeNull()
    this.hullId shouldBeEqual -1
    this.shieldsFront.shouldBeNaN()
    this.shieldsFrontMax.shouldBeNaN()
}

internal fun BaseArtemisShip<*>.shouldBeUnknownObject(id: Int, type: ObjectType) {
    (this as BaseArtemisShielded<*>).shouldBeUnknownObject(id, type)

    this.shieldsRear.strength.shouldBeUnspecified()
    this.shieldsRear.maxStrength.shouldBeUnspecified()
    this.impulse.shouldBeUnspecified()
    this.side.shouldBeUnspecified()
}

internal fun BaseArtemisShip<*>.shouldBeKnownObject(
    id: Int,
    type: ObjectType,
    baseData: BaseArtemisObjectData,
    shieldedData: BaseArtemisShieldedData,
    shipData: BaseArtemisShipData,
) {
    shouldBeKnownObject(id, type, baseData, shieldedData)

    this.shieldsRear.strength shouldContainValue shipData.shieldsRear.strength
    this.shieldsRear.maxStrength shouldContainValue shipData.shieldsRear.maxStrength
    this.impulse shouldContainValue shipData.impulse
    this.side shouldContainValue shipData.side
}

internal fun BaseArtemisShip.Dsl<*>.shouldBeReset() {
    (this as BaseArtemisShielded.Dsl<*>).shouldBeReset()

    this.shieldsRear.shouldBeNaN()
    this.shieldsRearMax.shouldBeNaN()
    this.impulse.shouldBeNaN()
    this.side shouldBeEqual -1
}
