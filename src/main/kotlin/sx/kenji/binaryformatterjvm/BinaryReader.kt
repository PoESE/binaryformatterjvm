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

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

class BinaryReaderInputStream(buf: ByteArray): ByteArrayInputStream(buf) {
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
