package net.fenstonsingel.fcbp.shared

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.ByteBuffer

object ClosedFCBPConnectionException : IOException()

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
    ByteArray(packetBytesBuffer.capacity()).apply { packetBytesBuffer.get(this) }
}
