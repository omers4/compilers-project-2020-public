package ast;

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

    private void visitBinaryExpr(BinaryExpr e, String infixSymbol) {
    }


    @Override
    public void visit(Program program) {
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
    public void visit(ClassDecl classDecl) {
    }

    @Override
    public void visit(MainClass mainClass) {
    }

    @Override
    public void visit(MethodDecl methodDecl) {
    }

    @Override
    public void visit(FormalArg formalArg) {
    }

    @Override
    public void visit(VarDecl varDecl) {
    }

    @Override
    public void visit(BlockStatement blockStatement) {
    }

    @Override
    public void visit(IfStatement ifStatement) {
    }

    @Override
    public void visit(WhileStatement whileStatement) {
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
    }

    @Override
    public void visit(AssignStatement assignStatement) {
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
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
    }

    @Override
    public void visit(ArrayLengthExpr e) {
    }

    @Override
    public void visit(MethodCallExpr e) {
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
    }

    @Override
    public void visit(NewObjectExpr e) {
    }

    @Override
    public void visit(NotExpr e) {
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
    public void visit(RefType t) {}

    @Override
    public String getField() {
        return currentRegisterName;
    }
}
