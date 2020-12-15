package Semantics;

import ast.*;

public interface ISemanticsVisitor {

    public void visit(Program program) throws InvalidSemanticsException;
    public void visit(ClassDecl classDecl) throws InvalidSemanticsException;
    public void visit(MainClass mainClass)  throws InvalidSemanticsException;
    public void visit(MethodDecl methodDecl) throws InvalidSemanticsException;
    public void visit(FormalArg formalArg) throws InvalidSemanticsException;
    public void visit(VarDecl varDecl) throws InvalidSemanticsException;

    public void visit(BlockStatement blockStatement) throws InvalidSemanticsException;
    public void visit(IfStatement ifStatement) throws InvalidSemanticsException;
    public void visit(WhileStatement whileStatement) throws InvalidSemanticsException;
    public void visit(SysoutStatement sysoutStatement) throws InvalidSemanticsException;
    public void visit(AssignStatement assignStatement) throws InvalidSemanticsException;
    public void visit(AssignArrayStatement assignArrayStatement) throws InvalidSemanticsException;

    public void visit(AndExpr e) throws InvalidSemanticsException;
    public void visit(LtExpr e) throws InvalidSemanticsException;
    public void visit(AddExpr e) throws InvalidSemanticsException;
    public void visit(SubtractExpr e) throws InvalidSemanticsException;
    public void visit(MultExpr e) throws InvalidSemanticsException;
    public void visit(ArrayAccessExpr e) throws InvalidSemanticsException;
    public void visit(ArrayLengthExpr e) throws InvalidSemanticsException;
    public void visit(MethodCallExpr e) throws InvalidSemanticsException;
    public void visit(IntegerLiteralExpr e) throws InvalidSemanticsException;
    public void visit(TrueExpr e) throws InvalidSemanticsException;
    public void visit(FalseExpr e) throws InvalidSemanticsException;
    public void visit(IdentifierExpr e) throws InvalidSemanticsException;
    public void visit(ThisExpr e) throws InvalidSemanticsException;
    public void visit(NewIntArrayExpr e) throws InvalidSemanticsException;
    public void visit(NewObjectExpr e) throws InvalidSemanticsException;
    public void visit(NotExpr e) throws InvalidSemanticsException;

    public void visit(IntAstType t) throws InvalidSemanticsException;
    public void visit(BoolAstType t) throws InvalidSemanticsException;
    public void visit(IntArrayAstType t) throws InvalidSemanticsException;
    public void visit(RefType t) throws InvalidSemanticsException;


}
