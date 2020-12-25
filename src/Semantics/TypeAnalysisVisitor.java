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
    private ClassDecl curClass;

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

    private Boolean isSubTypeOf(AstType A, AstType B) {
        var type = A;
        while(type != null) {
            if (type.equals(B))
                return true;
            if (!(type instanceof RefType && B instanceof RefType))
                return false;

            var info = classInfo.getClassNode(((RefType) type).id());
            if (info.superName() == null)
                break;

            type = new RefType(info.superName());
        }

        return false;
    }

    private Boolean ContainsOverloadingMethods(List<MethodDecl> methods,
                                               List<MethodDecl> parentMethods) {

        List<String> method2names = parentMethods.stream().map(MethodDecl::name).collect(Collectors.toList());
        for (var method : methods) {
            if (method2names.contains(method.name())) {
                var superMethod = parentMethods.stream().filter(x -> x.name()
                        .equals(method.name())).collect(Collectors.toList()).get(0);
                if (!AreFormalsTheSame(method.formals(), superMethod.formals()))
                    return true;
                if(!isSubTypeOf(method.returnType(), superMethod.returnType())) {
                    return true;
                }

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
            curClass = classdecl;
            classdecl.accept(this);
        }
    }

    @Override
    public void visit(ClassDecl classDecl) {

        Collection<String> curClassFields = classDecl.fields().stream().map(VariableIntroduction::name)
                .collect(Collectors.toList());
        var currentClassMethods = classDecl.methoddecls();
        var superName = classDecl.superName();

        while (superName != null) {

            ClassDecl superClassNode = classInfo.getClassNode(superName);

            Collection<String> superClassFields = superClassNode.fields().stream().map(VariableIntroduction::name)
                    .collect(Collectors.toList());

            if (valid && ContainsDuplicateValues(curClassFields, superClassFields)) {
                this.valid = false;
                return;
            }

            var superClassMethods = superClassNode.methoddecls();

            if (valid && ContainsOverloadingMethods(currentClassMethods, superClassMethods)) {
                this.valid = false;
                return;
            }

            superName = superClassNode.superName();
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
        if (valid && CheckForDuplicateValues(classDecl.methoddecls().stream()
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

        var formal_names = methodDecl.formals().stream()
                .map(VariableIntroduction::name).collect(toList());

        if (valid && CheckForDuplicateValues(formal_names)) {
            this.valid = false;
            return;
        }

        var var_names = methodDecl.vardecls().stream()
                .map(VariableIntroduction::name).collect(toList());

        if (valid && CheckForDuplicateValues(var_names)) {
            this.valid = false;
            return;
        }

        if (valid && ContainsDuplicateValues(formal_names, var_names)) {
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

        if (valid && !isSubTypeOf(lastType,methodDecl.returnType()))
            valid = false;
    }

    public Boolean isValidMethodUsage(List<Expr> actuals, List<FormalArg> args) {
        try {
            if(actuals.size() != args.size())
                return false;

            for (int i = 0; i < actuals.size(); i++) {
                actuals.get(i).accept(this);
                AstType actualType = lastType;

                // TODO: need to check that accepting this new formal wont affect the progeam
                args.get(i).accept(this);
                AstType formalType = lastType;

                if (actualType.equals(formalType))
                    continue;

                if (!(actualType instanceof RefType && formalType instanceof RefType)) {
                    return false;
                }

                var formalTypeId = ((RefType) formalType).id();
                var superName = classInfo.getClassNode(((RefType) actualType).id()).superName();

                while (superName != null) {
                    if (superName.equals(formalTypeId)) {
                        break;
                    }
                    superName = classInfo.getClassNode(superName).superName();
                }

                if (superName == null)
                    return false;

            }

            return true;
        }
        catch (Exception e) {
            return false;
        }

    }

    @Override
    public void visit(ThisExpr e) {
            lastType = new RefType(curClass.name());
    }


    private Collection<MethodDecl> getStaticTypeMethods(String staticType) {

        List<MethodDecl> methods = new ArrayList<>();
        var className = staticType;
        while (className != null) {
            ClassDecl classDecl = classInfo.getClassNode(className);
            var classMethods = classDecl.methoddecls();
            methods.addAll(classMethods);
            className = classDecl.superName();
        }

        return  methods;
    }

    @Override
    public void visit(MethodCallExpr e) {
        if (! (e.ownerExpr() instanceof NewObjectExpr || e.ownerExpr() instanceof ThisExpr ||
                e.ownerExpr() instanceof IdentifierExpr)) {
            valid = false;
            return;
        }


        isOwner = true;
        e.ownerExpr().accept(this);
        if (!valid)
             return;

        // TODO: Should be surrounded by try catch clause?
        if (!(lastType instanceof RefType)) {
            valid = false;
            return;
        }

        String ownerStaticType = ((RefType) lastType).id();
        isOwner = false;

        var classMethods = getStaticTypeMethods(ownerStaticType);
        if (valid && !classMethods.stream().map(MethodDecl::name).collect(Collectors.toList()).contains(e.methodId())) {
            valid = false;
            lastType = null;
            return;
        }
        var methodInfo = classMethods.stream().filter(x -> x.name()
                .equals(e.methodId())).collect(Collectors.toList()).get(0);
        if(!isValidMethodUsage(e.actuals(),methodInfo.formals())) {
            valid = false;
            lastType =null;
            return;
        }

        this.lastType = methodInfo.returnType();
    }

    private void visitBinaryExpr(BinaryExpr e, AstType requiredType) {
        lastType = null;
        e.e1().accept(this);
        if (lastType == null || lastType.getClass() != requiredType.getClass()) {
            valid = false;
            return;
        }

        e.e2().accept(this);
        if (lastType == null || lastType.getClass() != requiredType.getClass()) {
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
        if (valid) {
            lastType = new BoolAstType();
        }
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
            if (lastType == null || lastType.getClass() != staticType.getClass()) {
                valid = false;
                return;
            }

            // if it's a ref, we do an extra validation, that the classes inherit each other. otherwise it's not valid
            if (staticType instanceof RefType) {
                RefType sourceRef = (RefType) staticType;
                RefType destRef = (RefType) lastType;
                // A a = new B(); is allowed

                // Just to verify they exist
                var sourceCls = classInfo.getClassNode(sourceRef.id());
                var destCls =  classInfo.getClassNode(destRef.id());

                if (sourceCls == null || destCls == null || !isSubTypeOf(destRef, sourceRef)) {
                    valid = false;
                }
            }

        } catch (NoSuchElementException exc) {
            valid = false; // expr not an int array
            return;
        }

    }
}
