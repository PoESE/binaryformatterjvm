package sx.kenji.binaryformatterjvm

enum class RecordTypeEnumeration(val value: Byte) {
    SerializedStreamHeader(0),
    ClassWithId(1),
    SystemClassWithMembers(2),
    ClassWithMembers(3),
    SystemClassWithMembersAndTypes(4),
    ClassWithMembersAndTypes(5),
    BinaryObjectString(6),
    BinaryArray(7),
    MemberPrimitiveTyped(8),
    MemberReference(9),
    ObjectNull(10),
    MessageEnd(11),
    BinaryLibrary(12),
    ObjectNullMultiple256(13),
    ObjectNullMultiple(14),
    ArraySinglePrimitive(15),
    ArraySingleObject(16),
    ArraySingleString(17),
    MethodCall(21),
    MethodReturn(22);

    companion object {
        fun from(value: Byte): RecordTypeEnumeration {
            return values().first { it.value == value }
        }
    }
}

enum class BinaryTypeEnumeration(val value: Byte) {
    Primitive(0),
    String(1),
    Object(2),
    SystemClass(3),
    Class(4),
    ObjectArray(5),
    StringArray(6),
    PrimitiveArray(7);

    companion object {
        fun from(value: Byte): BinaryTypeEnumeration {
            return values().first { it.value == value }
        }
    }
}

enum class PrimitiveTypeEnumeration(val value: Byte) {
    Boolean(1),
    Byte_(2),
    Char(3),
    //NotUsed(4),
    Decimal(5),
    Double(6),
    Int16(7),
    Int32(8),
    Int64(9),
    SByte(10),
    Single(11),
    TimeSpan(12),
    DateTime(13),
    UInt16(14),
    UInt32(15),
    UInt64(16),
    Null(17),
    String(18);

    companion object {
        fun from(value: Byte): PrimitiveTypeEnumeration {
            return values().first { it.value == value }
        }
    }
}
