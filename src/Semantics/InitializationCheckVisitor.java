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

    Set<String> localVars; // all local vars names
    Set<String> InitializedVars; // local vars which initialized

    Scope scope = Scope.Method; // needs to know scope in order to assign to correct vars set

    Set<String> ThenInitializedVars; // local vars which initialized in then case
    Set<String> ElseInitializedVars; // local vars which initialized in else case

    Set<String> WhileInitializedVars; // local vars which initialized in while

    public InitializationCheckVisitor(IAstToSymbolTable symbolTable, ClassHierarchyForest hierarchy) {
        super(symbolTable, hierarchy);
        localVars = new HashSet<>();
        InitializedVars = new HashSet<>();
        ThenInitializedVars = new HashSet<>();
        ElseInitializedVars = new HashSet<>();
        WhileInitializedVars = new HashSet<>();
    }


    ///////////////////// add local vars /////////////////////

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


    ///////////////////// add to Initializated /////////////////////

    // assume type analysis, add lv to Initializated vars in relevant scope
    @Override
    public void visit(AssignStatement assignStatement) {
        assignStatement.rv().accept(this);
        String lv = assignStatement.lv();
        if(localVars.contains(lv)){
            switch(scope){
                case Method:
                    InitializedVars.add(lv);
                    break;
                case Than:
                    ThenInitializedVars.add(lv);
                    break;
                case Else:
                    ElseInitializedVars.add(lv);
                    break;
                case While:
                    WhileInitializedVars.add(lv);
                    break;
            }
        }
    }


    ///////////////////// is Initializated? /////////////////////

    //  check if e.id() initializated in relevant scope if it's a local var
    @Override
    public void visit(IdentifierExpr e) {
        String id = e.id();
        if (localVars.contains(id)){
            switch(scope){
                case Method:
                    if( !( InitializedVars.contains(id) )){
                        valid = false;
                    }
                    break;
                case Than:
                    if( !( ThenInitializedVars.contains(id) )){
                        valid = false;
                    }
                    break;
                case Else:
                    if( !( ElseInitializedVars.contains(id) )){
                        valid = false;
                    }
                    break;
                case While:
                    if( !( WhileInitializedVars.contains(id) )){
                        valid = false;
                    }
                    break;
            }
        }
    }


    ///////////////////// while /////////////////////

    @Override
    public void visit(WhileStatement whileStatement) {

        // visit cond, previous scope setting
        whileStatement.cond().accept(this);

        // save before scope + sets state
        Scope before = scope;
        Set<String> beforeInitialized = new HashSet<>(InitializedVars);
        Set<String> beforeThan = new HashSet<>(ThenInitializedVars);
        Set<String> beforeElse = new HashSet<>(ElseInitializedVars);
        Set<String> beforeWhile = new HashSet<>(WhileInitializedVars);

        // add initialized vars from previous scope to current while scope
        switch(before){
            case Method:
                WhileInitializedVars = new HashSet<>(beforeInitialized);
                break;
            case Than:
                WhileInitializedVars = new HashSet<>(beforeThan);
                break;
            case Else:
                WhileInitializedVars = new HashSet<>(beforeElse);
                break;
            case While:
                break;
        }

        // while part, with while scope
        scope = Scope.While;
        whileStatement.body().accept(this);

        // Inside a while, the initializations don't count for uses after the loop, because we can't be sure that the loop executes at all.
        // back to before scope + sets state
        InitializedVars = new HashSet<>(beforeInitialized);
        ThenInitializedVars = new HashSet<>(beforeThan);
        ElseInitializedVars = new HashSet<>(beforeElse);
        WhileInitializedVars = new HashSet<>(beforeWhile);
        scope = before;
    }


    ///////////////////// if-else /////////////////////

    @Override
    public void visit(IfStatement ifStatement) {

        // visit cond, previous scope setting
        ifStatement.cond().accept(this);

        // save before scope + sets state
        Scope before = scope;
        Set<String> beforeInitialized = new HashSet<>(InitializedVars);
        Set<String> beforeThan = new HashSet<>(ThenInitializedVars);
        Set<String> beforeElse = new HashSet<>(ElseInitializedVars);
        Set<String> beforeWhile = new HashSet<>(WhileInitializedVars);

        // add initialized vars from previous scope to current then-else scope
        switch(before){
            case Method:
                ThenInitializedVars = new HashSet<>(beforeInitialized);
                ElseInitializedVars = new HashSet<>(beforeInitialized);
                break;
            case Than:
                ThenInitializedVars = new HashSet<>(beforeThan);
                ElseInitializedVars = new HashSet<>(beforeThan);
                break;
            case Else:
                ThenInitializedVars = new HashSet<>(beforeElse);
                ElseInitializedVars = new HashSet<>(beforeElse);
                break;
            case While:
                ThenInitializedVars = new HashSet<>(beforeWhile);
                ElseInitializedVars = new HashSet<>(beforeWhile);
                break;
        }

        // then part, with then scope
        scope = Scope.Than;
        ifStatement.thencase().accept(this);

        // get intersection of these sets - first then set
        Set<String> intersectSet = new HashSet<>(ThenInitializedVars);

        // else part, with else scope
        scope = Scope.Else;
        ifStatement.elsecase().accept(this);

        // get intersection of these sets - intersect with else set
        intersectSet.retainAll(ElseInitializedVars);

        // back to before scope + sets state
        InitializedVars = new HashSet<>(beforeInitialized);
        ThenInitializedVars = new HashSet<>(beforeThan);
        ElseInitializedVars = new HashSet<>(beforeElse);
        WhileInitializedVars = new HashSet<>(beforeWhile);
        scope = before;

        // add intersection to before scope set
        switch(before){
            case Method:
                InitializedVars.addAll(intersectSet);
                break;
            case Than:
                ThenInitializedVars.addAll(intersectSet);
                break;
            case Else:
                ElseInitializedVars.addAll(intersectSet);
                break;
            case While:
                WhileInitializedVars.addAll(intersectSet);
                break;
        }

    }

}
