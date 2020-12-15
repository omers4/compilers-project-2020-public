package Semantics;

import ast.*;
import java.util.NoSuchElementException;

public class TypeAnalysisVisitor extends ClassSemanticsVisitor {
    private void visitBinaryExpr(BinaryExpr e, AstType requiredType) {
        lastType = null;
        e.e1().accept(this);
        if (lastType.getClass() != requiredType.getClass()) {
            valid = false;
            return;
        }

        e.e2().accept(this);
        if (lastType.getClass() != requiredType.getClass()) {
            valid = false;
            return;
        }

        lastType = requiredType;
    }

    public TypeAnalysisVisitor(IAstToSymbolTable symbolTable, ClassHierarchyForest hierarchy) {
        super(symbolTable, hierarchy);
    }

    @Override
    public void visit(AndExpr e) {
        visitBinaryExpr(e, new BoolAstType());
    }

    @Override
    public void visit(LtExpr e) {
        visitBinaryExpr(e, new IntAstType());
        ;
    }

    @Override
    public void visit(AddExpr e) {
        visitBinaryExpr(e, new IntAstType());
        ;
    }

    @Override
    public void visit(SubtractExpr e) {
        visitBinaryExpr(e, new IntAstType());
    }

    @Override
    public void visit(MultExpr e) {
        visitBinaryExpr(e, new IntAstType());
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        lastType = null;

        var symbolTableOfStmt = symbolTable.getSymbolTable(assignArrayStatement);
        var symbolTableEntry = symbolTableOfStmt.get(new SymbolItemKey(assignArrayStatement.lv(), SymbolType.Var));
        if (symbolTableEntry == null || !(symbolTableEntry.getType() instanceof IntArrayAstType)) {
            valid = false; // variable not found
            return;
        }

        assignArrayStatement.index().accept(this);
        if (!(lastType instanceof IntAstType)) {
            valid = false;
            return;
        }

        lastType = null;
        assignArrayStatement.rv().accept(this);
        if (!(lastType instanceof IntAstType)) {
            valid = false;
        }

    }

    @Override
    public void visit(ArrayAccessExpr e) {
        e.arrayExpr().accept(this);
        if (!(lastType instanceof IntArrayAstType)) {
            valid = false;
            return;
        }

        lastType = null;
        e.indexExpr().accept(this);
        if (!(lastType instanceof IntAstType)) {
            valid = false;
            return;
        }

        lastType = new IntAstType();
    }

    @Override
    public void visit(NewIntArrayExpr e) {
        e.lengthExpr().accept(this);
        if (!(lastType instanceof IntAstType)) {
            valid = false;
            lastType = null;
            return;
        }
        lastType = new IntArrayAstType();
    }

    @Override
    public void visit(IdentifierExpr e) {
        var symbolTableOfStmt = symbolTable.getSymbolTable(e);

        try {
            var symbolTableEntry = symbolTableOfStmt.get(new SymbolItemKey(e.id(), SymbolType.Var));
            lastType = symbolTableEntry.getType();
        } catch (NoSuchElementException exc) {
            valid = false; // expr not an int array
            lastType = null;
        }
    }

    @Override
    public void visit(ArrayLengthExpr e) {
        e.arrayExpr().accept(this);

        if (!(lastType instanceof IntArrayAstType)) {
            valid = false;
            lastType = null;
        }

        lastType = new IntAstType();

    }

    @Override
    public void visit(IfStatement ifStatement) {
        lastType = null;
        ifStatement.cond().accept(this);
        if (!(lastType instanceof BoolAstType)) {
            valid = false;
            return;
        }

        lastType = null;
        ifStatement.elsecase().accept(this);
        if (!(lastType instanceof BoolAstType)) {
            valid = false;
        }

    }

    @Override
    public void visit(WhileStatement whileStatement) {
        lastType = null;
        whileStatement.cond().accept(this);
        if (!(lastType instanceof BoolAstType)) {
            valid = false;
        }
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
        lastType = null;
        sysoutStatement.arg().accept(this);
        if (!(lastType instanceof IntAstType)) {
            valid = false;
        }
    }

    public void visit(AssignStatement assignStatement) {
        var symbolTableOfStmt = symbolTable.getSymbolTable(assignStatement);

        // First we need to get the type of the lv
        try {
            var symbolTableEntry = symbolTableOfStmt.get(new SymbolItemKey(assignStatement.lv(), SymbolType.Var));
            var staticType = symbolTableEntry.getType();
            // if we are here we found the lv and it's type.]


            assignStatement.rv().accept(this);
            if (lastType.getClass() != staticType.getClass()) {
                valid = false;
                return;
            }

            // if it's a ref, we do an extra validation, that the classes inherit each other. otherwise it's not valid
            if (staticType instanceof RefType) {
                RefType sourceRef = (RefType) staticType;
                RefType destRef = (RefType) lastType;
                // A a = new B(); is allowed

                // Just to verify they exist
                symbolTableOfStmt.get(new SymbolItemKey(sourceRef.id(), SymbolType.Class));
                symbolTableOfStmt.get(new SymbolItemKey(destRef.id(), SymbolType.Class));

                if (!hierarchy.isParent(sourceRef.id(), destRef.id())) {
                    valid = false;
                }
            }

        } catch (NoSuchElementException exc) {
            valid = false; // expr not an int array
            return;
        }

    }
}
