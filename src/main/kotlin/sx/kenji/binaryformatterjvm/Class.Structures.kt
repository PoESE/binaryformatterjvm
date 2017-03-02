package sx.kenji.binaryformatterjvm

class AdditionalInfo {
    var primitive: PrimitiveTypeEnumeration? = null
    var systemClass: String? = null
    var `class`: ClassTypeInfo? = null
    var primitiveArray: PrimitiveTypeEnumeration? = null

    constructor(systemClass: String) {
        this.systemClass = systemClass
    }

    constructor(`class`: ClassTypeInfo) {
        this.`class` = `class`
    }

    constructor(primitive: PrimitiveTypeEnumeration, array: Boolean) {
        if(array) this.primitiveArray = primitive
        else this.primitive = primitive
    }

    fun write(os: BinaryWriterOutputStream) {
        when {
            this.primitive != null -> os.writeByte(this.primitive!!.value)
            this.systemClass != null -> os.writeString(this.systemClass!!)
            this.`class` != null -> this.`class`!!.write(os)
            this.primitiveArray != null -> os.writeByte(this.primitiveArray!!.value)
            else -> throw Exception("ADDITIONAL INFO MUST BE INITIALIZED") // TODO: Better exception
        }
    }
}

class MemberTypeInfo(stream: BinaryReaderInputStream, memberCount: Int) {
    val binaryTypeEnums = mutableListOf<BinaryTypeEnumeration>()
    val additionalInfos = hashMapOf<Int, AdditionalInfo>()

    init {
        for(i in 0..memberCount-1) {
            this.binaryTypeEnums.add(BinaryTypeEnumeration.from(stream.readByte()))
        }
        for((i, bte) in this.binaryTypeEnums.withIndex()) {
            when(bte) {
                BinaryTypeEnumeration.Primitive -> {
                    val ai = AdditionalInfo(PrimitiveTypeEnumeration.Companion.from(stream.readByte()), false)
                    this.additionalInfos[i] = ai
                }
                BinaryTypeEnumeration.SystemClass -> {
                    val ai = AdditionalInfo(stream.readString())
                    this.additionalInfos[i] = ai
                }
                BinaryTypeEnumeration.Class -> {
                    val ai = AdditionalInfo(ClassTypeInfo(stream))
                    this.additionalInfos[i] = ai
                }
                BinaryTypeEnumeration.PrimitiveArray -> {
                    val ai = AdditionalInfo(PrimitiveTypeEnumeration.Companion.from(stream.readByte()), true)
                    this.additionalInfos[i] = ai
                }
                else -> { }
            }
        }
    }

    fun write(os: BinaryWriterOutputStream) {
        this.binaryTypeEnums.forEach { os.writeByte(it.value) }
        this.additionalInfos.values.forEach { it.write(os) }
    }
}
