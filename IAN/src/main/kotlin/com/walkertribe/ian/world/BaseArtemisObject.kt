package com.walkertribe.ian.world

import kotlin.math.atan2
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.primaryConstructor

/**
 * Base implementation for all ArtemisObjects.
 */
abstract class BaseArtemisObject(
    final override val id: Int,
    final override val timestamp: Long
) : ArtemisObject {
    override val x = Property.FloatProperty(timestamp)
    override val y = Property.FloatProperty(timestamp)
    override val z = Property.FloatProperty(timestamp)

    override val hasPosition: Boolean get() = x.hasValue && z.hasValue

    abstract fun checkType(other: ArtemisObject)

    override fun distanceSquaredTo(other: ArtemisObject): Float {
        check(hasPosition && other.hasPosition) {
            "Can't compute distance if both objects don't have a position"
        }
        val y0 = other.y.valueOrZero
        val y1 = y.valueOrZero
        val dX = other.x.value - x.value
        val dY = y0 - y1
        val dZ = other.z.value - z.value
        return dX * dX + dY * dY + dZ * dZ
    }

    override fun horizontalDistanceSquaredTo(other: ArtemisObject): Float {
        check(hasPosition && other.hasPosition) {
            "Can't compute distance if both objects don't have a position"
        }
        val dX = other.x.value - x.value
        val dZ = other.z.value - z.value
        return dX * dX + dZ * dZ
    }

    override fun headingTo(other: ArtemisObject): Float {
        val dX = other.x.value - x.value
        val dZ = other.z.value - z.value
        return atan2(dX, dZ)
    }

    override fun updates(other: ArtemisObject) {
        checkType(other)

        updateProp(other, ArtemisObject::x)
        updateProp(other, ArtemisObject::y)
        updateProp(other, ArtemisObject::z)
    }

    /**
     * Returns true if this object contains any data.
     */
    protected open val hasData: Boolean get() = x.hasValue || y.hasValue || z.hasValue

    override fun equals(other: Any?): Boolean =
        this === other || (other is ArtemisObject && id == other.id)

    override fun hashCode(): Int = id

    internal open class Dsl<T : BaseArtemisObject>(private val objectClass: KClass<T>) {
        var x: Float = Float.NaN
        var y: Float = Float.NaN
        var z: Float = Float.NaN

        fun create(id: Int, timestamp: Long): T = checkNotNull(objectClass.primaryConstructor) {
            "${objectClass.java} must have a primary constructor"
        }.call(id, timestamp).also(this::updates)

        protected open fun isObjectEmpty(obj: T): Boolean = !obj.hasData

        open infix fun updates(obj: T) {
            require(isObjectEmpty(obj)) { "Cannot apply Dsl to an already-populated object" }

            obj.x.value = x
            obj.y.value = y
            obj.z.value = z

            x = Float.NaN
            y = Float.NaN
            z = Float.NaN
        }
    }
}

internal fun <Obj : ArtemisObject, V> Obj.updateProp(
    other: Obj,
    prop: KProperty1<Obj, Property<V>>,
    ifNotUpdated: () -> Unit = { }
) {
    prop.get(this).updates(prop.get(other), ifNotUpdated)
}
