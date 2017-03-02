package sx.kenji.binaryformatterjvm

class BinaryArray(bf: BinaryFormatter) : RecordWithObjectId {
    private val recordTypeEnum = RecordTypeEnumeration.BinaryArray

    override val objectId = bf.stream.readInt32()
    val binaryArrayTypeEnum = BinaryArrayTypeEnumeration.from(bf.stream.readByte())
    val rank = bf.stream.readInt32()
    val lengths = mutableListOf<Int>()
    var lowerBounds: MutableList<Int>? = null
    val typeEnum: BinaryTypeEnumeration
    var additionalInfo: AdditionalInfo? = null

    val _values = mutableListOf<Any>() // FIXME: Make this private

    init {
        for(i in 0..this.rank-1) {
            this.lengths.add(bf.stream.readInt32())
        }
        val hasOffset = (
            this.binaryArrayTypeEnum == BinaryArrayTypeEnumeration.SingleOffset ||
            this.binaryArrayTypeEnum == BinaryArrayTypeEnumeration.JaggedOffset ||
            this.binaryArrayTypeEnum == BinaryArrayTypeEnumeration.RectangularOffset
        )
        if(hasOffset) {
            this.lowerBounds = mutableListOf()
            for(i in 0..this.rank-1) {
                this.lowerBounds?.add(bf.stream.readInt32())
            }
        }
        this.typeEnum = BinaryTypeEnumeration.from(bf.stream.readByte())
        when(this.typeEnum) {
            BinaryTypeEnumeration.Primitive -> {
                this.additionalInfo = AdditionalInfo(PrimitiveTypeEnumeration.Companion.from(bf.stream.readByte()), false)
            }
            BinaryTypeEnumeration.SystemClass -> {
                this.additionalInfo = AdditionalInfo(bf.stream.readString())
            }
            BinaryTypeEnumeration.Class -> {
                this.additionalInfo = AdditionalInfo(ClassTypeInfo(bf.stream))
            }
            BinaryTypeEnumeration.PrimitiveArray -> {
                this.additionalInfo = AdditionalInfo(PrimitiveTypeEnumeration.Companion.from(bf.stream.readByte()), true)
            }
            else -> { }
        }

        // TODO: Handle multidimensional arrays
        var length = this.lengths[0]
        var i = 0
        while(i < length) {
            // TODO: Handle different record sx.kenji.types (also null sx.kenji.types)
            val mr = bf.readRecord()
            if(mr is ObjectNullMultiple256) {
                length -= mr.nullCount
                continue
            }
            this._values.add(mr)
            i++
        }
    }

    override fun create(): Any {
        // TODO: Handle multidimensional arrays
        return Array(this._values.size, {
            val value = this._values[it]
            when(value) {
                is Record -> value.create()
                else -> value
            }
        })
    }

    override fun write(os: BinaryWriterOutputStream) {
        os.writeByte(this.recordTypeEnum.value)
        os.writeInt32(this.objectId)
        os.writeByte(this.binaryArrayTypeEnum.value)
        // TODO: Handle multidimensional arrays
        os.writeInt32(this.rank)

        os.writeInt32(this._values.size)
        // TODO: Handle offseted arrays
        os.writeByte(this.typeEnum.value)
        this.additionalInfo?.write(os)

        for(value in this._values) {
            // TODO: Handle other record types (primitives?)
            val record = value as Record
            record.write(os)
        }
    }
}

class ArraySinglePrimitive(stream: BinaryReaderInputStream) : RecordWithObjectId {
    private val recordTypeEnum = RecordTypeEnumeration.ArraySinglePrimitive

    /* ArrayInfo structure start */
    override val objectId = stream.readInt32()
    val length = stream.readInt32()
    /* ArrayInfo structure end */
    val primitiveTypeEnum = PrimitiveTypeEnumeration.from(stream.readByte())

    val _values = mutableListOf<Any>() // FIXME: Make this private

    init {
        for(i in 0..this.length-1) {
            // TODO: Handle other primitives
            this._values.add(stream.readInt32())
        }
    }

    override fun create() = Array(this.length, { this._values[it] })

    override fun write(os: BinaryWriterOutputStream) {
        os.writeByte(this.recordTypeEnum.value)
        os.writeInt32(this.objectId)
        os.writeInt32(this._values.size)
        os.writeByte(this.primitiveTypeEnum.value)

        for(value in this._values) {
            // TODO: Handle other primitives
            os.writeInt32(value as Int)
        }
    }
}
