package net.fenstonsingel.fcbp.core

import com.intellij.openapi.Disposable
import kotlinx.serialization.SerializationException
import net.fenstonsingel.fcbp.shared.FCBPBreakpoint
import net.fenstonsingel.fcbp.shared.FCBPConditionDelegated
import net.fenstonsingel.fcbp.shared.FCBPConditionInstrumented
import net.fenstonsingel.fcbp.shared.FCBPInstrumenterConnected
import net.fenstonsingel.fcbp.shared.FCBPPacket
import net.fenstonsingel.fcbp.shared.FCBPPacketBuffer
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

object FCBPServer : Disposable {

    private const val serverSocketPort = 14848
    private val serverSocketAddress = InetSocketAddress("localhost", serverSocketPort)

    private val selector: Selector = Selector.open()

    // TODO adequate threading per IntelliJ Platform's guidelines
    private val selectorThread = Thread {
        try {
            while (!Thread.interrupted()) {
                selector.select(FCBPServer::processSelectedKey)
            }
        } finally {
            selector.keys().forEach { key -> key.channel().close() }
            selector.close()
        }
    }

    init {
        val serverChannel: ServerSocketChannel = ServerSocketChannel.open()
        serverChannel.apply {
            configureBlocking(false)
            bind(serverSocketAddress)
            register(selector, SelectionKey.OP_ACCEPT)
        }
        selectorThread.start()
    }

    private fun processSelectedKey(key: SelectionKey) {
        try {
            when {
                !key.isValid -> return
                key.isAcceptable -> acceptInstrumenterConnection(key)
                key.isReadable -> readInstrumenterEvent(key)
            }
        } catch (e: Exception) {
            // TODO adequate exception handling
            when (val channel = key.channel()) {
                is SocketChannel -> channel.closeConnection()
                else -> throw e
            }
        }
    }

    private val packetBuffersBySocketChannels = mutableMapOf<SocketChannel, FCBPPacketBuffer>()

    private val sessionsByInstrumenterChannels = mutableMapOf<SocketChannel, FCBPSession>()

    private fun acceptInstrumenterConnection(key: SelectionKey) {
        val serverChannel = key.channel() as ServerSocketChannel
        val pendingConnectionChannel: SocketChannel = serverChannel.accept()
        pendingConnectionChannel.apply {
            configureBlocking(false)
            register(selector, SelectionKey.OP_READ)
        }
        packetBuffersBySocketChannels[pendingConnectionChannel] = FCBPPacketBuffer()
    }

    private fun SocketChannel.closeConnection() {
        close()
        packetBuffersBySocketChannels -= this
        sessionsByInstrumenterChannels -= this
    }

    private fun SocketChannel.readPacket(): FCBPPacket? {
        val packetBuffer = checkNotNull(packetBuffersBySocketChannels[this]) {
            "No FCBPPacketBuffer created for an FCBP-related socket connection"
        }
        return packetBuffer.read(this)
    }

    private fun readInstrumenterEvent(key: SelectionKey) {
        val channel = key.channel() as SocketChannel
        try {
            when (val packet = channel.readPacket() ?: return) {
                is FCBPInstrumenterConnected -> channel.processInstrumenterConnection(packet)
                is FCBPConditionInstrumented -> registerInstrumentedBreakpoint(channel, packet.breakpoint)
                is FCBPConditionDelegated -> registerDelegatedBreakpoint(channel, packet.breakpoint)
                // process more events from instrumenter side here
                else -> Unit
            }
        } catch (e: IOException) {
            channel.closeConnection()
        } catch (e: SerializationException) {
            channel.closeConnection()
        }
    }

    private fun SocketChannel.processInstrumenterConnection(connectionPacket: FCBPInstrumenterConnected) {
        val fcbpSession = FCBPSession.findByInstrumenterID(connectionPacket.instrumenterID)
        checkNotNull(fcbpSession) { "FCBP instrumenter ID sent to server doesn't resolve to any FCBP session" }

        if (connectionPacket.isOperative) {
            fcbpSession.initialize(this)
            sessionsByInstrumenterChannels[this] = fcbpSession
        } else {
            fcbpSession.invalidate()
            closeConnection()
        }
    }

    private fun registerInstrumentedBreakpoint(channel: SocketChannel, breakpoint: FCBPBreakpoint) {
        val session = sessionsByInstrumenterChannels[channel] ?: return
        session.registerInstrumentedBreakpoint(breakpoint)
    }

    private fun registerDelegatedBreakpoint(channel: SocketChannel, breakpoint: FCBPBreakpoint) {
        val session = sessionsByInstrumenterChannels[channel] ?: return
        session.registerDelegatedBreakpoint(breakpoint)
    }

    /**
     * Launches the FCBP server if it wasn't already.
     *
     * More precisely, this is a do-nothing function
     * that forces JVM to load and initialize the FCBPServer class
     * and exists to improve readability.
     */
    fun launch() {
        // do nothing
    }

    override fun dispose() {
        selectorThread.interrupt()
    }

}
