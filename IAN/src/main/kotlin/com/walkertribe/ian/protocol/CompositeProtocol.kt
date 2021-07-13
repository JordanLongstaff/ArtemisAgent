package com.walkertribe.ian.protocol

/**
 * Allows multiple AbstractProtocols to be combined into a single Protocol
 * implementation. Protocols added later have priority over earlier ones.
 * @author rjwut
 */
class CompositeProtocol : Protocol {
    private val list: MutableList<Protocol> = mutableListOf()

    /**
     * Adds this Protocol to the composite.
     */
    fun add(protocol: Protocol) {
        list.add(0, protocol)
    }

    override fun getFactory(type: Int, subtype: Byte?): PacketFactory<out ArtemisPacket>? {
        for (protocol in list) {
            val factory = protocol.getFactory(type, subtype)
            if (factory != null) {
                return factory
            }
        }
        return null
    }
}
