package sx.kenji.binaryformatterjvm

import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.modifier.Visibility

open class _Class {
    val _values = mutableListOf<Any>() // FIXME: Make this private

    open protected fun readValues(bf: BinaryFormatter, src: SystemClassWithMembersAndTypes) {
        for(i in 0..src.memberCount-1) {
            val bte = src.memberTypeInfo.binaryTypeEnums[i]
            when(bte) {
                BinaryTypeEnumeration.Primitive -> {
                    val ai = src.memberTypeInfo.additionalInfos[i]!!.primitive!!
                    when(ai) {
                        PrimitiveTypeEnumeration.Int32 -> {
                            this._values.add(bf.stream.readInt32())
                        }
                        PrimitiveTypeEnumeration.Boolean -> {
                            this._values.add(bf.stream.readBoolean())
                        }
                    }
                }
                else -> {
                    this._values.add(bf.readRecord())
                }
            }
        }
    }

    protected fun create(src: SystemClassWithMembersAndTypes): Any {
        // TODO: Make this more (hum) generic
        if("Generic.List`1" in src.name) {
            return Array(this._values[1] as Int, {
                ((this._values[0] as Record).create() as Array<Any>)[it]
            })
        }
        if("Generic.Dictionary`2" in src.name) {
            val value = (this._values[3] as Record).create() as Array<Any>
            return mutableMapOf(*value.map({
                Pair(
                    (it as DynClass).cls.getField("key").get(it.instance),
                    it.cls.getField("value").get(it.instance)
                )
            }).toTypedArray())
        }

        var name = src.name
        if("`" in name) {
            name = name.substring(0, name.indexOf('`'))
        }
        name = name.replace('+', '.')

        val cls = try {
            Class.forName(name)
        } catch(e: Exception) {
            var bb = ByteBuddy().subclass(Any::class.java).name(name)
            for(member in src.memberNames) {
                // TODO: Handle this (k__BackingField) better
                var m = member
                if(member.startsWith("<")) {
                    m = member.substring(1, member.indexOf('>'))
                }
                bb = bb.defineField(m, Any::class.java, Visibility.PUBLIC)
            }
            bb.make().load(ClassLoader.getSystemClassLoader()).loaded
        }

        val ins = cls.newInstance()
        for((i, member) in src.memberNames.withIndex()) {
            var value = this._values[i]
            if(value is Record) {
                value = value.create()
            }
            var m = member
            if(member.startsWith("<")) {
                m = member.substring(1, member.indexOf('>'))
            }
            cls.getField(m).set(ins, value)
        }
        return DynClass(cls, ins)
    }
}

class ClassWithId(bf: BinaryFormatter): _Class(), RecordWithObjectId {
    private val recordTypeEnum = RecordTypeEnumeration.ClassWithId

    override val objectId = bf.stream.readInt32()
    val metadataId = bf.stream.readInt32()

    private val scwmat: SystemClassWithMembersAndTypes

    init {
        val scwmatPromise = bf.want(this.metadataId)
        if(!scwmatPromise.isSuccess()) {
            throw Exception("SHOULD BE SUCCESS") // TODO: Do better exception
        }
        this.scwmat = scwmatPromise.get() as SystemClassWithMembersAndTypes
        this.readValues(bf, this.scwmat)
    }

    override fun create(): Any = this.create(this.scwmat)

    fun writeNoValues(os: BinaryWriterOutputStream) {
        os.writeByte(this.recordTypeEnum.value)
        os.writeInt32(this.objectId)
        os.writeInt32(this.metadataId)
        for(value in this._values) {
            when(value) {
                is Record -> value.write(os)
                else -> os.writePrimitive(value)
            }
        }
    }

    fun writeValues(os: BinaryWriterOutputStream) {
        for(value in this._values) {
            if(value is MemberReference) value.writeValue(os)
        }
    }

    override fun write(os: BinaryWriterOutputStream) {
        os.writeByte(this.recordTypeEnum.value)
        os.writeInt32(this.objectId)
        os.writeInt32(this.metadataId)

        for(value in this._values) {
            when(value) {
                is SystemClassWithMembersAndTypes -> value.writeNoValues(os)
                is ClassWithId -> value.writeNoValues(os)
                is Record -> value.write(os)
                else -> os.writePrimitive(value)
            }
        }
        for(value in this._values) {
            when(value) {
                is SystemClassWithMembersAndTypes -> value.writeValues(os)
                is ClassWithId -> value.writeValues(os)
                is MemberReference -> value.writeValue(os)
            }
            //            if(value is MemberReference) value.writeValue(os)
        }
    }
}

open class SystemClassWithMembersAndTypes(bf: BinaryFormatter): _Class(), RecordWithObjectId {
    open protected val recordTypeEnum = RecordTypeEnumeration.SystemClassWithMembersAndTypes

    /* ClassInfo structure start */
    override val objectId = bf.stream.readInt32()
    val name = bf.stream.readString()
    val memberCount = bf.stream.readInt32()
    val memberNames = mutableListOf<String>()
    /* ClassInfo structure end */
    val memberTypeInfo: MemberTypeInfo

    init {
        for(i in 0..this.memberCount-1) {
            this.memberNames.add(bf.stream.readString())
        }
        this.memberTypeInfo = MemberTypeInfo(bf.stream, this.memberCount)
        this.readValues(bf, this)
    }

    override fun create(): Any = this.create(this)

    fun writeNoValues(os: BinaryWriterOutputStream) {
        os.writeByte(this.recordTypeEnum.value)
        os.writeInt32(this.objectId)
        os.writeString(this.name)
        os.writeInt32(this.memberCount)
        this.memberNames.forEach { os.writeString(it) }
        this.memberTypeInfo.write(os)
        for(value in this._values) {
            when(value) {
                is Record -> value.write(os)
                else -> os.writePrimitive(value)
            }
        }
    }

    fun writeValues(os: BinaryWriterOutputStream) {
        for(value in this._values) {
            if(value is MemberReference) value.writeValue(os)
        }
    }

    override fun write(os: BinaryWriterOutputStream) {
        os.writeByte(this.recordTypeEnum.value)
        os.writeInt32(this.objectId)
        os.writeString(this.name)
        os.writeInt32(this.memberCount)
        this.memberNames.forEach { os.writeString(it) }
        this.memberTypeInfo.write(os)

        for(value in this._values) {
            when(value) {
                is SystemClassWithMembersAndTypes -> value.writeNoValues(os)
                is ClassWithId -> value.writeNoValues(os)
                is Record -> value.write(os)
                else -> os.writePrimitive(value)
            }
        }
        for(value in this._values) {
            when(value) {
                is SystemClassWithMembersAndTypes -> value.writeValues(os)
                is ClassWithId -> value.writeValues(os)
                is MemberReference -> value.writeValue(os)
            }
//            if(value is MemberReference) value.writeValue(os)
        }
    }
}

class ClassWithMembersAndTypes(bf: BinaryFormatter) : SystemClassWithMembersAndTypes(bf) {
    override val recordTypeEnum = RecordTypeEnumeration.ClassWithMembersAndTypes

    var libraryId: Int = 0

    override fun readValues(bf: BinaryFormatter, src: SystemClassWithMembersAndTypes) {
        this.libraryId = bf.stream.readInt32()
        super.readValues(bf, src)
    }

    override fun write(os: BinaryWriterOutputStream) {
        os.writeByte(this.recordTypeEnum.value)
        os.writeInt32(this.objectId)
        os.writeString(this.name)
        os.writeInt32(this.memberCount)
        this.memberNames.forEach { os.writeString(it) }
        this.memberTypeInfo.write(os)
        os.writeInt32(this.libraryId)

        for(value in this._values) {
            when(value) {
                is SystemClassWithMembersAndTypes -> value.writeNoValues(os)
                is ClassWithId -> value.writeNoValues(os)
                is Record -> value.write(os)
                else -> os.writePrimitive(value)
            }
        }
        for(value in this._values) {
            when(value) {
                is SystemClassWithMembersAndTypes -> value.writeValues(os)
                is ClassWithId -> value.writeValues(os)
                is MemberReference -> value.writeValue(os)
            }
            //            if(value is MemberReference) value.writeValue(os)
        }
    }
}
