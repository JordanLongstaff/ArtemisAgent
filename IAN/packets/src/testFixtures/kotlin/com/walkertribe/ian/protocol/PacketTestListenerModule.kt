package com.walkertribe.ian.protocol

import com.walkertribe.ian.iface.ListenerArgument
import com.walkertribe.ian.iface.ListenerFunction
import com.walkertribe.ian.world.ArtemisObject
import io.kotest.assertions.withClue
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.reflect.KClass

object PacketTestListenerModule : PacketListenerModule {
    val packets = mutableListOf<Packet.Server>()
    val objects = mutableListOf<ArtemisObject<*>>()

    override val acceptedTypes: Set<KClass<out ListenerArgument>> = setOf(Packet.Server::class)

    override val packetListeners: List<ListenerFunction<out Packet.Server>> =
        listOf(ListenerFunction(Packet.Server::class, packets::add))

    override fun onConnectionEvent(arg: ListenerArgument) {
        assert(false) { "onConnectionEvent should not be called" }
    }

    override fun onArtemisObject(arg: ListenerArgument) {
        objects.add(
            withClue({ "onArtemisObject cannot accept a ${arg::class}" }) {
                arg.shouldBeInstanceOf<ArtemisObject<*>>()
            }
        )
    }
}
