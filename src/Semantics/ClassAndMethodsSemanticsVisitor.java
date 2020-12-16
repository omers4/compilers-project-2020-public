package Semantics;

import ast.*;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class ClassAndMethodsSemanticsVisitor extends ClassSemanticsVisitor {

    public ClassAndMethodsSemanticsVisitor(IAstToSymbolTable symbolTable, ClassHierarchyForest hierarchy) {
        super(symbolTable, hierarchy);
    }

    private Boolean CheckForDuplicateFields(Collection<String> fields1, Collection<String> fields2) {

        HashSet<String> allFields = new HashSet<>();
        allFields.addAll(fields1);
        allFields.addAll(fields2);
        HashSet<String> fields1Set = new HashSet<>(fields1);
        HashSet<String> fields2Set = new HashSet<>(fields2);

        return fields1Set.size() == fields1.size() && fields2Set.size() == fields2.size()
                && fields1.size() + fields2.size() == allFields.size();
    }

    private Boolean AreFormalsTheSame(List<FormalArg> formals1, List<FormalArg> formals2) {
        if (formals1.size() != formals2.size())
            return false;

        for (int i = 0; i <formals1.size(); i++) {
            if(formals2.get(i).type().equals(formals1.get(i).type()))
                return false;
        }

        return true;
    }

    private Boolean CheckForOverloadingMethods(LinkedHashMap<String, MethodSignature> methods1,
                                               LinkedHashMap<String, MethodSignature> methods2) {

        List<Map.Entry<String, MethodSignature>> intersectionMethods = methods1.entrySet().stream()
                .distinct()
                .filter(methods2.entrySet()::contains)
                .collect(toList());

        for (var method : intersectionMethods) {
            var otherMethod = methods2.get(method.getKey());
            if (AreFormalsTheSame(method.getValue().getFormals(), otherMethod.getFormals()))
                return false;
        }

        return true;
    }

    private Boolean CheckForDuplicateValues(Collection<String> values) {
        HashSet<String> valuesSet = new HashSet<>(values);

        return values.size() == valuesSet.size();
    }

    @Override
    public void visit(ClassDecl classDecl) {

        if (classDecl.superName() != null) {
            SymbolTableItem superClassSymbolTable = symbolTable.getSymbolTable(classDecl)
                    .get(new SymbolItemKey(classDecl.superName(), SymbolType.Class));

            SymbolTableItem currentClassSymbolTable = symbolTable.getSymbolTable(classDecl)
                    .get(new SymbolItemKey(classDecl.name(), SymbolType.Class));

            var superClassFields = superClassSymbolTable.getVTable().getFields();
            var currentClassFields = currentClassSymbolTable.getVTable().getFields();

            if (CheckForDuplicateFields(currentClassFields.keySet(),superClassFields.keySet()))
                this.valid = false;

            var superClassMethods = superClassSymbolTable.getVTable().getMethods();
            var currentClassMethods = currentClassSymbolTable.getVTable().getMethods();


            if (CheckForOverloadingMethods(currentClassMethods,superClassMethods))
                this.valid = false;
        }

        if (CheckForDuplicateValues(classDecl.fields().stream()
                .map(VarDecl::name).collect(toList())))
            this.valid = false;

        for (var fieldDecl : classDecl.fields()) {
            fieldDecl.accept(this);
        }


        if (CheckForDuplicateValues(classDecl.methoddecls().stream()
                .map(MethodDecl::name).collect(toList())))
            this.valid = false;

        for (var methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
        }

    }

    @Override
    public void visit(MethodDecl methodDecl) {
        methodDecl.returnType().accept(this);

        if (CheckForDuplicateValues(methodDecl.formals().stream()
                .map(VariableIntroduction::name).collect(toList())))
            this.valid = false;

        if (CheckForDuplicateValues(methodDecl.vardecls().stream()
                .map(VariableIntroduction::name).collect(toList())))
            this.valid = false;

        for (var formal : methodDecl.formals()) {
            formal.accept(this);
        }
        for (var varDecl : methodDecl.vardecls()) {
            varDecl.accept(this);
        }
        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
        }
        methodDecl.ret().accept(this);
    }
    
    @Override
    public void visit(MethodCallExpr e) {
        e.ownerExpr().accept(this);
        for (Expr arg : e.actuals()) {
            arg.accept(this);
        }
    }
}
