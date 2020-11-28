package LLVM;

public enum LLVMType {
    Boolean,
    Byte,
    Int,
    IntPointer,
    Address,
    AddressPointer,
    AddressPointerPointer,
    Void;

    private int length;

    LLVMType() {
        this.length = -1;
    }

    public void setLength(int length) {
        this.length = length;
    }

    private String typeToString() {
        switch (this) {
            case Boolean:
                return "i1";
            case Byte:
                return "i8";
            case Address:
                return "i8*";
            case AddressPointer:
                return "i8**";
            case AddressPointerPointer:
                return "i8***";
            case Int: return "i32";
            case IntPointer:
                return "i32*";
            case Void:
                return "void";
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    public String toString() {
        String typeStr = typeToString();
        if(length > -1) {
            return String.format("[%d x %s]", length, typeStr);
        }
        return typeStr;
    }
}
