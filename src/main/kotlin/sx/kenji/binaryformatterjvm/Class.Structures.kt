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
