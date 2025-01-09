package com.walkertribe.ian.iface

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketListenerModule
import com.walkertribe.ian.world.ArtemisObject
import com.walkertribe.ian.world.ArtemisObjectListenerModule
import kotlin.reflect.KClass

data class CompositeListenerModule(
    override val connectionEventListeners: List<ListenerFunction<out ConnectionEvent>>,
    override val packetListeners: List<ListenerFunction<out Packet.Server>>,
    override val artemisObjectListeners: List<ListenerFunction<out ArtemisObject<*>>>,
) : ConnectionEventListenerModule, PacketListenerModule, ArtemisObjectListenerModule {
    override val acceptedTypes: Set<KClass<out ListenerArgument>> by lazy {
        (connectionEventListeners + packetListeners + artemisObjectListeners)
            .map { it.argumentClass }
            .toSet()
    }

    constructor(
        allListeners: List<ListenerFunction<out ListenerArgument>>
    ) : this(
        connectionEventListeners =
            allListeners.mapNotNull { it.checkIfAccepting(ConnectionEvent::class) },
        packetListeners = allListeners.mapNotNull { it.checkIfAccepting(Packet.Server::class) },
        artemisObjectListeners =
            allListeners.mapNotNull { it.checkIfAccepting(ArtemisObject::class) },
    )
}
