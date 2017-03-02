package sx.kenji.binaryformatterjvm

class SerializationHeaderRecord(stream: BinaryReaderInputStream) : Record {
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

class BinaryLibrary(stream: BinaryReaderInputStream) : Record {
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

class MessageEnd(stream: BinaryReaderInputStream) : Record {
    private val recordTypeEnum = RecordTypeEnumeration.MessageEnd

    override fun create() = this

    override fun write(os: BinaryWriterOutputStream) = os.writeByte(this.recordTypeEnum.value)
}
