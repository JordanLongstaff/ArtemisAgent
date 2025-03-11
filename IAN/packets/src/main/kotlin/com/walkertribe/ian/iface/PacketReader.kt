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
import io.ktor.utils.io.bits.reverseByteOrder
import io.ktor.utils.io.core.discard
import io.ktor.utils.io.core.preview
import io.ktor.utils.io.readInt
import io.ktor.utils.io.readPacket
import korlibs.io.lang.ASCII
import korlibs.io.lang.UTF16_LE
import korlibs.io.lang.toString
import kotlin.enums.enumEntries
import kotlin.reflect.full.isSubclassOf
import kotlinx.datetime.Clock
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.readFloatLe
import kotlinx.io.readIntLe
import kotlinx.io.readShortLe
import org.koin.core.Koin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.koinApplication
import org.koin.ksp.generated.defaultModule

/**
 * Facilitates reading packets from an [ByteReadChannel]. This object may be reused to read as many
 * packets as desired from a single [ByteReadChannel]. Individual packet classes can read their
 * properties by using the read*() methods on this class.
 *
 * @author rjwut
 */
class PacketReader(
    private val channel: ByteReadChannel,
    private val listenerRegistry: ListenerRegistry,
) : KoinComponent {
    private val koinApp = koinApplication { defaultModule() }

    private val protocol: Protocol by inject()

    private val rejectedObjectIDs = mutableSetOf<Int>()

    /**
     * Returns false if the current object's ID has been marked as one for which to reject updates,
     * true otherwise.
     */
    val isAcceptingCurrentObject: Boolean
        get() = !rejectedObjectIDs.contains(objectId)

    /**
     * Returns the server [Version]. Defaults to the latest version, but subject to change if a
     * [VersionPacket] is received.
     */
    var version = Version.DEFAULT

    private lateinit var payload: Source

    /** Returns true if the payload currently being read has more data; false otherwise. */
    val hasMore: Boolean
        get() = !payload.exhausted()

    /** Returns the ID of the current object being read from the payload. */
    var objectId = 0
        private set

    private var bitField: BitField? = null

    /** Returns the timestamp of the packet currently being parsed. */
    internal var packetTimestamp: Long = 0L
        private set

    /** Reads a single packet and returns it. */
    @Throws(PacketException::class)
    suspend fun readPacket(): ParseResult {
        objectId = 0
        bitField = null
        packetTimestamp = Clock.System.now().toEpochMilliseconds()

        val (packetType, payloadPacket) = readPayload()
        val subtype =
            if (payloadPacket.exhausted()) 0x00 else payloadPacket.preview { it.readByte() }
        val factory = protocol.getFactory(packetType, subtype)
        val result: ParseResult = ParseResult.Processing()

        // IAN wants certain packet types even if the code consuming IAN isn't
        // interested in them.
        payload = payloadPacket
        return try {
            // We need this packet
            factory
                ?.takeIf {
                    val factoryClass = it.factoryClass

                    // Find out if any listeners are interested in this packet type
                    result.addListeners(listenerRegistry.listeningFor(factoryClass))

                    result.isInteresting ||
                        factoryClass.isSubclassOf(ObjectUpdatePacket::class) ||
                        factoryClass.isSubclassOf(VersionPacket::class)
                }
                ?.build(this)
                ?.takeIf { packet ->
                    when (packet) {
                        is VersionPacket -> {
                            version = packet.version
                            true
                        }

                        is ObjectUpdatePacket -> {
                            packet.objectClasses.forEach {
                                result.addListeners(listenerRegistry.listeningFor(it))
                            }
                            result.isInteresting
                        }

                        else -> true
                    }
                }
                ?.let { packet -> ParseResult.Success(packet, result) } ?: ParseResult.Skip
        } catch (ex: PacketException) {
            // an exception occurred during payload parsing
            ex.appendParsingDetails(packetType, payload.readByteArray())
            ParseResult.Fail(ex)
        } catch (@Suppress("TooGenericExceptionCaught") ex: Exception) {
            ParseResult.Fail(PacketException(ex, packetType, payload.readByteArray()))
        } finally {
            payload.close()
        }
    }

    /** Returns the next byte in the current packet's payload without moving the pointer. */
    fun peekByte(): Byte = payload.preview { it.readByte() }

    /** Reads a single byte from the current packet's payload. */
    fun readByte(): Byte = payload.readByte()

    /** Reads a single byte from the current packet's payload and converts it to an [Enum] value. */
    inline fun <reified E : Enum<E>> readByteAsEnum(): E = enumEntries<E>()[readByte().toInt()]

    /** Convenience method for `readByte(bit.getIndex(version), defaultValue)`. */
    fun readByte(bit: Bit, defaultValue: Byte = -1): Byte =
        readByte(bit.getIndex(version), defaultValue)

    /**
     * Reads a single byte from the current packet's payload if the indicated bit in the current
     * [BitField] is on. Otherwise, the pointer is not moved, and the given default value is
     * returned.
     */
    fun readByte(bitIndex: Int, defaultValue: Byte = -1): Byte =
        if (has(bitIndex)) readByte() else defaultValue

    /** Convenience method for `readByteAsEnum<E>(bit.getIndex(version))`. */
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

    /** Convenience method for `readBool(bit.getIndex(version), bytes)`. */
    fun readBool(bit: Bit, bytes: Int): BoolState = readBool(bit.getIndex(version), bytes)

    /**
     * Reads the indicated number of bytes from the current packet's payload if the indicated bit in
     * the current [BitField] is on, then coerces the zeroth byte read into a [BoolState].
     * Otherwise, the pointer is not moved, and [BoolState.Unknown] is returned.
     */
    fun readBool(bitIndex: Int, bytes: Int): BoolState =
        if (has(bitIndex)) readBool(bytes) else BoolState.Unknown

    /** Reads a short from the current packet's payload. */
    fun readShort(): Int = payload.readShortLe().toInt()

    /**
     * Reads a short from the current packet's payload if the indicated bit in the current
     * [BitField] is on. Otherwise, the pointer is not moved, and the given default value is
     * returned.
     */
    fun readShort(bitIndex: Int, defaultValue: Int): Int =
        if (has(bitIndex)) readShort() else defaultValue

    /** Reads an integer from the current packet's payload. */
    fun readInt(): Int = payload.readIntLe()

    /** Reads an integer from the current packet's payload and converts it to an [Enum] value. */
    inline fun <reified E : Enum<E>> readIntAsEnum(): E = enumEntries<E>()[readInt()]

    /** Convenience method for `readInt(bit.getIndex(version), defaultValue)`. */
    fun readInt(bit: Bit, defaultValue: Int): Int = readInt(bit.getIndex(version), defaultValue)

    /**
     * Reads an integer from the current packet's payload if the indicated bit in the current
     * [BitField] is on. Otherwise, the pointer is not moved, and the given default value is
     * returned.
     */
    fun readInt(bitIndex: Int, defaultValue: Int): Int =
        if (has(bitIndex)) readInt() else defaultValue

    /** Reads a float from the current packet's payload. */
    fun readFloat(): Float = payload.readFloatLe()

    /** Convenience method for `readFloat(bit.getIndex(version))`. */
    fun readFloat(bit: Bit): Float = readFloat(bit.getIndex(version))

    /**
     * Reads a float from the current packet's payload if the indicated bit in the current
     * [BitField] is on. Otherwise, the pointer is not moved, and [Float.NaN] is returned instead.
     */
    fun readFloat(bitIndex: Int): Float = if (has(bitIndex)) readFloat() else Float.NaN

    /** Reads a UTF-16LE String from the current packet's payload. */
    fun readString(): String =
        payload.readByteArray(payload.readIntLe() * 2).toString(UTF16_LE).substringBefore(Char(0))

    /** Reads an ASCII String from the current packet's payload. */
    fun readUsAsciiString(): String = payload.readByteArray(payload.readIntLe()).toString(ASCII)

    /** Convenience method for readString(bit.getIndex(version)). */
    fun readString(bit: Bit): String? = readString(bit.getIndex(version))

    /**
     * Reads a UTF-16LE String from the current packet's payload if the indicated bit in the current
     * [BitField] is on. Otherwise, the pointer is not moved, and null is returned.
     */
    fun readString(bitIndex: Int): String? = if (has(bitIndex)) readString() else null

    /** Reads the given number of bytes from the current packet's payload. */
    fun readBytes(byteCount: Int): ByteArray = payload.readByteArray(byteCount)

    /**
     * Reads the given number of bytes from the current packet's payload if the indicated bit in the
     * current [BitField] is on. Otherwise, the pointer is not moved, and null is returned.
     */
    fun readBytes(bitIndex: Int, byteCount: Int): ByteArray? =
        if (has(bitIndex)) readBytes(byteCount) else null

    /** Skips the given number of bytes in the current packet's payload. */
    fun skip(byteCount: Int) {
        payload.discard(byteCount.toLong())
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

    /** Removes the given object ID from the set of IDs for which to reject updates. */
    fun acceptObjectID(id: Int) {
        rejectedObjectIDs.remove(id)
    }

    /** Adds the current object ID to the set of IDs for which to reject updates. */
    fun rejectCurrentObject() {
        rejectedObjectIDs.add(objectId)
    }

    /** Clears all information related to object IDs that get rejected on object update. */
    fun clearObjectIDs() = rejectedObjectIDs.clear()

    /** Convenience method for `has(bit.getIndex(version))`. */
    fun has(bit: Bit): Boolean = has(bit.getIndex(version))

    /** Returns true if the current [BitField] has the indicated bit turned on. */
    fun has(bitIndex: Int): Boolean = bitField?.get(bitIndex) ?: false

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun readHeaderAndLength(): Int {
        val header = channel.readInt().reverseByteOrder()
        if (header != Packet.HEADER) {
            throw PacketException("Illegal packet header: ${header.toHexString()}")
        }

        val length = channel.readInt().reverseByteOrder()
        if (length < Packet.PREAMBLE_SIZE) {
            throw PacketException("Illegal packet length: $length")
        }

        return length
    }

    private suspend fun readPayload(): Pair<Int, Source> {
        val length = readHeaderAndLength()
        val origin = Origin(channel.readInt().reverseByteOrder())
        val padding = channel.readInt().reverseByteOrder()
        val remaining = channel.readInt().reverseByteOrder()
        val packetType = channel.readInt().reverseByteOrder()

        val payloadLength = length - Packet.PREAMBLE_SIZE
        val payloadPacket = channel.readPacket(payloadLength)

        val expectedRemaining = payloadLength + Int.SIZE_BYTES
        val requiredOrigin = Origin.SERVER

        when {
            !origin.isValid -> "Unknown origin: ${origin.value}"
            origin != requiredOrigin -> "Origin mismatch: expected $requiredOrigin, got $origin"
            padding != 0 -> "No empty padding after connection type?"
            remaining != expectedRemaining ->
                "Packet length discrepancy: total length = $length; expected $expectedRemaining " +
                    "for remaining bytes field, but got $remaining"
            else -> null
        }?.also { error -> throw PacketException(error, packetType, payloadPacket.readByteArray()) }

        return packetType to payloadPacket
    }

    override fun getKoin(): Koin = koinApp.koin
}
