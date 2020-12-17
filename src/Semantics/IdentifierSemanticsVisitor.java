package Semantics;

import ast.*;

import java.util.ArrayList;

public class IdentifierSemanticsVisitor extends ClassSemanticsVisitor{

    private ArrayList<String> classes_names = new ArrayList<>();
    private boolean from_owner;

    public IdentifierSemanticsVisitor(IAstToSymbolTable symbolTable, ClassHierarchyForest hierarchy) {
        super(symbolTable, hierarchy);
        hierarchy.getTreesNames(classes_names);
        from_owner = false;
    }

    /////////////////////RefType/////////////////////

    private void type_in_classes(String type_declaration){
        for(var class_name: this.classes_names){
            if(type_declaration.equals(class_name))
                return;
        }
        valid = false;
    }

    // A type declaration of a reference type of A refers to classes that are defined somewhere in the file (8 | 5a)
    // get here for all decla - formalArg.type(), varDecl.type() (fields and locals), methodDecl.returnType()
    @Override
    public void visit(RefType t) {
        type_in_classes(t.id());
    }

    // new A() is invoked for a class A that is defined somewhere in the file (9 | 5b)
    @Override
    public void visit(NewObjectExpr e) {
        type_in_classes(e.classId());
    }

    @Override
    public void visit(MethodCallExpr e) {
        from_owner = true;
        e.ownerExpr().accept(this);
        from_owner = false;
        for (Expr arg : e.actuals()) {
            arg.accept(this);
        }
    }

    // In method invocation, the static type of the object is a reference type (not int, bool, or int[]) (10 | 5c)
    @Override
    public void visit(IdentifierExpr e) {

        // A reference in an expression to a variable, is to a local variable or formal parameter
        // defined in the current method, or to a field defined in the current class or its superclasses. (14 | 7a)
        try {
            var symbolTableOfStmt = symbolTable.getSymbolTable(e);
            var symbolTableEntry = symbolTableOfStmt.get(new SymbolItemKey(e.id(), SymbolType.Var));
        }
        catch (Exception exception){
            valid = false;
            return;
        }

        if(from_owner){
            var symbolTableOfStmt = symbolTable.getSymbolTable(e);
            var symbolTableEntry = symbolTableOfStmt.get(new SymbolItemKey(e.id(), SymbolType.Var));

            AstType IdentifierType = symbolTableEntry.getType();
            if (!(IdentifierType instanceof RefType)){
                valid = false;
            }
        }
    }

    @Override
    public void visit(AssignStatement assignStatement) {
        assignStatement.rv().accept(this);
        // looking for lv as var.
        try{
            var symbolTableOfStmt = symbolTable.getSymbolTable(assignStatement);
            var symbolTableEntry = symbolTableOfStmt.get(new SymbolItemKey(assignStatement.lv(), SymbolType.Var));
        }
        catch (Exception exception){
            valid = false;
            return;
        }
    }

}
