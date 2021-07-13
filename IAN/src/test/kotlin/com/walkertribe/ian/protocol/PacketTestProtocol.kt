package com.walkertribe.ian.protocol

import kotlin.reflect.KClass

class PacketTestProtocol<T : ArtemisPacket>(val packetClass: KClass<T>) : AbstractProtocol() {
    init {
        register(packetClass)
    }

    companion object {
        inline operator fun <reified T : ArtemisPacket> invoke(): PacketTestProtocol<T> =
            PacketTestProtocol(T::class)
    }
}
