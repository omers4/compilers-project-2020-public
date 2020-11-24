package ast;

public interface IAstToSymbolTable {

    public SymbolTable getSymbolTable(AstNode node);
}
