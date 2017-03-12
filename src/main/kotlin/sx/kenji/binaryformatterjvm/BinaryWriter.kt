/*
 * binaryformatterjvm
 * Copyright (C) 2017 Karol 'Kenji Takahashi' Woźniak
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
