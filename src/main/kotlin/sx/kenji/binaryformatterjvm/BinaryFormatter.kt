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

import nl.komponents.kovenant.Deferred
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred

data class DynClass(val cls: Class<*>, val instance: Any)

interface Record {
    fun create(): Any
    fun write(os: BinaryWriterOutputStream)
}

interface RecordWithValues: Record {
    fun writeMetadata(os: BinaryWriterOutputStream)
    fun writeValues(os: BinaryWriterOutputStream)

    override fun write(os: BinaryWriterOutputStream) {
        this.writeMetadata(os)
        this.writeValues(os)
    }
}

interface RecordWithObjectId: Record {
    val objectId: Int
}

class R: Record {
    override fun create() = this
    override fun write(os: BinaryWriterOutputStream) {}
}

class ClassTypeInfo(stream: BinaryReaderInputStream) {
    val typeName = stream.readString()
    val libraryId = stream.readInt32()

    fun write(os: BinaryWriterOutputStream) {
        os.writeString(this.typeName)
        os.writeInt32(this.libraryId)
    }
}

class MemberReference(bf: BinaryFormatter): RecordWithValues {
    private val recordTypeEnum = RecordTypeEnumeration.MemberReference

    val idRef = bf.stream.readInt32()

    val _promise = bf.want(this.idRef) // FIXME: Make this private

    override fun create() = this._promise.get().create()

    override fun writeMetadata(os: BinaryWriterOutputStream) {
        os.writeByte(this.recordTypeEnum.value)
        os.writeInt32(this.idRef)
    }

    override fun writeValues(os: BinaryWriterOutputStream) = this._promise.get().write(os)
}

class ObjectNullMultiple256(stream: BinaryReaderInputStream): Record {
    private val recordTypeEnum = RecordTypeEnumeration.ObjectNullMultiple256

    val nullCount = stream.readByte()

    override fun create() = this.nullCount

    override fun write(os: BinaryWriterOutputStream) {
        os.writeByte(this.recordTypeEnum.value)
        os.writeByte(this.nullCount)
    }
}

class BinaryObjectString(stream: BinaryReaderInputStream): RecordWithObjectId {
    private val recordTypeEnum = RecordTypeEnumeration.BinaryObjectString

    override val objectId = stream.readInt32()
    val value = stream.readString()

    override fun create() = this.value

    override fun write(os: BinaryWriterOutputStream) {
        os.writeByte(this.recordTypeEnum.value)
        os.writeInt32(this.objectId)
        os.writeString(this.value)
    }
}

class BinaryFormatter(bytearray: ByteArray) {
    var serializationHeaderRecord: SerializationHeaderRecord? = null
    val libraries = hashMapOf<Int, BinaryLibrary>()
    val classes = hashMapOf<Int, SystemClassWithMembersAndTypes>()
    val records = mutableListOf<Record>()

    internal val stream = BinaryReaderInputStream(bytearray)

    private val deferreds = hashMapOf<Int, Deferred<RecordWithObjectId, Exception>>()

    internal fun want(objectId: Int): Promise<RecordWithObjectId, Exception> {
        val d = this.deferreds.getOrPut(objectId, { deferred() })
        this.classes[objectId]?.let {
            if(!d.promise.isDone()) {
                d.resolve(it)
            }
        }
        return d.promise
    }

    fun deserialize(): Any {
        do {
            val record = this.readRecord()
        } while(record !is MessageEnd)

        // Simple consistency check
        for(deferred in this.deferreds.values) {
            if(!deferred.promise.isDone()) {
                throw Exception("FAILED TO RESOLVE $deferred")
            }
        }

        val entryPoint = this.classes.toSortedMap().values.first()

        val mr = entryPoint._values[3] as MemberReference
        val ba = mr._promise.get() as BinaryArray
        val cwi = ba._values[5] as ClassWithId
        val mr2 = cwi._values[1] as MemberReference
        val cwi2 = mr2._promise.get() as ClassWithId

        val triggeredEvents = cwi2._values[0] as MemberReference
        val te = triggeredEvents._promise.get() as ClassWithId
        val te2 = te._values[0] as MemberReference
        val te3 = te2._promise.get() as ArraySinglePrimitive
        te3._values[0] = 0

        val visitedStates = cwi2._values[1] as MemberReference
        val vs = visitedStates._promise.get() as ClassWithId
        val vs2 = vs._values[0] as MemberReference
        val vs3 = vs2._promise.get() as ArraySinglePrimitive
        vs3._values[0] = 0

        return entryPoint.create()
    }

    fun reserialize(os: BinaryWriterOutputStream) {
        this.serializationHeaderRecord?.write(os) ?: return
        for(library in this.libraries.values) {
            library.write(os)
        }

        val entryPoint = this.classes.toSortedMap().values.first()
        entryPoint.write(os)

        MessageEnd(this.stream).write(os)
    }

    internal fun readRecord(): Record {
        val recordType = RecordTypeEnumeration.from(stream.readByte())
        val record = when(recordType) {
            RecordTypeEnumeration.SerializedStreamHeader -> {
                SerializationHeaderRecord(stream)
            }
            RecordTypeEnumeration.ClassWithId -> {
                ClassWithId(this)
            }
            RecordTypeEnumeration.SystemClassWithMembersAndTypes -> {
                SystemClassWithMembersAndTypes(this)
            }
            RecordTypeEnumeration.ClassWithMembersAndTypes -> {
                ClassWithMembersAndTypes(this)
            }
            RecordTypeEnumeration.BinaryObjectString -> {
                BinaryObjectString(stream)
            }
            RecordTypeEnumeration.BinaryArray -> {
                BinaryArray(this)
            }
            RecordTypeEnumeration.MemberReference -> {
                MemberReference(this)
            }
            RecordTypeEnumeration.MessageEnd -> {
                MessageEnd(stream)
            }
            RecordTypeEnumeration.BinaryLibrary -> {
                // Intercepting this as it seems that .NET is spitting
                // BL info wherever it feels like, despite the doc
                val bl = BinaryLibrary(stream)
                this.libraries[bl.libraryId] = bl
                this.readRecord()
            }
            RecordTypeEnumeration.ObjectNullMultiple256 -> {
                ObjectNullMultiple256(stream)
            }
            RecordTypeEnumeration.ArraySinglePrimitive -> {
                ArraySinglePrimitive(stream)
            }
            else -> R() // TODO: remove when we exhaust when
        }

        if(record is RecordWithObjectId) {
            this.deferreds[record.objectId]?.resolve(record)
        }
        when(record) {
            is SerializationHeaderRecord -> {
                this.serializationHeaderRecord = record
            }
            is SystemClassWithMembersAndTypes -> {
                this.classes[record.objectId] = record
            }
        }
        this.records.add(record)

        return record
    }
}
