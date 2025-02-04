package com.walkertribe.ian.iface

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Contains [ListenerModule]s to be invoked when a corresponding event occurs.
 *
 * @author rjwut
 */
class ListenerRegistry {
    private val listeners: MutableStateFlow<List<ListenerModule>> = MutableStateFlow(listOf())

    /** Registers all functions in the given [ListenerModule] with the registry. */
    fun register(module: ListenerModule) {
        listeners.value += module
    }

    /**
     * Returns a [List] containing all the [ListenerModule]s which are interested in objects of the
     * given typey.
     */
    fun <T : ListenerArgument> listeningFor(kClass: KClass<T>): List<ListenerModule> =
        listeners.value.filter { it.acceptedTypes.any(kClass::isSubclassOf) }

    /** Notifies interested listeners about this argument. */
    fun offer(argument: ListenerArgument) {
        listeners.value.forEach(argument::offerTo)
    }

    /** Removes all listeners from the registry. */
    fun clear() {
        listeners.value = listOf()
    }
}
