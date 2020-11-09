package ast;
import java.util.HashSet;
import java.util.Set;

public class AstFieldRenameVisitor implements Visitor {

    private String originalName;
    private int originalLine;
    private String newName;
    private Boolean inChangeScope;
    private Set<String> classesWithChangedField;

    public AstFieldRenameVisitor(String originalName, int originalLine, String newName) {
            this.originalLine = originalLine;
            this.originalName = originalName;
            this.newName = newName;
            this.inChangeScope = false;
            this.classesWithChangedField = new HashSet<String>();
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
        if (classDecl.superName() != null) {
            if (this.classesWithChangedField.contains(classDecl.superName())) {
                this.inChangeScope = true;
                this.classesWithChangedField.add(classDecl.name());
            }
        }



        for (var fieldDecl : classDecl.fields()) {
            if (fieldDecl.lineNumber == this.originalLine) {
                this.inChangeScope = true;
                this.classesWithChangedField.add(classDecl.name());
            }
            fieldDecl.accept(this);
        }
        for (var methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
        }

        this.inChangeScope = false;
    }

    @Override
    public void visit(MainClass mainClass) {
        mainClass.mainStatement().accept(this);
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        methodDecl.returnType().accept(this);
        Boolean tempInScope = this.inChangeScope;

        for (var formal : methodDecl.formals()) {
            // Order of the If's matter!
            if(this.inChangeScope && formal.name().equals(this.originalName)) {
                this.inChangeScope = false;
            }
            if(formal.lineNumber == this.originalLine) {
                this.inChangeScope = true;
            }
            formal.accept(this);
        }

        for (var varDecl : methodDecl.vardecls()) {
            if(this.inChangeScope && varDecl.name().equals(this.originalName)) {
                this.inChangeScope = false;
            }
            if(varDecl.lineNumber == this.originalLine) {
                this.inChangeScope = true;
            }
            varDecl.accept(this);
        }
        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
        }

        methodDecl.ret().accept(this);

        this.inChangeScope = tempInScope;
    }

    @Override
    public void visit(FormalArg formalArg) {
        if (this.inChangeScope && formalArg.name().equals(this.originalName))
            formalArg.setName(this.newName);

        formalArg.type().accept(this);
    }

    @Override
    public void visit(VarDecl varDecl) {
        if (this.inChangeScope && varDecl.name().equals(this.originalName))
            varDecl.setName(this.newName);
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
        if (this.inChangeScope && assignStatement.lv().equals(this.originalName))
            assignStatement.setLv(this.newName);

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
        visitBinaryExpr(e, "<");
    }

    @Override
    public void visit(AddExpr e) {
        visitBinaryExpr(e, "+");
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

    @Override
    public void visit(MethodCallExpr e) {
        e.ownerExpr().accept(this);

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

    @Override
    public void visit(IdentifierExpr e) {
        if (this.inChangeScope && e.id().equals(this.originalName))
            e.setId(this.newName);
    }

    public void visit(ThisExpr e) {

    }

    @Override
    public void visit(NewIntArrayExpr e) {
        e.lengthExpr().accept(this);
    }

    @Override
    public void visit(NewObjectExpr e) {

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
