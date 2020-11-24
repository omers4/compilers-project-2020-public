package ast;

import java.util.HashMap;
import java.util.Map;

public class AstToSymbolTable implements IAstToSymbolTable{

    private Map<AstNode,SymbolTable> _mapping;

    public AstToSymbolTable() {
        _mapping = new HashMap<>();
    }

    @Override
    public SymbolTable getSymbolTable(AstNode node) {
        return _mapping.get(node);
    }

    public void addMapping(AstNode node, SymbolTable symbolTable) {
        _mapping.put(node,symbolTable);
    }
}
