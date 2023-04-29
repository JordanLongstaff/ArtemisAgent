package com.walkertribe.ian.iface

import com.walkertribe.ian.protocol.Packet
import com.walkertribe.ian.protocol.PacketException
import com.walkertribe.ian.protocol.core.setup.WelcomePacket
import com.walkertribe.ian.util.Version
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.io.IOException

/**
 * Default implementation of [ArtemisNetworkInterface] that uses Ktor. When started, it launches
 * four coroutines:
 * * The **connection listener coroutine**, which listens for changes to the connection and
 *   heartbeats.
 * * The **receiver coroutine**, which reads and parses packets from the input stream.
 * * The **parse result dispatch coroutine**, which fires listeners to respond to incoming packets
 *   and object updates.
 * * The **sender coroutine**, which writes outgoing packets to the output stream.
 */
class KtorArtemisNetworkInterface(override val maxVersion: Version?) :
    ArtemisNetworkInterface, CoroutineScope {
    override val coroutineContext = Dispatchers.IO

    private lateinit var socket: Socket
    private lateinit var reader: PacketReader
    private lateinit var writer: PacketWriter

    private lateinit var receiveJob: Job
    internal lateinit var sendJob: Job
    internal lateinit var parseResultDispatchJob: Job
    internal lateinit var connectionListenerJob: Job

    internal val sendingChannel = Channel<Packet.Client>(Channel.BUFFERED)
    internal val parseResultsChannel = Channel<ParseResult.Success>(Channel.BUFFERED)
    internal val connectionEventChannel = Channel<ConnectionEvent>(Channel.BUFFERED)
    internal var startTime: Long? = null
        private set

    private var disconnectCause: DisconnectCause? = DisconnectCause.LocalDisconnect
    private val heartbeatManager = HeartbeatManager(this)
    private val listeners = ListenerRegistry()
    override var version: Version = Version.DEFAULT
        private set(value) {
            if (field == value) return
            field = value

            if (value < Version.MINIMUM || maxVersion?.takeIf { value > it } != null) {
                disconnectCause = DisconnectCause.UnsupportedVersion(value)
                stop()
            }
        }

    private val receiveExceptionHandler by lazy {
        CoroutineExceptionHandler { _, throwable ->
            reader.close(throwable)
            disconnectCause =
                when (throwable) {
                    is PacketException -> DisconnectCause.PacketParseError(throwable)
                    else -> DisconnectCause.RemoteDisconnect
                }
            stop()
        }
    }

    private val sendExceptionHandler by lazy {
        CoroutineExceptionHandler { _, throwable ->
            disconnectCause =
                when (throwable) {
                    is IOException -> DisconnectCause.IOError(throwable)
                    else -> DisconnectCause.UnknownError(throwable)
                }
            stop()
        }
    }

    private val disconnectCauseException
        get() =
            when (val cause = disconnectCause) {
                is DisconnectCause.IOError -> cause.exception
                is DisconnectCause.PacketParseError -> cause.exception
                is DisconnectCause.UnknownError -> cause.throwable
                else -> null
            }

    init {
        addListeners(
            listOf(
                ListenerFunction(WelcomePacket::class) {
                    val wasConnected = isConnected
                    isConnected = true
                    if (!wasConnected) {
                        listeners.offer(ConnectionEvent.Success(it.message))
                    }
                }
            ) + heartbeatManager.listeners
        )
    }

    override fun addListenerModule(module: ListenerModule) {
        listeners.register(module)
    }

    override fun setAutoSendHeartbeat(autoSendHeartbeat: Boolean) {
        heartbeatManager.setAutoSendHeartbeat(autoSendHeartbeat)
    }

    override fun setTimeout(timeout: Long) {
        heartbeatManager.setTimeout(timeout)
    }

    override fun start() {
        if (startTime == null && disconnectCause == null) {
            startTime = Clock.System.now().toEpochMilliseconds()

            sendJob =
                launch(sendExceptionHandler) {
                    while (isRunning && isActive) {
                        sendingChannel.tryReceive().onSuccess { packet ->
                            packet.writeTo(writer)
                            writer.flush()
                        }

                        heartbeatManager.sendHeartbeatIfNeeded()
                    }
                }

            receiveJob =
                launch(receiveExceptionHandler) {
                    while (isRunning) {
                        // read packet and process
                        when (val result = reader.readPacket()) {
                            is ParseResult.Success -> {
                                parseResultsChannel.send(result)
                                version = reader.version
                            }
                            is ParseResult.Fail -> throw result.exception
                            else -> {}
                        }
                    }
                }

            connectionListenerJob = launch {
                while (isRunning && isActive) {
                    connectionEventChannel.tryReceive().onSuccess(listeners::offer)

                    heartbeatManager.checkForHeartbeat()
                }
            }

            parseResultDispatchJob = launch {
                while (isRunning) {
                    parseResultsChannel.receive().fireListeners()
                }
            }
        }
    }

    override var isConnected: Boolean = false
    private val isRunning: Boolean
        get() = disconnectCause == null && startTime != null

    override fun sendPacket(packet: Packet.Client) {
        sendingChannel.trySend(packet)
    }

    override fun sendConnectionEvent(event: ConnectionEvent) {
        connectionEventChannel.trySend(event)
    }

    override fun stop() {
        if (startTime == null) return

        isConnected = false
        startTime = null
        socket.close()

        receiveJob.cancel()
        sendJob.cancel()
        parseResultDispatchJob.cancel()
        connectionListenerJob.cancel()

        while (sendingChannel.tryReceive().isSuccess) {
            // Empty out sending packets channel
        }
        while (parseResultsChannel.tryReceive().isSuccess) {
            // Empty out parse results channel
        }
        while (connectionEventChannel.tryReceive().isSuccess) {
            // Empty out connection events channel
        }

        listeners.offer(
            ConnectionEvent.Disconnect(disconnectCause ?: DisconnectCause.LocalDisconnect)
        )
        writer.close(disconnectCauseException)
        reader.close(disconnectCauseException)
    }

    /**
     * Attempts an outgoing client connection to an Artemis server. The send and receive streams
     * won't actually be opened until [start] is called.
     *
     * @param host the hostname of the server.
     * @param port the port on which to connect.
     * @param timeoutMs how long (in milliseconds) IAN will wait for the connection to be
     *   established before returning false.
     * @return Whether the connection was successful.
     */
    override suspend fun connect(host: String, port: Int, timeoutMs: Long): Boolean {
        if (isRunning) stop()

        socket =
            try {
                withTimeout(timeoutMs) {
                    aSocket(SelectorManager(Dispatchers.IO)).tcp().connect(host, port) {
                        keepAlive = true
                    }
                }
            } catch (_: TimeoutCancellationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: IOException) {
                null
            } ?: return false

        reader = PacketReader(socket.openReadChannel(), listeners)
        writer = PacketWriter(socket.openWriteChannel())
        disconnectCause = null

        return true
    }

    override fun dispose() {
        stop()
        sendingChannel.close()
        parseResultsChannel.close()
        connectionEventChannel.close()
        listeners.clear()
    }
}
