package com.walkertribe.ian.iface

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.ArtemisPacketException
import com.walkertribe.ian.protocol.Protocol
import com.walkertribe.ian.protocol.core.RequiredPacket
import com.walkertribe.ian.protocol.core.setup.VersionPacket
import com.walkertribe.ian.protocol.core.world.ObjectUpdatePacket
import com.walkertribe.ian.util.Bit
import com.walkertribe.ian.util.BitField
import com.walkertribe.ian.util.BoolState
import com.walkertribe.ian.util.NullTerminatedString
import com.walkertribe.ian.util.readBitField
import com.walkertribe.ian.util.readBoolState
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.isNotEmpty
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.core.readFloatLittleEndian
import io.ktor.utils.io.core.readIntLittleEndian
import io.ktor.utils.io.core.readShortLittleEndian
import io.ktor.utils.io.readIntLittleEndian
import korlibs.io.lang.ASCII
import korlibs.io.lang.toString
import kotlin.reflect.full.isSubclassOf

/**
 * Facilitates reading packets from an InputStream. This object may be reused to
 * read as many packets as desired from a single InputStream. Individual packet
 * classes can read their properties by using the read*() methods on this class.
 * @author rjwut
 */
class PacketReader(
    private val channel: ByteReadChannel,
    private val protocol: Protocol,
    private val listenerRegistry: ListenerRegistry,
) {
    private val rejectedObjectIDs = mutableSetOf<Int>()

    /**
     * Returns the server Version. Defaults to the latest version, but subject to change
     * if a VersionPacket is received.
     */
    var version = ArtemisNetworkInterface.LATEST_VERSION
        internal set

    private var payload: ByteReadPacket = buildPacket { }

    /**
     * Returns the type of the current object being read from the payload.
     */
    private var objectType: ObjectType? = null

    /**
     * Returns the ID of the current object being read from the payload.
     */
    var objectId = 0
        private set

    private var bitField: BitField? = null

    /**
     * Reads a single packet and returns it.
     */
    @Throws(ArtemisPacketException::class)
    suspend fun readPacket(): ParseResult {
        objectType = null
        objectId = 0
        bitField = null

        while (true) {
            // header (0xdeadbeef)
            val header = channel.readIntLittleEndian()
            if (header != ArtemisPacket.HEADER) {
                throw ArtemisPacketException(
                    "Illegal packet header: ${Integer.toHexString(header)}"
                )
            }

            // packet length
            val len = channel.readIntLittleEndian()
            if (len < ArtemisPacket.PREAMBLE_SIZE) {
                throw ArtemisPacketException("Illegal packet length: $len")
            }

            // Read the rest of the packet
            val originValue = channel.readIntLittleEndian()
            val origin = Origin[originValue]
            val padding = channel.readIntLittleEndian()
            val remainingBytes = channel.readIntLittleEndian()
            val packetType = channel.readIntLittleEndian()
            val remaining = len - ArtemisPacket.PREAMBLE_SIZE
            val payloadPacket = channel.readPacket(remaining)

            // Check preamble fields for issues
            if (origin == null) {
                throw ArtemisPacketException(
                    "Unknown origin: $originValue",
                    null,
                    packetType,
                    payloadPacket
                )
            }

            val requiredOrigin = Origin.SERVER
            if (origin !== requiredOrigin) {
                throw ArtemisPacketException(
                    "Origin mismatch: expected $requiredOrigin, got $origin",
                    origin,
                    packetType,
                    payloadPacket
                )
            }

            // padding
            if (padding != 0) {
                throw ArtemisPacketException(
                    "No empty padding after connection type?",
                    origin,
                    packetType,
                    payloadPacket
                )
            }

            // remaining bytes
            val expectedRemainingBytes = remaining + Int.SIZE_BYTES
            if (remainingBytes != expectedRemainingBytes) {
                throw ArtemisPacketException(
                    "Packet length discrepancy: total length = $len; " +
                        "expected $expectedRemainingBytes for remaining bytes field, " +
                        "but got $remainingBytes",
                    origin,
                    packetType,
                    payloadPacket
                )
            }

            // Find the PacketFactory that knows how to handle this packet type
            val subtype = if (remaining == 0) 0x00 else payloadPacket.tryPeek()
            val factory = protocol.getFactory(packetType, subtype.toByte()) ?: continue
            val factoryClass = factory.factoryClass
            var result: ParseResult = ParseResult.Processing(origin, packetType, payloadPacket)
            var packet: ArtemisPacket = result.packet

            // Find out if any listeners are interested in this packet type
            result.setPacketListeners(listenerRegistry.listeningFor(factoryClass))

            // IAN wants certain packet types even if the code consuming IAN isn't
            // interested in them.
            val required = factoryClass.isSubclassOf(RequiredPacket::class)
            payload = payloadPacket.copy()
            if (required || result.isInteresting) {
                // We need this packet
                try {
                    packet = factory.build(this)
                } catch (ex: ArtemisPacketException) {
                    ex.appendParsingDetails(origin, packetType, payloadPacket)
                    result = ParseResult.Fail(ex)
                }
                payload.close()
                payloadPacket.close()
                if (result is ParseResult.Fail) {
                    // an exception occurred during payload parsing
                    return result
                }
                when (packet) {
                    is VersionPacket -> version = packet.version
                    is ObjectUpdatePacket -> {
                        packet.objectClasses.forEach {
                            result.setObjectListeners(listenerRegistry.listeningFor(it))
                        }
                        if (!result.isInteresting) continue
                    }
                }
            } else {
                // Nothing is interested in this packet
                payload.close()
                payloadPacket.close()
                continue
            }
            return ParseResult.Success(packet, result)
        }
    }

    /**
     * Returns true if the payload currently being read has more data; false
     * otherwise.
     */
    val hasMore: Boolean
        get() = payload.isNotEmpty && (bitField == null || payload.tryPeek() != 0)

    /**
     * Returns the next byte in the current packet's payload without moving the
     * pointer.
     */
    fun peekByte(): Byte = payload.tryPeek().toByte()

    /**
     * Reads a single byte from the current packet's payload.
     */
    fun readByte(): Byte = payload.readByte()

    /**
     * Reads a single byte from the current packet's payload and converts it to an
     * Enum value.
     */
    inline fun <reified E : Enum<E>> readByteAsEnum(): E = enumValues<E>()[readByte().toInt()]

    /**
     * Convenience method for `readByte(bit.getIndex(version), defaultValue)`.
     */
    fun readByte(bit: Bit, defaultValue: Byte = -1): Byte =
        readByte(bit.getIndex(version), defaultValue)

    /**
     * Reads a single byte from the current packet's payload if the indicated
     * bit in the current BitField is on. Otherwise, the pointer is not moved,
     * and the given default value is returned.
     */
    fun readByte(bitIndex: Int, defaultValue: Byte = -1): Byte =
        if (has(bitIndex)) readByte() else defaultValue

    /**
     * Convenience method for `readByteAsEnum<E>(bit.getIndex(version))`.
     */
    inline fun <reified E : Enum<E>> readByteAsEnum(bit: Bit): E? =
        readByteAsEnum<E>(bit.getIndex(version))

    /**
     * Reads a single byte from the current packet's payload and converts it to an
     * Enum value if the indicated bit in the current BitField is on. Otherwise, the
     * pointer is not moved, and null is returned.
     */
    inline fun <reified E : Enum<E>> readByteAsEnum(bitIndex: Int): E? =
        if (has(bitIndex)) readByteAsEnum<E>() else null

    /**
     * Reads the indicated number of bytes from the current packet's payload,
     * then coerces the zeroeth byte read into a BoolState.
     */
    fun readBool(byteCount: Int): BoolState = payload.readBoolState(byteCount)

    /**
     * Convenience method for readBool(bit.getIndex(version), bytes).
     */
    fun readBool(bit: Bit, bytes: Int): BoolState = readBool(bit.getIndex(version), bytes)

    /**
     * Reads the indicated number of bytes from the current packet's payload if
     * the indicated bit in the current BitField is on, then coerces the zeroeth
     * byte read into a BoolState. Otherwise, the pointer is not moved, and
     * BoolState.Unknown is returned.
     */
    fun readBool(bitIndex: Int, bytes: Int): BoolState =
        if (has(bitIndex)) readBool(bytes) else BoolState.Unknown

    /**
     * Reads a short from the current packet's payload.
     */
    fun readShort(): Int = payload.readShortLittleEndian().toInt()

    /**
     * Reads a short from the current packet's payload if the indicated bit in
     * the current BitField is on. Otherwise, the pointer is not moved, and the
     * given default value is returned.
     */
    fun readShort(bitIndex: Int, defaultValue: Int): Int =
        if (has(bitIndex)) readShort() else defaultValue

    /**
     * Reads an int from the current packet's payload.
     */
    fun readInt(): Int = payload.readIntLittleEndian()

    /**
     * Reads an int from the current packet's payload and converts it to an
     * Enum value.
     */
    inline fun <reified E : Enum<E>> readIntAsEnum(): E = enumValues<E>()[readInt()]

    /**
     * Convenience method for readInt(bit.getIndex(version), defaultValue).
     */
    fun readInt(bit: Bit, defaultValue: Int): Int = readInt(bit.getIndex(version), defaultValue)

    /**
     * Reads an int from the current packet's payload if the indicated bit in
     * the current BitField is on. Otherwise, the pointer is not moved, and the
     * given default value is returned.
     */
    fun readInt(bitIndex: Int, defaultValue: Int): Int =
        if (has(bitIndex)) readInt() else defaultValue

    /**
     * Reads a float from the current packet's payload.
     */
    fun readFloat(): Float = payload.readFloatLittleEndian()

    /**
     * Convenience method for readFloat(bit.getIndex(version)).
     */
    fun readFloat(bit: Bit): Float = readFloat(bit.getIndex(version))

    /**
     * Reads a float from the current packet's payload if the indicated bit in
     * the current BitField is on. Otherwise, the pointer is not moved, and
     * Float.NaN is returned instead.
     */
    fun readFloat(bitIndex: Int): Float = if (has(bitIndex)) readFloat() else Float.NaN

    /**
     * Reads a UTF-16LE String from the current packet's payload.
     */
    fun readString(): CharSequence =
        NullTerminatedString(payload.readBytes(payload.readIntLittleEndian() * 2))

    /**
     * Reads a US ASCII String from the current packet's payload.
     */
    fun readUsAsciiString(): String =
        payload.readBytes(payload.readIntLittleEndian()).toString(ASCII)

    /**
     * Convenience method for readString(bit.getIndex(version)).
     */
    fun readString(bit: Bit): CharSequence? = readString(bit.getIndex(version))

    /**
     * Reads a UTF-16LE String from the current packet's payload if the
     * indicated bit in the current BitField is on. Otherwise, the pointer is
     * not moved, and null is returned.
     */
    fun readString(bitIndex: Int): CharSequence? = if (has(bitIndex)) readString() else null

    /**
     * Reads the given number of bytes from the current packet's payload.
     */
    fun readBytes(byteCount: Int): ByteArray = payload.readBytes(byteCount)

    /**
     * Reads the given number of bytes from the current packet's payload if
     * the indicated bit in the current BitField is on. Otherwise, the pointer
     * is not moved, and null is returned.
     */
    fun readBytes(bitIndex: Int, byteCount: Int): ByteArray? =
        if (has(bitIndex)) readBytes(byteCount) else null

    /**
     * Skips the given number of bytes in the current packet's payload.
     */
    fun skip(byteCount: Int) {
        payload.discard(byteCount)
    }

    /**
     * Starts reading an object from an ObjectUpdatePacket. This will read off
     * an object ID (int) and (if bitCount is greater than 0) a BitField from
     * the current packet's payload. This also clears the unknownObjectProps
     * property. The ObjectType is then returned.
     */
    fun startObject(type: ObjectType, bitCount: Int): ObjectType = type.also {
        objectType = it
        objectId = readInt()
        bitField = if (bitCount != 0) payload.readBitField(bitCount) else null
    }

    fun close(cause: Throwable? = null) {
        channel.cancel(cause)
    }

    /**
     * Returns false if the current object's ID has been marked as one for which to
     * reject updates, true otherwise.
     */
    val isAcceptingCurrentObject: Boolean get() = !rejectedObjectIDs.contains(objectId)

    /**
     * Removes the given object ID from the set of IDs for which to reject updates.
     */
    fun acceptObjectID(id: Int) {
        rejectedObjectIDs.remove(id)
    }

    /**
     * Adds the current object ID to the set of IDs for which to reject updates.
     */
    fun rejectCurrentObject() {
        rejectedObjectIDs.add(objectId)
    }

    /**
     * Clears all information related to object IDs that get rejected on
     * object update.
     */
    fun clearObjectIDs() = rejectedObjectIDs.clear()

    /**
     * Convenience method for has(bit.getIndex(version)).
     */
    fun has(bit: Bit): Boolean = has(bit.getIndex(version))

    /**
     * Returns true if the current BitField has the indicated bit turned on.
     */
    fun has(bitIndex: Int): Boolean = bitField?.get(bitIndex) ?: false
}
