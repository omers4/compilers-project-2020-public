package ast;

import LLVM.ComparisonType;
import LLVM.LLVMCommandFormatter;
import LLVM.LLVMType;

public class LLVMPrintVisitor implements IVisitorWithField<String> {
    private StringBuilder builder = new StringBuilder();

    private int indent = 0;
    private int registersCounter = 0;
    private int labelsCounter = 0;
    private String currentRegisterName;
    private LLVMCommandFormatter formatter = new LLVMCommandFormatter();

    public String getString() {
        return builder.toString();
    }

    private void appendWithIndent(String str) {
        builder.append("\t".repeat(indent));
        builder.append(str);
    }

    //TODO: del examples
    public void examples(){
        // Examples of usage:
        builder.append(formatter.formatAlloca("%3", LLVMType.Boolean));
        builder.append("\n");
        builder.append(formatter.formatLoad("%3", LLVMType.Int,"%4"));
        builder.append("\n");
        builder.append(formatter.formatStore(LLVMType.Int, "%3","%4"));
        builder.append("\n");

        builder.append(formatter.formatAdd("%3", LLVMType.Int,"%4", "%5"));
        builder.append("\n");
        builder.append(formatter.formatAnd("%3", LLVMType.Int,"%4", "%5"));
        builder.append("\n");
        builder.append(formatter.formatSub("%3", LLVMType.Int,"%4", "%5"));
        builder.append("\n");
        builder.append(formatter.formatXOR("%3", LLVMType.Int,"%4", "%5"));
        builder.append("\n");
        builder.append(formatter.formatMul("%3", LLVMType.Int,"%4", "%5"));
        builder.append("\n");
    }


    @Override
    public void visit(Program program) {
        program.mainClass().accept(this);
        for (ClassDecl classdecl : program.classDecls()) {
            classdecl.accept(this);
        }
    }

    /////////////////////Declaration/////////////////////

    @Override
    public void visit(ClassDecl classDecl) {
        for (var fieldDecl : classDecl.fields()) {
            fieldDecl.accept(this);
        }
        for (var methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
        }
    }

    @Override
    public void visit(MainClass mainClass) {
        mainClass.mainStatement().accept(this);
    }

    @Override
    public void visit(MethodDecl methodDecl) {

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

    /////////////////////Statement/////////////////////

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

    /////////////////////Binary Expression/////////////////////

    // get registers of e1 and e2, and format the relevant operation.
    private void visitBinaryExpr(BinaryExpr e, String infixSymbol) {

        e.e1().accept(this);
        String register_e1 = this.getField();

        e.e2().accept(this);
        String register_e2 = this.getField();

        String resultRegister = getNextTmpRegister();
        currentRegisterName = resultRegister;

        switch (infixSymbol){
            case "+":
                builder.append(formatter.formatAdd( resultRegister, LLVMType.Int, register_e1, register_e2));
                break;
            case "-":
                builder.append(formatter.formatSub( resultRegister, LLVMType.Int, register_e1, register_e2));
                break;
            case "*":
                builder.append(formatter.formatMul( resultRegister, LLVMType.Int, register_e1, register_e2));
                break;

            case "&&":
                builder.append(formatter.formatAnd( resultRegister, LLVMType.Boolean, register_e1, register_e2));
                break;

            case "<":
                builder.append(formatter.formatCompare( resultRegister, ComparisonType.Less , LLVMType.Boolean, register_e1, register_e2));
                break;
        }
    }

    // call to visitBinaryExpr
    @Override
    public void visit(AndExpr e) {
        visitBinaryExpr(e, "&&");
    }

    // call to visitBinaryExpr
    @Override
    public void visit(LtExpr e) {
        visitBinaryExpr(e, "<");;
    }

    // call to visitBinaryExpr
    @Override
    public void visit(AddExpr e) {
        visitBinaryExpr(e, "+");;
    }

    // call to visitBinaryExpr
    @Override
    public void visit(SubtractExpr e) {
        visitBinaryExpr(e, "-");
    }

    // call to visitBinaryExpr
    @Override
    public void visit(MultExpr e) {
        visitBinaryExpr(e, "*");
    }

    /////////////////////Array & Method Expression/////////////////////

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

    /////////////////////Expression/////////////////////

    // create int register with the value of the integer literal, set currentRegisterName.
    @Override
    public void visit(IntegerLiteralExpr e) {
        int value = e.num();
        String resultRegister = getNextTmpRegister();
        currentRegisterName = resultRegister;
        builder.append(formatter.formatAdd(resultRegister, LLVMType.Int,"0", Integer.toString(value)));
    }

    // create boolean register with the value 1, set currentRegisterName.
    @Override
    public void visit(TrueExpr e) {
        String resultRegister = getNextTmpRegister();
        currentRegisterName = resultRegister;
        builder.append(formatter.formatAnd(resultRegister, LLVMType.Boolean,"1", "1"));
    }

    // create boolean register with the value 0, set currentRegisterName.
    @Override
    public void visit(FalseExpr e) {
        String resultRegister = getNextTmpRegister();
        currentRegisterName = resultRegister;
        builder.append(formatter.formatAnd(resultRegister, LLVMType.Boolean,"0", "0"));
    }

    @Override
    public void visit(IdentifierExpr e) {
        // TODO: use RegisterAllocator for e.id() and set to currentRegisterName
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

    // create boolean register with the negative value of e, set currentRegisterName.
    @Override
    public void visit(NotExpr e) {
        e.e().accept(this);
        String exprRegister = this.getField();
        // assume exprRegister is boolean expression

        String resultRegister = getNextTmpRegister();
        currentRegisterName = resultRegister;
        builder.append(formatter.formatSub(resultRegister, LLVMType.Boolean,"1", exprRegister));
    }

    /////////////////////AstType/////////////////////

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

    /////////////////////Others/////////////////////

    @Override
    public String getField() {
        return currentRegisterName;
    }

    // return string with %_<registersCounter> and add 1 to the counter
    private String getNextTmpRegister(){
        String nextTmpRegister = "%_" + Integer.toString(registersCounter);
        registersCounter++;
        return nextTmpRegister;
    }

}
