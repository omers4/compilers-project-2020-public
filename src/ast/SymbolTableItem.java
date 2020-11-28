package ast;

import LLVM.ObjectVTable;

public class SymbolTableItem {

    private String _id;
    private SymbolType _kind;
    private String _registerAddr;
    private AstType _type;
    private ObjectVTable _vTable;

    public SymbolTableItem(String id, SymbolType kind, AstType type) {
        this._id = id;
        this._kind = kind;
        _registerAddr = null;
        _type = type;
    }

    public String getRegisterId() {
        return _registerAddr;
    }

    public AstType getType() {
        return _type;
    }

    public void setRegisterId(String id) {
        _registerAddr = id;
    }


}
