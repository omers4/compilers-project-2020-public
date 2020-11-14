package ast;

public class ASTUtils implements IASTUtils {
    private final Program program;

    ASTUtils(Program program) {
        this.program = program;
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

    @Override
    public ClassDecl getClassFromName(String name) {
        for (ClassDecl classDecl : program.classDecls()) {
            if (classDecl.name().equals(name)) {
                return classDecl;
            }
        }
        return null;
    }
}
