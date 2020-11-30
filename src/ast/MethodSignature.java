package ast;

import LLVM.LLVMMethodParam;
import LLVM.LLVMMethodSignature;
import LLVM.LLVMType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MethodSignature  {
    private String name;
    private AstType ret;
    private List<FormalArg> params;

    public MethodSignature(String name, AstType ret, List<FormalArg> params) {
        this.name=name;
        this.ret=ret;
        this.params=params;
    }

    public String getName() { return this.name; }
    public AstType getRet() { return this.ret; }
    public List<FormalArg> getFormals() { return this.params; }

    private LLVMType convertFromAst(AstType astType) {
        if (astType.getClass() == IntAstType.class) {
            return LLVMType.Int;
        }
        if (astType.getClass() == BoolAstType.class) {
            return LLVMType.Boolean;
        }
        return LLVMType.Address;
    }


    public LLVMMethodSignature toLLVMSignature() {

        List<LLVMMethodParam> llvmMethodParams = new ArrayList<>();
        llvmMethodParams.add(new LLVMMethodParam(LLVMType.Address,""));
        for (var param : params) {
            llvmMethodParams.add(new LLVMMethodParam(convertFromAst(param.type()),param.name()));
        }

        return new LLVMMethodSignature(name, convertFromAst(this.ret), llvmMethodParams);
    }
}
