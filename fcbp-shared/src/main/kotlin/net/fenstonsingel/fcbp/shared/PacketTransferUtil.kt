package net.fenstonsingel.fcbp.shared

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

fun SocketChannel.sendFCBPPacket(packet: FCBPPacket) { write(packet.toByteBuffer()) }

fun Socket.sendFCBPPacket(packet: FCBPPacket) { getOutputStream().write(packet.toByteArray()) }

class FCBPPacketBuffer {

    private var state = State.AT_SIZE

    private val sizeBuffer: ByteBuffer = ByteBuffer.allocate(4)
    private var dataBuffer: ByteBuffer? = null

    private fun advanceBufferState(packetSize: Int) {
        dataBuffer = ByteBuffer.allocate(packetSize)
        state = State.AT_DATA
    }

    private fun restartBuffer() {
        sizeBuffer.clear()
        dataBuffer = null
        state = State.AT_SIZE
    }

    fun read(channel: SocketChannel): FCBPPacket? {
        if (State.AT_SIZE == state) {
            if (-1 == channel.read(sizeBuffer)) throw ClosedFCBPConnectionException
            if (!sizeBuffer.hasRemaining()) {
                val size = sizeBuffer.flip().int
                advanceBufferState(size)
            }
        }

        if (State.AT_DATA == state) {
            with(dataBuffer) {
                checkNotNull(this) { "No data ByteBuffer found despite FCBPPacketBuffer's AT_DATA state" }
                if (-1 == channel.read(this)) throw ClosedFCBPConnectionException
                if (!hasRemaining()) {
                    val dataBytes = ByteArray(capacity()).apply { flip().get(this) }
                    restartBuffer()
                    return Json.decodeFromString<FCBPPacket>(dataBytes.decodeToString())
                }
            }
        }

        return null
    }

    private enum class State { AT_SIZE, AT_DATA }

}

fun Socket.readFCBPPacket(): FCBPPacket = with(getInputStream()) {
    val sizeBytes: ByteArray = readNBytes(4)
    val size = ByteBuffer.wrap(sizeBytes).int
    val dataBytes: ByteArray = readNBytes(size)
    Json.decodeFromString(dataBytes.decodeToString())
}
