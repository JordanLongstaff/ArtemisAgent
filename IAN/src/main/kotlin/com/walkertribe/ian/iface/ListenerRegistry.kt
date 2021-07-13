package com.walkertribe.ian.iface

import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation

/**
 * Contains ListenerMethods to be invoked when a corresponding event occurs.
 * @author rjwut
 */
class ListenerRegistry {
    private val listeners: MutableList<ListenerFunction> = CopyOnWriteArrayList()

    /**
     * Registers all methods on the given Object that have the Listener
     * annotation with the registry.
     */
    fun register(obj: Any) {
        obj::class.declaredMemberFunctions.filter {
            it.findAnnotation<Listener>() != null
        }.forEach {
            listeners.add(ListenerFunction(obj, it))
        }
    }

    /**
     * Returns a List containing all the ListenerMethods which are interested in
     * objects of the given Class.
     */
    fun listeningFor(clazz: KClass<out ListenerArgument>): List<ListenerFunction> =
        listeners.filter { it.accepts(clazz) }

    /**
     * Notifies interested listeners about this event.
     */
    fun fire(event: ConnectionEvent) {
        listeners.forEach { it.offer(event) }
    }

    /**
     * Removes all listeners from the registry.
     */
    fun clear() {
        listeners.clear()
    }
}
