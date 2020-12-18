package ast;

public class SymbolTableItem {

    private String _id;
    private String _registerAddr;
    private AstType _type;
    private ObjectVTable _vTable;
    private MethodSignature _methodSignature;
    private SymbolType _subKind;
    private AstNode _node;


    public SymbolTableItem(String id, AstType type, SymbolType subKind) {
        this._id = id;
        _registerAddr = null;
        _type = type;
        _subKind = subKind;
    }

    public SymbolTableItem(String id, ObjectVTable vTable) {
        this._id = id;
        _registerAddr = null;
        _vTable = vTable;
    }

    public SymbolTableItem(String id, MethodSignature methodSignature) {
        this._id = id;
        _registerAddr = null;
        _methodSignature = methodSignature;
    }

    public SymbolTableItem(String id, ObjectVTable vTable, AstNode node) {
        this._id = id;
        _registerAddr = null;
        _vTable = vTable;
        _node = node;
    }

    public String getRegisterId() {
        return _registerAddr;
    }

    public AstType getType() {
        return _type;
    }

    public SymbolType getKind() {
        return _subKind;
    }

    public String getId() { return _id;}

    public void setRegisterId(String id) {
        _registerAddr = id;
    }

    public ObjectVTable getVTable() {return _vTable; }

    public AstNode getNode() {return _node; }

    public MethodSignature getMethodSignature() {return _methodSignature; }

}
