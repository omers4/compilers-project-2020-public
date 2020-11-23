package LLVM;

public class LLVMMethodParam {
    private LLVMType type;
    private String name;

    LLVMMethodParam(LLVMType paramType, String paramName) {
        type=paramType;
        name=paramName;
    }

    String getName() {
        return name;
    }

    LLVMType getType() {
        return type;
    }
}
