package ast;

public class SymbolTableItem {

    private String _id;
    private SymbolType _kind;
    private String _registerAddr;

    public SymbolTableItem(String id, SymbolType kind) {
        this._id = id;
        this._kind = kind;
        _registerAddr = null;
    }

    public String getRegisterId() {
        return _registerAddr;
    }

    public void setRegisterId(String id) {
        _registerAddr = id;
    }
}
