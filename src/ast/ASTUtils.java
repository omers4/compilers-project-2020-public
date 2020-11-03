package ast;

public class ASTUtils implements IASTUtils {
    private final Program program;

    ASTUtils(Program program) {
        this.program = program;
    }

    @Override
    public ClassDecl getFieldClassFromLineNumber(int lineNumber) {
        return null;
    }

    @Override
    public MethodDecl getArgumentMethodFromLineNumber(int lineNumber) {
        return null;
    }

    @Override
    public MethodDecl getVariableMethodFromLineNumber(int lineNumber) {
        return null;
    }

    public MethodDecl getMethodFromLineNumber(int lineNumber) {
        for (ClassDecl classDecl : program.classDecls()) {
            for (MethodDecl methodDecl: classDecl.methoddecls()) {
                if (methodDecl.lineNumber == lineNumber) {
                    return methodDecl;
                }
            }
        }
        return null;
    }

    public ClassDecl getClassFromLineNumber(int lineNumber) {
        for (ClassDecl classDecl : program.classDecls()) {
            for (MethodDecl methodDecl: classDecl.methoddecls()) {
                if (methodDecl.lineNumber == lineNumber) {
                    return classDecl;
                }
            }
        }
        return null;
    }
}
