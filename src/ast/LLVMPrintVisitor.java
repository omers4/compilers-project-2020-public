package ast;

import LLVM.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LLVMPrintVisitor implements IVisitorWithField<String> {
    private final String CALLOC = "calloc";


    private final IAstToSymbolTable symbolTable;
    private StringBuilder builder = new StringBuilder();
    private int indent = 0;
    private int labelsCounter = 0;
    private String currentRegisterName;
    private LLVMCommandFormatter formatter = new LLVMCommandFormatter();
    private LLVMRegisterAllocator registerAllocator;
    private VTableUtils vTable;
    private ClassDecl currentClass;
    private String prevLrRegister;

    public String getString() {
        return builder.toString();
    }

    private void appendWithIndent(String str) {
        builder.append("\t".repeat(indent));
        builder.append(str);
    }

    private LLVMType ASTypeToLLVMType(AstType type) {
        if (type instanceof IntAstType) {
            return LLVMType.Int;
        }

        if (type instanceof BoolAstType) {
            return LLVMType.Boolean;
        }

        if (type instanceof IntArrayAstType) {
            return LLVMType.IntPointer;
        }

        if (type instanceof RefType) {
            return LLVMType.Address;
        }
        return null;
    }

    //TODO: del examples
    public void examples(){
        // Examples of usage:
        var a = new ArrayList<LLVMMethodParam>();
        a.add(new LLVMMethodParam(LLVMType.Boolean, "var1"));
        var t = LLVMType.Int;
        t.setLength(5);
        a.add(new LLVMMethodParam(t, "var2"));
        LLVMMethodSignature sig = new LLVMMethodSignature("@TV.Start", LLVMType.Int, a);
        var sigs = new ArrayList<LLVMMethodSignature>();
        sigs.add(sig);
        builder.append(formatter.formatGlobalVTable("@.TV_vtable", sigs));

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

    public LLVMPrintVisitor(IAstToSymbolTable symbolTable, LLVMRegisterAllocator registerAllocator) {
        this.symbolTable = symbolTable;
        this.registerAllocator = registerAllocator;
    }


    @Override
    public void visit(Program program) {
        builder.append(getHelperFunctions());

        program.mainClass().accept(this);
        for (ClassDecl classdecl : program.classDecls()) {
            classdecl.accept(this);
        }
    }

    /////////////////////Declaration/////////////////////

    @Override
    public void visit(ClassDecl classDecl) {
        currentClass = classDecl;
        for (var fieldDecl : classDecl.fields()) {
            fieldDecl.accept(this);
        }
        for (var methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
        }
    }

    @Override
    public void visit(MainClass mainClass) {
        builder.append(formatter.formatMethodDefinition(LLVMType.Int, "main", new ArrayList<>()));
        indent++;
        mainClass.mainStatement().accept(this);
        appendWithIndent(formatter.formatReturn(LLVMType.Int, "0"));
        indent--;
        builder.append("}\n\n");
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        String methodName = String.format("%s.%s", currentClass.name(), methodDecl.name());
        var params = new ArrayList<LLVMMethodParam>();
        params.add(new LLVMMethodParam(LLVMType.Address, "%this"));
        for (var formalArg: methodDecl.formals()) {
            params.add(new LLVMMethodParam(ASTypeToLLVMType(formalArg.type()), formatter.formatRegisterName(formatter.formatFormalArgName(formalArg.name()))));
        }

        builder.append(formatter.formatMethodDefinition(ASTypeToLLVMType(methodDecl.returnType()), methodName, params));
        indent++;

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
        appendWithIndent(formatter.formatReturn(ASTypeToLLVMType(methodDecl.returnType()), currentRegisterName));

        indent--;
        builder.append("}\n\n");
    }

    @Override
    public void visit(FormalArg formalArg) {
        String reg_name = registerAllocator.allocateAddressRegister(formalArg.name(), formalArg);
        formalArg.type().accept(this);
        appendWithIndent(formatter.formatAlloca(reg_name, ASTypeToLLVMType(formalArg.type())));
        appendWithIndent(formatter.formatStore(LLVMType.Int, formatter.formatRegisterName(formatter.formatFormalArgName(formalArg.name())), reg_name));
    }

    @Override
    public void visit(VarDecl varDecl) {
        String reg_name = registerAllocator.allocateAddressRegister(varDecl.name(), varDecl);
        appendWithIndent(formatter.formatAlloca(reg_name, ASTypeToLLVMType(varDecl.type())));
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
        appendWithIndent(formatter.formatConditionalBreak(condRegister, if0, if1));

        builder.append(formatter.formatLabelName(if0));
        ifStatement.thencase().accept(this);
        appendWithIndent(formatter.formatBreak(if2));

        builder.append(formatter.formatLabelName(if1));
        ifStatement.elsecase().accept(this);
        appendWithIndent(formatter.formatBreak(if2));

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
        appendWithIndent(formatter.formatConditionalBreak(condRegister, while1, while2));

        builder.append(formatter.formatLabelName(while1));
        whileStatement.body().accept(this);
        appendWithIndent(formatter.formatBreak(while0));

        builder.append(formatter.formatLabelName(while2));
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
        sysoutStatement.arg().accept(this);
    }

    @Override
    public void visit(AssignStatement assignStatement) {
        String dest = registerAllocator.allocateAddressRegister(assignStatement.lv(), assignStatement);
        prevLrRegister = dest;
        assignStatement.rv().accept(this);
        var symbolTableOfStmt = symbolTable.getSymbolTable(assignStatement);
        var symbolTableEntry = symbolTableOfStmt.get(assignStatement.lv());
        appendWithIndent(formatter.formatStore(ASTypeToLLVMType(symbolTableEntry.getType()), currentRegisterName, dest));
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
                appendWithIndent(formatter.formatAdd( resultRegister, LLVMType.Int, register_e1, register_e2));
                break;
            case "-":
                appendWithIndent(formatter.formatSub( resultRegister, LLVMType.Int, register_e1, register_e2));
                break;
            case "*":
                appendWithIndent(formatter.formatMul( resultRegister, LLVMType.Int, register_e1, register_e2));
                break;
            case "<":
                appendWithIndent(formatter.formatCompare( resultRegister, ComparisonType.Less , LLVMType.Boolean, register_e1, register_e2));
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
        appendWithIndent(formatter.formatConditionalBreak(register_e1, andcond1, andcond3));

        builder.append(formatter.formatLabelName(andcond1));
        e.e2().accept(this);
        String register_e2 = this.getField();
        appendWithIndent(formatter.formatBreak(andcond2));

        builder.append(formatter.formatLabelName(andcond2));
        appendWithIndent(formatter.formatBreak(andcond3));

        builder.append(formatter.formatLabelName(andcond3));
        String resultRegister = registerAllocator.allocateNewTempRegister();
        appendWithIndent(formatter.formatPhi(resultRegister, "0", andcond0, register_e2, andcond2));

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
        ArrayList<LLVMMethodParam> actuals = new ArrayList<>();
        // TODO resolve the method we are calling

        e.ownerExpr().accept(this);  // can be this.foo() (new A()).foo x.foo
        String methodLocation = currentRegisterName;
        for (Expr arg : e.actuals()) {
            arg.accept(this);
            // TODO type - once we resolve the method
            actuals.add(new LLVMMethodParam(LLVMType.Int, currentRegisterName));
        }
        String callResult = registerAllocator.allocateNewTempRegister();
        currentRegisterName = callResult;
        // TODO the real ret type, the real method location - once we resolve the method
        appendWithIndent(formatter.formatCall(callResult, LLVMType.Int, methodLocation, actuals));
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
        appendWithIndent(formatter.formatAnd(resultRegister, LLVMType.Boolean,"1", "1"));
    }

    // create boolean register with the value 0, set currentRegisterName.
    @Override
    public void visit(FalseExpr e) {
        String resultRegister = registerAllocator.allocateNewTempRegister();
        currentRegisterName = resultRegister;
        appendWithIndent(formatter.formatAnd(resultRegister, LLVMType.Boolean,"0", "0"));
    }

    @Override
    public void visit(IdentifierExpr e) {
        String tempRegister = registerAllocator.allocateNewTempRegister();
        String resultRegister = registerAllocator.allocateAddressRegister(e.id(), e);
        currentRegisterName = tempRegister;
        var symbolTableOfStmt = symbolTable.getSymbolTable(e);
        var symbolTableEntry = symbolTableOfStmt.get(e.id());
        appendWithIndent(formatter.formatLoad(tempRegister, ASTypeToLLVMType(symbolTableEntry.getType()), resultRegister));
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
        appendWithIndent(formatter.formatCompare( condRegister, ComparisonType.Less , LLVMType.Boolean, length, "0"));

        String arr_alloc0 = getNextLabel();
        String arr_alloc1 = getNextLabel();

        appendWithIndent(formatter.formatConditionalBreak(condRegister, arr_alloc0, arr_alloc1));

        //  Size was negative, throw negative size exception
        builder.append(formatter.formatLabelName(arr_alloc0));
        appendWithIndent(formatter.formatCall("", LLVMType.Void, "throw_oob", null));
        appendWithIndent(formatter.formatBreak(arr_alloc1));

        // All ok, we can proceed with the allocation
        builder.append(formatter.formatLabelName(arr_alloc1));

        // Calculate size bytes to be allocated for the array (new arr[sz] -> add i32 1, sz)
        // We need an additional int worth of space, to store the size of the array.
        String sizeRegister = registerAllocator.allocateNewTempRegister();
        appendWithIndent(formatter.formatAdd( sizeRegister, LLVMType.Int, length, "1"));

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
        appendWithIndent(formatter.formatStore(LLVMType.Int, length, castedRegister));

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
        formatter.formatCall(objectRegister, LLVMType.Address, CALLOC, allocationParams);

//        ; Next we need to set the vtable pointer to point to the correct vtable (Base_vtable)
//        ; First we bitcast the object pointer from i8* to i8***
//        ; This is done because:
//        ;   -> The vtable stores values of type i8*.
//        ;   -> Thus, a pointer that points to the start of the vtable (equivalently at the first entry
//        ;      of the vtable) must have type i8**.
//        ;   -> Thus, to set the vtable pointer at the start of the object, we need to have its address
//        ;      (first byte of the object) in a register of type i8***
//        ;		- it's a pointer to a location where we will be storing i8**.
        // %_1 = bitcast i8* %_0 to i8***
        String bitcastRegister = registerAllocator.allocateNewTempRegister();
        formatter.formatBitcast(bitcastRegister, LLVMType.Address,objectRegister, LLVMType.AddressPointerPointer);


//        ; Get the address of the first element of the Base_vtable
//                ; The getelementptr arguments are as follows:
//        ;   * The first argument is the type of elements our Base_vtable ptr points to.
//        ;   * The second argument is the Base_vtable ptr.
//        ;   * The third and fourth arguments are indexes
//        ;; (alternative to getelementpr: %_2 = bitcast [2 x i8*]* @.Base_vtable to i8**)
        // %_2 = getelementptr [2 x i8*], [2 x i8*]* @.Base_vtable, i32 0, i32 0
        String elementPrtRegister = registerAllocator.allocateNewTempRegister();
        LLVMType type = LLVMType.Address;

        // TODO: What is the meaning of this 2?
        type.setLength(2);
        formatter.formatGetElementPtr(elementPrtRegister, type, objectRegister, 0, 0);

//        ; Set the vtable to the correct address.
//                store i8** %_2, i8*** %_1
        formatter.formatStore(LLVMType.AddressPointer, elementPrtRegister, bitcastRegister);

//        ; Store the address of the new object on the stack (var b), as a byte array (i8*).
//                store i8* %_0, i8** %b
        formatter.formatStore(LLVMType.Void, objectRegister, prevLrRegister);
    }

    // create boolean register with the negative value of e, set currentRegisterName.
    @Override
    public void visit(NotExpr e) {
        e.e().accept(this);
        String exprRegister = this.getField();
        // assume exprRegister is boolean expression

        String resultRegister = registerAllocator.allocateNewTempRegister();
        currentRegisterName = resultRegister;
        appendWithIndent(formatter.formatSub(resultRegister, LLVMType.Boolean,"1", exprRegister));
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
