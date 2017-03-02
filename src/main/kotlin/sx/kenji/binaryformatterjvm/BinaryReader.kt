package sx.kenji.binaryformatterjvm

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

class BinaryReaderInputStream(buf: ByteArray) : ByteArrayInputStream(buf) {
    fun readByte(): Byte {
        return this.readBytesWrapped(1).get()
    }

    fun readInt32(): Int {
        return this.readBytesWrapped(4).int
    }

    fun readString(): String {
        return String(this.readNBytes(this.readStringLength()), StandardCharsets.UTF_8)
    }

    fun readBoolean(): Boolean {
        return this.read() != 0
    }


    private fun readStringLength(): Int {
        var shift = 0
        var count = 0
        do {
            val b = this.read()
            count = count or ((b and 0x7f) shl shift)
            shift += 7
        } while(b and 0x80 != 0)
        return count
    }

    private fun readBytesWrapped(n: Int): ByteBuffer {
        return ByteBuffer.wrap(this.readNBytes(n)).order(ByteOrder.LITTLE_ENDIAN)
    }

    private fun readNBytes(n: Int): ByteArray {
        val bytes = ByteArray(n)
        this.read(bytes)
        return bytes
    }
}
