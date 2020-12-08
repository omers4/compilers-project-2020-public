package ast;

import LLVM.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LLVMPrintVisitor implements IVisitorWithField<String> {
    private final String CALLOC = "@calloc";


    private final IAstToSymbolTable symbolTable;
    private final ClassHierarchyForest classHierarchy;
    private StringBuilder builder = new StringBuilder();
    private int indent = 0;
    private int labelsCounter = 0;
    private String currentRegisterName;
    private AstType currentRegisterType;
    private ILLVMCommandFormatter formatter;
    private LLVMRegisterAllocator registerAllocator;
    private ClassDecl currentClass;
    private ClassInfo classInfo;

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

    public LLVMPrintVisitor(IAstToSymbolTable symbolTable, LLVMRegisterAllocator registerAllocator, ILLVMCommandFormatter formatter, ClassHierarchyForest classHierarchy) {
        this.symbolTable = symbolTable;
        this.registerAllocator = registerAllocator;
        this.formatter = formatter;
        this.classHierarchy = classHierarchy;
    }


    @Override
    public void visit(Program program) {
        ast.LLVMPreProcessVisitor preProcessVisitor = new ast.LLVMPreProcessVisitor(symbolTable,formatter,registerAllocator);
        preProcessVisitor.visit(program);
        this.classInfo = preProcessVisitor.getClassInfo();
        builder.append(preProcessVisitor.getField());
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
//        for (var fieldDecl : classDecl.fields()) {
//            fieldDecl.accept(this);
//        }
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
        registerAllocator.resetCounter();
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
        registerAllocator.resetCounter();
    }

    @Override
    public void visit(FormalArg formalArg) {
        String reg_name = registerAllocator.allocateAddressRegister(formalArg.name(), SymbolType.Var, formalArg);
        formalArg.type().accept(this);
        appendWithIndent(formatter.formatAlloca(reg_name, ASTypeToLLVMType(formalArg.type())));
        appendWithIndent(formatter.formatStore(ASTypeToLLVMType(formalArg.type()), formatter.formatRegisterName(formatter.formatFormalArgName(formalArg.name())), reg_name));
    }

    @Override
    public void visit(VarDecl varDecl) {
        String reg_name = registerAllocator.allocateAddressRegister(varDecl.name(),SymbolType.Var, varDecl);
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

        String if0 = String.format("if%s", getNextLabel()); // then case
        String if1 = String.format("if%s", getNextLabel()); // else case
        String if2 = String.format("if%s", getNextLabel()); // after else block

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

        String while0 = "loop" + getNextLabel(); // check condition
        String while1 = "loop" + getNextLabel(); // while body
        String while2 = "loop" + getNextLabel(); // after body block

        appendWithIndent(formatter.formatBreak(while0));

        builder.append(formatter.formatLabelName(while0));
        whileStatement.cond().accept(this);
        String condRegister = this.getField();
        appendWithIndent(formatter.formatConditionalBreak(condRegister, while1, while2));

        builder.append(formatter.formatLabelName(while1));
        whileStatement.body().accept(this);
        appendWithIndent(formatter.formatBreak(while0));

        builder.append(formatter.formatLabelName(while2));
    }

    // call print int
    @Override
    public void visit(SysoutStatement sysoutStatement) {
        sysoutStatement.arg().accept(this);
        String arg = getField();
        List<LLVMMethodParam> params = Arrays.asList(new LLVMMethodParam(LLVMType.Int, arg));
        appendWithIndent(formatter.formatCall("", LLVMType.Void, "@print_int", params));
    }

    private String loadFieldOrLocalVar(AstNode context, String name) {
        String resultRegister = registerAllocator.allocateAddressRegister(name,SymbolType.Var, context);
        var symbolTableOfStmt = symbolTable.getSymbolTable(context);
        var symbolTableEntry = symbolTableOfStmt.get(new SymbolItemKey(name, SymbolType.Var));
        if (symbolTableEntry.getKind() != SymbolType.Field) {
            return resultRegister;
        }


        ObjectVTable objectVtable = classInfo.getClassVTable(currentClass.name());
        var field = objectVtable.getFields().get(name);
        if (field != null) {
            String vtableRegister = registerAllocator.allocateNewTempRegister();
            int fieldOffset = objectVtable.getFieldIndex(name);
            appendWithIndent(formatter.formatGetElementPtr(vtableRegister, LLVMType.Byte, "%this", String.format("%d", fieldOffset), ""));
            String bitcastRegister = registerAllocator.allocateNewTempRegister();
            appendWithIndent(formatter.formatBitcast(bitcastRegister, LLVMType.Byte, vtableRegister, ASTypeToLLVMType(symbolTableEntry.getType())));
            resultRegister = bitcastRegister;
        }
        return resultRegister;
    }

    @Override
    public void visit(AssignStatement assignStatement) {
        assignStatement.rv().accept(this);
        String valueLocation = currentRegisterName;

        var where = loadFieldOrLocalVar(assignStatement, assignStatement.lv());

        var symbolTableOfStmt = symbolTable.getSymbolTable(assignStatement);
        var symbolTableEntry = symbolTableOfStmt.get(new SymbolItemKey(assignStatement.lv(), SymbolType.Var));
        appendWithIndent(formatter.formatStore(ASTypeToLLVMType(symbolTableEntry.getType()), valueLocation, where));
    }

    // implements the array store arr[index] = rv
    // calls access_array
    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {

        // Load the address of the array
        String array = loadFieldOrLocalVar(assignArrayStatement, assignArrayStatement.lv());
        String addressArray = registerAllocator.allocateNewTempRegister();
        appendWithIndent(formatter.formatLoad(addressArray, LLVMType.IntPointer, array));

        // get index
        assignArrayStatement.index().accept(this);
        String index = getField();

        // get rv
        assignArrayStatement.rv().accept(this);
        String rvRegister = getField();

        // get accessPtrRegister
        String accessPtrRegister = access_array(index, addressArray);

        // store rv to address (accessPtrRegister)
        appendWithIndent(formatter.formatStore(LLVMType.Int, rvRegister, accessPtrRegister));

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
        currentRegisterType = new IntAstType();

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
                appendWithIndent(formatter.formatCompare( resultRegister, ComparisonType.Less , LLVMType.Int, register_e1, register_e2));
                break;
        }
    }

    // Short-Circuit And
    // Evaluating e1, If true, continuing; otherwise skipping, Evaluating e2, Joining using phi
    @Override
    public void visit(AndExpr e) {
        e.e1().accept(this);
        String register_e1 = this.getField();

        String andcond0 = "andcond" + getNextLabel(); // check result, short circuit if false
        String andcond1 = "andcond" + getNextLabel(); // check e2
        String andcond2 = "andcond" +  getNextLabel(); // this label seems redundant, but this becomes useful when compiling expressions a && b && c
        String andcond3 = "andcond" + getNextLabel(); // get appropriate value, depending on the predecessor block

        appendWithIndent(formatter.formatBreak(andcond0));

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
        currentRegisterType = new BoolAstType();
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

    // call access_array, load from accessPtrRegister and set currentRegisterName
    @Override
    public void visit(ArrayAccessExpr e) {
        // Load the address of the array
        e.arrayExpr().accept(this);
        String addressArray = getField();

        // get accessPtrRegister
        e.indexExpr().accept(this);
        String index = getField();
        String accessPtrRegister = access_array(index, addressArray);

        // load
        String resultRegister = registerAllocator.allocateNewTempRegister();
        appendWithIndent(formatter.formatLoad(resultRegister, LLVMType.Int, accessPtrRegister));

        // set currentRegisterName
        currentRegisterName = resultRegister;
        currentRegisterType = new IntAstType();

    }

    // load array address (because length in first index) and set currentRegisterName
    @Override
    public void visit(ArrayLengthExpr e) {
        e.arrayExpr().accept(this);
        String address = getField();
        String lengthRegister = registerAllocator.allocateNewTempRegister();
        appendWithIndent(formatter.formatLoad(lengthRegister, LLVMType.Int, address));
        currentRegisterName = lengthRegister;
    }

    @Override
    public void visit(MethodCallExpr e) {
        ArrayList<LLVMMethodParam> actuals = new ArrayList<>();
        int i = 0;

        e.ownerExpr().accept(this);  // can be this.foo() (new A()).foo x.foo
        RefType ref = (RefType) currentRegisterType;
        var methodSig = classInfo.getClassVTable(ref.id()).getMethods().get(e.methodId());
        int methodPos = new ArrayList<String>(classInfo.getClassVTable(ref.id()).getMethods().keySet()).indexOf(e.methodId());
        String objectLocationStr = currentRegisterName;

        String bitcastRegister = registerAllocator.allocateNewTempRegister();
        appendWithIndent(formatter.formatBitcast(bitcastRegister, LLVMType.Byte, currentRegisterName, LLVMType.AddressPointer));
        String objectRegister = registerAllocator.allocateNewTempRegister();
        appendWithIndent(formatter.formatLoad(objectRegister, LLVMType.AddressPointer, bitcastRegister));

        String vtableRegister = registerAllocator.allocateNewTempRegister();
        appendWithIndent(formatter.formatGetElementPtr(vtableRegister, LLVMType.Address, objectRegister, String.format("%d", methodPos), ""));
        String vtableEntryRegister = registerAllocator.allocateNewTempRegister();
        appendWithIndent(formatter.formatLoad(vtableEntryRegister, LLVMType.Address, vtableRegister));
        String methodRegister = registerAllocator.allocateNewTempRegister();
        appendWithIndent(formatter.formatBitcast(methodRegister, LLVMType.Byte, vtableEntryRegister, methodSig.toLLVMSignature()));


        actuals.add(new LLVMMethodParam(ASTypeToLLVMType(ref), objectLocationStr));
        for (Expr arg : e.actuals()) {
            arg.accept(this);
            var astFormalType = methodSig.getFormals().get(i).type();
            actuals.add(new LLVMMethodParam(ASTypeToLLVMType(astFormalType), currentRegisterName));
            i++;
        }
        String callResult = registerAllocator.allocateNewTempRegister();
        currentRegisterName = callResult;
        currentRegisterType = methodSig.getRet();
        appendWithIndent(formatter.formatCall(callResult, ASTypeToLLVMType(methodSig.getRet()), methodRegister, actuals));
    }

    /////////////////////Expression/////////////////////

    // set currentRegisterName to the int value
    @Override
    public void visit(IntegerLiteralExpr e) {
        currentRegisterName = Integer.toString(e.num());
        currentRegisterType = new IntAstType();
    }

    // set currentRegisterName to 1.
    @Override
    public void visit(TrueExpr e) {
        currentRegisterName = "1";
        currentRegisterType = new BoolAstType();
    }

    // set currentRegisterName to 0.
    @Override
    public void visit(FalseExpr e) {
        currentRegisterName = "0";
        currentRegisterType = new BoolAstType();
    }

    @Override
    public void visit(IdentifierExpr e) {
        var symbolTableOfStmt = symbolTable.getSymbolTable(e);
        var symbolTableEntry = symbolTableOfStmt.get(new SymbolItemKey(e.id(), SymbolType.Var));

        String resultRegister = loadFieldOrLocalVar(e, e.id());

        String tempRegister = registerAllocator.allocateNewTempRegister();
        currentRegisterName = tempRegister;
        currentRegisterType = symbolTableEntry.getType();
        appendWithIndent(formatter.formatLoad(tempRegister, ASTypeToLLVMType(symbolTableEntry.getType()), resultRegister));

    }

    public void visit(ThisExpr e) {

        var refType = new RefType();
        refType.setId(currentClass.name());
        currentRegisterName = "%this";
        currentRegisterType = refType;
    }

    // new int[length]
    @Override
    public void visit(NewIntArrayExpr e) {
        // allocate space on stack in varDecl

        // Check that the size of the array is not negative
        e.lengthExpr().accept(this);
        String length = getField();
        check_index(length, "0");

        // Calculate size bytes to be allocated for the array (new arr[sz] -> add i32 1, sz)
        // We need an additional int worth of space, to store the size of the array.
        String sizeRegister = registerAllocator.allocateNewTempRegister();
        appendWithIndent(formatter.formatAdd( sizeRegister, LLVMType.Int, length, "1"));

        // Allocate sz + 1 integers (4 bytes each)
        String allocateRegister = registerAllocator.allocateNewTempRegister();
        List<LLVMMethodParam> params = Arrays.asList(
                new LLVMMethodParam(LLVMType.Int,"4"),
                new LLVMMethodParam(LLVMType.Int,sizeRegister));
        appendWithIndent(formatter.formatCall(allocateRegister, LLVMType.Address, "@calloc", params));

        // Cast the returned pointer
        String castedRegister = registerAllocator.allocateNewTempRegister();
        appendWithIndent(formatter.formatBitcast(castedRegister, LLVMType.Byte, allocateRegister, LLVMType.Int));

        // Store the size of the array in the first position of the arraY
        appendWithIndent(formatter.formatStore(LLVMType.Int, length, castedRegister));

        // This concludes the array allocation (new int[2])
        // Assign the array pointer to it's register
        // will happen in AssignStatement
        currentRegisterName = castedRegister;

        currentRegisterType = new IntArrayAstType();

        // appendWithIndent(formatter.formatStore(LLVMType.IntPointer, castedRegister, prevLrRegister));
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
        int classSize = classInfo.getClassPhysicalSize(e.classId());
        List<LLVMMethodParam> allocationParams = new ArrayList<>();
        allocationParams.add(new LLVMMethodParam(LLVMType.Int,"1"));
        allocationParams.add(new LLVMMethodParam(LLVMType.Int, Integer.toString(classSize)));
        appendWithIndent(formatter.formatCall(objectRegister, LLVMType.Address, CALLOC, allocationParams));

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
        appendWithIndent(formatter.formatBitcast(bitcastRegister, LLVMType.Byte,objectRegister, LLVMType.AddressPointer));


//        ; Get the address of the first element of the Base_vtable
//                ; The getelementptr arguments are as follows:
//        ;   * The first argument is the type of elements our Base_vtable ptr points to.
//        ;   * The second argument is the Base_vtable ptr.
//        ;   * The third and fourth arguments are indexes
//        ;; (alternative to getelementpr: %_2 = bitcast [2 x i8*]* @.Base_vtable to i8**)
        // %_2 = getelementptr [2 x i8*], [2 x i8*]* @.Base_vtable, i32 0, i32 0
        String vTableRegister = registerAllocator.allocateVTableRegister(e.classId());
        String elementPrtRegister = registerAllocator.allocateNewTempRegister();
        LLVMType type = LLVMType.Address;

        type.setLength(classInfo.getClassVTable(e.classId()).getMethods().size());
        appendWithIndent(formatter.formatGetElementPtr(elementPrtRegister, type, vTableRegister, "0", "0"));
        type.setLength(-1);

//        ; Set the vtable to the correct address.
//                store i8** %_2, i8*** %_1
        appendWithIndent(formatter.formatStore(LLVMType.AddressPointer, elementPrtRegister, bitcastRegister));

////        ; Store the address of the new object on the stack (var b), as a byte array (i8*).
////                store i8* %_0, i8** %b
//        appendWithIndent(formatter.formatStore(LLVMType.Void, objectRegister, prevLrRegister));

        var refType = new RefType();
        refType.setId(e.classId());

        currentRegisterName = objectRegister;
        currentRegisterType = refType;
    }


    // create boolean register with the negative value of e, set currentRegisterName.
    @Override
    public void visit(NotExpr e) {
        e.e().accept(this);
        String exprRegister = this.getField();
        // assume exprRegister is boolean expression

        String resultRegister = registerAllocator.allocateNewTempRegister();
        currentRegisterName = resultRegister;

        currentRegisterType = new BoolAstType();
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
                "\t%_str = bitcast [4 x i8]* @_cint to i8*\n" +
                "\tcall i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n" +
                "\tret void\n" +
                "}\n" +
                "\n" +
                "define void @throw_oob() {\n" +
                "\t%_str = bitcast [15 x i8]* @_cOOB to i8*\n" +
                "\tcall i32 (i8*, ...) @printf(i8* %_str)\n" +
                "\tcall void @exit(i32 1)\n" +
                "\tret void\n" +
                "}\n"+
                "\n";
        return helper;
    }

    /////////////////////Array Functions/////////////////////

    // generate code for oob check:
    // for access: length <= index
    // for allocate: index < 0
    private void check_index(String index, String length){
        String condRegister = registerAllocator.allocateNewTempRegister();
        if (length == "0")
            appendWithIndent(formatter.formatCompare( condRegister, ComparisonType.Less , LLVMType.Int, index, "0"));
        else
            appendWithIndent(formatter.formatCompare( condRegister, ComparisonType.LessOrEquals , LLVMType.Int, length, index));

        String labelNeg = String.format("arr_alloc%s", getNextLabel());
        String labelPos = String.format("arr_alloc%s", getNextLabel());

        appendWithIndent(formatter.formatConditionalBreak(condRegister, labelNeg, labelPos));

        // Size/index was negative, throw negative size exception
        builder.append(formatter.formatLabelName(labelNeg));
        appendWithIndent(formatter.formatCall("", LLVMType.Void, "@throw_oob", null));
        appendWithIndent(formatter.formatBreak(labelPos));

        // All ok, we can proceed with the allocation/access
        builder.append(formatter.formatLabelName(labelPos));
    }

    private String access_array(String index, String addressArray){
        // Load the address of the array - addressArray

        // Check that the index is greater than zero
        check_index(index, "0");

        // Load the size of the array (first integer of the array)
        String sizePtrRegister = registerAllocator.allocateNewTempRegister();
        appendWithIndent(formatter.formatGetElementPtr(sizePtrRegister, LLVMType.Int, addressArray,"0", ""));
        String sizeRegister = registerAllocator.allocateNewTempRegister();
        appendWithIndent(formatter.formatLoad(sizeRegister, LLVMType.Int, sizePtrRegister));

        // Check that the index is less than the size of the array
        check_index(index, sizeRegister);

        // All ok, we can safely index the array now
        // We'll be accessing our array at index + 1, since the first element holds the size
        String indexPlusOneRegister = registerAllocator.allocateNewTempRegister();
        appendWithIndent(formatter.formatAdd(indexPlusOneRegister, LLVMType.Int, index, "1"));

        // Get pointer to the i + 1 element of the array
        String accessPtrRegister = registerAllocator.allocateNewTempRegister();
        appendWithIndent(formatter.formatGetElementPtr(accessPtrRegister, LLVMType.Int, addressArray,indexPlusOneRegister, ""));

        return accessPtrRegister;
    }

}
