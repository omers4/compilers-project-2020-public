package ast;

import java.util.*;

public class SymbolTableVisitor<IAstToSymbolTable> implements IVisitorWithField<IAstToSymbolTable> {

    private Stack<SymbolTable> _symbolTableHierarchy;
    private Map<String,SymbolTable> _classesSymbolTable;
    private AstToSymbolTable _astToSymbolTable;
    private SymbolType _type = SymbolType.Method_Var;
    private Boolean _isValid = true;

    public SymbolTableVisitor() {
        _classesSymbolTable = new HashMap<>();
        _astToSymbolTable = new AstToSymbolTable();
        _symbolTableHierarchy = new Stack<>();
        _symbolTableHierarchy.push(null);
    }

    private ObjectVTable createVTable(ClassDecl classDecl) {
        ObjectVTable vTable = new ObjectVTable(classDecl.name(), classDecl.superName());

        // If parent exists we want it's fields reside first in VTable
        // Order is important! We first want to add the parent fields
        if (classDecl.superName() != null) {
            SymbolTable parentSymbolTable = _classesSymbolTable.get(classDecl.superName());
            SymbolTableItem parentClassItem = parentSymbolTable.get(new SymbolItemKey(classDecl.superName(), SymbolType.Class));
            for (var entry : parentClassItem.getVTable().getFields().entrySet()) {
                vTable.addField(entry.getKey(),entry.getValue());
            }

            for (var entry : parentClassItem.getVTable().getMethods().entrySet()) {
                vTable.addOrUpdateMethod(entry.getKey(), entry.getValue());
            }
        }

        for (var fieldDecl : classDecl.fields()) {
            vTable.addField(fieldDecl.name(), fieldDecl.type());
        }
        for (var methodDecl : classDecl.methoddecls()) {
            vTable.addOrUpdateMethod(methodDecl.name(), classDecl.name(), methodDecl);
        }

        return vTable;
    }

    private MethodSignature createMethodSignature(MethodDecl methodDecl) {

        return new MethodSignature(methodDecl.name(), methodDecl.returnType(), methodDecl.formals());
    }

    private void visitBinaryExpr(BinaryExpr e, String infixSymbol) {
        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
        e.e1().accept(this);
        e.e2().accept(this);
    }

    @Override
    public void visit(Program program) {
        // Order matters. We first connect the Ast to a new SymbolTable and then we add it to the mapping using peek
        _symbolTableHierarchy.push((new SymbolTable(null)));
        _astToSymbolTable.addMapping(program, _symbolTableHierarchy.peek());

        // TODO: run over classes, add to private filed that contains VTable to Class ID
        // TODO: When encounter object, add to symboltableItem it's VTAble

        program.mainClass().accept(this);

        for (ClassDecl classdecl : program.classDecls()) {
            classdecl.accept(this);
        }
    }

    @Override
    public void visit(ClassDecl classDecl) {

        SymbolTable parentSymbolTable = null;

        if (classDecl.superName() != null) {
            parentSymbolTable = _classesSymbolTable.get(classDecl.superName());
        }

        // Order matters. We first connect the Ast to a new SymbolTable and then we add it to the mapping using peek
        _symbolTableHierarchy.push((new SymbolTable(parentSymbolTable)));
        _astToSymbolTable.addMapping(classDecl, _symbolTableHierarchy.peek());

        ObjectVTable vTable = createVTable(classDecl);
        SymbolTable curContextSymbolTable = _symbolTableHierarchy.peek();
        SymbolItemKey key = new SymbolItemKey(classDecl.name(), SymbolType.Class);
        SymbolTableItem val  = new SymbolTableItem(classDecl.name(), vTable, classDecl);
        curContextSymbolTable.addSymbol(key, val);

        _type = SymbolType.Field;
        for (var fieldDecl : classDecl.fields()) {
            fieldDecl.accept(this);
        }

        _type = SymbolType.Method_Var;
        for (var methodDecl : classDecl.methoddecls()) {
            methodDecl.accept(this);
        }

        _classesSymbolTable.put(classDecl.name(), _symbolTableHierarchy.peek());
        _symbolTableHierarchy.pop();
    }

    @Override
    public void visit(MainClass mainClass) {
        mainClass.mainStatement().accept(this);
    }

    @Override
    public void visit(MethodDecl methodDecl) {
        _symbolTableHierarchy.push(new SymbolTable(_symbolTableHierarchy.peek()));
        _astToSymbolTable.addMapping(methodDecl, _symbolTableHierarchy.peek());

        MethodSignature methodSignature = createMethodSignature(methodDecl);
        SymbolTable curContextSymbolTable = _symbolTableHierarchy.peek();
        SymbolItemKey key = new SymbolItemKey(methodDecl.name(), SymbolType.Method);
        curContextSymbolTable.addSymbol(key, new SymbolTableItem(methodDecl.name(), methodSignature));

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

        _symbolTableHierarchy.pop();
    }

    @Override
    public void visit(FormalArg formalArg) {
        SymbolTable curContextSymbolTable = _symbolTableHierarchy.peek();
        _astToSymbolTable.addMapping(formalArg, curContextSymbolTable);
        SymbolItemKey key = new SymbolItemKey(formalArg.name(), SymbolType.Var);
        curContextSymbolTable.addSymbol(key, new SymbolTableItem(formalArg.name(), formalArg.type(), SymbolType.Method_Var));
        formalArg.type().accept(this);
    }

    @Override
    public void visit(VarDecl varDecl) {
        SymbolTable curContextSymbolTable = _symbolTableHierarchy.peek();
        _astToSymbolTable.addMapping(varDecl, _symbolTableHierarchy.peek());

        // Add the current variable to the current Symbol Table representing its scope
        SymbolItemKey key = new SymbolItemKey(varDecl.name(), SymbolType.Var);
        curContextSymbolTable.addSymbol(key, new SymbolTableItem(varDecl.name(), varDecl.type(), _type));
        varDecl.type().accept(this);
    }

    @Override
    public void visit(BlockStatement blockStatement) {
        _astToSymbolTable.addMapping(blockStatement, _symbolTableHierarchy.peek());
        for (var s : blockStatement.statements()) {
            s.accept(this);
        }
    }

    @Override
    public void visit(IfStatement ifStatement) {
        _astToSymbolTable.addMapping(ifStatement, _symbolTableHierarchy.peek());
        ifStatement.cond().accept(this);
        ifStatement.thencase().accept(this);
        ifStatement.elsecase().accept(this);
    }

    @Override
    public void visit(WhileStatement whileStatement) {
        _astToSymbolTable.addMapping(whileStatement, _symbolTableHierarchy.peek());
        whileStatement.cond().accept(this);
        whileStatement.body().accept(this);
    }

    @Override
    public void visit(SysoutStatement sysoutStatement) {
        _astToSymbolTable.addMapping(sysoutStatement, _symbolTableHierarchy.peek());
        sysoutStatement.arg().accept(this);
    }

    @Override
    public void visit(AssignStatement assignStatement) {
        _astToSymbolTable.addMapping(assignStatement, _symbolTableHierarchy.peek());
        assignStatement.rv().accept(this);
    }

    @Override
    public void visit(AssignArrayStatement assignArrayStatement) {
        _astToSymbolTable.addMapping(assignArrayStatement, _symbolTableHierarchy.peek());
        assignArrayStatement.index().accept(this);
        assignArrayStatement.rv().accept(this);
    }

    @Override
    public void visit(AndExpr e) {
        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
        visitBinaryExpr(e, "&&");
    }

    @Override
    public void visit(LtExpr e) {
        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
        visitBinaryExpr(e, "<");
    }

    @Override
    public void visit(AddExpr e) {
        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
        visitBinaryExpr(e, "+");
    }

    @Override
    public void visit(SubtractExpr e) {
        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
        visitBinaryExpr(e, "-");
    }

    @Override
    public void visit(MultExpr e) {
        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
        visitBinaryExpr(e, "*");
    }

    @Override
    public void visit(ArrayAccessExpr e) {
        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
        e.arrayExpr().accept(this);
        e.indexExpr().accept(this);
    }

    @Override
    public void visit(ArrayLengthExpr e) {
        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
        e.arrayExpr().accept(this);
    }

    @Override
    public void visit(MethodCallExpr e) {
        if (e.ownerExpr() == null) {
            _isValid = false;
            return;
        }
        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
        e.ownerExpr().accept(this);

        for (Expr arg : e.actuals()) {
            arg.accept(this);
        }
    }

    @Override
    public void visit(IntegerLiteralExpr e) {
        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
    }

    @Override
    public void visit(TrueExpr e) {
        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
    }

    @Override
    public void visit(FalseExpr e) {
        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
    }

    @Override
    public void visit(IdentifierExpr e) {
        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
    }

    public void visit(ThisExpr e) {
        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
    }

    @Override
    public void visit(NewIntArrayExpr e) {
        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
        e.lengthExpr().accept(this);
    }

    @Override
    public void visit(NewObjectExpr e) {

        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
    }

    @Override
    public void visit(NotExpr e) {
        _astToSymbolTable.addMapping(e, _symbolTableHierarchy.peek());
        e.e().accept(this);
    }

    @Override
    public void visit(IntAstType t) {
        _astToSymbolTable.addMapping(t, _symbolTableHierarchy.peek());
    }

    @Override
    public void visit(BoolAstType t) {
        _astToSymbolTable.addMapping(t, _symbolTableHierarchy.peek());
    }

    @Override
    public void visit(IntArrayAstType t) {
        _astToSymbolTable.addMapping(t, _symbolTableHierarchy.peek());
    }

    @Override
    public void visit(RefType t) {
        _astToSymbolTable.addMapping(t, _symbolTableHierarchy.peek());
    }

    @Override
    public IAstToSymbolTable getField() {
        if (!_isValid)
            return null;
        return (IAstToSymbolTable) _astToSymbolTable;
    }
}