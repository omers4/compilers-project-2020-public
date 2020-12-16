package Semantics;

import ast.*;

import java.util.ArrayList;

public class IdentifierSemanticsVisitor extends ClassSemanticsVisitor{

    private ArrayList<String> classes_names;

    public IdentifierSemanticsVisitor(IAstToSymbolTable symbolTable, ClassHierarchyForest hierarchy) {
        super(symbolTable, hierarchy);
        classes_names = (ArrayList<String>) hierarchy.getTreesNames();
    }

    /////////////////////RefType/////////////////////

    private void ref_in_classes(String type_declaration){
        for(var class_name: this.classes_names){
            if(type_declaration == class_name)
                return;
        }
        valid = false;
    }

    // get here for all decla - formalArg.type(), varDecl.type() (fields and locals), methodDecl.returnType()
    @Override
    public void visit(RefType t) {
        ref_in_classes(t.id());
    }

    @Override
    public void visit(NewObjectExpr e) {
        ref_in_classes(e.classId());
    }

}
