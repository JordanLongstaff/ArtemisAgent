package com.walkertribe.ian.iface

import kotlin.reflect.KClass

class TestListenerModule(private val listeners: List<ListenerFunction<out ListenerArgument>>) :
    ListenerModule {
    override val acceptedTypes: Set<KClass<out ListenerArgument>> = setOf(ListenerArgument::class)

    override fun onArtemisObject(arg: ListenerArgument) {
        fireAllListeners(arg)
    }

    override fun onConnectionEvent(arg: ListenerArgument) {
        fireAllListeners(arg)
    }

    override fun onPacket(arg: ListenerArgument) {
        fireAllListeners(arg)
    }

    private fun fireAllListeners(arg: ListenerArgument) {
        listeners.forEach { it.offer(arg) }
    }
}
