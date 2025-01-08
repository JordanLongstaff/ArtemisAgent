package com.walkertribe.ian.world

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.iface.ListenerArgument

/**
 * This interface represents information about an object in the game world. It may contain all the
 * information known about that object, or just updates. Every object has the following properties:
 * * an ID
 * * a type
 * * a position (x, y, z)
 *
 * Many objects also have a name, but not all of them do, and the name is not guaranteed to be
 * unique. However, any one update is only guaranteed to specify the ID.
 *
 * <h2>Unspecified properties vs. unknown properties</h2>
 * A property is unspecified if no value has been given for it. Since object update packets
 * typically contain values for properties which have changed, other properties will be unspecified.
 * To avoid instantiating a lot of objects, special values are used to indicate whether a primitive
 * property is unspecified. The documentation for each property's accessor method will tell you what
 * that value is. The "unspecified" value depends on the property's type and what its permissible
 * values are:
 *
 * <dl> <dt>BoolState</dt> <dd>BoolState.Unknown</dd> <dt>Other Objects</dt> <dd>null</dd>
 * <dt>float</dt> <dd>Float.NaN</dd> <dt>Other numeric primitives</dt> <dd>-1, or the type's
 * MIN_VALUE if -1 is a permissible value for that property</dd></dl>
 *
 * An unknown property is one whose purpose is currently unknown. It may have a specified value, but
 * we don't know what that value means. IAN is capable of tracking unknown property values, but this
 * capability is really only useful for people who are trying to determine what these properties
 * mean.
 *
 * <h2>Updating objects</h2>
 * The ObjectUpdatePacket produces objects which implement this interface. These objects will
 * contain only the property values that were updated by that packet; all other values will be
 * unspecified. You can use the updateFrom() method to transfer all specified properties from one
 * object to another; this allows you to keep around a single instance that always has the latest
 * known state for that world object.
 *
 * <h2>Object positions</h2>
 * A sector is a three-dimensional rectangular prism. From the perspective of a ship with a heading
 * of 0 degrees, the X axis runs from port to starboard, the Y axis runs up and down, and the Z axis
 * runs bow to stern. The boundaries of the sector are (0, 500, 0) (top northeast corner) to
 * (100000, -500, 100000) (bottom southwest corner). However, some objects, such as asteroids and
 * nebulae, may lie outside these bounds.
 *
 * @author dhleong
 */
interface ArtemisObject<T : ArtemisObject<T>> : ListenerArgument {
    /** The object's unique identifier. This property should always be specified. */
    val id: Int

    /** The object's type. */
    val type: ObjectType

    /** The object's position along the X-axis. Unspecified: Float.NaN */
    val x: Property.FloatProperty

    /** The object's position along the Y-axis Unspecified: Float.NaN */
    val y: Property.FloatProperty

    /** The object's position along the Z-axis Unspecified: Float.NaN */
    val z: Property.FloatProperty

    /**
     * Returns true if this object's coordinates are specified. Note that objects which start out at
     * `y=0` and have never deviated from it will have an undefined `y` property. If `y` is
     * undefined, 0 will be assumed and this will still return true if `x` and `z` are defined.
     */
    val hasPosition: Boolean

    /**
     * Returns the distance between this object and the given object. This method will throw an
     * [IllegalStateException] if either object's [hasPosition] would return false. If the
     * Y-coordinate for an object is undefined, 0 is assumed.
     */
    infix fun distanceTo(other: ArtemisObject<*>): Float

    /**
     * Returns the square of the distance between this object and the given object. This method will
     * throw an [IllegalStateException] if either object's [hasPosition] would return false. If the
     * Y-coordinate for an object is undefined, 0 is assumed.
     *
     * This function neglects a square-root operation required to calculate the distance, making it
     * a more efficient calculation. Use [distanceTo] if you actually need the distance instead of
     * its square.
     */
    infix fun distanceSquaredTo(other: ArtemisObject<*>): Float

    /**
     * Same as [distanceTo], but ignores the Y-axis, giving the distance between the objects from
     * the top-down perspective.
     */
    infix fun horizontalDistanceTo(other: ArtemisObject<*>): Float

    /**
     * Same as [distanceSquaredTo], but ignores the Y-axis, giving the square of the distance
     * between the objects from the top-down perspective.
     */
    infix fun horizontalDistanceSquaredTo(other: ArtemisObject<*>): Float

    /** Returns the direction this object would have to travel to reach the given object. */
    infix fun headingTo(other: ArtemisObject<*>): Float

    /**
     * Updates the given object's properties to match any updates provided by this object. If any
     * property of this object is unspecified, the given object's corresponding property will not be
     * updated.
     */
    infix fun updates(other: T)
}
