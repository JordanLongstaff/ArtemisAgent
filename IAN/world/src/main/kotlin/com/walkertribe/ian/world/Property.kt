package com.walkertribe.ian.world

import com.walkertribe.ian.util.BoolState
import com.walkertribe.ian.util.isKnown

sealed class Property<V, P : Property<V, P>>(
    initialValue: V,
    initialTimestamp: Long,
    private var onSet: (V) -> Unit = {},
) {
    class FloatProperty(timestamp: Long, onSet: (Float) -> Unit = {}) :
        Property<Float, FloatProperty>(Float.NaN, timestamp, onSet), Comparable<FloatProperty> {
        override val hasValue: Boolean
            get() = !value.isNaN()

        val valueOrZero: Float
            get() = if (value.isNaN()) 0f else value

        override fun compareTo(other: FloatProperty): Int =
            when {
                !other.hasValue -> if (hasValue) 1 else 0
                hasValue -> compareValuesBy(this, other) { it.value }
                else -> -1
            }
    }

    class ByteProperty(
        timestamp: Long,
        private val initialValue: Byte = -1,
        onSet: (Byte) -> Unit = {},
    ) : Property<Byte, ByteProperty>(initialValue, timestamp, onSet), Comparable<ByteProperty> {
        override val hasValue: Boolean
            get() = value != initialValue

        override fun compareTo(other: ByteProperty): Int =
            when {
                !other.hasValue -> if (hasValue) 1 else 0
                hasValue -> compareValuesBy(this, other) { it.value }
                else -> -1
            }

        override fun checkCanUpdate(property: ByteProperty) {
            require(initialValue == property.initialValue) {
                "Property could not be updated: initial value mismatch; " +
                    "expected $initialValue, found ${property.initialValue}"
            }
        }
    }

    class IntProperty(
        timestamp: Long,
        private val initialValue: Int = -1,
        onSet: (Int) -> Unit = {},
    ) : Property<Int, IntProperty>(initialValue, timestamp, onSet), Comparable<IntProperty> {
        override val hasValue: Boolean
            get() = value != initialValue

        override fun compareTo(other: IntProperty): Int =
            when {
                !other.hasValue -> if (hasValue) 1 else 0
                hasValue -> compareValuesBy(this, other) { it.value }
                else -> -1
            }

        override fun checkCanUpdate(property: IntProperty) {
            require(initialValue == property.initialValue) {
                "Property could not be updated: initial value mismatch; " +
                    "expected $initialValue, found ${property.initialValue}"
            }
        }
    }

    class BoolProperty(timestamp: Long, onSet: (BoolState) -> Unit = {}) :
        Property<BoolState, BoolProperty>(BoolState.Unknown, timestamp, onSet) {
        override val hasValue: Boolean
            get() = value.isKnown
    }

    class ObjectProperty<V : Any>(timestamp: Long, onSet: (V?) -> Unit = {}) :
        Property<V?, ObjectProperty<V>>(null, timestamp, onSet) {
        override val hasValue: Boolean
            get() = value != null
    }

    var value: V = initialValue
        set(newValue) {
            field = newValue
            onSet(newValue)
        }

    private var timestamp: Long = initialTimestamp

    abstract val hasValue: Boolean

    internal fun addListener(onSet: (V) -> Unit) {
        this.onSet = onSet
    }

    protected open fun checkCanUpdate(property: P) {}

    infix fun updates(property: P) {
        updates(property) {}
    }

    internal fun updates(property: P, ifNotUpdated: () -> Unit) {
        synchronized(property) {
            checkCanUpdate(property)

            if (timestamp < property.timestamp) {
                return
            }

            if (hasValue) {
                property.timestamp = timestamp
                property.value = value
            } else {
                ifNotUpdated()
            }
        }
    }
}
