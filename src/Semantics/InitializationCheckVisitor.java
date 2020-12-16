package Semantics;

import ast.*;

import java.util.HashSet;
import java.util.Set;

enum Scope {
    Method,
    Than,
    Else,
    While
}

public class InitializationCheckVisitor  extends ClassSemanticsVisitor{

    Set<String> localVars;
    Set<String> InitializedVars;

    Scope scope = Scope.Method;
    // Inside a while, the initializations don't count for uses after the loop, because we can't be sure that the loop executes at all.

    // add intersection of these sets to InitializedVars
    Set<String> ThenInitializedVars;
    Set<String> ElseInitializedVars;

    public InitializationCheckVisitor(IAstToSymbolTable symbolTable, ClassHierarchyForest hierarchy) {
        super(symbolTable, hierarchy);
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        scope = Scope.Method;
        localVars = new HashSet<>();
        InitializedVars = new HashSet<>();

        methodDecl.returnType().accept(this);
        for (var formal : methodDecl.formals()) {
            formal.accept(this);
        }
        // the AST structure already enforces some properties of MiniJava, which we do not need to check explicitly here
        // for example, there's simply no way to write down a method with variable declarations not at the beginning.
        for (var varDecl : methodDecl.vardecls()) {
            localVars.add(varDecl.name());
            varDecl.accept(this);
        }
        for (var stmt : methodDecl.body()) {
            stmt.accept(this);
        }
        methodDecl.ret().accept(this);
    }

    @Override
    public void visit(AssignStatement assignStatement) {
        assignStatement.rv().accept(this);
        String lv = assignStatement.lv();
        switch(scope){
            case Method:
                localVars.add(lv);
                break;
            case Than:
                ThenInitializedVars.add(lv);
                break;
            case Else:
                ElseInitializedVars.add(lv);
                break;
            case While:
                break;
        }
    }

    @Override
    public void visit(IdentifierExpr e) {
        if(!(InitializedVars.contains(e.id()))){
            valid = false;
        }
    }

    @Override
    public void visit(IfStatement ifStatement) {
        ThenInitializedVars = new HashSet<>();
        ElseInitializedVars = new HashSet<>();

        ifStatement.cond().accept(this);
        scope = Scope.Than;
        ifStatement.thencase().accept(this);
        scope = Scope.Else;
        ifStatement.elsecase().accept(this);

        Set<String> intersectSet = new HashSet<>(ThenInitializedVars);
        intersectSet.retainAll(ElseInitializedVars);
        InitializedVars.addAll(intersectSet);

        scope = Scope.Method;
    }

    @Override
    public void visit(WhileStatement whileStatement) {
        whileStatement.cond().accept(this);
        scope = Scope.While;
        whileStatement.body().accept(this);
        scope = Scope.Method;
    }


}
