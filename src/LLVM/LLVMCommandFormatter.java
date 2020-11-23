package LLVM;

import java.util.List;

public class LLVMCommandFormatter implements ILLVMCommandFormatter {
    private String formatType(LLVMType type) {
        switch (type) {
            case Boolean:
                return "i1";
            case Byte:
                return "i8";
            case String:
                break;
            case Int: return "i32";
            case IntPointer:
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
        return "";
    }

    private String formatComparisonType(ComparisonType type) {
        switch (type) {
            case Equals:
                return "eq";
            case LessOrEquals:
                return "sle";
            case GreatOrEquals:
                return "sge";
        }
        return "";
    }

    private String formatParams(List<LLVMMethodParam> params) {
        StringBuilder paramsString = new StringBuilder();
        for (var param : params) {
            paramsString.append(String.format("%s %s,", param.getType(), param.getName()));
        }
        paramsString.substring(0, paramsString.length()-1);
        return paramsString.toString();
    }

    @Override
    public String formatExternalMethodDeclaration(LLVMType retType, String methodName, String params) {
        return String.format("declare %s @%s(%s)", formatType(retType), methodName, params);
    }

    @Override
    public String formatMethodDefinition(LLVMType retType, String name, List<LLVMMethodParam> params) {
        return String.format("define %s @%s(%s)", formatType(retType), name, formatParams(params));
    }

    @Override
    public String formatReturn(LLVMType retType, String register) {
        return String.format("ret %s %s", formatType(retType), register);
    }

    @Override
    public String formatAlloca(String register, LLVMType type) {
        return String.format("%s = alloca %s", register, formatType(type));
    }

    @Override
    public String formatStore(LLVMType sourceType, String sourceRegister, String destRegister) {
        return String.format("store %s %s, %s* %s",
                formatType(sourceType), sourceRegister,
                formatType(sourceType), destRegister);
    }

    @Override
    public String formatLoad(String register, LLVMType valueType, String sourcePointer) {
        return String.format("%s = load %s, %s* %s", register,
                formatType(valueType), formatType(valueType), sourcePointer);
    }

    @Override
    public String formatCall(String register, LLVMType retType, List<LLVMMethodParam> params) {
        return String.format("%s = call %s @calloc(%s)", register,
                formatType(retType), formatParams(params));
    }

    @Override
    public String formatAdd(String register, LLVMType resultType, String first, String second) {
        return String.format("%s = add %s %s, %s", register, formatType(resultType),
                first, second);
    }

    @Override
    public String formatAnd(String register, LLVMType resultType, String first, String second) {
        return String.format("%s = and %s %s, %s", register, formatType(resultType),
                first, second);
    }

    @Override
    public String formatSub(String register, LLVMType resultType, String first, String second) {
        return String.format("%s = sub %s %s, %s", register, formatType(resultType),
                first, second);
    }

    @Override
    public String formatMul(String register, LLVMType resultType, String first, String second) {
        return String.format("%s = mul %s %s, %s", register, formatType(resultType),
                first, second);
    }

    @Override
    public String formatXOR(String register, LLVMType resultType, String first, String second) {
        return String.format("%s = xor %s %s, %s", register, formatType(resultType),
                first, second);
    }

    @Override
    public String formatCompare(String register, ComparisonType compareType, LLVMType variableType, LLVMType type,
                                String register1, String register2) {
        return String.format("%s = icmp %s %s %s, %s",
                register,
                formatComparisonType(compareType),
                formatType(type),
                register1, register2);
    }

    @Override
    public String formatConditionalBreak(String booleanRegister, String ifLabel, String elseLabel) {
        return String.format("br i1 %s, label %s, label %s", booleanRegister, ifLabel, elseLabel);
    }

    @Override
    public String formatBreak(String label) {
        return String.format("br label %s%s", "%", label);
    }

    @Override
    public String formatLabelName(String labelName) {
        return String.format("%s:", labelName);
    }

    @Override
    public String formatBitcast(String register, LLVMType fromType, String fromRegister, LLVMType toType) {
        return String.format("%s = bitcast %s* %s to %s*",
                register, formatType(fromType),
                fromRegister, formatType(toType));
    }

    // TODO when we learn about arrays
    @Override
    public String formatGetElementPtr(String register, LLVMType type, String pointerRegister, int index) {
        return null;
    }

    @Override
    public String formatConstant(String register, int length, LLVMType type, String constantValue) {
        return String.format("@%s = constant [%d x %s] c%s", register, length, formatType(type), constantValue);
    }

    // TODO when we learn about them
    @Override
    public String formatGlobalVTable(List<String> table) {
        return null;
    }

    @Override
    public String formatPhi(String register, String valueIfLabel1, String label1, String valueIfLabel2, String label2) {
        return String.format("%s = phi i32 [%s, %s%s], [%s, %s%s]",
                register,
                valueIfLabel1,
                "%",
                label1,
                valueIfLabel2,
                "%",
                label2);
    }
}
