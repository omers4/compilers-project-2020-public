package ast;

import java.util.ArrayList;
import java.util.List;

/*
* The target of this class is to rename the *calls* to a method "originalName" to a call on the method "newName".
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
    private boolean isMethodCall; // used to know if we in owner method expr or varDecl from method

    private boolean isRefInFamily; // used to know if RefType is in family
    private ArrayList<String> methodVarFamily; // var in method scope, which their type in family
    private ArrayList<String> methodVarAll; // all var in method scope
    private ClassDecl currClassDecl; // current class decl, used to look for var decl

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
            predecessor.getFamilyList(this.familyList);
        }

        this.isRefInFamily = false;
        this.methodVarFamily = new ArrayList<>();
        this.methodVarAll  = new ArrayList<>();
        this.currClassDecl = null;
    }

    // given fieldName and current classDecl, return AstType of this fieldName
    private AstType getFieldType(ClassDecl classDecl, String fieldName){
        ClassTree classTree = this.classHierarchy.findClassTree(classDecl);
        return getFieldType(classTree,fieldName);
    }

    /* like above but with classTree
    assume fieldName is decl in classTree or his parents
     */
    private AstType getFieldType(ClassTree classTree, String fieldName){
        for(var field: classTree.getData().fields()){
            if (field.name().equals(fieldName))
                return field.type();
        }
        return getFieldType(classTree.getParent(),fieldName);
    }

    // given name of class return true if class in predecessor family (predecessor as root)
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
        // saving current classDecl
        this.currClassDecl = classDecl;

        // check if class in family
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

    /* visit MethodDecl:
     1. save all vars, and all vars in family type. (vars means formal or local)
     2. if its name originalName check to if class in family and changed to newName
     */
    @Override
    public void visit(MethodDecl methodDecl) {
        this.methodVarFamily = new ArrayList<>();
        this.methodVarAll = new ArrayList<>();

        if (this.isInFamily && methodDecl.name().equals(this.originalName)) {
            methodDecl.setName(this.newName);
        }

        methodDecl.returnType().accept(this);

        for (var formal : methodDecl.formals()) {
            formal.accept(this);
        }

        this.isMethodCall = true;
        for (var varDecl : methodDecl.vardecls()) {
            varDecl.accept(this);
        }
        this.isMethodCall = false;

        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
        }

        methodDecl.ret().accept(this);

    }

    /* visit FormalArg:
     add var name to methodVarAll list and if Ref type in family add to methodVarFamily list too
     */
    @Override
    public void visit(FormalArg formalArg) {
        formalArg.type().accept(this);
        if (this.isRefInFamily)
            this.methodVarFamily.add(formalArg.name());
        this.isRefInFamily = false;
        this.methodVarAll.add(formalArg.name());
    }

    /* visit VarDecl:
     if VarDecl called from method,
     add var name to methodVarAll list and if Ref type in family add to methodVarFamily list too
     */
    @Override
    public void visit(VarDecl varDecl) {
        varDecl.type().accept(this);
        if (this.isMethodCall){
            this.methodVarAll.add(varDecl.name());
            if (this.isRefInFamily)
                this.methodVarFamily.add(varDecl.name());
        }
        this.isRefInFamily = false;
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

    /* visit MethodCallExpr:
    if originalName is called,
    visit ownerExpr and if after changeMethod is true, change to newName
     */
    @Override
    public void visit(MethodCallExpr e) {
        if(e.methodId().equals(this.originalName)){
            this.isMethodCall = true;
            e.ownerExpr().accept(this); // visit ownerExpr only when originalName method is called
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

    /* visit IdentifierExpr:
    if IdentifierExpr is the ownerExpr of MethodCallExpr and one of these:
    1. if id (var name) in method vars which in family
    2. if id isn't method var so look for his type, if it's RefType and isRefInFamily set to true
    than set changeMethod to true
     */
    @Override
    public void visit(IdentifierExpr e) {
        if (this.isMethodCall){
            if(this.methodVarFamily.contains(e.id()))
                this.changeMethod = true;
            if(!this.methodVarAll.contains(e.id())){
                AstType type = getFieldType(this.currClassDecl, e.id());
                type.accept(this);
                if (this.isRefInFamily)
                    this.changeMethod = true;
                this.isRefInFamily = false;
            }

        }
    }

    /* visit ThisExpr:
    if ThisExpr is the ownerExpr of MethodCallExpr,
    and the current class is in family, than set changeMethod to true
     */
    public void visit(ThisExpr e) {
        if (this.isMethodCall && this.isInFamily)
            this.changeMethod = true;
    }

    @Override
    public void visit(NewIntArrayExpr e) {
        e.lengthExpr().accept(this);
    }

    /* visit NewObjectExpr:
    if NewObjectExpr is the ownerExpr of MethodCallExpr,
    and the classId is in family, than set changeMethod to true
     */
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

    /* visit RefType:
    if id (var type) in family set isRefInFamily to true
     */
    @Override
    public void visit(RefType t) {
        if (this.isClassNameInPredecessorFamily(t.id())) {
            this.isRefInFamily = true;
        }
    }
}

