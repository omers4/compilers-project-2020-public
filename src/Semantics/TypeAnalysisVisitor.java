package Semantics;

import LLVM.LLVMCommandFormatter;
import LLVM.LLVMRegisterAllocator;
import ast.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class TypeAnalysisVisitor extends ClassSemanticsVisitor {

    private boolean isOwner;
    private ClassInfo classInfo;

    public TypeAnalysisVisitor(IAstToSymbolTable symbolTable, ClassHierarchyForest hierarchy) {
        super(symbolTable, hierarchy);
    }

    private Boolean ContainsDuplicateValues(Collection<String> values1, Collection<String> values2) {

        HashSet<String> allFields = new HashSet<>();
        allFields.addAll(values1);
        allFields.addAll(values2);
        HashSet<String> fields1Set = new HashSet<>(values1);
        HashSet<String> fields2Set = new HashSet<>(values2);

        return !(fields1Set.size() == values1.size() && fields2Set.size() == values2.size()
                && values1.size() + values2.size() == allFields.size());
    }

    private Boolean AreFormalsTheSame(List<FormalArg> formals1, List<FormalArg> formals2) {
        if (formals1.size() != formals2.size())
            return false;

        for (int i = 0; i < formals1.size(); i++) {
            if (!formals2.get(i).type().equals(formals1.get(i).type()))
                return false;
        }

        return true;
    }

    private Boolean ContainsOverridingMethods(List<MethodDecl> methods1,
                                              List<MethodDecl> methods2) {

        List<String> method2names = methods2.stream().map(MethodDecl::name).collect(Collectors.toList());
        for (var method : methods1) {
            if (method2names.contains(method.name())) {
                var superMethod = methods2.stream().filter(x -> x.name()
                        .equals(method.name())).collect(Collectors.toList()).get(0);
                if (!AreFormalsTheSame(method.formals(), superMethod.formals()))
                    return true;
            }
        }

        return false;
    }

    private Boolean CheckForDuplicateValues(Collection<String> values) {
        HashSet<String> valuesSet = new HashSet<>(values);

        return !(values.size() == valuesSet.size());
    }

    @Override
    public void visit(Program program) {
        ast.LLVMPreProcessVisitor preProcessVisitor = new ast.LLVMPreProcessVisitor(symbolTable, new LLVMCommandFormatter(), new LLVMRegisterAllocator(symbolTable));
        preProcessVisitor.visit(program);
        this.classInfo = preProcessVisitor.getClassInfo();

        program.mainClass().accept(this);
        for (ClassDecl classdecl : program.classDecls()) {
            classdecl.accept(this);
        }
    }

    @Override
    public void visit(ClassDecl classDecl) {

        if (classDecl.superName() != null) {

            ClassDecl superClassNode = classInfo.getClassNode(classDecl.superName());

            Collection<String> curClassFields = classDecl.fields().stream().map(VariableIntroduction::name)
                    .collect(Collectors.toList());
            Collection<String> superClassFields = superClassNode.fields().stream().map(VariableIntroduction::name)
                    .collect(Collectors.toList());

            if (ContainsDuplicateValues(curClassFields, superClassFields)) {
                this.valid = false;
                return;
            }

            var superClassMethods = classDecl.methoddecls();
            var currentClassMethods = superClassNode.methoddecls();


            if (ContainsOverridingMethods(currentClassMethods, superClassMethods)) {
                this.valid = false;
                return;
            }
        }

        if (CheckForDuplicateValues(classDecl.fields().stream()
                .map(VarDecl::name).collect(toList()))) {
            this.valid = false;
            return;
        }

        for (var fieldDecl : classDecl.fields()) {
            fieldDecl.accept(this);
        }

//
        if (CheckForDuplicateValues(classDecl.methoddecls().stream()
                .map(MethodDecl::name).collect(toList()))) {
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
                .map(VariableIntroduction::name).collect(toList()))) {
            this.valid = false;
            return;
        }

        if (CheckForDuplicateValues(methodDecl.vardecls().stream()
                .map(VariableIntroduction::name).collect(toList()))) {
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

        if (valid && !lastType.equals(methodDecl.returnType()))
            valid = false;
    }

    @Override
    public void visit(MethodCallExpr e) {
        isOwner = true;
        e.ownerExpr().accept(this);
        String ownerStaticType = ((RefType) lastType).id();
        isOwner = false;
        ClassDecl classDecl = classInfo.getClassNode(ownerStaticType);
        var classMethods = classDecl.methoddecls();
        if (!classMethods.stream().map(MethodDecl::name).collect(Collectors.toList()).contains(e.methodId())) {
            valid = false;
            lastType = null;
            return;
        }
        var methodInfo = classMethods.stream().filter(x -> x.name()
                .equals(e.methodId())).collect(Collectors.toList()).get(0);
        var methodFormalsDeclaration = methodInfo.formals();
        for (int i = 0; i < e.actuals().size(); i++) {
            e.actuals().get(i).accept(this);

            String formalStaticType = ((RefType) lastType).id();

            // Get static type of declaration variable in the same way
            // TODO: need to check that accepting this new formal wont affect the progeam
            methodFormalsDeclaration.get(i).accept(this);
            ;
            String formalDeclStaticType = ((RefType) lastType).id();

            if (formalDeclStaticType.equals(formalStaticType))
                continue;

            String classSuperName = symbolTable.getSymbolTable(e).get(new SymbolItemKey(formalStaticType, SymbolType.Class))
                    .getVTable().superName();

            // We can check only for ine super name because there is not deep inheritance in mini java
            if (classSuperName != null && classSuperName.equals(formalDeclStaticType))
                continue;

            valid = false;

        }
        this.lastType = methodInfo.returnType();
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

    @Override
    public void visit(NewObjectExpr e) {

        lastType = new RefType(e.classId());
        if (null == classInfo.getClassNode(e.classId())) {
            valid = false;
            lastType = null;
        }
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
