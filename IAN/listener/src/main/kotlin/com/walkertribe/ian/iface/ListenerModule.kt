package com.walkertribe.ian.iface

import kotlin.reflect.KClass

interface ListenerModule {
    val acceptedTypes: Set<KClass<out ListenerArgument>>

    fun onConnectionEvent(arg: ListenerArgument)

    fun onPacket(arg: ListenerArgument)

    fun onArtemisObject(arg: ListenerArgument)
}
