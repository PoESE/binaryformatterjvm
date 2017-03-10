package sx.kenji.binaryformatterjvm

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

class BinaryWriterOutputStream: ByteArrayOutputStream() {
    fun writeInt32(v: Int) {
        this.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array())
    }

    fun writeString(v: String) {
        val b = v.toByteArray(StandardCharsets.UTF_8)
        var l = b.size
        while(l >= 0x80) {
            this.write(l or 0x80)
            l = l shr 7
        }
        this.write(l)
        this.write(b)
    }

    fun writePrimitive(v: Any) {
        when(v) {
            is Int -> this.writeInt32(v)
            is String -> this.writeString(v)
            is Boolean -> this.write(if(v) 1 else 0)
            else -> throw Exception("A") // TODO: Better error handling
        }
    }

    fun writeByte(v: Byte) {
        this.write(v.toInt())
    }
}
