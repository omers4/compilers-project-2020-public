package ast;

import LLVM.*;

import java.util.stream.Collectors;

public class LLVMPreProcessVisitor implements IVisitorWithField<String> {

    private IAstToSymbolTable astToSymbolTable;
    private ILLVMCommandFormatter formatter;
    private ILLVMRegisterAllocator registerAllocator;
    private StringBuilder stringBuilder;
    private ClassInfo classInfo;

    public LLVMPreProcessVisitor(IAstToSymbolTable astToSymbolTable, ILLVMCommandFormatter formatter, ILLVMRegisterAllocator registerAllocator) {
        this.astToSymbolTable = astToSymbolTable;
        this.formatter = formatter;
        this.registerAllocator = registerAllocator;
        this.stringBuilder = new StringBuilder();
        this.classInfo = new ClassInfo();
    }

    private void printClassVTAble(SymbolTableItem classItem) {
        String globalVTableName = registerAllocator.allocateVTableRegister(classItem.getId());
        classInfo.addClassInfo(classItem.getId(), classItem.getVTable(), (ClassDecl)classItem.getNode());
        stringBuilder.append(formatter.formatGlobalVTable(globalVTableName, classItem.getVTable().getMethods().values().stream()
                .map(MethodSignature::toLLVMSignature).collect( Collectors.toList())));
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
         SymbolTable symbolTable = astToSymbolTable.getSymbolTable(classDecl);
         SymbolTableItem classItem = symbolTable.get(new SymbolItemKey(classDecl.name(), SymbolType.Class));
         printClassVTAble(classItem);
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
    }

    @Override
    public void visit(LtExpr e) {
    }

    @Override
    public void visit(AddExpr e) {
    }

    @Override
    public void visit(SubtractExpr e) {
    }

    @Override
    public void visit(MultExpr e) {
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

    @Override
    public String getField() {
        return stringBuilder.toString();
    }

    public ClassInfo getClassInfo() { return this.classInfo; }
}
