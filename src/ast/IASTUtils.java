package ast;

/*
* This is a utils class that provide utilities on the AST.
* */
public interface IASTUtils {
    /*
    * returns the MethodDecl that is matching the line number in the AST
    * */
    MethodDecl getMethodFromLineNumber(int lineNumber);

    /*
     * returns the ClassDecl that is matching the line number in the AST
     * */
    ClassDecl getClassFromLineNumber(int lineNumber);

    /*
     * returns the class in the AST whose name is given
     * */
    ClassDecl getClassFromName(String name);
}
