package Semantics;

import ast.*;

import java.util.NoSuchElementException;

public class TypeAnalysisVisitor extends ClassSemanticsVisitor {
    private void visitBinaryExpr(BinaryExpr e, Class requiredType) {
        lastType = null;
        e.e1().accept(this);
        if (lastType != requiredType) {
            valid = false;
            return;
        }

        e.e2().accept(this);
        if (lastType != requiredType) {
            valid = false;
            return;
        }

        lastType = requiredType;
    }

    public TypeAnalysisVisitor(IAstToSymbolTable symbolTable) {
        super(symbolTable);
    }

    @Override
    public void visit(AndExpr e) {
        visitBinaryExpr(e, BoolAstType.class);
    }

    @Override
    public void visit(LtExpr e) {
        visitBinaryExpr(e, IntAstType.class);
        ;
    }

    @Override
    public void visit(AddExpr e) {
        visitBinaryExpr(e, IntAstType.class);
        ;
    }

    @Override
    public void visit(SubtractExpr e) {
        visitBinaryExpr(e, IntAstType.class);
    }

    @Override
    public void visit(MultExpr e) {
        visitBinaryExpr(e, IntAstType.class);
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        lastType = null;

        var symbolTableOfStmt = symbolTable.getSymbolTable(assignArrayStatement);
        var symbolTableEntry = symbolTableOfStmt.get(new SymbolItemKey(assignArrayStatement.lv(), SymbolType.Var));
        if (symbolTableEntry == null || symbolTableEntry.getType().getClass() != IntArrayAstType.class) {
            valid = false; // variable not found
            return;
        }

        assignArrayStatement.index().accept(this);
        if (lastType != IntAstType.class) {
            valid = false;
            return;
        }

        lastType = null;
        assignArrayStatement.rv().accept(this);
        if (lastType != IntAstType.class) {
            valid = false;
        }

    }

    @Override
    public void visit(ArrayAccessExpr e) {
        e.arrayExpr().accept(this);
        if (lastType != IntArrayAstType.class) {
            valid = false;
            return;
        }

        lastType = null;
        e.indexExpr().accept(this);
        if (lastType != IntAstType.class) {
            valid = false;
            return;
        }

        lastType = IntAstType.class;
    }

    @Override
    public void visit(NewIntArrayExpr e) {
        e.lengthExpr().accept(this);
        if (!(lastType == IntAstType.class)) {
            valid = false;
            lastType = null;
            return;
        }
        lastType = IntArrayAstType.class;
    }

    @Override
    public void visit(IdentifierExpr e) {
        var symbolTableOfStmt = symbolTable.getSymbolTable(e);

        try {
            var symbolTableEntry = symbolTableOfStmt.get(new SymbolItemKey(e.id(), SymbolType.Var));
            lastType = symbolTableEntry.getType().getClass();
        } catch (NoSuchElementException exc) {
            valid = false; // expr not an int array
            lastType = null;
        }
    }

    @Override
    public void visit(ArrayLengthExpr e) {
        e.arrayExpr().accept(this);

        if (!(lastType == IntArrayAstType.class)) {
            valid = false;
            lastType = null;
        }

        lastType = IntAstType.class;

    }

    @Override
    public void visit(IfStatement ifStatement) {
        lastType = null;
        ifStatement.cond().accept(this);
        if (lastType != BoolAstType.class) {
            valid = false;
            return;
        }

        lastType = null;
        ifStatement.elsecase().accept(this);
        if (lastType != BoolAstType.class) {
            valid = false;
        }

    }

    @Override
    public void visit(WhileStatement whileStatement) {
        lastType = null;
        whileStatement.cond().accept(this);
        if (lastType != BoolAstType.class) {
            valid = false;
        }
    }

    public void visit(SysoutStatement sysoutStatement) {
        lastType = null;
        sysoutStatement.arg().accept(this);
        if (!(lastType == IntAstType.class)) {
            valid = false;
        }
    }
}
