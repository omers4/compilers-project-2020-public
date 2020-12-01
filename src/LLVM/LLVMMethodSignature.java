package LLVM;

import java.util.ArrayList;
import java.util.List;

public class LLVMMethodSignature {
    private String name;
    private LLVMType ret;
    private List<LLVMMethodParam> params;

    public LLVMMethodSignature(String name, LLVMType ret, List<LLVMMethodParam> params) {
        this.name=name;
        this.ret=ret;
        this.params=params;
    }

    public String toShortString() {
        List<String> paramsString = new ArrayList<>();
        for (var param: params) {
            paramsString.add(param.getType().toString());
        }

        return String.format("%s (%s)", ret.toString(), String.join(", ", paramsString));
    }

    public String toString() {
        List<String> paramsString = new ArrayList<>();
        for (var param: params) {
            paramsString.add(param.getType().toString());
        }

        return String.format("i8* bitcast (%s (%s)* %s to i8*)", ret.toString(),
                String.join(", ", paramsString),
                name);
    }
}
