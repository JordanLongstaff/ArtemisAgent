package com.walkertribe.ian.iface

import com.walkertribe.ian.enums.ObjectType
import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketException
import com.walkertribe.ian.protocol.Protocol
import com.walkertribe.ian.protocol.core.setup.VersionPacket
import com.walkertribe.ian.protocol.core.world.ObjectUpdatePacket
import com.walkertribe.ian.util.Bit
import com.walkertribe.ian.util.BitField
import com.walkertribe.ian.util.BoolState
import com.walkertribe.ian.util.Version
import com.walkertribe.ian.util.readBitField
import com.walkertribe.ian.util.readBoolState
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.isNotEmpty
import io.ktor.utils.io.core.readBytes
import io.ktor.utils.io.core.readFloatLittleEndian
import io.ktor.utils.io.core.readIntLittleEndian
import io.ktor.utils.io.core.readShortLittleEndian
import io.ktor.utils.io.readIntLittleEndian
import korlibs.io.lang.ASCII
import korlibs.io.lang.UTF16_LE
import korlibs.io.lang.toString
import kotlinx.datetime.Clock
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.koinApplication
import org.koin.ksp.generated.defaultModule
import kotlin.enums.enumEntries
import kotlin.reflect.full.isSubclassOf

/**
 * Facilitates reading packets from an [ByteReadChannel]. This object may be reused to read as many
 * packets as desired from a single [ByteReadChannel]. Individual packet classes can read their
 * properties by using the read*() methods on this class.
 * @author rjwut
 */
class PacketReader(
    private val channel: ByteReadChannel,
    private val listenerRegistry: ListenerRegistry,
) : KoinComponent {
    private val koinApp = koinApplication { defaultModule() }
    override fun getKoin(): Koin = koinApp.koin

    private val protocol: Protocol by inject()

    private val rejectedObjectIDs = mutableSetOf<Int>()

    /**
     * Returns the server [Version]. Defaults to the latest version, but subject to change if a
     * [VersionPacket] is received.
     */
    var version = Version.LATEST

    private lateinit var payload: ByteReadPacket

    /**
     * Returns the ID of the current object being read from the payload.
     */
    var objectId = 0
        private set

    private var bitField: BitField? = null

    /**
     * Returns the timestamp of the packet currently being parsed.
     */
    internal var packetTimestamp: Long = 0L
        private set

    /**
     * Reads a single packet and returns it.
     */
    @Throws(PacketException::class)
    suspend fun readPacket(): ParseResult {
        objectId = 0
        bitField = null

        while (true) {
            // header (0xdeadbeef)
            val header = channel.readIntLittleEndian()
            packetTimestamp = Clock.System.now().toEpochMilliseconds()

            if (header != Packet.HEADER) {
                throw PacketException(
                    "Illegal packet header: ${Integer.toHexString(header)}"
                )
            }

            // packet length
            val len = channel.readIntLittleEndian()
            if (len < Packet.PREAMBLE_SIZE) {
                throw PacketException("Illegal packet length: $len")
            }

            // Read the rest of the packet
            val origin = Origin(channel.readIntLittleEndian())
            val padding = channel.readIntLittleEndian()
            val remainingBytes = channel.readIntLittleEndian()
            val packetType = channel.readIntLittleEndian()
            val remaining = len - Packet.PREAMBLE_SIZE
            val payloadPacket = channel.readPacket(remaining)

            // Check preamble fields for issues
            if (!origin.isValid) {
                throw PacketException(
                    "Unknown origin: ${origin.value}",
                    packetType,
                    payloadPacket.readBytes(),
                )
            }

            val requiredOrigin = Origin.SERVER
            if (origin != requiredOrigin) {
                throw PacketException(
                    "Origin mismatch: expected $requiredOrigin, got $origin",
                    packetType,
                    payloadPacket.readBytes(),
                )
            }

            // padding
            if (padding != 0) {
                throw PacketException(
                    "No empty padding after connection type?",
                    packetType,
                    payloadPacket.readBytes(),
                )
            }

            // remaining bytes
            val expectedRemainingBytes = remaining + Int.SIZE_BYTES
            if (remainingBytes != expectedRemainingBytes) {
                throw PacketException(
                    "Packet length discrepancy: total length = $len; " +
                        "expected $expectedRemainingBytes for remaining bytes field, " +
                        "but got $remainingBytes",
                    packetType,
                    payloadPacket.readBytes(),
                )
            }

            // Find the PacketFactory that knows how to handle this packet type
            val subtype = if (remaining == 0) 0x00 else payloadPacket.tryPeek()
            val factory = protocol.getFactory(packetType, subtype.toByte()) ?: continue
            val factoryClass = factory.factoryClass
            val payloadBytes = payloadPacket.copy().use { it.readBytes() }
            val result: ParseResult = ParseResult.Processing()
            var packet: Packet.Server

            // Find out if any listeners are interested in this packet type
            result.addListeners(listenerRegistry.listeningFor(factoryClass))

            // IAN wants certain packet types even if the code consuming IAN isn't
            // interested in them.
            payload = payloadPacket
            if (
                result.isInteresting ||
                factoryClass.isSubclassOf(ObjectUpdatePacket::class) ||
                factoryClass.isSubclassOf(VersionPacket::class)
            ) {
                // We need this packet
                try {
                    packet = factory.build(this)
                } catch (ex: PacketException) {
                    // an exception occurred during payload parsing
                    ex.appendParsingDetails(packetType, payloadBytes)
                    return ParseResult.Fail(ex)
                } catch (@Suppress("TooGenericExceptionCaught") ex: Exception) {
                    return ParseResult.Fail(PacketException(ex, packetType, payloadBytes))
                } finally {
                    payload.close()
                }

                when (packet) {
                    is VersionPacket -> version = packet.version
                    is ObjectUpdatePacket -> {
                        packet.objectClasses.forEach {
                            result.addListeners(listenerRegistry.listeningFor(it))
                        }
                        if (!result.isInteresting) continue
                    }
                    else -> { }
                }
            } else {
                // Nothing is interested in this packet
                payload.close()
                continue
            }
            return ParseResult.Success(packet, result)
        }
    }

    /**
     * Returns true if the payload currently being read has more data; false otherwise.
     */
    val hasMore: Boolean get() = payload.isNotEmpty

    /**
     * Returns the next byte in the current packet's payload without moving the pointer.
     */
    fun peekByte(): Byte = payload.tryPeek().toByte()

    /**
     * Reads a single byte from the current packet's payload.
     */
    fun readByte(): Byte = payload.readByte()

    /**
     * Reads a single byte from the current packet's payload and converts it to an [Enum] value.
     */
    inline fun <reified E : Enum<E>> readByteAsEnum(): E = enumEntries<E>()[readByte().toInt()]

    /**
     * Convenience method for `readByte(bit.getIndex(version), defaultValue)`.
     */
    fun readByte(bit: Bit, defaultValue: Byte = -1): Byte =
        readByte(bit.getIndex(version), defaultValue)

    /**
     * Reads a single byte from the current packet's payload if the indicated bit in the current
     * [BitField] is on. Otherwise, the pointer is not moved, and the given default value is
     * returned.
     */
    fun readByte(bitIndex: Int, defaultValue: Byte = -1): Byte =
        if (has(bitIndex)) readByte() else defaultValue

    /**
     * Convenience method for `readByteAsEnum<E>(bit.getIndex(version))`.
     */
    inline fun <reified E : Enum<E>> readByteAsEnum(bit: Bit): E? =
        readByteAsEnum<E>(bit.getIndex(version))

    /**
     * Reads a single byte from the current packet's payload and converts it to an [Enum] value if
     * the indicated bit in the current [BitField] is on. Otherwise, the pointer is not moved, and
     * null is returned.
     */
    inline fun <reified E : Enum<E>> readByteAsEnum(bitIndex: Int): E? =
        if (has(bitIndex)) readByteAsEnum<E>() else null

    /**
     * Reads the indicated number of bytes from the current packet's payload, then coerces the
     * zeroth byte read into a [BoolState].
     */
    fun readBool(byteCount: Int): BoolState = payload.readBoolState(byteCount)

    /**
     * Convenience method for `readBool(bit.getIndex(version), bytes)`.
     */
    fun readBool(bit: Bit, bytes: Int): BoolState = readBool(bit.getIndex(version), bytes)

    /**
     * Reads the indicated number of bytes from the current packet's payload if the indicated bit in
     * the current [BitField] is on, then coerces the zeroth byte read into a [BoolState].
     * Otherwise, the pointer is not moved, and [BoolState.Unknown] is returned.
     */
    fun readBool(bitIndex: Int, bytes: Int): BoolState =
        if (has(bitIndex)) readBool(bytes) else BoolState.Unknown

    /**
     * Reads a short from the current packet's payload.
     */
    fun readShort(): Int = payload.readShortLittleEndian().toInt()

    /**
     * Reads a short from the current packet's payload if the indicated bit in the current
     * [BitField] is on. Otherwise, the pointer is not moved, and the given default value is
     * returned.
     */
    fun readShort(bitIndex: Int, defaultValue: Int): Int =
        if (has(bitIndex)) readShort() else defaultValue

    /**
     * Reads an integer from the current packet's payload.
     */
    fun readInt(): Int = payload.readIntLittleEndian()

    /**
     * Reads an integer from the current packet's payload and converts it to an [Enum] value.
     */
    inline fun <reified E : Enum<E>> readIntAsEnum(): E = enumEntries<E>()[readInt()]

    /**
     * Convenience method for `readInt(bit.getIndex(version), defaultValue)`.
     */
    fun readInt(bit: Bit, defaultValue: Int): Int = readInt(bit.getIndex(version), defaultValue)

    /**
     * Reads an integer from the current packet's payload if the indicated bit in the current
     * [BitField] is on. Otherwise, the pointer is not moved, and the given default value is
     * returned.
     */
    fun readInt(bitIndex: Int, defaultValue: Int): Int =
        if (has(bitIndex)) readInt() else defaultValue

    /**
     * Reads a float from the current packet's payload.
     */
    fun readFloat(): Float = payload.readFloatLittleEndian()

    /**
     * Convenience method for `readFloat(bit.getIndex(version))`.
     */
    fun readFloat(bit: Bit): Float = readFloat(bit.getIndex(version))

    /**
     * Reads a float from the current packet's payload if the indicated bit in the current
     * [BitField] is on. Otherwise, the pointer is not moved, and [Float.NaN] is returned instead.
     */
    fun readFloat(bitIndex: Int): Float = if (has(bitIndex)) readFloat() else Float.NaN

    /**
     * Reads a UTF-16LE String from the current packet's payload.
     */
    fun readString(): String =
        payload.readBytes(payload.readIntLittleEndian() * 2)
            .toString(UTF16_LE)
            .substringBefore(Char(0))

    /**
     * Reads an ASCII String from the current packet's payload.
     */
    fun readUsAsciiString(): String =
        payload.readBytes(payload.readIntLittleEndian())
            .toString(ASCII)

    /**
     * Convenience method for readString(bit.getIndex(version)).
     */
    fun readString(bit: Bit): String? = readString(bit.getIndex(version))

    /**
     * Reads a UTF-16LE String from the current packet's payload if the indicated bit in the current
     * [BitField] is on. Otherwise, the pointer is not moved, and null is returned.
     */
    fun readString(bitIndex: Int): String? = if (has(bitIndex)) readString() else null

    /**
     * Reads the given number of bytes from the current packet's payload.
     */
    fun readBytes(byteCount: Int): ByteArray = payload.readBytes(byteCount)

    /**
     * Reads the given number of bytes from the current packet's payload if the indicated bit in the
     * current [BitField] is on. Otherwise, the pointer is not moved, and null is returned.
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
     * Starts reading an object from an [ObjectUpdatePacket]. This will read off an object ID (int)
     * and (if [bitCount] is greater than 0) a [BitField] from the current packet's payload. The
     * [ObjectType] is then returned.
     */
    fun startObject(bitCount: Int) {
        objectId = readInt()
        bitField = payload.readBitField(bitCount)
    }

    fun close(cause: Throwable? = null) {
        channel.cancel(cause)
        koinApp.close()
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
     * Clears all information related to object IDs that get rejected on object update.
     */
    fun clearObjectIDs() = rejectedObjectIDs.clear()

    /**
     * Convenience method for `has(bit.getIndex(version))`.
     */
    fun has(bit: Bit): Boolean = has(bit.getIndex(version))

    /**
     * Returns true if the current [BitField] has the indicated bit turned on.
     */
    fun has(bitIndex: Int): Boolean = bitField?.get(bitIndex) ?: false
}
