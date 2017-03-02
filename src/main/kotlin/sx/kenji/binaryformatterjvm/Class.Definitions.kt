package sx.kenji.binaryformatterjvm

import net.bytebuddy.ByteBuddy
import net.bytebuddy.description.modifier.Visibility

class ClassWithId(bf: BinaryFormatter) : RecordWithObjectId {
    private val recordTypeEnum = RecordTypeEnumeration.ClassWithId

    override val objectId = bf.stream.readInt32()
    val metadataId = bf.stream.readInt32()

    private val _name: String
    private val _memberNames: List<String>
    val _values = mutableListOf<Any>() // FIXME: Make this private

    init {
        val scwmatPromise = bf.want(this.metadataId)
        if(!scwmatPromise.isSuccess()) {
            throw Exception("SHOULD BE SUCCESS") // TODO: Do better exception
        }
        val scwmat = scwmatPromise.get() as SystemClassWithMembersAndTypes

        this._name = scwmat.name
        this._memberNames = scwmat.memberNames

        // REFACTOR: This is very similar to SCWMAT.readValues
        for(i in 0..scwmat.memberCount-1) {
            val bte = scwmat.memberTypeInfo.binaryTypeEnums[i]
            when(bte) {
                BinaryTypeEnumeration.Primitive -> {
                    val ai = scwmat.memberTypeInfo.additionalInfos[i]!!.primitive!!
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

    override fun create(): Any {
        // TODO: Make this more (hum) generic
        if("Generic.List`1" in this._name) {
            return Array(this._values[1] as Int, {
                ((this._values[0] as Record).create() as Array<Any>)[it]
            })
        }
        if("Generic.Dictionary`2" in this._name) {
            val value = (this._values[3] as Record).create() as Array<Any>
            return mutableMapOf(*value.map({
                Pair(
                    (it as DynClass).cls.getField("key").get(it.instance),
                    it.cls.getField("value").get(it.instance)
                )
            }).toTypedArray())
        }

        var name = this._name
        if("`" in name) {
            name = name.substring(0, name.indexOf('`'))
        }
        name = name.replace('+', '.')

        val cls = try {
            Class.forName(name)
        } catch(e: Exception) {
            var bb = ByteBuddy().subclass(Any::class.java).name(name)
            for(member in this._memberNames) {
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
        for((i, member) in this._memberNames.withIndex()) {
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

    override fun write(os: BinaryWriterOutputStream) {
        os.writeByte(this.recordTypeEnum.value)
        os.writeInt32(this.objectId)
        os.writeInt32(this.metadataId)

        for(value in this._values) {
            when(value) {
                is Record -> value.write(os)
                else -> os.writePrimitive(value)
            }
        }
        for(value in this._values) {
            if(value is MemberReference) value.writeValue(os)
        }
    }
}

open class SystemClassWithMembersAndTypes(bf: BinaryFormatter) : RecordWithObjectId {
    private val recordTypeEnum = RecordTypeEnumeration.SystemClassWithMembersAndTypes

    /* ClassInfo structure start */
    override val objectId = bf.stream.readInt32()
    val name = bf.stream.readString()
    val memberCount = bf.stream.readInt32()
    val memberNames = mutableListOf<String>()
    /* ClassInfo structure end */
    val memberTypeInfo: MemberTypeInfo

    val _values = mutableListOf<Any>() // FIXME: Make this private

    init {
        for(i in 0..this.memberCount-1) {
            this.memberNames.add(bf.stream.readString())
        }
        this.memberTypeInfo = MemberTypeInfo(bf.stream, this.memberCount)
        this.readValues(bf)
    }

    open protected fun readValues(bf: BinaryFormatter) {
        for(i in 0..this.memberCount-1) {
            val bte = this.memberTypeInfo.binaryTypeEnums[i]
            when(bte) {
                BinaryTypeEnumeration.Primitive -> {
                    val ai = this.memberTypeInfo.additionalInfos[i]!!.primitive!!
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

    override fun create(): Any {
        // TODO: Make this more (hum) generic
        if("Generic.List`1" in this.name) {
            return Array(this._values[1] as Int, {
                ((this._values[0] as Record).create() as Array<Any>)[it]
            })
        }
        if("Generic.Dictionary`2" in this.name) {
            val value = (this._values[3] as Record).create() as Array<Any>
            return mutableMapOf(*value.map({
                Pair(
                    (it as DynClass).cls.getField("key").get(it.instance),
                    it.cls.getField("value").get(it.instance)
                )
            }).toTypedArray())
        }

        var name = this.name
        if("`" in name) {
            name = name.substring(0, name.indexOf('`'))
        }
        name = name.replace('+', '.')

        val cls = try {
            Class.forName(name)
        } catch(e: Exception) {
            var bb = ByteBuddy().subclass(Any::class.java).name(name)
            for(member in this.memberNames) {
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
        for((i, member) in this.memberNames.withIndex()) {
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

    override fun write(os: BinaryWriterOutputStream) {
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
        for(value in this._values) {
            if(value is MemberReference) value.writeValue(os)
        }
    }
}

class ClassWithMembersAndTypes(bf: BinaryFormatter) : SystemClassWithMembersAndTypes(bf) {
    private val recordTypeEnum = RecordTypeEnumeration.ClassWithMembersAndTypes

    var libraryId: Int = 0

    override fun readValues(bf: BinaryFormatter) {
        this.libraryId = bf.stream.readInt32()
        super.readValues(bf)
    }
}
