package ast;

import LLVM.*;

public class LLVMPreProcessVisitor implements IVisitorWithField<String> {

    private String currentRegisterName;
    private LLVMCommandFormatter formatter = new LLVMCommandFormatter();

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

    // evaluating the condition - add labels and br
    @Override
    public void visit(IfStatement ifStatement) {

        String if0 = getNextLabel(); // then case
        String if1 = getNextLabel(); // else case
        String if2 = getNextLabel(); // after else block

        ifStatement.cond().accept(this);
        String condRegister = this.getField();
        builder.append(formatter.formatConditionalBreak(condRegister, if0, if1));

        builder.append(formatter.formatLabelName(if0));
        ifStatement.thencase().accept(this);
        builder.append(formatter.formatBreak(if2));

        builder.append(formatter.formatLabelName(if1));
        ifStatement.elsecase().accept(this);
        builder.append(formatter.formatBreak(if2));

        builder.append(formatter.formatLabelName(if2));

    }

    // evaluating the condition, end of body jump back to condition
    @Override
    public void visit(WhileStatement whileStatement) {

        String while0 = getNextLabel(); // check condition
        String while1 = getNextLabel(); // while body
        String while2 = getNextLabel(); // after body block

        builder.append(formatter.formatLabelName(while0));
        whileStatement.cond().accept(this);
        String condRegister = this.getField();
        builder.append(formatter.formatConditionalBreak(condRegister, while1, while2));

        builder.append(formatter.formatLabelName(while1));
        whileStatement.body().accept(this);
        builder.append(formatter.formatBreak(while0));

        builder.append(formatter.formatLabelName(while2));
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

        String resultRegister = registerAllocator.allocateNewTempRegister();
        currentRegisterName = resultRegister;

        // TODO: LLVMType according to register_e1,e2 types

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
            case "<":
                builder.append(formatter.formatCompare( resultRegister, ComparisonType.Less , LLVMType.Boolean, register_e1, register_e2));
                break;
        }
    }

    // Short-Circuit And
    // Evaluating e1, If true, continuing; otherwise skipping, Evaluating e2, Joining using phi
    @Override
    public void visit(AndExpr e) {
        e.e1().accept(this);
        String register_e1 = this.getField();

        String andcond0 = getNextLabel(); // check result, short circuit if false
        String andcond1 = getNextLabel(); // check e2
        String andcond2 = getNextLabel(); // this label seems redundant, but this becomes useful when compiling expressions a && b && c
        String andcond3 = getNextLabel(); // get appropriate value, depending on the predecessor block

        builder.append(formatter.formatLabelName(andcond0));
        builder.append(formatter.formatConditionalBreak(register_e1, andcond1, andcond3));

        builder.append(formatter.formatLabelName(andcond1));
        e.e2().accept(this);
        String register_e2 = this.getField();
        builder.append(formatter.formatBreak(andcond2));

        builder.append(formatter.formatLabelName(andcond2));
        builder.append(formatter.formatBreak(andcond3));

        builder.append(formatter.formatLabelName(andcond3));
        String resultRegister = registerAllocator.allocateNewTempRegister();
        builder.append(formatter.formatPhi(resultRegister, "0", andcond0, register_e2, andcond2));

        // set currentRegisterName
        currentRegisterName = resultRegister;
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

    // set currentRegisterName to the int value itself
    @Override
    public void visit(IntegerLiteralExpr e) {
        currentRegisterName = Integer.toString(e.num());

        /* TODO: del?
        // create int register with the value of the integer literal, set currentRegisterName.
        int value = e.num();
        String resultRegister = registerAllocator.allocateNewTempRegister();
        currentRegisterName = resultRegister;
        builder.append(formatter.formatAdd(resultRegister, LLVMType.Int,"0", Integer.toString(value)));*/
    }

    // create boolean register with the value 1, set currentRegisterName.
    @Override
    public void visit(TrueExpr e) {
        String resultRegister = registerAllocator.allocateNewTempRegister();
        currentRegisterName = resultRegister;
        builder.append(formatter.formatAnd(resultRegister, LLVMType.Boolean,"1", "1"));
    }

    // create boolean register with the value 0, set currentRegisterName.
    @Override
    public void visit(FalseExpr e) {
        String resultRegister = registerAllocator.allocateNewTempRegister();
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

        String resultRegister = registerAllocator.allocateNewTempRegister();
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

    // return string with <labelsCounter> and add 1 to the counter
    private String getNextLabel(){
        String nextLabel = Integer.toString(labelsCounter);
        labelsCounter++;
        return nextLabel;
    }

    // Helper Functions from ex2 instructions
    private String getHelperFunctions(){
        String helper = "declare i8* @calloc(i32, i32)\n" +
                "declare i32 @printf(i8*, ...)\n" +
                "declare void @exit(i32)\n" +
                "\n" +
                "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n" +
                "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n" +
                "define void @print_int(i32 %i) {\n" +
                "    %_str = bitcast [4 x i8]* @_cint to i8*\n" +
                "    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" +
                "    ret void\n" +
                "}\n" +
                "\n" +
                "define void @throw_oob() {\n" +
                "    %_str = bitcast [15 x i8]* @_cOOB to i8*\n" +
                "    call i32 (i8*, ...) @printf(i8* %_str)\n" +
                "    call void @exit(i32 1)\n" +
                "    ret void\n" +
                "}\n"+
                "\n";
        return helper;
    }

}
