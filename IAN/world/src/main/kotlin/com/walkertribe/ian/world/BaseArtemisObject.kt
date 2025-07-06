package com.walkertribe.ian.world

import com.walkertribe.ian.iface.ListenerModule
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/** Base implementation for all ArtemisObjects. */
abstract class BaseArtemisObject<T : ArtemisObject<T>>(
    final override val id: Int,
    final override val timestamp: Long,
) : ArtemisObject<T> {
    override val x = Property.FloatProperty(timestamp)
    override val y = Property.FloatProperty(timestamp)
    override val z = Property.FloatProperty(timestamp)

    /** Returns true if this object contains any data. */
    internal open val hasData: Boolean
        get() = x.hasValue || y.hasValue || z.hasValue

    override val hasPosition: Boolean
        get() = x.hasValue && z.hasValue

    override fun distanceTo(other: ArtemisObject<*>): Float {
        check(hasPosition && other.hasPosition) { cannotCompute(DISTANCE) }

        val y0 = other.y.valueOrZero
        val y1 = y.valueOrZero
        val dX = other.x.value.toDouble() - x.value.toDouble()
        val dY = y0.toDouble() - y1.toDouble()
        val dZ = other.z.value.toDouble() - z.value.toDouble()

        return when {
            dX == 0.0 && dY == 0.0 -> abs(dZ)
            dX == 0.0 && dZ == 0.0 -> abs(dY)
            dY == 0.0 && dZ == 0.0 -> abs(dX)
            else -> sqrt(dX * dX + dY * dY + dZ * dZ)
        }.toFloat()
    }

    override fun distanceSquaredTo(other: ArtemisObject<*>): Float {
        check(hasPosition && other.hasPosition) { cannotCompute(DISTANCE) }

        val y0 = other.y.valueOrZero
        val y1 = y.valueOrZero
        val dX = other.x.value.toDouble() - x.value.toDouble()
        val dY = y0.toDouble() - y1.toDouble()
        val dZ = other.z.value.toDouble() - z.value.toDouble()
        return (dX * dX + dY * dY + dZ * dZ).toFloat()
    }

    override fun horizontalDistanceTo(other: ArtemisObject<*>): Float {
        check(hasPosition && other.hasPosition) { cannotCompute(DISTANCE) }

        val dX = other.x.value.toDouble() - x.value.toDouble()
        val dZ = other.z.value.toDouble() - z.value.toDouble()

        return when {
            dX == 0.0 -> abs(dZ)
            dZ == 0.0 -> abs(dX)
            else -> sqrt(dX * dX + dZ * dZ)
        }.toFloat()
    }

    override fun horizontalDistanceSquaredTo(other: ArtemisObject<*>): Float {
        check(hasPosition && other.hasPosition) { cannotCompute(DISTANCE) }

        val dX = other.x.value.toDouble() - x.value.toDouble()
        val dZ = other.z.value.toDouble() - z.value.toDouble()
        return (dX * dX + dZ * dZ).toFloat()
    }

    override fun headingTo(other: ArtemisObject<*>): Float {
        check(hasPosition && other.hasPosition) { cannotCompute(HEADING) }

        val dX = other.x.value - x.value
        val dZ = other.z.value - z.value
        return (atan2(dX, dZ) * RAD_TO_DEG + HALF_CIRCLE) % FULL_CIRCLE
    }

    override fun updates(other: T) {
        x updates other.x
        y updates other.y
        z updates other.z
    }

    final override fun offerTo(module: ListenerModule) {
        module.onArtemisObject(this)
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is ArtemisObject<*> && id == other.id && type == other.type)

    override fun hashCode(): Int = id

    abstract class Dsl<T : BaseArtemisObject<T>> {
        var x: Float = Float.NaN
        var y: Float = Float.NaN
        var z: Float = Float.NaN

        fun build(id: Int, timestamp: Long): T = create(id, timestamp).also(this::updates)

        protected abstract fun create(id: Int, timestamp: Long): T

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

    private companion object {
        const val HALF_CIRCLE = 180f
        const val FULL_CIRCLE = HALF_CIRCLE * 2f
        const val RAD_TO_DEG = (HALF_CIRCLE / PI).toFloat()

        const val DISTANCE = "distance"
        const val HEADING = "heading"

        fun cannotCompute(type: String): String =
            "Can't compute $type if both objects don't have a position"
    }
}
