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

class SerializationHeaderRecord(stream: BinaryReaderInputStream): Record {
    private val recordTypeEnum = RecordTypeEnumeration.SerializedStreamHeader

    val rootId = stream.readInt32()
    val headerId = stream.readInt32()
    val majorVersion = stream.readInt32()
    val minorVersion = stream.readInt32()

    override fun create() = this

    override fun write(os: BinaryWriterOutputStream) {
        os.writeByte(this.recordTypeEnum.value)
        os.writeInt32(this.rootId)
        os.writeInt32(this.headerId)
        os.writeInt32(this.majorVersion)
        os.writeInt32(this.minorVersion)
    }
}

class BinaryLibrary(stream: BinaryReaderInputStream): Record {
    private val recordTypeEnum = RecordTypeEnumeration.BinaryLibrary

    val libraryId = stream.readInt32()
    val libraryName = stream.readString()

    override fun create() = this

    override fun write(os: BinaryWriterOutputStream) {
        os.writeByte(this.recordTypeEnum.value)
        os.writeInt32(this.libraryId)
        os.writeString(this.libraryName)
    }
}

class MessageEnd(stream: BinaryReaderInputStream): Record {
    private val recordTypeEnum = RecordTypeEnumeration.MessageEnd

    override fun create() = this

    override fun write(os: BinaryWriterOutputStream) = os.writeByte(this.recordTypeEnum.value)
}
