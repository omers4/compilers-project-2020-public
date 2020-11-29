package ast;

import LLVM.LLVMMethodSignature;
import LLVM.ObjectVTable;

public class SymbolTableItem {

    private String _id;
    private SymbolType _kind;
    private String _registerAddr;
    private AstType _type;
    private ObjectVTable _vTable;
    private MethodSignature _methodSignature;

    public SymbolTableItem(String id, SymbolType kind, AstType type) {
        this._id = id;
        this._kind = kind;
        _registerAddr = null;
        _type = type;
    }

    public SymbolTableItem(String id, SymbolType kind,  ObjectVTable vTable) {
        this._id = id;
        this._kind = kind;
        _registerAddr = null;
        _vTable = vTable;
    }

    public SymbolTableItem(String id, SymbolType kind,  MethodSignature methodSignature) {
        this._id = id;
        this._kind = kind;
        _registerAddr = null;
        _methodSignature = methodSignature;
    }

    public String getRegisterId() {
        return _registerAddr;
    }

    public AstType getType() {
        return _type;
    }

    public String getId() { return _id;}

    public void setRegisterId(String id) {
        _registerAddr = id;
    }

    public ObjectVTable getVTable() {return _vTable; }

    public MethodSignature getMethodSignature() {return _methodSignature; }

}
