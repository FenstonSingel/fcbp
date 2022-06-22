package net.fenstonsingel.fcbp.shared

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

fun FCBPPacket.toByteBuffer(): ByteBuffer {
    val dataBytes = Json.encodeToString(this).encodeToByteArray()
    val packetBytesBuffer: ByteBuffer = ByteBuffer.allocate(4 + dataBytes.size)
    packetBytesBuffer.apply {
        putInt(dataBytes.size)
        put(dataBytes)
        flip()
    }
    return packetBytesBuffer
}

fun FCBPPacket.toByteArray(): ByteArray = toByteBuffer().let { packetBytesBuffer ->
    ByteArray(packetBytesBuffer.capacity()).also { byteArray -> packetBytesBuffer.get(byteArray) }
}

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
            with(sizeBuffer) {
                if (-1 == channel.read(this)) throw ClosedFCBPConnectionException
                if (!hasRemaining()) {
                    flip()
                    val size = int
                    advanceBufferState(size)
                }
            }
        }

        if (State.AT_DATA == state) {
            with(dataBuffer) {
                checkNotNull(this) { "No data ByteBuffer found despite FCBPPacketBuffer's AT_DATA state" }
                if (-1 == channel.read(this)) throw ClosedFCBPConnectionException
                if (!hasRemaining()) {
                    flip()
                    val dataBytes = ByteArray(capacity()).also { byteArray -> get(byteArray) }
                    restartBuffer()
                    return Json.decodeFromString(dataBytes.decodeToString())
                }
            }
        }

        return null
    }

    private companion object {

        private enum class State { AT_SIZE, AT_DATA }

        private object ClosedFCBPConnectionException : IOException()

    }

}

fun Socket.readFCBPPacket(): FCBPPacket = with(getInputStream()) {
    val sizeBytes: ByteArray = readNBytes(4)
    val size = ByteBuffer.wrap(sizeBytes).int
    val dataBytes: ByteArray = readNBytes(size)
    Json.decodeFromString(dataBytes.decodeToString())
}
