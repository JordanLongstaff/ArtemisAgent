package com.walkertribe.ian.world

import com.walkertribe.ian.enums.ObjectType
import io.kotest.matchers.booleans.*
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.floats.shouldBeNaN
import io.kotest.matchers.nulls.shouldBeNull

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
    x: Float,
    y: Float,
    z: Float,
) {
    this.id shouldBeEqual id
    this.type shouldBeEqual type
    this.x shouldContainValue x
    this.y shouldContainValue y
    this.z shouldContainValue z
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
    name: String,
    x: Float,
    y: Float,
    z: Float,
    hullId: Int,
    shieldsFront: Float,
    shieldsFrontMax: Float,
) {
    shouldBeKnownObject(id, type, x, y, z)

    this.name shouldContainValue name
    this.hullId shouldContainValue hullId
    this.shieldsFront.strength shouldContainValue shieldsFront
    this.shieldsFront.maxStrength shouldContainValue shieldsFrontMax
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
    name: String,
    x: Float,
    y: Float,
    z: Float,
    hullId: Int,
    shieldsFront: Float,
    shieldsFrontMax: Float,
    shieldsRear: Float,
    shieldsRearMax: Float,
    impulse: Float,
    side: Byte,
) {
    shouldBeKnownObject(id, type, name, x, y, z, hullId, shieldsFront, shieldsFrontMax)

    this.shieldsRear.strength shouldContainValue shieldsRear
    this.shieldsRear.maxStrength shouldContainValue shieldsRearMax
    this.impulse shouldContainValue impulse
    this.side shouldContainValue side
}

internal fun BaseArtemisShip.Dsl<*>.shouldBeReset() {
    (this as BaseArtemisShielded.Dsl<*>).shouldBeReset()

    this.shieldsRear.shouldBeNaN()
    this.shieldsRearMax.shouldBeNaN()
    this.impulse.shouldBeNaN()
    this.side shouldBeEqual -1
}
