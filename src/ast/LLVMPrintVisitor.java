package ast;

import LLVM.*;

import java.util.Arrays;
import java.util.List;

public class LLVMPrintVisitor implements IVisitorWithField<String> {
    private StringBuilder builder = new StringBuilder();

    private int indent = 0;
    private int labelsCounter = 0;
    private String currentRegisterName;
    private LLVMCommandFormatter formatter = new LLVMCommandFormatter();
    private LLVMRegisterAllocator registerAllocator;
    private ObjectsVTable vTable;

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
        builder.append(getHelperFunctions());

        registerAllocator = new LLVMRegisterAllocator(program);

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
    }

    public void visit(ThisExpr e) {
    }

    @Override
    public void visit(NewIntArrayExpr e) {
        // Already allocate space on stack: VarDecl -> IntArrayAstType

        // Check that the size of the array is not negative
        e.lengthExpr().accept(this);
        String length = getField();
        String condRegister = registerAllocator.allocateNewTempRegister();
        builder.append(formatter.formatCompare( condRegister, ComparisonType.Less , LLVMType.Boolean, length, "0"));

        String arr_alloc0 = getNextLabel();
        String arr_alloc1 = getNextLabel();

        builder.append(formatter.formatConditionalBreak(condRegister, arr_alloc0, arr_alloc1));

        //  Size was negative, throw negative size exception
        builder.append(formatter.formatLabelName(arr_alloc0));
        builder.append(formatter.formatCall("", LLVMType.Void, "throw_oob", null));
        builder.append(formatter.formatBreak(arr_alloc1));

        // All ok, we can proceed with the allocation
        builder.append(formatter.formatLabelName(arr_alloc1));

        // Calculate size bytes to be allocated for the array (new arr[sz] -> add i32 1, sz)
        // We need an additional int worth of space, to store the size of the array.
        String sizeRegister = registerAllocator.allocateNewTempRegister();
        builder.append(formatter.formatAdd( sizeRegister, LLVMType.Int, length, "1"));

        // Allocate sz + 1 integers (4 bytes each)
        String allocateRegister = registerAllocator.allocateNewTempRegister();
        List<LLVMMethodParam> params = Arrays.asList(
                new LLVMMethodParam(LLVMType.Int,"4"),
                new LLVMMethodParam(LLVMType.Int,sizeRegister));
        //builder.append(formatter.formatCall(allocateRegister, "i8*", "calloc", params));

        // Cast the returned pointer
        String castedRegister = registerAllocator.allocateNewTempRegister();
        //builder.append(formatter.formatBitcast(castedRegister, "i8*", allocateRegister, "i32*"));

        // Store the size of the array in the first position of the arra
        builder.append(formatter.formatStore(LLVMType.Int, length, castedRegister));

        // This concludes the array allocation (new int[2])
        // Assign the array pointer to x
        // store i32* %_3, i32** %x
        // TODO: WHERE?

    }

    @Override
    public void visit(NewObjectExpr e) {
//        ; First, we allocate the required memory on heap for our object.
//        ; We call calloc to achieve this:
//        ;   * The first argument is the amount of objects we want to allocate
//                ;     (always 1 for object allocation, but handy in arrays)
//        ;   * The second argument is the size of the object. This is calculated as the sum of the
//                ;     size of the fields of the class and all the super classes PLUS 8 bytes, to account for
//        ;     the vtable pointer.
//        ; In our case, we have a single int field so it's 4 + 8 = 12 bytes
//                %_0 = call i8* @calloc(i32 1, i32 12)
        String objectRegister = registerAllocator.allocateNewTempRegister();
        int classSize = vTable.getClassPhysicalSize(e.classId());
        List<LLVMMethodParam> allocationParams = new ArrayList<>();
        allocationParams.add(new LLVMMethodParam(LLVMType.Int,"1"));
        allocationParams.add(new LLVMMethodParam(LLVMType.Int, Integer.toString(classSize)));
        formatter.formatCall(objectRegister, LLVMType.IntPointer, allocationParams); // TODO: Change later to i8* and not i32*
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
        // Allocate space on stack for the array reference (it's a local variable)
        // VarDecl -> IntArrayAstType // %x = alloca i32*
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
