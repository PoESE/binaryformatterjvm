package sx.kenji.binaryformatterjvm

enum class BinaryArrayTypeEnumeration(val value: Byte) {
    Single(0),
    Jagged(1),
    Rectangular(2),
    SingleOffset(3),
    JaggedOffset(4),
    RectangularOffset(5);

    companion object {
        fun from(value: Byte): BinaryArrayTypeEnumeration {
            return values().first { it.value == value }
        }
    }
}
