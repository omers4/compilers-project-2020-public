package ast;

public interface IASTUtils {
    ClassDecl getFieldClassFromLineNumber(int lineNumber);
    MethodDecl getArgumentMethodFromLineNumber(int lineNumber);
    MethodDecl getVariableMethodFromLineNumber(int lineNumber);
    MethodDecl getMethodFromLineNumber(int lineNumber);
    ClassDecl getClassFromLineNumber(int lineNumber);
}
