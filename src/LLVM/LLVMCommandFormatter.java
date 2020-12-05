package LLVM;

import java.util.ArrayList;
import java.util.List;

public class LLVMCommandFormatter implements ILLVMCommandFormatter {

    private String formatComparisonType(ComparisonType type) {
        switch (type) {
            case Equals:
                return "eq";
            case Less:
                return "slt";
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
            paramsString.append(String.format("%s %s, ", param.getType().toString(), param.getName()));
        }
        if (!params.isEmpty()) {
            return paramsString.substring(0, paramsString.length()-2);
        }
        //paramsString.append(" \n");
        return paramsString.toString();
    }

    @Override
    public String formatExternalMethodDeclaration(LLVMType retType, String methodName, String params) {
        return String.format("declare %s @%s(%s) {\n", retType.toString(), methodName, params);
    }

    @Override
    public String formatMethodDefinition(LLVMType retType, String name, List<LLVMMethodParam> params) {
        return String.format("define %s @%s(%s) {\n", retType.toString(), name, formatParams(params));
    }

    @Override
    public String formatReturn(LLVMType retType, String register) {
        return String.format("ret %s %s\n", retType.toString(), register);
    }

    @Override
    public String formatAlloca(String register, LLVMType type) {
        return String.format("%s = alloca %s\n", register, type.toString());
    }

    @Override
    public String formatStore(LLVMType sourceType, String sourceRegister, String destRegister) {
        return String.format("store %s %s, %s* %s\n",
                sourceType.toString(), sourceRegister,
                sourceType.toString(), destRegister);
    }

    @Override
    public String formatLoad(String register, LLVMType valueType, String sourcePointer) {
        return String.format("%s = load %s, %s* %s\n", register,
                valueType.toString(), valueType.toString(), sourcePointer);
    }

    @Override
    public String formatCall(String register, LLVMType retType, String methodName ,List<LLVMMethodParam> params) {
        String paramsString;
        if (params != null)
            paramsString = formatParams(params);
        else
            paramsString = "";

        if (retType == LLVMType.Void) {
            /* String voidParamsString = "";
            List<String> paramsTypes = new ArrayList<>();
            if (params != null && !params.isEmpty()) {
                for (var param : params) {
                    paramsTypes.add(param.getType().toString());
                }
                voidParamsString = String.format(" (%s)", String.join(",", paramsTypes));
            }

            return String.format("call %s%s %s(%s)\n", retType.toString(), voidParamsString, methodName, paramsString);*/
            
            if (methodName == "@print_int")
                return String.format("call %s (i32) %s(%s)\n", retType.toString(), methodName, paramsString);
            return String.format("call %s %s(%s)\n", retType.toString(), methodName, paramsString);
        }
        return String.format("%s = call %s %s(%s)\n", register,
                retType.toString(), methodName, paramsString);
    }

    @Override
    public String formatAdd(String register, LLVMType resultType, String first, String second) {
        return String.format("%s = add %s %s, %s\n", register, resultType.toString(),
                first, second);
    }

    @Override
    public String formatAnd(String register, LLVMType resultType, String first, String second) {
        return String.format("%s = and %s %s, %s\n", register, resultType.toString(),
                first, second);
    }

    @Override
    public String formatSub(String register, LLVMType resultType, String first, String second) {
        return String.format("%s = sub %s %s, %s\n", register, resultType.toString(),
                first, second);
    }

    @Override
    public String formatMul(String register, LLVMType resultType, String first, String second) {
        return String.format("%s = mul %s %s, %s\n", register, resultType.toString(),
                first, second);
    }

    @Override
    public String formatXOR(String register, LLVMType resultType, String first, String second) {
        return String.format("%s = xor %s %s, %s\n", register, resultType.toString(),
                first, second);
    }

    @Override
    public String formatCompare(String register, ComparisonType compareType, LLVMType type,
                                String register1, String register2) {
        return String.format("%s = icmp %s %s %s, %s\n",
                register,
                formatComparisonType(compareType),
                type.toString(),
                register1, register2);
    }

    @Override
    public String formatConditionalBreak(String booleanRegister, String ifLabel, String elseLabel) {
        return String.format("br i1 %s, label %s%s, label %s%s\n", booleanRegister, "%", ifLabel, "%", elseLabel);
    }

    @Override
    public String formatBreak(String label) {
        return String.format("br label %s%s\n", "%", label);
    }

    @Override
    public String formatLabelName(String labelName) {
        return String.format("%s:\n", labelName);
    }

    @Override
    public String formatBitcast(String register, LLVMType fromType, String fromRegister, LLVMType toType) {
        return String.format("%s = bitcast %s* %s to %s*\n",
                register, fromType.toString(),
                fromRegister, toType.toString());
    }

    @Override
    public String formatBitcast(String register, LLVMType fromType, String fromRegister, LLVMMethodSignature signature) {
        return String.format("%s = bitcast %s* %s to %s*\n",
                register, fromType.toString(),
                fromRegister, signature.toShortString());
    }

    @Override
    public String formatGetElementPtr(String register, LLVMType type, String pointerRegister, String rowIndex, String columnIndex) {
        String secondIndex = "";
        if (columnIndex != "") {
            secondIndex = String.format(", i32 %s", columnIndex);
        }
        return String.format("%s = getelementptr %s, %s* %s, i32 %s%s\n",
                register, type, type, pointerRegister, rowIndex, secondIndex);
    }

    @Override
    public String formatConstant(String register, int length, LLVMType type, String constantValue) {
        return String.format("@%s = constant [%d x %s] c%s\n", register, length, type.toString(), constantValue);
    }

    @Override
    public String formatGlobalVTable(String globalVtableName, List<LLVMMethodSignature> signatures) {
        List<String> signaturesStrings = new ArrayList<>();
        for (var sig : signatures) {
            signaturesStrings.add(sig.toString());
        }

        return String.format("%s = global [%d x i8*] [%s]\n",
                globalVtableName,
                signatures.size(),
                String.join(", ", signaturesStrings));
    }

    @Override
    public String formatPhi(String register, String valueIfLabel1, String label1, String valueIfLabel2, String label2) {
        return String.format("%s = phi i1 [%s, %s%s], [%s, %s%s]\n",
                register,
                valueIfLabel1,
                "%",
                label1,
                valueIfLabel2,
                "%",
                label2);
    }

    @Override
    public String formatRegisterName(String register) {
        return String.format("%s%s", "%", register);
    }

    public String formatFormalArgName(String formalArg) {
        return String.format(".%s", formalArg);
    }
}
