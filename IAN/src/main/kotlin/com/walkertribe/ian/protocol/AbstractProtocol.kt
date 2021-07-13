package com.walkertribe.ian.protocol

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.iface.PacketReader
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmName

/**
 * An abstract Protocol implementation which provides a mechanism to register
 * packet classes with the Packet annotation and retrieve them again. It
 * assumes that every packet has a constructor which takes a PacketReader.
 * @author rjwut
 */
abstract class AbstractProtocol : Protocol {
    private var registry = mutableMapOf<Factory.Key, Factory<out ArtemisPacket>>()

    /**
     * Invoked by the Protocol implementation to register a single packet
     * class. The class must have a Packet annotation and an accessible
     * constructor with a single PacketReader argument.
     */
    protected fun <T : ArtemisPacket> register(clazz: KClass<T>) {
        val factory = Factory(clazz)
        val anno = requireNotNull(clazz.findAnnotation<Packet>()) {
            "$clazz has no @Packet annotation"
        }
        require(anno.origin == Origin.SERVER) {
            "$clazz is not a server packet type"
        }
        val type = anno.getHash()
        val subtype = anno.subtype
        registry[Factory.Key(type, subtype.takeIf { it >= 0 })] = factory
    }

    override fun getFactory(type: Int, subtype: Byte?): PacketFactory<*>? {
        var factory = registry[Factory.Key(type, subtype)]
        if (factory == null && subtype != null) {
            // no factory found for that subtype; try without subtype
            factory = registry[Factory.Key(type, null)]
        }
        return factory
    }

    /**
     * PacketFactory implementation that invokes a constructor with a
     * PacketReader argument.
     * @author rjwut
     */
    private class Factory<T : ArtemisPacket>(
        override val factoryClass: KClass<T>
    ) : PacketFactory<T> {
        private val constructor = requireNotNull(
            factoryClass.constructors.find {
                it.parameters.size == 1 && it.parameters.all { p -> p.type == PACKET_READER_TYPE }
            }
        ) {
            "${factoryClass.jvmName} has no constructors accepting a PacketReader"
        }

        @Throws(ArtemisPacketException::class)
        override fun build(reader: PacketReader): T = try {
            constructor.call(reader)
        } catch (ex: ArtemisPacketException) {
            throw ex
        } catch (ex: Exception) {
            throw ArtemisPacketException(ex)
        }

        /**
         * Entries in the registry are stored in a Map using this class as the key.
         * @author rjwut
         */
        data class Key(private val type: Int, private val subtype: Byte?)
    }

    private companion object {
        val PACKET_READER_TYPE = PacketReader::class.createType()
    }
}
