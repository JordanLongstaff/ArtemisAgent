package com.walkertribe.ian.world

import com.walkertribe.ian.iface.ListenerArgument
import com.walkertribe.ian.iface.ListenerFunction
import kotlin.reflect.KClass

object ArtemisObjectTestModule : ArtemisObjectListenerModule {
    override val acceptedTypes: Set<KClass<out ListenerArgument>> = setOf(ArtemisObject::class)

    val collected = mutableListOf<ArtemisObject<*>>()

    override val artemisObjectListeners: List<ListenerFunction<out ArtemisObject<*>>> =
        listOf(ListenerFunction(ArtemisObject::class, collected::add))

    override fun onConnectionEvent(arg: ListenerArgument) {
        assert(false) { "onConnectionEvent should not be called" }
    }

    override fun onPacket(arg: ListenerArgument) {
        assert(false) { "onPacket should not be called" }
    }
}
