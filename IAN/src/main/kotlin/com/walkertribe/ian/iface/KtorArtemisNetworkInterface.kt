package com.walkertribe.ian.iface

import com.walkertribe.ian.enums.Origin
import com.walkertribe.ian.protocol.ArtemisPacket
import com.walkertribe.ian.protocol.ArtemisPacketException
import com.walkertribe.ian.protocol.CompositeProtocol
import com.walkertribe.ian.protocol.Protocol
import com.walkertribe.ian.protocol.core.CoreArtemisProtocol
import com.walkertribe.ian.protocol.core.GameOverReasonPacket
import com.walkertribe.ian.protocol.core.GameStartPacket
import com.walkertribe.ian.protocol.core.HeartbeatPacket
import com.walkertribe.ian.protocol.core.setup.VersionPacket
import com.walkertribe.ian.protocol.core.setup.WelcomePacket
import com.walkertribe.ian.util.Version
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Default implementation of ArtemisNetworkInterface. Kicks off three threads:
 *  * The **receiver thread**, which reads and parses packets from the input
 * stream.
 *
 *  * The **event dispatch thread**, which fires listeners to respond to
 * incoming packets, object updates, or events.
 *
 *  * The **sender thread**, which writes outgoing packets to the output
 * stream.
 */
class KtorArtemisNetworkInterface private constructor(
    private val socket: Socket,
    override val debugMode: Boolean,
) : ArtemisNetworkInterface, CoroutineScope {
    @OptIn(DelicateCoroutinesApi::class)
    override val coroutineContext = newFixedThreadPoolContext(NUM_THREADS, "IAN")

    private val protocol = CompositeProtocol().apply { add(CoreArtemisProtocol()) }
    private val listeners = ListenerRegistry()
    private val reader: PacketReader = PacketReader(
        socket.openReadChannel(),
        protocol,
        listeners,
    )
    private val writer = PacketWriter(socket.openWriteChannel())
    private var receiveJob: Job? = null
    private var sendJob: Job? = null
    private var dispatchJob: Job? = null
    private val sendingChannel = Channel<ArtemisPacket>(Channel.BUFFERED)
    private val dispatchChannel = Channel<DispatchEvent>(Channel.BUFFERED)
    private var startTime: Long? = null
    private var disconnectCause: DisconnectCause = DisconnectCause.LocalDisconnect
    private val heartbeatManager = HeartbeatManager(this)
    override var version: Version = ArtemisNetworkInterface.LATEST_VERSION
        private set(value) {
            field = value
            reader.version = value

            if (
                value < ArtemisNetworkInterface.MIN_VERSION ||
                (!debugMode && value > ArtemisNetworkInterface.LATEST_VERSION)
            ) {
                disconnectCause = DisconnectCause.UnsupportedServerVersion(value)
                stop()
            }
        }

    private val receiveExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        reader.close(throwable)
        if (isRunning) {
            disconnectCause = when (throwable) {
                is ArtemisPacketException -> DisconnectCause.PacketParseError(throwable)
                else -> DisconnectCause.RemoteDisconnect
            }
        }
        stop()
    }

    private val sendExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        if (throwable is IOException) {
            disconnectCause = DisconnectCause.IOError(throwable)
        }
        stop()
    }

    private val disconnectCauseException get() = when (val cause = disconnectCause) {
        is DisconnectCause.IOError -> cause.exception
        is DisconnectCause.PacketParseError -> cause.exception
        else -> null
    }

    override fun registerProtocol(protocol: Protocol) {
        this.protocol.add(protocol)
    }

    override fun addListener(listener: Any) {
        listeners.register(listener)
    }

    override fun setAutoSendHeartbeat(autoSendHeartbeat: Boolean) {
        heartbeatManager.setAutoSendHeartbeat(autoSendHeartbeat)
    }

    override fun setTimeout(timeout: Long) {
        heartbeatManager.setTimeout(timeout)
    }

    override fun start() {
        if (startTime == null) {
            startSending()
            startReceiving()
            startDispatch()
            startTime = System.currentTimeMillis()
        }
    }

    private fun startReceiving() {
        receiveJob = launch(receiveExceptionHandler) {
            while (isRunning) {
                // read packet
                val result = reader.readPacket()
                if (result is ParseResult.Fail) {
                    throw result.exception
                }
                if (isRunning) {
                    // Enqueue to the event dispatch thread
                    dispatchChannel.send(result)
                }
            }
        }
    }

    private fun startSending() {
        sendJob = launch(sendExceptionHandler) {
            while (isRunning) {
                sendingChannel.tryReceive().onSuccess { packet ->
                    packet.writeTo(writer)
                    writer.flush()
                }

                heartbeatManager.sendHeartbeatIfNeeded()
            }
        }
    }

    private fun startDispatch() {
        dispatchJob = launch {
            while (isRunning) {
                dispatchChannel.tryReceive().onSuccess { event ->
                    when (event) {
                        is ParseResult -> {
                            when (val pkt = event.packet) {
                                is WelcomePacket -> onPacket(pkt)
                                is VersionPacket -> onPacket(pkt)
                                is HeartbeatPacket ->
                                    heartbeatManager.onHeartbeat(pkt)
                                is GameStartPacket ->
                                    heartbeatManager.onGameStart(pkt)
                                is GameOverReasonPacket ->
                                    heartbeatManager.onGameOver()
                            }
                            event.fireListeners()
                        }
                        is ConnectionEvent -> listeners.fire(event)
                    }
                }

                heartbeatManager.checkForHeartbeat()
            }
        }
    }

    /**
     * Receiving a WelcomePacket is how we know we're connected to the
     * server. Send a ConnectionSuccessEvent.
     */
    private fun onPacket(pkt: WelcomePacket) {
        val wasConnected = isConnected
        isConnected = true
        if (!wasConnected) {
            listeners.fire(ConnectionEvent.Success(pkt.message))
        }
    }

    /**
     * Check the Version against our minimum required version and disconnect
     * if we don't support it.
     */
    private fun onPacket(pkt: VersionPacket) {
        version = pkt.version
    }

    override var isConnected: Boolean = false
    private val isRunning: Boolean get() = sendJob != null

    override fun send(packet: ArtemisPacket) {
        if (packet.origin == Origin.CLIENT) {
            sendingChannel.trySend(packet)
        }
    }

    override fun send(packets: Collection<ArtemisPacket>) {
        packets.forEach(this::send)
    }

    override fun dispatch(event: DispatchEvent) {
        dispatchChannel.trySend(event)
    }

    override fun stop() {
        launch {
            isConnected = false
            withContext(Dispatchers.IO) { socket.close() }

            listeners.fire(ConnectionEvent.Disconnect(disconnectCause))
            listeners.clear()
            sendingChannel.close()
            writer.close(disconnectCauseException)
            reader.close(disconnectCauseException)
            dispatchChannel.close()
        }

        receiveJob = null
        sendJob = null
        dispatchJob = null
    }

    companion object {
        private const val NUM_THREADS = 3

        /**
         * Attempts an outgoing client connection to an Artemis server. The send and
         * receive streams won't actually be opened until start() is called. The
         * timeoutMs value indicates how long (in milliseconds) IAN will wait for
         * the connection to be established before returning null.
         */
        suspend fun connect(
            host: String,
            port: Int,
            timeoutMs: Long = 1L,
            debugMode: Boolean = false,
        ): KtorArtemisNetworkInterface? = withTimeoutOrNull(timeoutMs) {
            try {
                KtorArtemisNetworkInterface(
                    aSocket(SelectorManager(Dispatchers.IO)).tcp().connect(host, port) {
                        keepAlive = true
                    },
                    debugMode,
                )
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: IOException) {
                null
            }
        }
    }
}
