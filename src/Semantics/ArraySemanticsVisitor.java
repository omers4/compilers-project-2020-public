package Semantics;

import ast.*;

public class ArraySemanticsVisitor extends ClassSemanticsVisitor {

    private AstType lastType;

    public ArraySemanticsVisitor(IAstToSymbolTable symbolTable) {
        super(symbolTable);
    }



    @Override
    public void visit(NewIntArrayExpr e) {
        e.lengthExpr().accept(this);
        if (!(lastType instanceof IntAstType)) {
            valid = false;
        }
    }

    @Override
    public void visit(IdentifierExpr e) {

    }

    @Override
    public void visit(ArrayLengthExpr e) {
        lastType = null;
        e.arrayExpr().accept(this);
        // TODO somehow get the type of the last expression that was parsed.
        if (!(lastType instanceof IntArrayAstType)) {
            valid = false;
        }

//        IdentifierExpr idExpr = (IdentifierExpr) e.arrayExpr();
//
//        var symbolTableOfStmt = symbolTable.getSymbolTable(e);
//        var symbolTableEntry = symbolTableOfStmt.get(new SymbolItemKey(idExpr.id(), SymbolType.Var));
//        if (symbolTableEntry == null || !(symbolTableEntry.getType() instanceof IntArrayAstType)) {
//            valid = false; // expr not an int array
//            return;
//        }

    }
}
