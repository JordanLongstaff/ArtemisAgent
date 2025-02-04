package com.walkertribe.ian.iface

/**
 * Annotation that marks methods which are to be invoked when certain events occur. To be eligible
 * to be a Listener, a method must: 1) be public, 2) have a void return type, and 3) have exactly
 * one argument of an accepted type (or any of their subtypes). The accepted types are:
 * * [Packet][com.walkertribe.ian.protocol.Packet]
 * * [ArtemisObject][com.walkertribe.ian.world.ArtemisObject]
 * * [ConnectionEvent][com.walkertribe.ian.iface.ConnectionEvent]
 *
 * Annotating the method alone is not enough to get notifications; you must register the object that
 * has the annotated method with the ArtemisNetworkInterface implementation that will be receiving
 * the packets or events.
 *
 * The Listener annotation is inherited; classes which override a superclass's listener method need
 * not use the Listener annotation themselves, although there is no harm in doing so.
 *
 * @author rjwut
 */
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
)
annotation class Listener // no properties
