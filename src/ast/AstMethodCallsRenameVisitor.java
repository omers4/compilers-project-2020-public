package ast;

import java.util.ArrayList;
import java.util.List;

/*
* The target of this class is to rename the *calls* to a method "foo" to a call on the method "bar".
* There are 3 such cases-
* 1. new B().foo();
* 2. this.foo();
* 3. B b; ....; b.foo();
* */
public class AstMethodCallsRenameVisitor implements Visitor {

    // Used for understanding if, for example, B is a subclass of A.
    private ClassHierarchyForest classHierarchy;
    // Used to represent the predecessor of the class of the one the original function we are trying to change
    private ClassTree predecessor;

    private String originalName;
    private int originalLine;
    private String newName;

    private ArrayList<ClassTree> familyList; // predecessor family (predecessor as root)
    private boolean isInFamily; // used to know if in class scope in the family
    private boolean changeMethod; // used to know if we need to change method name
    private boolean isMethodCall; // used to know if we in owner method expr


    public AstMethodCallsRenameVisitor(ClassHierarchyForest classHierarchy, ClassTree predecessor,
                                       String originalName, int originalLine, String newName) {
        this.classHierarchy = classHierarchy;
        this.predecessor = predecessor;
        this.originalName = originalName;
        this.originalLine = originalLine;
        this.newName = newName;

        this.isMethodCall = false;
        this.changeMethod = false;
        this.isInFamily = false;
        this.familyList = null;
        if (this.predecessor != null){
            this.familyList = new ArrayList<>();
            this.familyList.add(predecessor);
            this.familyList = (ArrayList<ClassTree>) predecessor.getFamilyList(this.familyList);
        }

    }


    private boolean isClassNameInPredecessorFamily(String name){
        if (this.predecessor != null)
            return (this.predecessor).isNameInFamily(this.familyList, name);
        return false;
    }


    private void visitBinaryExpr(BinaryExpr e, String infixSymbol) {
        e.e1().accept(this);
        e.e2().accept(this);
    }

    @Override
    public void visit(Program program) {
        program.mainClass().accept(this);
        for (ClassDecl classdecl : program.classDecls()) {
            classdecl.accept(this);
        }
    }

    @Override
    public void visit(ClassDecl classDecl) {
        //if (classDecl.superName() != null) {}

        if (this.isClassNameInPredecessorFamily(classDecl.name())) {
            this.isInFamily = true;
        }

        for (var fieldDecl : classDecl.fields()) {
            fieldDecl.accept(this);
        }
        for (var methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
        }

        this.isInFamily = false;
    }

    @Override
    public void visit(MainClass mainClass) {
        mainClass.mainStatement().accept(this);
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        // TODO: renameMethodNameInSubtree or here?
        if (this.isInFamily && methodDecl.name().equals(this.originalName)) {
            methodDecl.setName(this.newName);
        }

        methodDecl.returnType().accept(this);

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
    public void visit(FormalArg formalArg) {
        formalArg.type().accept(this);
    }

    @Override
    public void visit(VarDecl varDecl) {
        varDecl.type().accept(this);
    }

    @Override
    public void visit(BlockStatement blockStatement) {
        for (var s : blockStatement.statements()) {
            s.accept(this);
        }
    }

    @Override
    public void visit(IfStatement ifStatement) {
        ifStatement.cond().accept(this);
        ifStatement.thencase().accept(this);
        ifStatement.elsecase().accept(this);
    }

    @Override
    public void visit(WhileStatement whileStatement) {
        whileStatement.cond().accept(this);
        whileStatement.body().accept(this);
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
        sysoutStatement.arg().accept(this);
    }

    @Override
    public void visit(AssignStatement assignStatement) {
        assignStatement.rv().accept(this);
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        assignArrayStatement.index().accept(this);
        assignArrayStatement.rv().accept(this);
    }

    @Override
    public void visit(AndExpr e) {
        visitBinaryExpr(e, "&&");
    }

    @Override
    public void visit(LtExpr e) {
        visitBinaryExpr(e, "<");;
    }

    @Override
    public void visit(AddExpr e) {
        visitBinaryExpr(e, "+");;
    }

    @Override
    public void visit(SubtractExpr e) {
        visitBinaryExpr(e, "-");
    }

    @Override
    public void visit(MultExpr e) {
        visitBinaryExpr(e, "*");
    }

    @Override
    public void visit(ArrayAccessExpr e) {
        e.arrayExpr().accept(this);
        e.indexExpr().accept(this);
    }

    @Override
    public void visit(ArrayLengthExpr e) {
        e.arrayExpr().accept(this);
    }

    // TODO
    @Override
    public void visit(MethodCallExpr e) {

        if(e.methodId().equals(this.originalName)){
            this.isMethodCall = true;
            e.ownerExpr().accept(this); // is this correct? visit only when rename method?
            if(this.changeMethod)
                e.setMethodId(this.newName);
        }
        this.isMethodCall = false;
        this.changeMethod = false;

        for (Expr arg : e.actuals()) {
            arg.accept(this);
        }
    }

    @Override
    public void visit(IntegerLiteralExpr e) {
    }

    @Override
    public void visit(TrueExpr e) {
    }

    @Override
    public void visit(FalseExpr e) {
    }

    // TODO: static type
    @Override
    public void visit(IdentifierExpr e) {
    }

    //
    public void visit(ThisExpr e) {
        if (this.isMethodCall && this.isInFamily)
            this.changeMethod = true;
    }

    @Override
    public void visit(NewIntArrayExpr e) {
        e.lengthExpr().accept(this);
    }

    // TODO:
    @Override
    public void visit(NewObjectExpr e) {
        if(this.isMethodCall && isClassNameInPredecessorFamily(e.classId()))
                this.changeMethod = true;
    }

    @Override
    public void visit(NotExpr e) {
        e.e().accept(this);
    }

    @Override
    public void visit(IntAstType t) {
    }

    @Override
    public void visit(BoolAstType t) {
    }

    @Override
    public void visit(IntArrayAstType t) {
    }

    @Override
    public void visit(RefType t) {
    }
}

