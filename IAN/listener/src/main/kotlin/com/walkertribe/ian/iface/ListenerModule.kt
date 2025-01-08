package com.walkertribe.ian.iface

import kotlin.reflect.KClass

interface ListenerModule {
    fun onConnectionEvent(arg: ListenerArgument)

    fun onPacket(arg: ListenerArgument)

    fun onArtemisObject(arg: ListenerArgument)

    val acceptedTypes: Set<KClass<out ListenerArgument>>
}
