package ast;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class SymbolTable {

    private Map<SymbolItemKey, SymbolTableItem> _scopeToVars;
    private SymbolTable _parentSymbolTable;

    public SymbolTable(SymbolTable parentSymbolTable) {
        _scopeToVars = new HashMap<>();
        this._parentSymbolTable = parentSymbolTable;
    }

    public void addSymbol(SymbolItemKey key, SymbolTableItem symbol) {
        _scopeToVars.put(key, symbol);
    }

    public SymbolTableItem get(SymbolItemKey key) throws NoSuchElementException {
        SymbolTableItem value = _scopeToVars.get(key);
        if(value == null && _parentSymbolTable != null)
            return _parentSymbolTable.get(key);
        else if(value == null)
            throw new NoSuchElementException();
        else
            return value;
    }

    public void printSymbolTableItems(){
        System.out.println("=========== Printing Symbol Table =============");
        if (_parentSymbolTable != null){
            System.out.println("*********** the pre symbol table ************");
            _parentSymbolTable.printSymbolTableItems();
        }
        System.out.println("*********** this symbol table ************");
        for (var entry: _scopeToVars.entrySet()){
            System.out.println(entry.getKey());
        }
    }

}
