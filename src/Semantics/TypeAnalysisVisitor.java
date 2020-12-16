package Semantics;

import ast.*;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class TypeAnalysisVisitor extends ClassSemanticsVisitor {

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

            if (CheckForDuplicateFields(currentClassFields.keySet(),superClassFields.keySet())) {
                this.valid = false;
                return;
            }

            var superClassMethods = superClassSymbolTable.getVTable().getMethods();
            var currentClassMethods = currentClassSymbolTable.getVTable().getMethods();


            if (CheckForOverloadingMethods(currentClassMethods,superClassMethods)){
                this.valid = false;
                return;
            }
        }

        if (CheckForDuplicateValues(classDecl.fields().stream()
                .map(VarDecl::name).collect(toList())))
        {
            this.valid = false;
            return;
        }

        for (var fieldDecl : classDecl.fields()) {
            fieldDecl.accept(this);
        }


        if (CheckForDuplicateValues(classDecl.methoddecls().stream()
                .map(MethodDecl::name).collect(toList())))
        {
            this.valid = false;
            return;
        }

        for (var methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
        }

    }

    @Override
    public void visit(MethodDecl methodDecl) {
        methodDecl.returnType().accept(this);

        if (CheckForDuplicateValues(methodDecl.formals().stream()
                .map(VariableIntroduction::name).collect(toList())))
        {
            this.valid = false;
            return;
        }

        if (CheckForDuplicateValues(methodDecl.vardecls().stream()
                .map(VariableIntroduction::name).collect(toList())))
        {
            this.valid = false;
            return;
        }

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

        if (!lastType.equals(methodDecl.returnType()))
            valid = false;
    }

    @Override
    public void visit(MethodCallExpr e) {
        e.ownerExpr().accept(this);
        for (Expr arg : e.actuals()) {
            arg.accept(this);
        }
    }

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
