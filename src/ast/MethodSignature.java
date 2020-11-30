package ast;

import LLVM.LLVMMethodParam;
import LLVM.LLVMMethodSignature;
import LLVM.LLVMType;

import java.util.List;

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


    public LLVMMethodSignature toLLVMSignature() {

        return null;
        //return new LLVMMethodSignature(name, LLVMType.)
    }
}
